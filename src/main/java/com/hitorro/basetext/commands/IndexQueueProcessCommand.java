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
package com.hitorro.basetext.commands;

import com.hitorro.base.docprocessing.commands.QueueProcessCommand;
import com.hitorro.basetext.text.indexer.DocumentIndexer;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.json.keys.FileProperty;
import com.hitorro.util.typesystem.BaseType;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Generate an index from the queue:
 * <p/>
 * queue.index startdate=091016 path=file:///hthome/queue/docs/ indexdir=/tstindex enddate=091018;
 */
@CommandDef(command = "queue.index", description = "Process queue elements indexing them")
public class IndexQueueProcessCommand extends QueueProcessCommand {
    @CommandArgument(required = true)
    public static final FileProperty indexDirKey = new FileProperty("indexdir", "index directory");

    @Override
    public boolean process(final Iterator<BaseType> iter, final String rawValue, final JVS args, final Response response, final CommandSession session) throws IOException {
        File dir = indexDirKey.apply(args);
        FileUtil.ensureDirectoryExists(dir);
        DocumentIndexer indexer = new DocumentIndexer("index", dir);
        indexer.init(true);
        boolean indexed = false;

        indexed = indexer.indexBaseTypeIterator(iter);

        indexer.optimize();
        indexer.close();
        return indexed;
    }
}
