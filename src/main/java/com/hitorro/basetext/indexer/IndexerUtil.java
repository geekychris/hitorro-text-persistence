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
package com.hitorro.basetext.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hitorro.language.Iso639Table;
import com.hitorro.language.IsoLanguage;
import com.hitorro.obj.core.GenericAnalyzer;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.core.events.LocalEventHub;
import com.hitorro.util.core.events.cache.Cache;
import com.hitorro.util.io.ResetableStringReader;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.typesystem.TypeField;
import com.hitorro.util.typesystem.TypeFieldIntf;
import com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.AttributeFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**

 * some notes about filters: http://www.javaranch.com/journal/2009/02/filtering-a-lucene-search.html
 */
public class IndexerUtil {
    /*
      Determines the minimal number of documents required before the buffered in-memory documents are merging and a new Segment is created.
      Since Documents are merged in a RAMDirectory , large value gives faster indexing. At the same time, mergeFactor limits the number
      of files open in a FSDirectory.
     */
    public static IntegerProperty MaxBufferedDocs = new IntegerProperty("indexer.maxbuffereddocs", "", 8000);

    /*
      Determines the largest number of documents ever merged by addDocument(). Small values
      (e.g., less than 10,000) are best for interactive indexing, as this limits the length
      of pauses while indexing to a few seconds. Larger values are best for batched indexing and speedier searches.
     */
    public static IntegerProperty MaxMergeDocs = new IntegerProperty("indexer.maxmerge", "", 20000);

    /*
      Determines how often segment indices are merged by addDocument(). With smaller values, less RAM is used while indexing,
      and searches on unoptimized indices are faster, but indexing speed is slower. With larger values, more RAM is used
      during indexing, and while searches on unoptimized indices are slower, indexing is faster. Thus larger values
      (> 10) are best for batch index creation, and smaller values (< 10) for indices that are interactively maintained.

      This must never be less than 2. The default value is 10.
     */

    public static IntegerProperty MergFactor = new IntegerProperty("indexer.mergefactor", "", 10);
    /*
      The maximum number of terms that will be indexed for a single field in a document.
      This limits the amount of memory required for indexing, so that collections with very
      large files will not crash the indexing process by running out of memory.

      Note that this effectively truncates large documents, excluding from the index terms
      that occur further in the document. If you know your source documents are large, be
      sure to set this value high enough to accomodate the expected size. If you set it to
      Integer.MAX_VALUE, then the only limit is your memory, but you should anticipate an OutOfMemoryError.

      By default, no more than 10,000 terms will be indexed for a field.
     */
    public static IntegerProperty MaxFieldLength = new IntegerProperty("indexer.maxfieldsize", "", 1000);
    public static com.hitorro.basetext.indexer.IndexerFieldAdapter defaultAdapter = new com.hitorro.basetext.indexer.IndexerFieldAdapter();
    private static Map<Class, com.hitorro.basetext.indexer.IndexerFieldAdapter> adapters = new HashMap<Class, com.hitorro.basetext.indexer.IndexerFieldAdapter>();

    public static SortField getSortField(TypeField tf, boolean reverse) {
        FullTextAttributeMetaInfo info = tf.getFullTextMeta();
        if (info.stored()) {
            return new SortField(info.luceneFieldName(), SortField.Type.SCORE, reverse);
        }
        // not a stored field
        return null;
    }

    /**
     * Dump to the console tokens from a stream
     *
     * @param ga
     * @param rsr
     * @throws IOException
     */
    public static void printTokens(final GenericAnalyzer ga, final ResetableStringReader rsr) throws IOException {
        TokenStream ts = ga.tokenStream("foo", rsr);
        CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
        KeywordAttribute ka = ts.getAttribute(KeywordAttribute.class);
        PositionIncrementAttribute pia = ts.getAttribute(PositionIncrementAttribute.class);
        OffsetAttribute offsetAtt = ts.getAttribute(OffsetAttribute.class);


        boolean isKA = false;
        //
        while (ts.incrementToken()) {
            String term = termAttribute.toString();
            int pos = pia.getPositionIncrement();
            if (ka != null) {
                isKA = ka.isKeyword();
            }
            Console.println("term: %s position: %s isKA: %s start: %s end: %s", term, pos, isKA, offsetAtt.startOffset(), offsetAtt.endOffset());
        }
        ts.close();
    }


    public static JsonNode getTokensAsJson(final GenericAnalyzer ga, final ResetableStringReader rsr, boolean asArray) throws IOException {
        TokenStream ts = ga.tokenStream("foo", rsr);
        CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);

        StringBuilder sb = new StringBuilder();
        if (asArray) {
            ArrayNode an = JsonNodeFactory.instance.arrayNode();
            while (ts.incrementToken()) {
                String term = termAttribute.toString();
                an.add(JsonNodeFactory.instance.textNode(term));
            }
            ts.close();
            return an;
        }

        while (ts.incrementToken()) {
            String term = termAttribute.toString();
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(term);
        }
        ts.close();
        return JsonNodeFactory.instance.textNode(sb.toString());
    }

    public static void indexFieldFromTypeField(List<IndexableField> addMe,
                                                     TypeFieldIntf f,
                                                     Object valueRaw,
                                                     IsoLanguage language) {
        if (valueRaw == null) {
            // For now we do not index "null"
            return;
        }
        FullTextAttributeMetaInfo ftm = f.getFullTextMeta();

        // for now we will assume we just do a toString on a valueRa


        boolean stored = ftm.stored();
        boolean indexed = ftm.isFullTextIndexable();
        boolean addToDefaultField = ftm.allField();
        boolean literal = ftm.stringLiteral();
        boolean isDate = ftm.isDate();
        String field = ftm.luceneFieldName();
        Class clazz = ftm.indexerClass();
        com.hitorro.basetext.indexer.IndexerFieldAdapter adap = IndexerUtil.getAdapter(clazz);

        indexField(stored, indexed, addMe, field, valueRaw, literal, addToDefaultField, isDate, clazz, language, adap);
    }

    public static IndexSearcher get(File dir) throws IOException {
        Directory d = FSDirectory.open(dir.toPath());
        //TODO Update (Hopefully this is the right mechanism to construct the searcher)
        DirectoryReader ir = DirectoryReader.open(d);
        IndexSearcher is = new IndexSearcher(ir);
        Log.indexer.info("Loaded index path: %s with %s documents", dir, is.getIndexReader().numDocs());
        return is;
    }

    public static void indexField(boolean stored,
                                        boolean indexed,
                                        List<IndexableField> addMe,
                                        String field,
                                        Object valueRaw,
                                        boolean literal,
                                        boolean addToDefaultField,
                                        boolean isDate,
                                        Class clazz,
                                        IsoLanguage language,
                                        com.hitorro.basetext.indexer.IndexerFieldAdapter adap) {

        if (adap == null) {
            return;
        }
        adap.index(isDate, valueRaw, stored, indexed, addMe, field, literal, addToDefaultField, language);
    }

    public static com.hitorro.basetext.indexer.IndexerFieldAdapter getAdapter(Class clazz) {
        com.hitorro.basetext.indexer.IndexerFieldAdapter adap;
        if (clazz != null && clazz != Object.class) {
            adap = adapters.get(clazz);
            if (adap == null) {
                adap = (com.hitorro.basetext.indexer.IndexerFieldAdapter) ClassUtil.getInstanceSwallowError(clazz, com.hitorro.basetext.indexer.IndexerFieldAdapter.class);
                if (adap == null) {
                    // blow up
                    return null;
                }
            }
        } else {
            adap = defaultAdapter;
        }
        return adap;
    }

    public static IndexWriter constructWriter(File index, boolean reInit, Analyzer analyzer, boolean ignoreLock) throws IOException {
        IndexWriter indexer = null;
        // figure out if we need to create the index
        boolean create = shouldCreateIndex(index, reInit);
        if (create) {
            Log.indexer.info("Creating index for %s", index.getName());
        }
        //TODO Update
        FSDirectory idx = FSDirectory.open(index.toPath());
        if (ignoreLock) {
            //TODO UPDATE
                /*
            try
            {


                if (IndexWriter.isLocked(idx))
                {
                    IndexWriter.unlock(idx);
                    Log.indexer.info("Forced unlock of index %s", index);
                }

            }
            catch (IOException e)
            {
                Log.indexer.error("Failed to create or unlock index %s %s %e", index, e, e);

                return null;
            }
            */
        }
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);

        conf.setMaxBufferedDocs(IndexerUtil.MaxBufferedDocs.apply());
        /*
        conf.setMergeFactor(IndexerUtil.MergFactor.apply());
        conf.setMaxMergeDocs(IndexerUtil.MaxMergeDocs.apply());
        conf.setMaxFieldLength(IndexerUtil.MaxFieldLength.apply());

        indexer.setUseCompoundFile(true);
        */

        indexer = new IndexWriter(idx, conf);


        return indexer;
    }

    public static void indexerClose(IndexWriter writer, String name) throws IOException {
        if (writer != null) {
            writer.close();
        }

        IndexerUtil.flushIndexReader(name);
    }

    public static void flushIndexReader(String name) {
        List l = new ArrayList();
        l.add(name);
        LocalEventHub.get().event(SearcherCache.IndexChangeEvent, Cache.FlushCache, l);
    }

    public static boolean shouldCreateIndex(File indexDirectory, boolean reInit) {
        if (reInit == true) {
            return true;
        }
        return !indexDirectory.exists() || !indexDirectory.isDirectory();
    }

    /**
     * Given a listFiles of ids (could be guids), the field that the indexVersion is stored on and an index version go
     * out and remove any rows from a query for those guids that is not the current version. In other words, this is
     * used in conjunction with indexing to remove dupes that are old versions of a document.
     *
     * @param ids
     * @param chunkingSize
     * @param field
     * @param indexName
     * @param indexVersion
     * @throws IOException
     */
    public static void deleteOld(List<String> ids,
                                       int chunkingSize,
                                       String field,
                                       String indexName,
                                       long indexVersion) throws IOException {
        IndexSearcher searcher = SearcherCache.getCache().get(indexName);
        if (searcher == null) {
            // nothing to delete as there is no searcher.
            return;
        }
        IndexReader reader = searcher.getIndexReader();

        // doesnt matter what language, were just deleting
        Analyzer analyzers = com.hitorro.basetext.indexer.AllTypesAnalyzerCache.getCache().get(Iso639Table.english);
        try {
            if (analyzers == null) {
                return;
            }


            Query version = new TermQuery(new Term(field, Long.toString(indexVersion)));
            while (ids.size() > 0) {
                int chunk = Math.min(chunkingSize, ids.size());
                BooleanQuery.Builder builder = new BooleanQuery.Builder();

                builder.add(version, BooleanClause.Occur.MUST_NOT);

                for (int i = 0; i < chunk; i++) {
                    int end = ids.size() - 1;
                    String guid = ids.remove(end);
                    builder.add(new TermQuery(new Term("guid", guid)), BooleanClause.Occur.SHOULD);
                }
                BooleanQuery q = builder.build();
                q.rewrite(reader);
                deleteFromQuery(searcher, q, reader);
            }
            IndexerUtil.flushIndexReader(indexName);
            reader.close();
        } finally {

        }

    }

    private static final void deleteFromQuery(IndexSearcher searcher, BooleanQuery q, IndexReader reader) {
        //TODO UPDATE
        /*
        TopFieldDocs hits = searcher.search(q, null, 100000, Sort.INDEXORDER);
        if (hits != null)
        {
            ScoreDoc docs[] = hits.scoreDocs;
            for (int i = 0; i < docs.length; i++)
            {
                int id = docs[i].doc;
                reader.deleteDocument(id);
            }
        }
        */
    }

    public static void test(String string) throws IOException {
        Console.println("tokenizing: %s");

        Map map = new HashMap();
        JapaneseTokenizerFactory factory = new JapaneseTokenizerFactory(map);
        AttributeFactory af = AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY;
        JapaneseTokenizer jt = factory.create(af);
        TokenFilter curr = new CJKWidthFilter(jt);
        jt.close();
        ResetableStringReader reader = new ResetableStringReader(string);
        jt.setReader(reader);
        jt.reset();


        int count = 0;

        CharTermAttribute termAttribute = jt.getAttribute(CharTermAttribute.class);
        while (curr.incrementToken()) {
            count++;
            String term = termAttribute.toString();
            Console.println(term);
        }
        Console.println();
    }

    /*
    Example 1
15_10月度(7月20日締め)直取り商品明細（並行 交渉)
15 _ October degree (July 20, tightening) straight up commodity specification (parallel negotiations)


 TOKENIZATION
15                  Fifteen
10                  Ten
月                   Month
度                   soil
7                   Seven
月                   Month
20                  Twenty
締め                 Tighten
直                   straight
取り                 Take
商品                 Product
明細                 Specification
並行                 Concurrent
交渉                 Negotiation


15
10
月
度
7
月
20
日
締め
直
取り
商品
明細
並行
交渉







15                  Fifteen
11                  Eleven
月                   Month
度                   soil
8                   Eight
月                   Month
20                  Twenty
日                   Day
締め                 Tighten
直                   straight
取り                 Take
商品                 Product
明細                 Specification
並行                 Concurrent
交渉                 Negotiation



This 直取 (Chokuto) was not found.  This seems odd as the tokenizer seems to split this into two terms, even if those are the only terms given to the tokenizer (doesnt seem state based).
Its not a stop word! or stop tag.


取り is tokenized to      直 取り so it is two terms and should be found.
     */
}