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

import com.hitorro.obj.core.LuceneResultIteratorAdapter;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import org.apache.lucene.document.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 */
public class RankerResult implements LuceneResultIteratorAdapter<RankerResultRow> {
    private TermMatchQuery m_query;
    private List<RankerResultRow> m_rows = new ArrayList<RankerResultRow>();
    private String m_error;
    private List<String> m_termsUsed = new ArrayList<String>();

    public RankerResult(TermMatchQuery query) {
        m_query = query;
    }

    public String getError() {
        return m_error;
    }

    public void setError(String msg, Object... args) {
        m_error = Fmt.S(msg, args);
    }

    public void process(Iterator<RankerResultRow> iter) {
        while (iter.hasNext()) {
            addRow(iter.next());
        }
    }

    public void addRow(RankerResultRow row) {
        m_rows.add(row);
    }

    public List<RankerResultRow> getRows() {
        return m_rows;
    }

    public List<String> getTermsUsed() {
        return m_termsUsed;
    }

    public void addTermUsed(String term) {
        m_termsUsed.add(term);
    }

    public void addTermsUsed(List<String> termsUsed) {
        for (String term : termsUsed) {
            addTermUsed(term);
        }
    }

    public TermMatchQuery getQuery() {
        return m_query;
    }

    public RankerResultRow map(float score, Document doc) {
        String guid = doc.get("guid");
        String hash = doc.get("identityHash");
        long hashL = 0;
        if (!StringUtil.nullOrEmptyString(hash)) {
            hashL = Long.parseLong(hash);
        }
        String link = doc.get("moreLink");
        return new RankerResultRow(guid, score, hashL, link);
    }
}
