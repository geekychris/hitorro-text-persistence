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
package com.hitorro.basetext.text.indexer;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.typesystem.BaseSession;
import org.hibernate.query.Query;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Nov 10, 2006 Time: 12:42:48 PM
 */
public class DocumentIndexCollection {
    private BaseSession m_session;
    private String m_query;
    private int m_indexCount;
    private File m_indexDirectory;
    private DocumentIndexer m_indexer;
    private boolean m_reInit;

    private Object m_queryArgs[];

    public DocumentIndexCollection(String name) {
        m_indexer = new DocumentIndexer(name);
    }

    public boolean isValid() {
        if (m_indexer != null) {
            return m_indexer.isValid();
        }
        return false;
    }

    public boolean init(BaseSession session, String query, Object args[], boolean reInit) {
        m_session = session;
        m_query = query;
        m_queryArgs = args;

        m_reInit = reInit;

        return true;
    }

    public Date getLastIndexTime() {
        return m_indexer.getLastIndexTime();
    }


    public boolean init(BaseSession session, String query, boolean reInit) {
        return init(session, query, null, reInit);
    }

    public int index(int batchSize) throws IOException {
        try {
            Iterator<String> iter = null;
            Query query = ((DMSSession) m_session).createQuery(m_query);
            if (m_queryArgs != null && m_queryArgs.length > 0) {
                iter = m_session.getIteratorFromQueryArgs(m_query, m_queryArgs);
            } else {
                iter = ((Query) m_session.createQuery(m_query)).stream().iterator();
            }

            m_indexer.init(m_reInit);
            int i = 0;
            while (iter.hasNext()) {
                i++;
                String guid = iter.next();
                m_indexer.indexItemFromGuid(guid);

                if (i > batchSize) {
                    closeForReopen();
                    m_indexer.init(false);
                    i = 0;
                }
                m_indexCount++;
            }
            // dont flushToDisk out new marker if it didnt succeed!
            this.m_indexer.setLastIndexTimeAsNow();
        } finally {

            close();
        }
        return m_indexCount;
    }

    private void closeForReopen() throws IOException {
        if (m_indexer != null) {
            m_indexer.close();
            m_indexer.deleteOld();
        }

    }

    public void close() throws IOException {
        closeForReopen();
        m_indexer = null;
    }
}
