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
package com.hitorro.obj.core;

import com.hitorro.base.objects.Post;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basetext.indexer.AllTypesAnalyzerCache;
import com.hitorro.basetext.indexer.IndexerFieldAdapter;
import com.hitorro.basetext.inverter.TermTuple;
import com.hitorro.basetext.inverter.TermTupleSet;
import com.hitorro.basetext.inverter.TermTupleSetGroup;
import com.hitorro.basetext.termmatch.TermMatchQuery;
import com.hitorro.language.Iso639Table;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.ResetableStringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.List;


public class LuceneUtils {
    public static final Version CurrentVersion = Version.LUCENE_9_9_2;

    // this doesnt work
    public static final PhraseQuery.Builder getPhraseQuery(String field, String text) throws IOException {
        GenericAnalyzer m_defaultAnalyzer = new GenericAnalyzer(FilterBits.StandardFilters, Iso639Table.english, GenericAnalyzer.Mode.Index);
        ResetableStringReader m_reader = new ResetableStringReader(null);
        if (StringUtil.nullOrEmptyOrBlankString(text)) {
            return null;
        }

        m_reader.set(text);
        TokenStream ts = m_defaultAnalyzer.tokenStream(field, m_reader);
        CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        while (ts.incrementToken()) {
            String term = termAttribute.toString();
            builder.add(new Term(field, term), 6);
        }
        ts.close();
        return builder;
    }

    // basic query
    public static final BooleanQuery.Builder getQuery(String field, String text, BooleanClause.Occur boolClause) throws IOException {
        GenericAnalyzer m_defaultAnalyzer = new GenericAnalyzer(FilterBits.DefaultFilters, Iso639Table.english, GenericAnalyzer.Mode.Index);
        ResetableStringReader m_reader = new ResetableStringReader(null);
        if (StringUtil.nullOrEmptyOrBlankString(text)) {
            return null;
        }

        m_reader.set(text);
        TokenStream ts = m_defaultAnalyzer.tokenStream(field, m_reader);
        CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        while (ts.incrementToken()) {
            String t = termAttribute.toString();
            builder.add(new TermQuery(new Term(field, t)), boolClause);
        }
        ts.close();
        return builder;
    }

    /**
     * TermMatchQuery contains terms selected from a page we want to represent as a query Q.  We take n terms from this
     * query and construct a boolean query out of it.
     *
     * @param query
     * @param maxTerms
     * @param termsUsed
     * @param section
     * @param luceneField
     * @param booleanClause
     * @return boolean query
     */
    public static final BooleanQuery.Builder getBooleanQueryFromTermMatchQuery(TermMatchQuery query, int maxTerms,
                                                                               List<String> termsUsed, String section,
                                                                               String luceneField,
                                                                               BooleanClause.Occur booleanClause) {

        TermTupleSetGroup setGroup = query.getTupleSet();

        if (setGroup == null) {
            return null;
        }
        TermTupleSet<TermTuple> set = setGroup.getByName(section);
        if (set == null) {

            return null;
        }
        return getBooleanQueryFromTermTupleSet(set, maxTerms, termsUsed, luceneField, booleanClause);
    }

    /**
     * @param set
     * @param maxTerms
     * @param termsUsed
     * @param luceneField
     * @param booleanClause
     * @return
     */
    public static final BooleanQuery.Builder getBooleanQueryFromTermTupleSet(TermTupleSet<TermTuple> set,
                                                                             int maxTerms,
                                                                             List<String> termsUsed,
                                                                             String luceneField,
                                                                             BooleanClause.Occur booleanClause) {
        int count = 0;
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (TermTuple tt : set.getTuplesList()) {
            if (tt.isGood()) {
                if (count >= maxTerms) {
                    break;
                }
                count++;
                if (termsUsed != null) {
                    termsUsed.add(tt.getTerm());
                }
                TermQuery tq = new TermQuery(new Term(luceneField, tt.getTerm()));

                //tq.setBoost(maxTerms - count);
                builder.add(tq, booleanClause);
            }
        }
        return builder;
    }

    public static final Query getStringLiteralQuery(String field, String text, boolean lowerCase) {
        if (lowerCase) {
            text = text.toLowerCase();
        }
        return new TermQuery(new Term(field, text));
    }

    public static final BooleanQuery.Builder getBooleanQueryFreeText(String field, String text, String contentField, String contentFieldString, BooleanClause.Occur booleanClause) throws IOException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        GenericAnalyzer m_defaultAnalyzer = new GenericAnalyzer(FilterBits.DefaultFilters, Iso639Table.english, GenericAnalyzer.Mode.Index);
        ResetableStringReader m_reader = new ResetableStringReader(null);
        addTerms(m_reader, text, m_defaultAnalyzer, field, builder, booleanClause);
        addTerms(m_reader, contentFieldString, m_defaultAnalyzer, contentField, builder, booleanClause);
        return builder;
    }

    private static void addTerms(ResetableStringReader m_reader, String text, GenericAnalyzer m_defaultAnalyzer, String field, BooleanQuery.Builder builder, BooleanClause.Occur booleanClause) throws IOException {
        if (StringUtil.nullOrEmptyOrBlankString(text)) {
            return;
        }

        m_reader.set(text);
        TokenStream ts = m_defaultAnalyzer.tokenStream(field, m_reader);
        CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
        while (ts.incrementToken()) {
            builder.add(new TermQuery(new Term(field, termAttribute.toString())), booleanClause);
        }
    }

    public static final QueryParser getQueryParser() {
        Analyzer analyzers = AllTypesAnalyzerCache.getCache().get(Iso639Table.english);
        if (analyzers == null) {
            return null;
        }
        return new QueryParser(IndexerFieldAdapter.DEFAULT_SEARCH_FIELD, analyzers);
    }

    /**
     * put a constraint to a master constraint that states that it must have 0-depth for its postDegree field
     *
     * @param builder
     * @param depth
     */
    public static final void addMaxPostdegree(BooleanQuery.Builder builder, int depth) {
        BooleanQuery.Builder b1 = new BooleanQuery.Builder();
        for (int i = 0; i <= depth; i++) {
            b1.add(new TermQuery(new Term(Post.PostDegreeKey, Integer.toString(i))), BooleanClause.Occur.SHOULD);
        }
        builder.add(b1.build(), BooleanClause.Occur.MUST);
    }

    /**
     * Constrain the search to something of the provided type or subtype
     *
     * @param builder
     * @param typeNameShort
     */
    public static final void addTypeMustBeClassOrSubclassOf(BooleanQuery.Builder builder, String typeNameShort, boolean includeSubtype) {
        if (includeSubtype) {
            builder.add(new TermQuery(new Term(VersionableObject.TypeOrSubtypeKey, typeNameShort)), BooleanClause.Occur.MUST);
        } else {
            builder.add(new TermQuery(new Term(VersionableObject.TypeKey, typeNameShort)), BooleanClause.Occur.MUST);
        }
    }

    /**
     * Constrain the search to something of the provided type or subtype
     *
     * @param builder
     * @param typeNameShort
     */
    public static final void addMustBeType(BooleanQuery.Builder builder, String typeNameShort) {
        builder.add(new TermQuery(new Term(VersionableObject.TypeKey, typeNameShort)), BooleanClause.Occur.MUST);
    }


}
