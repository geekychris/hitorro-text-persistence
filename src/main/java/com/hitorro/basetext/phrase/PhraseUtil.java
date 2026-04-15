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
package com.hitorro.basetext.phrase;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.Timer;
import com.hitorro.util.core.iterator.AbstractIterator;
import com.hitorro.util.core.params.HTProperties;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.largedata.iterator.HTSerializableInputIterator;
// BaseFileResourceCache and BaseFileResourceContext removed during cleanup
import com.hitorro.util.typesystem.BaseSession;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


public class PhraseUtil {

    /**
     * Generate a phrase index in the resource cache if the phrase listFiles exists also in the resource cache.
     *
     * @param min
     * @param max
     * @param desc
     * @param query
     * @param writeDumpText
     * @return
     * @throws IOException
     */


    public static boolean writePhrasesFromDBIntoResourceCache(String query, String analyzer, boolean generateSortedFile) throws Exception {
        // BaseFileResourceCache removed during cleanup - resource cache no longer available
        return false;
    }

    public static boolean writePhrasesFromDB(BaseFile dir, int bucketSize,
                                                   int mergeSize, String analyzers,
                                                   String query, boolean createSortedFile) throws Exception {
        BaseFile target = dir.getChild(PhraseUtilBasic.AlphaNumeric);
        if (!target.exists()) {
            target = writePhrasesFromDB(dir, query, analyzers, bucketSize, mergeSize, 3, 1, target);
        }
        if (target.exists()) {

            BaseFile frequencySortedFile = dir.getChild(PhraseUtilBasic.PhraseFreq);
            if (createSortedFile && !frequencySortedFile.exists()) {
                frequencySortedFile = mergePhraseFiles(dir, target, bucketSize, mergeSize, frequencySortedFile, true);
            }
            return true;
        }
        return false;

    }

    public static BaseFile writePhrasesFromDB(BaseFile directory,
                                                    String query,
                                                    String analyzer,
                                                    int bucketSize,
                                                    int maxMergeFiles,
                                                    int maxPhraseDepth,
                                                    int minDepth,
                                                    BaseFile targetFile) throws Exception {
        BaseFile f = PhraseUtil.writePhrases(query, directory, "phrase", maxPhraseDepth, minDepth, analyzer, bucketSize, maxMergeFiles);

        f.renameTo(targetFile);
        return targetFile;
    }

    public static BaseFile mergePhraseFiles(BaseFile dir, BaseFile inputFile, int bucketSize, int fileMergeCount, BaseFile outputFile, boolean deleteInputs) throws Exception {
        String fileExtension = "phraseFreq";
        PhraseUtilBasic.mergePhraseElementByFrequency(inputFile, dir, fileExtension, fileMergeCount, deleteInputs);
        outputFile.renameTo(outputFile);
        return outputFile;
    }


    public static boolean dumpHistogram(File inputFile, File outputFile) throws Exception {
        HTSerializableInputIterator<PhraseElement, DMSSession> iter = new HTSerializableInputIterator<PhraseElement, DMSSession>(inputFile, null, false);

        PrintWriter pw = FileUtil.getBufferedPrintWriterFromFile(outputFile);
        int currVal = -1;
        int freq = 0;
        pw.println("Freq, PhrasesWithThatFreq");
        while (iter.hasNext()) {
            PhraseElement pe = iter.next();
            if (pe.getFrequency() == currVal) {
                freq++;
            } else {
                if (currVal != -1) {
                    pw.print(currVal);
                    pw.print(" ");
                    pw.println(freq);
                }
                freq = 1;
                currVal = pe.getFrequency();
            }
        }
        iter.close();
        pw.flush();
        pw.close();
        return true;
    }

    public static BaseFile writePhrases(String query,
                                              BaseFile dir,
                                              String fileExtension,
                                              int phraseDepth,
                                              int minDepth,
                                              String analyzers,
                                              int phrasesPerBucket,
                                              int mergeFilesLimit)
            throws Exception {
        BaseSession q = DMSSessionFactory.getFactory().getSession();
        BaseSession f = DMSSessionFactory.getFactory().getSession();
        GetDocumentText adapter = new GetDocumentText(f);

        try {
            AbstractIterator<String> textIter = ((DMSSession) q).getIteratorFromQuery(adapter, query);

            return PhraseUtilBasic.writePhrases(textIter, dir, fileExtension, phraseDepth, minDepth, analyzers, phrasesPerBucket, mergeFilesLimit);
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(q);
            DMSSessionFactory.getFactory().rollbackClose(f);
        }
    }


}

