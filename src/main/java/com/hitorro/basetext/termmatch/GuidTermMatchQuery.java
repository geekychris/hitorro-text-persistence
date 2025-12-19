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

import com.hitorro.base.objects.Document;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basetext.inverter.InverterUtils;
import com.hitorro.basetext.inverter.TermTupleSetGroup;
import com.hitorro.util.core.Log;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Jan 17, 2005 Time: 9:47:22 AM
 */
public class GuidTermMatchQuery extends TermMatchQuery {
    private String title;
    private String body;
    private boolean isPostId;

    public GuidTermMatchQuery() {
        this.setSourceType(MatchSourceType.Guid);
    }

    public TermTupleSetGroup getTupleSet() {
        BaseSession session = null;
        try {
            session = DMSSessionFactory.getFactory().getSession();
            HTSerializable pts = null;
            if (isPostId) {
                pts = session.getHTSerializableFromGUID(m_query);
            }
            if (pts instanceof Document) {
                Document doc = (Document) pts;
                title = doc.getTitle();
                body = doc.getContentText();
                try {
                    return InverterUtils.getTupleSet("title", getTitle(), "body", getBody());
                } catch (IOException e) {
                    Log.util.error("%s %e", e, e);
                    return null;
                }
            }
        } finally {
            if (session != null) {
                DMSSessionFactory.getFactory().rollbackCloseSession(session);
            }
        }
        return null;
    }

    public String getTitle() {
        return title;
    }


    public String getBody() {
        return body;
    }

    public boolean isPostId() {
        return isPostId;
    }

    public void setPostId(final boolean postId) {
        isPostId = postId;
    }
}
