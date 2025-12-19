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

import com.hitorro.basetext.text.indexer.DocumentIndexer;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.json.keys.StringProperty;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Jan 17, 2005 Time: 12:57:32 PM
 */
@CommandDef(command = "text.luceneindexoptimize", description = "Optimize a lucene index")
public class IndexOptimizer extends Command {
    @CommandArgument(required = true)
    private StringProperty IndexName = new StringProperty("indexname", "index directory", null);

    public boolean execute(String rawValue, JVS args, Response response, CommandSession session) {
        String name = IndexName.apply(args);
        DocumentIndexer indexer = new DocumentIndexer(name);
        if (indexer.isValid()) {
            try {
                indexer.init(false);
                indexer.markForOptimize();
            } catch (IOException e) {
                this.writeSimpleError(response, "Error optimizing index %s %s, %e", name, e, e);
                return false;
            }
            this.writeSimpleError(response, "Marked for optimization");
        } else {
            this.writeSimpleError(response, "Unable to get index called %s", name);
        }
        response.end();

        return true;
    }
}
