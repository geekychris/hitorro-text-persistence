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
package com.hitorro.basetext.text.classifier;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basetext.indexer.Log;
import com.hitorro.basetext.text.indexer.DocumentIndexCollection;
import com.hitorro.basetext.text.indexer.IndexerScheduledJob;
import com.hitorro.util.typesystem.BaseSession;

import java.io.IOException;
import java.util.Date;


public class ClassificationIndexer {

    public void index() {
        String indexName = "isQuestionClassifier";
        Object lock = IndexerScheduledJob.s_lockBox.getLock(indexName);

        synchronized (lock) {
            DocumentIndexCollection col = new DocumentIndexCollection(indexName);
            Date date = col.getLastIndexTime();

            String hql = "select guid from Post where type=10";
            Object args[] = new Object[]{indexName, date};
            BaseSession sess = DMSSessionFactory.getFactory().getSession();
            try {

                if (!col.init(sess, hql, args, false)) {
                    Log.indexer.error("Unable to get index called %s", indexName);
                    return;
                }

                try {
                    int count = col.index(4000);
                    Log.indexer.info("Wrote %s", count);
                } catch (IOException io) {
                    Log.indexer.error("Unable to index documents %s %e", io, io);
                }
            } finally {
                try {
                    col.close();
                } catch (IOException e) {
                    Log.indexer.error("Unable to close DocumentIndexCollection %s %e", e, e);
                }
                DMSSessionFactory.closeSession(sess);
            }
        }
    }

}
