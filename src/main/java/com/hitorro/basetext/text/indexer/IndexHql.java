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
import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;

import java.io.IOException;

/**
 * <p/>
 * Debug mechanism to take a set of objects and
 */
@CommandDef(command = "dms.indexhql", description = "Index some documents")
public class IndexHql extends Command {
    @CommandArgument(required = true)
    private StringProperty Hql = new StringProperty("hql", "hql command", null);
    @CommandArgument(required = true)
    private StringProperty IndexName = new StringProperty("indexname", "index directory", null);

    public boolean execute(String rawValue, JsonNode args, Response response, CommandSession session) {
        String hql = Hql.apply(args);
        String name = IndexName.apply(args);
        BaseSession sess = DMSSessionFactory.getFactory().getSession();
        try {
            DocumentIndexCollection col = new DocumentIndexCollection(name);
            if (!col.init(sess, hql, false)) {
                this.writeSimpleError(response, "Unable to get index called %s", name);
                return false;
            }

            try {
                int count = col.index(8000);
                col.close();
                this.writeSuccess(response, "Wrote %s", Integer.toString(count));
            } catch (IOException io) {

                this.writeSimpleError(response, "Unable to index documents %s %e", io, io);
            }

            response.end();
        } finally {

            DMSSessionFactory.closeSession(sess);
        }
        return true;
    }
}