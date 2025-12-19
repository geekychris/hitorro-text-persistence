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
package com.hitorro.basetext.phrase;

import com.hitorro.base.objects.Document;
import com.hitorro.basedms.session.HibernateQueryResultObjectAdapter;
import com.hitorro.util.typesystem.BaseSession;
import org.hibernate.type.Type;

import java.util.Iterator;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 * <p/>
 * Given a query result that is a set of guids, try to retrieve each object and render the "text"
 */
public class GetDocumentText implements HibernateQueryResultObjectAdapter<String> {
    private BaseSession m_session;

    private int m_count = 0;


    public GetDocumentText(BaseSession session) {
        m_session = session;
    }

    public int getRowsRetrieved() {
        return m_count;
    }

    public String map(Iterator iter, Type[] types, String[] aliases) {
        m_session.rollback();

        String guid = (String) iter.next();
        Document doc = (Document) m_session.getHTSerializableFromGUID(guid);
        if (doc == null) {
            return null;
        }
        String text = doc.getContentText();
        m_count++;
        return text;
    }
}
