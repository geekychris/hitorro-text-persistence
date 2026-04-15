/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.obj.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import gnu.trove.set.hash.TLongHashSet;
import com.hitorro.analysis.brat.OpenNLPBratTrainer;
import com.hitorro.basetext.indexer.IndexerUtil;
import com.hitorro.basetext.inverter.InverterUtils;
import com.hitorro.basetext.inverter.TermTupleSetGroup;
import com.hitorro.language.Iso639Table;
import com.hitorro.obj.core.objectstore.ObjectStoreService;
import com.hitorro.obj.core.objectstore.ObjectStoreShard;
import com.hitorro.obj.core.solr.ExternalFeatureField;
import com.hitorro.obj.core.solr.JVS2JVSEnrichMapper;
import com.hitorro.obj.core.solr.JVS2SolrMapper;
import com.hitorro.obj.core.solr.SolrDocumentSink;
import com.hitorro.obj.core.wikimedia.WikiXML2JVSMapper;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.FileInfo;
import com.hitorro.util.basefile.fs.FileInfoMapper;
import com.hitorro.util.cmdline.CommandLine;
import com.hitorro.util.commandandcontrol.RedirectHttp;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.ano.ArgType;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.DebugArgAno;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.hash.FPHash64;
import com.hitorro.util.core.http.HTTPAsyncUtil;
import com.hitorro.util.core.iterator.AbstractIterator;
import com.hitorro.util.core.iterator.JSONIterator;
import com.hitorro.util.core.iterator.queue.Dequeue;
import com.hitorro.util.core.iterator.queue.ParallelDequeue;
import com.hitorro.util.core.iterator.sinks.JsonSink;
import com.hitorro.util.core.iterator.sinks.MaxItemsPerTransactionSink;
import com.hitorro.util.core.iterator.sinks.Sink;
import com.hitorro.util.core.queue.OnlyOnceEnqueue;
import com.hitorro.util.core.queue.ThreadedQueue;
import com.hitorro.util.html.HTMLPage;
import com.hitorro.util.html.Link;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.ResetableStringReader;
import com.hitorro.util.io.csv.CSV2JsonNodeIterator;
import com.hitorro.util.io.csv.CSVIteratorImpl;
import com.hitorro.util.io.largedata.compressedstreams.OutputOutputStream;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.CollectionProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.json.keys.propaccess.Propaccess;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import com.hitorro.util.xml.StaxXMLBaseChainingIterator;
import com.hitorro.util.xml.XE;
import org.apache.solr.schema.FieldType;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@ServiceDefinition(
        shortName = "basetext",
        description = "basetext service",
        debugCommands = {},
        typeManagedClasses = {},
        uiDirectories = {})
public class BasetextService {
    public static StringProperty langProp = new StringProperty("lang", "", "en");
    public static BooleanProperty asArrayProp = new BooleanProperty("array", "", true);
    public static StringProperty analyzerProp = new StringProperty("", "LOWER,PHONETIC", "en");
    public static StringProperty textProp = new StringProperty("text", "", "This is a test");
    /*


    -javaagent:/Users/chris/.m2/repository/org/aspectj/aspectjweaver/1.9.1/aspectjweaver-1.9.1.jar

                        1.8.13
     */
    public static CollectionProperty<String> analyzersProp = new CollectionProperty("analyzers", "", null, analyzerProp);
    private static com.hitorro.obj.core.BasetextService service;
    @CommandDef(command = "crawl.crawl", description = "host ip address", isInternal = false)
    public static boolean crawl() throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/crawl"));
        if (!bf.exists()) {
            bf.mkdir();
        }
        String seed = "https://wiki.amiga.org/index.php/Main_Page";
        ThreadedQueue<String> queue = new ThreadedQueue<>(100);
        OnlyOnceEnqueue<String> enq= new OnlyOnceEnqueue<String>(queue) {
            private TLongHashSet map = new TLongHashSet();
            @Override
            protected boolean exists(String s) {
                long hash = FPHash64.getFP(s);
                return map.contains(hash);
            }

            @Override
            protected void remember(String s) {
                map.add(FPHash64.getFP(s));
            }
        };
        enq.add(seed);
        ParallelDequeue<String, String, Object> dq = new ParallelDequeue (new Dequeue(queue), null,
                new Function<String, String>() {

                    @Override
                    public String apply(String s) {
                        HTMLPage page = InverterUtils.fetchHTMLPage(s, 2000);
                        try {
                            TermTupleSetGroup ttsg = InverterUtils.getMergedTupleSetFromPage(page.getParser());
                            System.out.println();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (page != null) {
                            try {
                                List<Link> links = page.getLinks();
                                for (Link link : links) {
                                    String url = link.getUrl();
                                    System.out.println(url);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return null;
                    }
                }, "crawler", 1);
        dq.startThreads();
        Thread.sleep(100000);
        return true;
    }

    @CommandDef(command = "object.testwrite", description = "x")
    public static RedirectHttp x(@DebugArgAno(keyName = "",
            description = "",
            defaultValue = "",
            argType = ArgType.Raw) String raw,
                                 @DebugArgAno(keyName = "",
                                         description = "",
                                         defaultValue = "",
                                         argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args
            ,
                                 @DebugArgAno(keyName = "",
                                         description = "",
                                         defaultValue = "",
                                         argType = ArgType.Uri) String uri,
                                 @DebugArgAno(keyName = "",
                                         description = "",
                                         defaultValue = "",
                                         argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/enwiki-20240201-pages-articles-multistream.xml.bz2"));
        if (!bf.exists()) {
            return null;
        }
        AbstractIterator<XE> iter = new StaxXMLBaseChainingIterator(bf.getDataInputStream(), "page");
        Class c = Class.forName(ExternalFeatureField.class.getCanonicalName(), true, CommandLine.class.getClassLoader());
        Class c1 = c.asSubclass(FieldType.class);
        com.hitorro.jsontypesystem.Type t = com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("core_sysobject");
        WikiXML2JVSMapper mapper = new WikiXML2JVSMapper("en", "wiki", t);
        //AbstractIterator<JVS> jvsIter = iter.map(me).skipNTakeM(5, 60, false);
        AtomicLong counter = new AtomicLong();
        AbstractIterator<com.hitorro.jsontypesystem.JVS> jvsIter = iter.mapParallel(mapper).count(counter, 10000);
        ObjectStoreShard oss = ObjectStoreService.oss.getShard("wiki");
        jvsIter.sink(oss.maxPerTransaction(20000));
        oss.close();
        return null;
    }

    @CommandDef(command = "object.testwriteen", description = "x")
    public static RedirectHttp dbEnriched(@DebugArgAno(keyName = "",
            description = "",
            defaultValue = "",
            argType = ArgType.Raw) String raw,
                                          @DebugArgAno(keyName = "",
                                                  description = "",
                                                  defaultValue = "",
                                                  argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args
            ,
                                          @DebugArgAno(keyName = "",
                                                  description = "",
                                                  defaultValue = "",
                                                  argType = ArgType.Uri) String uri,
                                          @DebugArgAno(keyName = "",
                                                  description = "",
                                                  defaultValue = "",
                                                  argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/enwiki-20160501-pages-articles-multistream.xml.bz2"));
        if (!bf.exists()) {
            return null;
        }
        AbstractIterator<XE> iter = new StaxXMLBaseChainingIterator(bf.getDataInputStream(), "page");
        Class c = Class.forName(ExternalFeatureField.class.getCanonicalName(), true, CommandLine.class.getClassLoader());
        Class c1 = c.asSubclass(FieldType.class);
        com.hitorro.jsontypesystem.Type t = com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("core_sysobject");
        WikiXML2JVSMapper mapper = new WikiXML2JVSMapper("en", "wikipedia", t);
        //AbstractIterator<JVS> jvsIter = iter.map(me).skipNTakeM(5, 60, false);
        AtomicLong counter = new AtomicLong();
        AbstractIterator<com.hitorro.jsontypesystem.JVS> jvsIter = iter.mapParallel(mapper).count(counter, 10000);
        ObjectStoreShard oss = ObjectStoreService.oss.getShard("wikipedia");
        jvsIter.sink(oss.maxPerTransaction(20000));
        return null;
    }

    @CommandDef(command = "ind2", description = "x")
    public static RedirectHttp ind2(@DebugArgAno(keyName = "",
            description = "",
            defaultValue = "",
            argType = ArgType.Raw) String raw,
                                    @DebugArgAno(keyName = "",
                                            description = "",
                                            defaultValue = "",
                                            argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args
            ,
                                    @DebugArgAno(keyName = "",
                                            description = "",
                                            defaultValue = "",
                                            argType = ArgType.Uri) String uri,
                                    @DebugArgAno(keyName = "",
                                            description = "",
                                            defaultValue = "",
                                            argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/enwiki-20160501-pages-articles-multistream.xml.bz2"));
        if (!bf.exists()) {

            return null;
        }
        AbstractIterator<XE> iter = new StaxXMLBaseChainingIterator(bf.getDataInputStream(), "page");
        Class c = Class.forName(ExternalFeatureField.class.getCanonicalName(), true, CommandLine.class.getClassLoader());
        Class c1 = c.asSubclass(FieldType.class);
        com.hitorro.jsontypesystem.Type t = com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("core_sysobject");
        WikiXML2JVSMapper mapper = new WikiXML2JVSMapper("en", "wiki", t);
        //AbstractIterator<JVS> jvsIter = iter.map(me).skipNTakeM(5, 60, false);
        AtomicLong counter = new AtomicLong();
        JVS2JVSEnrichMapper enrich = new JVS2JVSEnrichMapper("ner", "parsed", "pos", "hash");
        AbstractIterator<com.hitorro.jsontypesystem.JVS> jvsIter = iter.skipNTakeM(1196, 1000, true).map(mapper.combine(enrich)).count(counter, 10000);

        SolrDocumentSink sds = new SolrDocumentSink("wiki", true);
        JVS2SolrMapper jvs2solrMapper = new JVS2SolrMapper();
        AbstractIterator<com.hitorro.jsontypesystem.JVS> indexiter = jvsIter.mapParallel(jvs2solrMapper);
        indexiter.sink(sds);
        sds.close();
        sds.commitIndex();
        return null;
    }

    @CommandDef(command = "object.testread", description = "x")
    public static RedirectHttp testread(@DebugArgAno(keyName = "",
            description = "",
            defaultValue = "",
            argType = ArgType.Raw) String raw,
                                        @DebugArgAno(keyName = "",
                                                description = "",
                                                defaultValue = "",
                                                argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args
            ,
                                        @DebugArgAno(keyName = "",
                                                description = "",
                                                defaultValue = "",
                                                argType = ArgType.Uri) String uri,
                                        @DebugArgAno(keyName = "",
                                                description = "",
                                                defaultValue = "",
                                                argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/enwiki-20160501-pages-articles-multistream.xml.bz2"));
        if (!bf.exists()) {
            return null;
        }
        AbstractIterator<XE> iter = new StaxXMLBaseChainingIterator(bf.getDataInputStream(), "page");
        Class c = Class.forName(ExternalFeatureField.class.getCanonicalName(), true, CommandLine.class.getClassLoader());
        Class c1 = c.asSubclass(FieldType.class);
        com.hitorro.jsontypesystem.Type t = com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("core_sysobject");
        WikiXML2JVSMapper mapper = new WikiXML2JVSMapper("en", "mydomain", t);
        AbstractIterator<com.hitorro.jsontypesystem.JVS> jvsIter = iter.map(mapper).skipNTakeM(5, 60, false);
        ObjectStoreShard oss = ObjectStoreService.oss.getShard("hitorro");
        File f = new File(Env.getData(), "testout.bin");
        OutputOutputStream oos = new OutputOutputStream(FileUtil.getDataOutputStreamForFile(f));
        oss.getValuesAsByteStream(jvsIter, oos);

        return null;
    }

    @CommandDef(command = "object.expandwrite", description = "x")
    public static RedirectHttp expandWrite(@DebugArgAno(keyName = "",
            description = "write the wikipedia file out as JVS enriched",
            defaultValue = "",
            argType = ArgType.Raw) String raw,
                                           @DebugArgAno(keyName = "",
                                                   description = "",
                                                   defaultValue = "",
                                                   argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args
            ,
                                           @DebugArgAno(keyName = "",
                                                   description = "",
                                                   defaultValue = "",
                                                   argType = ArgType.Uri) String uri,
                                           @DebugArgAno(keyName = "",
                                                   description = "",
                                                   defaultValue = "",
                                                   argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/enwiki-20160501-pages-articles-multistream.xml.bz2"));
        BaseFile outBf = Env.getBaseFile(new File("/Users/chris/wikienrichxx.json.bz2"));
        JsonSink js = new JsonSink(outBf);
        if (!bf.exists()) {

            return null;
        }
        //String enrichThis[] = {"ner", "parsed", "pos", "hash"};
        String enrichThis[] = {"hash"};
        AtomicLong counter = new AtomicLong();
        com.hitorro.jsontypesystem.Type t = com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("core_sysobject");
        new StaxXMLBaseChainingIterator(bf.getDataInputStream(), "page").
                mapParallel(new WikiXML2JVSMapper("en", "wiki", t).combine(
                        new JVS2JVSEnrichMapper(enrichThis))).
                count(counter, 1000).
                skipNTakeM(1000, 100000, false).
                map(com.hitorro.jsontypesystem.JVS2JsonMapper.me).sink(js);

        js.close();
        return null;
    }

    @CommandDef(command = "object.expandedwritetoshard", description = "x")
    public static RedirectHttp expandedWriteToShard(@DebugArgAno(argType = ArgType.Raw) String raw,
                                                    @DebugArgAno(argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args,
                                                    @DebugArgAno(argType = ArgType.Uri) String uri,
                                                    @DebugArgAno(argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/wikienrich.json.bz2"));
        String shardName = "wiki";
        AtomicLong counter = new AtomicLong();
        ObjectStoreShard oss = ObjectStoreService.oss.getShard(shardName);
        new JSONIterator(bf.getReader()).map(com.hitorro.jsontypesystem.Json2JVSMapper.me).
                count(counter, 100000).
                mapParallel(oss.getCompressionMapper(), 100, 1000).
                //map(oss.getCompressionMapper()).
                //skipNTakeM(1000, 100000, false).
                        sink(oss.getCompressedSink().maxPerTransaction(10000));
        return null;
    }

    @CommandDef(command = "ind", description = "x")
    public static RedirectHttp ind(@DebugArgAno(argType = ArgType.Raw) String raw,
                                   @DebugArgAno(argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args,
                                   @DebugArgAno(argType = ArgType.Uri) String uri,
                                   @DebugArgAno(argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/wikienrich.json.bz2"));
        String shardName = "wiki";
        AtomicLong counter = new AtomicLong();

        SolrDocumentSink sds = new SolrDocumentSink(shardName, true);
        MaxItemsPerTransactionSink maxSink = new MaxItemsPerTransactionSink(sds, 8000);
        JVS2SolrMapper jvs2solrMapper = new JVS2SolrMapper();
        new JSONIterator(bf.getReader())
                .map(com.hitorro.jsontypesystem.Json2JVSMapper.me).
                count(counter, 100000).
                //skipNTakeM(10000, 100000, false).
                        map(jvs2solrMapper).
                sink(maxSink);
        maxSink.close();
        return null;
    }

    @CommandDef(command = "csv.qa", description = "x")
    public static RedirectHttp readCSV(@DebugArgAno(argType = ArgType.Raw) String raw,
                                       @DebugArgAno(argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args,
                                       @DebugArgAno(argType = ArgType.Uri) String uri,
                                       @DebugArgAno(argType = ArgType.Request) HttpServletRequest req
    ) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/sap_saphire.csv"));
        BaseFile outF = Env.getBaseFile(new File("/Users/chris/out.json"));
        // BaseFile bf = Env.getBaseFile(new File("/Users/chris/foo.csv"));
        String shardName = "qanda";
        if (bf.exists()) {
            CSVIteratorImpl iter = new CSVIteratorImpl(bf, "UTF-8");
            CSV2JsonNodeIterator jsonIter = new CSV2JsonNodeIterator(iter);
            AbstractIterator<com.hitorro.jsontypesystem.JVS> jvsIter = jsonIter.map(new com.hitorro.obj.core.QAJVSMapper());

            JVS2JVSEnrichMapper enrich = new JVS2JVSEnrichMapper("ner", "parsed", "pos", "hash");


            Sink<com.hitorro.jsontypesystem.JVS> tee = new SolrDocumentSink(shardName, true).
                    map(new JVS2SolrMapper("basic", "advanced")).
                    tee(ObjectStoreService.oss.getShard(shardName));

            jvsIter.
                    map(enrich).
                    sink(tee);

            //JsonSink sink = new JsonSink(outF);
            //
            //jvsIter.map(enrich).map(jvs2solrMapper).map(JVS2JsonMapper.me).sink(sink);
        }
        return null;
    }

    public void init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        service = this;

    }

    @CommandDef(command = "enrich", description = "enrich", restOperations = {RestOperations.Get, RestOperations.Post})
    public boolean enrich(
            @DebugArgAno(keyName = "in",
                    argType = ArgType.JVSRequestIterator) AbstractIterator<com.hitorro.jsontypesystem.JVS> iter,
            @DebugArgAno(keyName = "out",
                    argType = ArgType.JVSResponseSink) Sink<com.hitorro.jsontypesystem.JVS> sink) throws Exception {
        JVS2JVSEnrichMapper mapper = new JVS2JVSEnrichMapper();
        iter.map(mapper).sink(sink);
        return true;
    }

    @CommandDef(command = "fi", description = "enrich", restOperations = {RestOperations.Get, RestOperations.Post})
    public boolean fi(
            @DebugArgAno(keyName = "in",
                    argType = ArgType.JVSRequestIterator) AbstractIterator<com.hitorro.jsontypesystem.JVS> iter,
            @DebugArgAno(keyName = "out",
                    argType = ArgType.JVSResponseSink) Sink<com.hitorro.jsontypesystem.JVS> sink) {

        BaseFile f = Env.getBaseFile(new File("/Users/chris/pics"));
        try {
            AbstractIterator<FileInfo> t = f.list().map(FileInfoMapper.me);
            t.map(fi -> new com.hitorro.jsontypesystem.JVS(fi.getAsJsonNode())).sink(sink);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @CommandDef(command = "async", description = "enrich", restOperations = {RestOperations.Get, RestOperations.Post})
    public boolean async(
            @DebugArgAno(keyName = "in",
                    argType = ArgType.JVSRequestIterator) AbstractIterator<com.hitorro.jsontypesystem.JVS> iter,
            @DebugArgAno(keyName = "out",
                    argType = ArgType.JVSResponseSink) Sink<com.hitorro.jsontypesystem.JVS> sink) throws Exception {
        HTTPAsyncUtil.foo();
        return true;
    }

    @CommandDef(command = "blaa", description = "enrich", restOperations = {RestOperations.Get, RestOperations.Post})
    public boolean blaa(
            @DebugArgAno(keyName = "in",
                    argType = ArgType.JVSRequestIterator) AbstractIterator<com.hitorro.jsontypesystem.JVS> iter,
            @DebugArgAno(keyName = "out",
                    argType = ArgType.JVSResponseSink) Sink<com.hitorro.jsontypesystem.JVS> sink) throws Exception {
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/1811-results.json"));
        JsonNode node = bf.readJsonElement();
        for (JsonNode n : node) {
            JsonNode t = n.get("plainText");
            if (t != null) {
                Console.println(t.asText());
            }
        }
        return true;
    }

    @CommandDef(command = "foo3", description = "", restOperations = {RestOperations.Get, RestOperations.Post})
    public boolean foo3(
            @DebugArgAno(keyName = "in",
                    argType = ArgType.JVSRequestIterator) AbstractIterator<com.hitorro.jsontypesystem.JVS> iter,
            @DebugArgAno(keyName = "out",
                    argType = ArgType.JVSResponseSink) Sink<com.hitorro.jsontypesystem.JVS> sink) throws Exception {
        OpenNLPBratTrainer.tst(null);
        return true;
    }

    @CommandDef(command = "foo4", description = "", restOperations = {RestOperations.Get, RestOperations.Post})
    public boolean foo4(
            @DebugArgAno(keyName = "in",
                    argType = ArgType.JVSRequestIterator) AbstractIterator<com.hitorro.jsontypesystem.JVS> iter,
            @DebugArgAno(keyName = "out",
                    argType = ArgType.JVSResponseSink) Sink<com.hitorro.jsontypesystem.JVS> sink) throws Exception {
        String args[] = {"--json", "select * from processes"};
        com.hitorro.obj.core.SimpleExec2 se = new com.hitorro.obj.core.SimpleExec2("/usr/local/bin/osqueryi", args);
        JSONIterator jiter = new JSONIterator(new InputStreamReader(se.getInputSteam()));
        while (jiter.hasNext()) {
            JsonNode node = jiter.next();
            int i = 1;
        }
        int err = se.getError();

        /*StringBuilder sb = new StringBuilder();

        String args[] = {"--json", "select * from processes"};
        int sex =  SimpleExec.exec("/usr/local/bin/osqueryi", sb, true, args);
        */
        return true;
    }

    @CommandDef(command = "analyze", description = "analyze tokens", restOperations = {RestOperations.Get, RestOperations.Post})
    public boolean analyze(
            @DebugArgAno(keyName = "in",
                    argType = ArgType.JVSRequestIterator) AbstractIterator<com.hitorro.jsontypesystem.JVS> iter,
            @DebugArgAno(keyName = "out",
                    argType = ArgType.JVSResponseSink) Sink<com.hitorro.jsontypesystem.JVS> sink) throws Exception {
        com.hitorro.jsontypesystem.JVS query = iter.getFirstItem();
        String lang = langProp.apply(query.getJsonNode());
        Collection<String> analyzers = analyzersProp.apply(query.getJsonNode());
        String value = textProp.apply(query.getJsonNode());
        boolean asArray = asArrayProp.apply(query.getJsonNode());


        ArrayNode an = JsonNodeFactory.instance.arrayNode();
        for (String analyzer : analyzers) {
            GenericAnalyzer ga = new GenericAnalyzer(analyzer, Iso639Table.getInstance().getRow(lang), GenericAnalyzer.Mode.Query);
            ResetableStringReader rsr = new ResetableStringReader(value);
            JsonNode jn = IndexerUtil.getTokensAsJson(ga, rsr, asArray);
            an.add(jn);
        }

        query.set("result", an);

        sink.start();
        sink.add(query);
        sink.stop();
        sink.close();
        return true;
    }
    //MaxItemsPerTransactionSink"
}

class QAJVSMapper implements Function<JsonNode, com.hitorro.jsontypesystem.JVS> {
    public static StringProperty qKey = new StringProperty("Question", "", null);
    public static StringProperty aKey = new StringProperty("Answer", "", null);
    public static Propaccess questionKey = new Propaccess("question.mls");
    public static Propaccess answerKey = new Propaccess("answer.mls");
    private AtomicLong counter = new AtomicLong();

    @Override
    public com.hitorro.jsontypesystem.JVS apply(final JsonNode jsonNode) {
        com.hitorro.jsontypesystem.JVS r = new com.hitorro.jsontypesystem.JVS();
        r.setType(com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("qanda"));
        String q = qKey.apply(jsonNode);
        String a = aKey.apply(jsonNode);
        r.setId("qanda", counter.toString());
        counter.incrementAndGet();
        r.setDates(new Date(), new Date());
        r.addLangTextTemporaryReLook
                (questionKey, q, "en");
        r.addLangTextTemporaryReLook
                (answerKey, a, "en");
        return r;
    }
}

/*

https://blog.espenberntsen.net/2010/03/20/aspectj-cheat-sheet/
 */


class SimpleExec2 extends InputStream {
    private String cmd;
    private Process process;
    private InputStream is;
    private String errMsg;

    public SimpleExec2(String cmd, String args[]) throws IOException {
        this.cmd = cmd;
        process = Runtime.getRuntime().exec(cmd, args, Env.getHome());
        is = process.getInputStream();
    }

    public InputStream getInputSteam() {
        return is;
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    public int getError() {
        return process.exitValue();
    }
}

