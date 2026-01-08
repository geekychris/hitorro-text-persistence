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

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basetext.indexer.Log;
import com.hitorro.util.core.map.LockBox;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.typesystem.BaseSession;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.io.IOException;
import java.util.Date;

/**
 * <p/>
 * Scheduled Job to see if there are items to index.
 */
public class IndexerScheduledJob implements Job {
    public static LockBox s_lockBox = new LockBox();

    public IndexerScheduledJob() {
    }

    public final void execute(JobExecutionContext jobExecutionContext) {
        if (!ServiceContext.getSC().isServiceInitialized(IndexerService.class)) {
            return;
        }
        Object lock = null;
        try {
            JobDetail detail = jobExecutionContext.getJobDetail();
            JobDataMap dmap = detail.getJobDataMap();

            String indexName = (String) dmap.get("name");
            if (StringUtil.nullOrEmptyOrBlankString(indexName)) {
                Log.indexer.error("No index name provided in IndexerScheduledJob");
                return;
            }
            lock = s_lockBox.getLock(indexName);
            synchronized (lock) {
                DocumentIndexCollection col = new DocumentIndexCollection(indexName);
                if (!col.isValid()) {
                    Log.indexer.error("Index could not be initialized, maybe index %s is not defined", indexName);
                    return;
                }
                Date date = col.getLastIndexTime();

                String hql = "select guid from VersionableObject where indexName= :a and modifiedDate > :b";
                Object args[] = new Object[]{indexName, date};
                BaseSession sess = DMSSessionFactory.getFactory().getSession();
                try {

                    if (!col.init(sess, hql, args, false)) {
                        Log.indexer.error("Unable to get index called %s", indexName);
                        return;
                    }

                    try {
                        int count = col.index(8000);
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
        } finally {
            // there is no need to release the lock as this is just a lock object and the fact we have exited the
            // synchronized block means we have released the lock.

        }

    }

}