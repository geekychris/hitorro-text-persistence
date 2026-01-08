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

import com.hitorro.language.Iso639Table;
import com.hitorro.util.typesystem.BaseSession;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class Search {


    public List<SearchResult> execute(BaseSession sess, String indexName, Query query) {
        IndexSearcher s = SearcherCache.getCache().get(indexName);
        if (s == null) {
            Log.indexer.error("Could not find index %s", indexName);
            return null;
        }
        try {

            s.setSimilarity(new ClassicSimilarity());
            TopDocs td = s.search(query, 100000);

            long totalCount = td.totalHits.value;
            ScoreDoc[] sd = td.scoreDocs;
            List<SearchResult> results = new ArrayList<SearchResult>();
            String fields[] = new String[]{};
            for (int i = 0; i < totalCount; i++) {
                Document doc = s.doc(sd[i].doc);
                SearchResult sr = new SearchResult();
                sr.setDocument(s, sd[i], fields);
                results.add(sr);
            }
            return results;
        } catch (IOException e) {
            Log.indexer.error("Could not search index %s, %s %e", indexName, e, e);
        }
        return null;
    }

    public List<SearchResult> execute(BaseSession sess, String indexName, String query) {
        Analyzer analyzers = com.hitorro.basetext.indexer.AllTypesAnalyzerCache.getCache().get(Iso639Table.english);
        QueryParser qp = new QueryParser(com.hitorro.basetext.indexer.IndexerFieldAdapter.DEFAULT_SEARCH_FIELD, analyzers);
        try {
            if (analyzers == null) {
                Log.indexer.error("Could not get analyzers");
                return null;
            }
            Query q = qp.parse(query);
            execute(sess, indexName, q);
        } catch (ParseException e) {
            Log.indexer.error("Could not search index %s, %s %e", indexName, e, e);
        }
        return null;
    }

}
