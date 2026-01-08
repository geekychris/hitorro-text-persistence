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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basetext.indexer.Log;
import com.hitorro.basetext.indexer.FullTextIndex;
import com.hitorro.basetext.indexer.IndexerUtil;
import com.hitorro.language.IsoLanguage;
import com.hitorro.obj.core.TypeFieldsAnalyzerCache;
import com.hitorro.util.core.events.cache.HashCache;
import com.hitorro.util.core.iterator.sinks.Sink;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.json.keys.FileProperty;
import com.hitorro.util.json.keys.StringProperty;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * <p/>
 * The document indexer knows nothing of the source of documents, merely that it is going to get called n times to index
 * n documents.  Once a sufficient amount of documents have been provided for indexing, the writer will be closed and
 * deleteOld documents will be called to remove old versions of the documents.
 * <p/>
 * Note that a search engine has no concept of update.  We have to manage those as adds and deletes.  The
 * DocumentIndexer when initialized creates a version number using named longs.  This version number is indexed with all
 * the new documents under the field "indexversion".  As we index each document, we also hold onto the guid of the
 * document being indexed.
 * <p/>
 * Once we have completed adding documents, we can then call deleteOld.  This will perform a query that finds documents
 * that have the guid of each document indexed in this index run but does not have the indexversion that those documents
 * were indexed with.  Each document that is found is then deleted from the index.
 * <p/>
 * Due to this behaviour it is possible that searches could get duplicate GUID's. Its not terribly detrimental unless
 * your doing some kind of non idempodent type work.  If that is the case, ensure you dont process guid's twice.
 */
public class DocumentIndexer<E extends com.hitorro.util.typesystem.BaseType> implements Sink<E> {
    public static final StringProperty NameKey = new StringProperty("name", "name of the index", null);
    public static final FileProperty DirectoryKey = new FileProperty("dir", "name of the index", "");
    private static final String OptimizeKey = "optimize.txt";
    private static int counter = 0;
    private File m_indexDir = null;
    private IndexWriter m_writer = null;
    //private GenericAnalyzer m_defaultAnalyzer = new GenericAnalyzer(FilterBits.DefaultFilters);
    private long m_indexVersion;
    private List<String> m_guids = new ArrayList<String>();
    private String m_name;
    private com.hitorro.util.typesystem.BaseSession m_session = DMSSessionFactory.getFactory().getSession();
    private boolean m_valid = false;
    private File m_lastIndexTimeFile;
    private Date m_testDate;
    private HashCache<IsoLanguage, Analyzer> indexerCache = com.hitorro.basetext.indexer.AllTypesAnalyzerCache.getCache(false);
    private com.hitorro.util.typesystem.TypeManager manager;
    // how many guids to construct into a delete query at a time.  The more we have, the fewer queries
    // we perform in general reducing general overhead.
    private int m_guidDeleteChunkingSize = 10;

    public DocumentIndexer() {

    }

    /**
     * Index construction that doesnt require a database to do its bidding.  You provide the path to the index where the
     * documents are to be placed.
     *
     * @param name
     * @param dir
     */
    public DocumentIndexer(String name, File dir) {
        init(name, dir);

    }

    /**
     * Indexer construction that figures out the index location from the database
     *
     * @param name
     */
    public DocumentIndexer(String name) {
        com.hitorro.util.typesystem.BaseSession session = DMSSessionFactory.getFactory().getSession();
        FullTextIndex fti = (FullTextIndex) session.getBySoftReference(FullTextIndex.class, name);
        File dir = fti.getRootDir();
        init(name, dir);
    }

    public DocumentIndexer(File directory, String name) {
        initDir(directory, name);
    }

    public long getLastIndexTimeMillis() {
        if (m_lastIndexTimeFile.exists()) {
            return m_lastIndexTimeFile.lastModified();
        } else {
            return -1000000000;
        }
    }

    public Date getLastIndexTime() {
        m_testDate = new Date();
        return new Date(getLastIndexTimeMillis());
    }

    public boolean setLastIndexTimeAsNow() {
        try {
            FileUtil.writeLongValToFile(m_lastIndexTimeFile, m_testDate.getTime());
        } catch (IOException e) {
            Log.indexer.error("%s %e", e, e);
        }
        return true;
    }

    private void init(final String name, final File dir) {
        m_valid = initDir(dir, name);
    }

    public boolean isValid() {
        return m_valid;
    }

    private boolean initDir(File directory, String name) {

        m_indexDir = directory;
        m_lastIndexTimeFile = new File(m_indexDir, Fmt.S("index-date-%s", name));
        m_name = name;
        return true;
    }

    public boolean init(boolean reInit) throws IOException {
        if (m_valid == false) {
            return false;
        }
        m_indexVersion = GuidBaseType.BaseTypeIdNamedLong.getNextValue();
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer());
        m_writer = IndexerUtil.constructWriter(m_indexDir, reInit, analyzer, true);
        m_guids.clear();
        manager = com.hitorro.util.typesystem.TypeManager.getTypeManager();


        return true;
    }

    public void optimize() {
        //TODO UPDATE
        //m_writer.optimize();
    }

    public void indexItemFromGuid(String guid) throws IOException {
        try {
            counter++;

            /*if (counter % 100 == 0)
            {
                Log.indexer.info("indexing %s %s", guid, counter);
            } */
            E bt = (E) m_session.getHTSerializableFromGUID(guid);
            if (bt != null) {
                if (add(bt)) {
                    return;
                }
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(m_session);
        }
    }

    public boolean addAll(List<E> list) throws IOException {
        for (E bt : list) {
            add(bt);
        }
        return true;
    }

    public boolean indexBaseTypeIterator(Iterator<E> iter) throws IOException {
        while (iter.hasNext()) {
            add(iter.next());
        }
        return true;
    }

    public boolean add(E bt) throws IOException {
        com.hitorro.util.typesystem.TypeIntf type = manager.getTypeForBaseType(bt);

        List<com.hitorro.util.typesystem.TypeFieldIntf> fields = com.hitorro.basetext.indexer.TypeFieldsCache.getCache().get(type);


        if (fields == null || fields.size() == 0) {
            // not indexable
            return true;
        }


        IsoLanguage lang = ((com.hitorro.util.typesystem.Type) type).getIsoLanguage(bt);

        Analyzer analyzer = TypeFieldsAnalyzerCache.index.get(type).getForLanguage(lang);
        m_guids.add(bt.getGuid());

        List<IndexableField> addMe = new ArrayList<>();
        for (com.hitorro.util.typesystem.TypeFieldIntf f : fields) {
            IndexerUtil.indexFieldFromTypeField(addMe, f, f.getValue(bt), lang);
        }
        addOutOfBandFields(addMe);


        m_writer.addDocument(addMe);
        return false;
    }

    public void deleteOld() throws IOException {
        IndexerUtil.deleteOld(m_guids, m_guidDeleteChunkingSize, "indexversion", m_name, m_indexVersion);
    }

    private boolean mustOptimize() {
        File f = new File(m_indexDir, OptimizeKey);
        return f.exists();
    }

    public void removeOptimizeMarker() {
        File f = new File(m_indexDir, OptimizeKey);
        f.delete();
    }


    public void markForOptimize() {
        File f = new File(m_indexDir, OptimizeKey);
        try {
            FileUtil.writeLongValToFile(f, System.currentTimeMillis());
        } catch (IOException e) {

        }
    }

    public void close() throws IOException {
        if (m_session != null) {
            DMSSessionFactory.getFactory().rollbackCloseSession(m_session);
        }
        if (mustOptimize() && m_writer != null) {
            optimize();
            removeOptimizeMarker();
        }
        IndexerUtil.indexerClose(m_writer, this.m_name);
    }


    private void addOutOfBandFields(List<IndexableField> addMe) {
        // version number
        addMe.add(new Field("indexversion", Long.toString(m_indexVersion),
                com.hitorro.basetext.indexer.IndexerFieldAdapter.getIndexEnum(true, true, false)));
    }


    @Override
    public boolean init(JsonNode node) {

        init(NameKey.apply(node), DirectoryKey.apply(node));
        return true;
    }

    @Override
    public boolean start() throws IOException {
        init(true);
        return true;
    }

    @Override
    public boolean stop() throws IOException {
        close();
        return true;
    }
}