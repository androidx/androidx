/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.arch.background.workmanager.background;

import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.Processor;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A {@link Processor} that handles execution of work in the background.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BackgroundProcessor extends Processor {

    private final ExecutionListener mOuterListener;

    public BackgroundProcessor(
            Context appContext,
            WorkDatabase workDatabase,
            Scheduler scheduler,
            ExecutionListener outerListener) {
        // TODO(sumir): Be more intelligent about the executor.
        this(
                appContext,
                workDatabase,
                scheduler,
                outerListener,
                Executors.newSingleThreadScheduledExecutor());
    }

    @VisibleForTesting
    BackgroundProcessor(
            Context appContext,
            WorkDatabase workDatabase,
            Scheduler scheduler,
            ExecutionListener outerListener,
            ScheduledExecutorService executorService) {
        super(appContext, workDatabase, scheduler, executorService);
        mOuterListener = outerListener;
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        super.onExecuted(workSpecId, needsReschedule);
        if (mOuterListener != null) {
            mOuterListener.onExecuted(workSpecId, needsReschedule);
        }
    }
}
