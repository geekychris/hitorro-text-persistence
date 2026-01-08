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

import com.hitorro.basetext.inverter.InverterUtils;
import com.hitorro.basetext.inverter.TermTupleSetGroup;
import com.hitorro.util.core.Log;
import com.hitorro.util.html.HTMLParser;

import java.io.IOException;

/**
 */
public class HTMLSourceTermMatchQuery extends TermMatchQuery {
    public HTMLSourceTermMatchQuery() {
        this.setSourceType(MatchSourceType.HTMLSource);
    }

    public TermTupleSetGroup getTupleSet() {
        HTMLParser parser = new HTMLParser();
        try {
            parser.setHtmlPage(m_query);
        } catch (IOException e) {
            Log.util.error("Unable to parser source %s %e", e, e);
            return null;
        }

        try {
            return InverterUtils.getMergedTupleSetFromPage(parser);
        } catch (IOException e) {
            Log.util.error("Unable to get the tuple set %s %e", e, e);
            return null;
        }

    }


}