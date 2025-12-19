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
package com.hitorro.basetext.termmatch;

import com.hitorro.basetext.indexer.SearcherCache;
import com.hitorro.obj.core.LuceneResultIterator;
import com.hitorro.obj.core.LuceneUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Jan 15, 2005 Time: 3:30:10 PM
 */
public class Ranker {

    public Ranker() {

    }

    public RankerResult rank(TermMatchQuery query, int maxResults, float minRank, int maxTerms) {
        RankerResult result = new RankerResult(query);

        String indexName = "fs_store1";
        IndexSearcher s = SearcherCache.getCache().get(indexName);
        if (s == null) {
            result.setError("Could not find index %s", indexName);
            return result;
        }
        QueryParser qp = LuceneUtils.getQueryParser();
        if (qp == null) {
            result.setError("Could not get analyzers");
            return result;
        }

        try {
            List<String> termsUsed = new ArrayList<String>();

            BooleanQuery.Builder q = LuceneUtils.getBooleanQueryFromTermMatchQuery(query, maxTerms, termsUsed, "all", "contenttext", BooleanClause.Occur.SHOULD);
            if (q == null) {
                return null;
            }


            LuceneResultIterator iter = new LuceneResultIterator(result,
                    maxResults, minRank, s, q.build());
            result.addTermsUsed(termsUsed);
            result.process(iter);
        } catch (IOException e) {
            result.setError("Could not search index %s, %s %e", indexName, e, e);
        }
        return result;
    }
}
