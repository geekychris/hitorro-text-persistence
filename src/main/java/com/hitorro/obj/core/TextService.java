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
import com.hitorro.basetext.commands.FetchPageCommand;
import com.hitorro.basetext.dfindex.DumpDF;
import com.hitorro.basetext.inverter.TermTuple;
import com.hitorro.basetext.inverter.TermTupleSet;
import com.hitorro.basetext.maxentclassifier.AnswerTypeClassifier;
import com.hitorro.basetext.phrase.PhraseElement;
import com.hitorro.basetext.text.dfindex.DFIndexCreatorEvent;
import com.hitorro.conceptnet5.ConceptNetDBBuilder;
import com.hitorro.jsontypesystem.executors.ExecutionBuilder;
import com.hitorro.jsontypesystem.executors.IndexerFactory;
import com.hitorro.jsontypesystem.executors.ProjectionContext;
import com.hitorro.jsontypesystem.grouppredicates.GroupNameFilter;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.language.Iso639Table;
import com.hitorro.language.LanguageId;
import com.hitorro.obj.core.solr.ExternalFeatureField;
import com.hitorro.obj.core.solr.JVS2SolrMapper;
import com.hitorro.obj.core.solr.SolrDocumentSink;
import com.hitorro.obj.core.wikimedia.WikiXML2JVSMapper;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.dfs.DFSFileSystem;
import com.hitorro.util.cmdline.CommandLine;
import com.hitorro.util.commandandcontrol.RedirectHttp;
import com.hitorro.util.commandandcontrol.ano.ArgType;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.DebugArgAno;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.events.LocalEventHub;
import com.hitorro.util.core.iterator.AbstractIterator;
import com.hitorro.util.core.iterator.sinks.JsonSink;
import com.hitorro.util.html.Link;
import com.hitorro.util.html.LinkSet;
import com.hitorro.util.json.keys.propaccess.Propaccess;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.redis.RedisTaskQueueBase;
import com.hitorro.util.redis.SynchronousRedisTaskQueue;
import com.hitorro.util.startupframework.Dependency;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import com.hitorro.util.typesystem.HTAnalyzerFactory;
import com.hitorro.util.xml.StaxXMLBaseChainingIterator;
import com.hitorro.util.xml.XE;
import io.lettuce.core.RedisClient;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.solr.schema.FieldType;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Predicate;

/**
 * <p/>
 * Textual analysis services.
 */
@ServiceDefinition(
        shortName = "text",
        description = "Text analysis service",
        debugCommands = {InvertUrlContent.class, DumpDF.class, FetchPageCommand.class},
        typeManagedClasses = {TermTuple.class, TermTupleSet.class, Link.class, LinkSet.class, PhraseElement.class},
        uiDirectories = {})
public class TextService {
    static RedisClient redisClient = null;
    static RedisTaskQueueBase rtqb;
    @Dependency(clazz = SolrService.class)
    SolrService solrService;
    @Dependency(clazz = com.hitorro.obj.core.BasetextService.class)
	BasetextService basetext;
    private DFIndexCreatorEvent m_indexCreatorEvent = new DFIndexCreatorEvent();

    @CommandDef(command = "w", description = "w")
    public static RedirectHttp w(@DebugArgAno(keyName = "",
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
    ) throws IOException {

        com.hitorro.jsontypesystem.JVS props = JVSProperties.getProperties();
        BaseFile bf = DFSFileSystem.getBaseFileFromPath("hdfs://localhost:9000");
        BaseFile path = bf.getChild("/chris/test");
        path.mkdir();
        BaseFile f = path.getChild("text.txt.gz");
        f.writeString("hello world");
        return null;
    }

    @CommandDef(command = "t", description = "t ")
    public static RedirectHttp t(@DebugArgAno(keyName = "",
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
    ) {
        com.hitorro.jsontypesystem.Type t = com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("core_sysobject");
        Propaccess path = new Propaccess("");
        ExecutionBuilder mtv = new ExecutionBuilder(new IndexerFactory());
        Predicate<com.hitorro.jsontypesystem.BaseT> f = (Predicate<com.hitorro.jsontypesystem.BaseT>) GroupNameFilter.indexFilter;
        t.visit(mtv, f, path);
        mtv.finalizeNode();
        return null;
    }

    public static void testResolve() {
        try {

            com.hitorro.jsontypesystem.JVS master = new com.hitorro.jsontypesystem.JVS();

            master.set("a.b", "ab_value");
            master.set("a.c.d", "acd_value");
            master.set("a.c.e", "acd_value");
            master.set("a.f", 1234);

            com.hitorro.jsontypesystem.JVS me = new com.hitorro.jsontypesystem.JVS();
            me.set("chris.foo", "${a.b}_${a.c.d}");
            me.set("chris.bla.foooo", "${a.c}");
            me.resolveVariables(master);
            int i = 1;

        } catch (PropaccessError propaccessError) {
            propaccessError.printStackTrace();
        }
    }

    @CommandDef(command = "x", description = "x")
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
        testResolve();
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
        SolrDocumentSink sds = new SolrDocumentSink("hitorro", true);
        JVS2SolrMapper jvs2solrMapper = new JVS2SolrMapper();
        AbstractIterator<com.hitorro.jsontypesystem.JVS> indexiter = jvsIter.map(jvs2solrMapper);
        indexiter.sink(sds);
        return null;
    }

    @CommandDef(command = "xx", description = "x")
    public static RedirectHttp wikiout(@DebugArgAno(keyName = "",
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
        testResolve();
        BaseFile bf = Env.getBaseFile(new File("/Users/chris/enwiki-20160501-pages-articles-multistream.xml.bz2"));
        BaseFile out = Env.getBaseFile(new File("/Users/chris/wiki.json.bz2"));

        if (!bf.exists()) {
            return null;
        }
        AbstractIterator<XE> iter = new StaxXMLBaseChainingIterator(bf.getDataInputStream(), "page");
        Class c = Class.forName(ExternalFeatureField.class.getCanonicalName(), true, CommandLine.class.getClassLoader());
        Class c1 = c.asSubclass(FieldType.class);
        com.hitorro.jsontypesystem.Type t = com.hitorro.jsontypesystem.JsonTypeSystem.getMe().getType("core_sysobject");
        WikiXML2JVSMapper mapper = new WikiXML2JVSMapper("en", "mydomain", t);
        AbstractIterator<com.hitorro.jsontypesystem.JVS> jvsIter = iter.map(mapper);
        JsonSink sink = new JsonSink(out);
        jvsIter.map(com.hitorro.jsontypesystem.JVS2JsonMapper.me).sink(sink);
        sink.close();
        return null;
    }

    @CommandDef(command = "xxsolr", description = "x")
    public static String xxsolr(@DebugArgAno(keyName = "",
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
        BaseFile out = Env.getBaseFile(new File("/Users/chris/wiki.json.bz2"));

        if (!out.exists()) {
            return null;
        }

        AbstractIterator<com.hitorro.jsontypesystem.JVS> jvsIter = out.getJsonIterator().skipNTakeM(1000, 600, false).map(com.hitorro.jsontypesystem.Json2JVSMapper.me);
        SolrDocumentSink sds = new SolrDocumentSink("mydomain", true);
        JVS2SolrMapper jvs2solrMapper = new JVS2SolrMapper();
        AbstractIterator<com.hitorro.jsontypesystem.JVS> indexiter = jvsIter.map(jvs2solrMapper);
        indexiter.sink(sds);
        sds.commitIndex();
        return "hello";
    }

    private static void computeIndex(final ExecutionBuilder mtv, final ProjectionContext pc, final com.hitorro.jsontypesystem.JVS node) throws PropaccessError {
        pc.source = node;
        pc.target = new com.hitorro.jsontypesystem.JVS();

        try {
            JsonNode val = node.get("body.mls[en].clean");
            List<String> sentences = node.getStringList("body.mls[en].segmented");
            if (sentences != null && sentences.size() > 0) {
                int i = 1;
                int s = node.getSize("body.mls[]");
                s = node.getSize("body.mls[]");
                s = node.getSize("body.mls[en].segmented");
                s = node.getSize("body.mls[0].segmented");
                int i2 = 1;
            }
        } catch (PropaccessError propaccessError) {
            propaccessError.printStackTrace();
        }
        mtv.getCurrentNode().project(pc);
    }

    @CommandDef(command = "foo", description = "foo")
    public static RedirectHttp redirect(@DebugArgAno(keyName = "",
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
    ) {

        /*BaseFile bf = Env.getBaseFile(new File("/Users/chris/000050.000000.json"));
        AbstractIterator<JVS> iter = BaseFileUtil.bf2jacksonjson.apply(bf).map(Json2JVSMapper.me);


        while (iter.hasNext())
        {
            JVS node = iter.next();
            int i = 1;
        }
        */
        compute();


        return null;
    }

    public static void compute() {
        ConceptNetDBBuilder b = new ConceptNetDBBuilder();
        try {
            b.read(new File("/Users/chris/Downloads/conceptnet-assertions-5.6.0.csv.gz"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (false) {
            AnswerTypeClassifier classifier = new AnswerTypeClassifier(null, Iso639Table.english);
            try {
                classifier.trainDefault(new File(Env.getData(), "/textclassifier/answertype.txt"));
            } catch (IOException e) {

            }
        }
    }

    @CommandDef(command = "build", description = "foo")
    public static RedirectHttp foo() throws IOException {
        BaseFile root = Env.getDataBaseFile().getChild("text/langs");
        BaseFile out = Env.getDataBaseFile().getChild("text/langid");
        LanguageId.buildModels(root, out);
        return null;
    }

    @CommandDef(command = "sap", description = "foo")
    public static String sap(@DebugArgAno(keyName = "",
            description = "",
            defaultValue = "",
            argType = ArgType.Raw) String raw,
                             @DebugArgAno(keyName = "",
                                     description = "",
                                     defaultValue = "",
                                     argType = ArgType.Args) com.hitorro.jsontypesystem.JVS args,
                             @DebugArgAno(keyName = "",
                                     description = "",
                                     defaultValue = "",
                                     argType = ArgType.Uri) String uri,
                             @DebugArgAno(keyName = "",
                                     description = "",
                                     defaultValue = "",
                                     argType = ArgType.Request) HttpServletRequest req
    ) throws IOException {
        if (rtqb == null) {
            rtqb = new RedisTaskQueueBase("10.1.10.150", 6379, "", 100);
        }
        SynchronousRedisTaskQueue sync = rtqb.getSyncQueue("test2");
        com.hitorro.jsontypesystem.JVS jvs = new com.hitorro.jsontypesystem.JVS();
        jvs.set("msg", "Hello there");
        System.out.println("Connected to Redis");

        //JVS retJvs = sync.execute(jvs);
        sync.executeAsync(jvs);
        //redisClient.shutdown();
        return "";
        //return retJvs.get("res").textValue();
    }

    public String registerHooks(boolean dbInit) {
        // we are special, lower tier requires exact implementation before INIT
        HTAnalyzerFactory.set(new GenericaAnalyzerFactory());
        return null;
    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        /*
        XXX TODO Should not be here
        dependentService = {JobService.class}
        JobService.getService().registerAppJob(HTMLFetcherAppJob.class, "HTML Fetcher App", HTMLFetcherAppJobParameters.class);
        JobService.getService().registerAppJob(PhraseListJob.class, "Generate a phrase listFiles from crawling the dms", PhraseListJobParameters.class);
        JobService.getService().registerAppJob(PhraseIndexJob.class, "Generate a phrase index from a phrase listFiles", PhraseIndexJobParameters.class);
        */
        LocalEventHub.get().addEventListener(m_indexCreatorEvent, m_indexCreatorEvent.eventName());
        return null;
    }

    public String run() {
        return null;
    }

    public String deInit() {
        return null;
    }

    public String start(boolean dbInit) {
        return null;
    }

    @CommandDef(command = "pdf", description = "foo")
    public void doPdf() throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        File f = new File("/Users/chris/Downloads/GuidetoWritingPolicy.pdf");
        PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(f));
        //PDPage page = tree.get(i);
        StringWriter writer = new StringWriter();

        stripper.writeText(doc, writer);
        System.out.println(writer.toString());
        int i = 1;
    }

}
