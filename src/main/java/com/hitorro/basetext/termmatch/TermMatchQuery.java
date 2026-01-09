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

import com.hitorro.basetext.inverter.DocumentInverter;
import com.hitorro.basetext.inverter.TFIDFTermMeasureFunction;
import com.hitorro.basetext.inverter.TermTupleSet;
import com.hitorro.basetext.inverter.TermTupleSetGroup;
import org.apache.lucene.search.BooleanQuery;

/**
 */
public abstract class TermMatchQuery {
    protected MatchType matchType = MatchType.TFIDF;
    protected MatchSourceType m_type;
    protected String m_query;
    protected String m_id;
    protected BooleanQuery masterQuery;
    private DocumentInverter docInverter = null;

    public abstract TermTupleSetGroup getTupleSet();

    public BooleanQuery getMasterQuery() {
        return masterQuery;
    }

    public void setMasterQuery(BooleanQuery master) {
        masterQuery = master;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    protected void setMatchType(MatchType type) {
        matchType = type;
    }

    public String getId() {
        return m_id;
    }

    /**
     * Id that is used to correlate the query with a result set.
     *
     * @param id
     */
    public void setId(String id) {
        m_id = id;
    }

    public String getQueryString() {
        return m_query;
    }

    public MatchSourceType getSourceType() {
        return m_type;
    }

    public void setSourceType(MatchSourceType type) {
        m_type = type;
    }

    public void setQuery(String q) {
        m_query = q;
    }

    protected DocumentInverter getDocumentInverter() {
        if (docInverter == null) {
            switch (this.matchType) {
                case TFIDF:
                    docInverter = new DocumentInverter("body", null, new TFIDFTermMeasureFunction(), TermTupleSet.s_MeasureDescendComparitor);
                    break;
                default:
                    // XXX to fix, implement BM25
                    docInverter = new DocumentInverter("body", null, new TFIDFTermMeasureFunction(), TermTupleSet.s_MeasureDescendComparitor);
            }
        }
        return docInverter;
    }

    public enum MatchSourceType {
        RawText, HTMLSource, Url, Guid, SuppliedObject
    }

    public enum MatchType {
        TFIDF, BM25
    }
}
