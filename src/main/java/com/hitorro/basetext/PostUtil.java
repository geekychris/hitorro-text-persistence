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
import com.hitorro.basetext.termmatch.Ranker;
import com.hitorro.basetext.termmatch.RankerResult;
import com.hitorro.basetext.termmatch.RankerResultRow;
import com.hitorro.util.core.GenericKeyValue;
import com.hitorro.util.core.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chris on 3/5/16.
 */
public class PostUtil {
    public static List<GenericKeyValue<String, Double>> getSimilarDocuments(Post post, int maxResults, float minRank, int maxTerms) {
        Ranker ranker = new Ranker();
        PostTermMatchQuery query = new PostTermMatchQuery(post);

        RankerResult result = ranker.rank(query, maxResults, minRank, maxTerms);
        if (result == null) {
            return null;
        }
        if (!StringUtil.nullOrEmptyString(result.getError())) {
            return null;
        }
        List<RankerResultRow> rows = result.getRows();
        List<GenericKeyValue<String, Double>> list = new ArrayList<GenericKeyValue<String, Double>>();
        for (RankerResultRow row : rows) {
            list.add(new GenericKeyValue<String, Double>(row.getGuid(), new Double(row.getScore())));
        }
        return list;
    }
}
