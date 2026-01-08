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

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ResponseShape;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.json.keys.StringProperty;

/**
 */
//TODO UPDATE
@CommandDef(command = "text.dumpterms", description = "Search a lucene index")
public class DumpTerms extends Command {
    @CommandArgument(required = true)
    private StringProperty IndexName = new StringProperty("indexname", "Index Name", null);
    @CommandArgument(required = true)
    private StringProperty Prefix = new StringProperty("prefix", "Index Name", null);

    @ResponseDefinition(command = "types",
            rowname = "type",
            columns = {@RespColumn(name = "Field", lName = "field"),
                    @RespColumn(name = "Value", lName = "value"),
                    @RespColumn(name = "Term", lName = "term")})
    private ResponseShape shape = new ResponseShape();

    public boolean execute(String rawValue, JVS args, Response response, CommandSession session) {
       /* String prefix = Prefix.apply(args);
        String indexName = IndexName.apply(args);
        IndexSearcher s = SearcherCache.getCache().get(indexName);

        try
        {
            TermEnum te = s.getIndexReader().terms();
            if (s == null)
            {
                this.writeSimpleError(response, "Could not find index %s", indexName);
                return false;
            }
            response.setResponseShape(shape);
            while (te.next())
            {
                Term term = te.term();


                if (term.field().startsWith(prefix))
                {
                    response.addRow(term.field(), term.text(), Integer.toString(te.docFreq()));
                }
            }
        }

        catch (IOException e)
        {
            writeSimpleError(response, "Could not get enum %s, %s %e", indexName, e, e);
            return false;
        }

        response.end();
        */
        return true;
    }
}