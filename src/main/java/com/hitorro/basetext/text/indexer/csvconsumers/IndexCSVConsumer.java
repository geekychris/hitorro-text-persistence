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
package com.hitorro.basetext.text.indexer.csvconsumers;

import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.basetext.indexer.FullTextIndex;
import com.hitorro.util.core.map.MapUtil;

/**
 * <p/>
 * Load the definition of an index into the system.
 */

public class IndexCSVConsumer extends CSVHibernateLoaderConsumer<FullTextIndex> {
    public static final String NameColumn = "name";
    public static final String StoreNameColumn = "storename";
    public static final String RootPathColumn = "rootpath";
    private static final String[][] Keys = {{"name", "name"}};

    public void start() {

    }

    public Class getPersistingClass() {
        return FullTextIndex.class;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, FullTextIndex index, boolean existsAlready) {
        String name = MapUtil.getColumnFromColumMap(NameColumn, m_headerMap, row);
        String storeName = MapUtil.getColumnFromColumMap(StoreNameColumn, m_headerMap, row);
        String rootPath = MapUtil.getColumnFromColumMap(RootPathColumn, m_headerMap, row);

        index.setName(name);
        index.setStore(storeName);
        index.setRootPath(rootPath);

        this.saveOrUpdate(existsAlready, index);
        return true;
    }

    public void done() {

    }

}
