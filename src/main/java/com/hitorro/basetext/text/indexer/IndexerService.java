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

import com.hitorro.basedms.RichDMSService;
import com.hitorro.basedms.scheduler.SchedulerService;
import com.hitorro.basetext.commands.DumpTerms;
import com.hitorro.basetext.commands.SearchCommand;
import com.hitorro.basetext.indexer.FullTextIndex;
import com.hitorro.obj.core.TextService;
import com.hitorro.basetext.indexer.IndexOptimizer;
import com.hitorro.basetext.commands.IndexQueueProcessCommand;
import com.hitorro.util.startupframework.phases.ServiceDefinition;

/**
 */
@ServiceDefinition(dependentService = {SchedulerService.class, RichDMSService.class,
        IndexerService.class, TextService.class},
        shortName = "indexer",
        description = "indexer service",
        debugCommands = {IndexHql.class, SearchCommand.class, DumpTerms.class,
                IndexOptimizer.class, IndexQueueProcessCommand.class},
        typeManagedClasses = {FullTextIndex.class},
        uiDirectories = {})
public class IndexerService {
    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        SchedulerService.getService().scheduleJobs("indexer");
        return null;
    }

    public String deInit() {
        return null;
    }

    public String start(boolean dbInit) {
        return null;
    }
}
