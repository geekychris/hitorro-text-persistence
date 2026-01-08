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

import com.hitorro.basetext.indexer.AllTypesAnalyzerCache;
import com.hitorro.basetext.indexer.IndexerFieldAdapter;
import com.hitorro.basetext.indexer.SearcherCache;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.language.Iso639Table;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ResponseShape;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.json.keys.StringProperty;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;

/**
 */
@CommandDef(command = "text.searchluceneindex", description = "Search a lucene index")
public class SearchCommand extends Command {
    @CommandArgument(required = true)
    private StringProperty QueryString = new StringProperty("query", "lucene query", null);
    @CommandArgument(required = true)
    private StringProperty IndexName = new StringProperty("indexname", "Index Name", null);

    @ResponseDefinition(command = "search",
            rowname = "doc",
            columns = {@RespColumn(name = "Guid", lName = "guid")})
    private ResponseShape shape = new ResponseShape();

    public boolean execute(String rawValue, JVS args, Response response, CommandSession session) {
        String query = QueryString.apply(args);
        String indexName = IndexName.apply(args);
        IndexSearcher s = SearcherCache.getCache().get(indexName);
        if (s == null) {
            this.writeSimpleError(response, "Could not find index %s", indexName);
            return false;
        }
        Analyzer analyzers = AllTypesAnalyzerCache.getCache().get(Iso639Table.english);
        if (analyzers == null) {
            writeSimpleError(response, "Could not get analyzers");
            return false;
        }
        QueryParser qp = new QueryParser(IndexerFieldAdapter.DEFAULT_SEARCH_FIELD, analyzers);
        Query q = null;
        try {

            q = qp.parse(query);
            TopDocs td = s.search(q, 1000);

            long totalCount = td.totalHits.value;
            ScoreDoc[] sd = td.scoreDocs;
            response.setResponseShape(shape);
            for (int i = 0; i < totalCount; i++) {
                Document doc = s.doc(sd[i].doc);
                String guid = doc.get("guid");
                response.addRow(guid);
            }
        } catch (ParseException e) {
            writeSimpleError(response, "Could not search index %s, %s %e", indexName, e, e);
            return false;
        } catch (IOException e) {
            writeSimpleError(response, "Could not search index %s, %s %e", indexName, e, e);
            return false;
        }

        response.end();
        return true;
    }
}
