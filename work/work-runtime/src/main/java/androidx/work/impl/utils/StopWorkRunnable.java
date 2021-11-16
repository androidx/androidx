/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl.utils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.impl.Processor;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpecDao;

/**
 * A {@link Runnable} that can stop work and set the {@link WorkInfo.State} to
 * {@link WorkInfo.State#ENQUEUED} if it's in {@link WorkInfo.State#RUNNING}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StopWorkRunnable implements Runnable {

    private static final String TAG = Logger.tagWithPrefix("StopWorkRunnable");

    private final WorkManagerImpl mWorkManagerImpl;
    private final String mWorkSpecId;
    private final boolean mStopInForeground;

    public StopWorkRunnable(
            @NonNull WorkManagerImpl workManagerImpl,
            @NonNull String workSpecId,
            boolean stopInForeground) {
        mWorkManagerImpl = workManagerImpl;
        mWorkSpecId = workSpecId;
        mStopInForeground = stopInForeground;
    }

    @Override
    public void run() {
        WorkDatabase workDatabase = mWorkManagerImpl.getWorkDatabase();
        Processor processor = mWorkManagerImpl.getProcessor();
        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        workDatabase.beginTransaction();
        try {
            boolean isForegroundWork = processor.isEnqueuedInForeground(mWorkSpecId);
            boolean isStopped;
            if (mStopInForeground) {
                isStopped = mWorkManagerImpl
                        .getProcessor()
                        .stopForegroundWork(mWorkSpecId);
            } else {
                if (!isForegroundWork
                        && workSpecDao.getState(mWorkSpecId) == WorkInfo.State.RUNNING) {
                    workSpecDao.setState(WorkInfo.State.ENQUEUED, mWorkSpecId);
                }
                // This call is safe to make for foreground work because Processor ignores requests
                // to stop for foreground work.
                isStopped = mWorkManagerImpl
                        .getProcessor()
                        .stopWork(mWorkSpecId);
            }

            Logger.get().debug(
                    TAG,
                    "StopWorkRunnable for " + mWorkSpecId + "; Processor.stopWork = " + isStopped);

            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }
    }
}
