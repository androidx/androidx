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
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.WorkRunId;

/**
 * A {@link Runnable} that requests {@link androidx.work.impl.Processor} to stop the work
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StopWorkRunnable implements Runnable {

    private static final String TAG = Logger.tagWithPrefix("StopWorkRunnable");

    private final WorkManagerImpl mWorkManagerImpl;
    private final WorkRunId mWorkSpecId;
    private final boolean mStopInForeground;

    public StopWorkRunnable(
            @NonNull WorkManagerImpl workManagerImpl,
            @NonNull WorkRunId workSpecId,
            boolean stopInForeground) {
        mWorkManagerImpl = workManagerImpl;
        mWorkSpecId = workSpecId;
        mStopInForeground = stopInForeground;
    }
    @Override
    public void run() {
        boolean isStopped;
        if (mStopInForeground) {
            isStopped = mWorkManagerImpl
                    .getProcessor()
                    .stopForegroundWork(mWorkSpecId.getWorkSpecId());
        } else {
            // This call is safe to make for foreground work because Processor ignores requests
            // to stop for foreground work.
            isStopped = mWorkManagerImpl
                    .getProcessor()
                    .stopWork(mWorkSpecId);
        }

        Logger.get().debug(
                TAG,
                "StopWorkRunnable for " + mWorkSpecId.getWorkSpecId() + "; Processor.stopWork = "
                        + isStopped);

    }
}
