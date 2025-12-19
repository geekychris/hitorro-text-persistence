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

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.events.cache.HashCache;
import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.typesystem.BaseSession;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Nov 13, 2006 Time: 5:45:36 PM
 */
public class SearcherCache extends BaseMapper<String, IndexSearcher> {
    public static final String IndexChangeEvent = "IndexChangedEvent";
    private static final HashCache<String, IndexSearcher> s_cache = new HashCache<String, IndexSearcher>(IndexChangeEvent, new SearcherCache());

    /**
     * Constructor
     */
    public SearcherCache() {
    }

    public static final HashCache<String, IndexSearcher> getCache() {
        return s_cache;
    }

    public IndexSearcher apply(String key) {
        Log.indexer.debug("Loading index name: %s", key);
        BaseSession session = null;

        FullTextIndex fti = null;
        try {
            session = DMSSessionFactory.getFactory().getSession();
            fti = (FullTextIndex) session.getBySoftReference(FullTextIndex.class, key);

        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }

        if (fti == null) {
            return null;
        }
        File dir = fti.getRootDir();
        if (FileUtil.notNullAndExists(dir)) {
            try {
                Directory fsd = FSDirectory.open(dir.toPath());
                //TODO Update (Hopefully this is the right mechanism to construct the searcher)
                DirectoryReader ir = DirectoryReader.open(fsd);
                IndexSearcher is = new IndexSearcher(ir);
                Log.indexer.info("Loaded index name: %s with %s documents", key, is.getIndexReader().numDocs());
                return is;
            } catch (IOException e) {
                Log.indexer.error("Unable to retrieve index %s", dir.getAbsolutePath());
            }
        }

        return null;

    }

    public String eventName() {
        return "SearcherCache";
    }
}
