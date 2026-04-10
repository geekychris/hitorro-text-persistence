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
package com.hitorro.basetext.text.dfindex;

import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.Post;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basetext.dfindex.DFIndex;
import com.hitorro.basetext.dfindex.DFIndexBuilder;
import com.hitorro.basetext.inverter.DocumentInverter;
import com.hitorro.basetext.inverter.TermTupleSet;
import com.hitorro.language.Iso639Table;
import com.hitorro.language.IsoLanguage;
import com.hitorro.obj.core.GenericAnalyzer;
import com.hitorro.obj.core.Log;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.events.EventListener;
// BaseFileResourceCache and BaseFileResourceContext removed during cleanup
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseType;
import org.hibernate.query.Query;

import java.io.IOException;
import java.util.Iterator;


/**
 */
public class DFIndexCreatorEvent implements EventListener {
    private static final String Q2 = "select post.title, cc.guid from Post post join post.contents cc where cc.id in " +
            "(select c1.id from Content as c1 left join c1.categories as cat where cat.domain='" + Post.DocPartsDomain + "' and cat.value='" + Post.BodyFromPermaLinkLabel + "')";
    private static final String QueryString = "select guid from " + Post.class.getCanonicalName();
    private boolean m_running = false;

    public boolean event(String topic, String subTopic, Object args) {
        boolean iSetTrue = false;
        try {
            synchronized (this) {
                if (m_running == true) {
                    // already running
                    return false;
                }
                iSetTrue = true;
                m_running = true;

                try {
                    createDFIndex();
                } catch (IOException e) {
                    Log.util.error("%s %e", e, e);
                }
            }

        } finally {
            if (iSetTrue) {
                m_running = false;
            }
            iSetTrue = false;
        }
        return true;
    }

    private void createDFIndex() throws IOException {
        Log.dfindexer.info("Starting dfindex creation");
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        BaseSession guidSession = DMSSessionFactory.getFactory().getSession();
        DFIndexBuilder builder = new DFIndexBuilder();
        builder.setDescription("Event driven df index build", QueryString);
        boolean complete = false;
        try {
            Iterator<Object[]> iter = ((Query) session.createQuery(Q2)).stream().iterator();
            while (iter.hasNext()) {
                try {
                    Object[] o = iter.next();
                    String title = (String) o[0];
                    String guid = (String) o[1];
                    BaseType bt = (BaseType) guidSession.getHTSerializableFromGUID(guid);
                    IsoLanguage language = Iso639Table.english;

                    if (bt instanceof Content) {
                        Content cont = (Content) bt;

                        String body = cont.getStringValue();
                        DocumentInverter inverter = new DocumentInverter("body", GenericAnalyzer.Standard.apply(), null, null);

                        try {
                            TermTupleSet titleSet = inverter.setText("title", title, language);
                            TermTupleSet bodySet = inverter.setText("body", body, language);
                            builder.addDocument(titleSet, bodySet);
                        } catch (IOException e) {
                            Log.util.error("%s %e", e, e);
                        }
                    }
                } finally {
                    DMSSessionFactory.getFactory().rollbackClose(guidSession);
                }

            }
            complete = true;
            Log.dfindexer.info("Completed dfindex data collection step");
        } finally {

            session.rollback();
        }
        if (complete == true) {
            // BaseFileResourceCache removed during cleanup - resource cache no longer available
            Log.dfindexer.warn("DFIndex creation skipped - resource cache no longer available");
        }
    }

    public String eventName() {
        return "CreateDFIndexEvent";
    }

    public boolean runAsync() {
        return true;
    }
}