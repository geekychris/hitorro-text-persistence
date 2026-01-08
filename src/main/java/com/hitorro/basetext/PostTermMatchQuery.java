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
package com.hitorro.basetext;

import com.hitorro.base.objects.Post;
import com.hitorro.basedms.Log;
import com.hitorro.basetext.inverter.InverterUtils;
import com.hitorro.basetext.inverter.TermTupleSetGroup;
import com.hitorro.basetext.termmatch.TermMatchQuery;
import com.hitorro.language.Iso639Table;

import java.io.IOException;



public class PostTermMatchQuery extends TermMatchQuery {
    private Post m_post;

    public PostTermMatchQuery(Post post) {
        m_post = post;
        this.setSourceType(MatchSourceType.SuppliedObject);
    }

    public TermTupleSetGroup getTupleSet() {

        String title = m_post.getTitle();
        String body = m_post.getBodyText();
        try {
            return InverterUtils.getTupleSet("title", title, "body", body, "body", Iso639Table.english);
        } catch (IOException e) {
            Log.basedms.error("Unable to get TermTupleSetGroup %s %e", e, e);
            return null;
        }

    }
}
