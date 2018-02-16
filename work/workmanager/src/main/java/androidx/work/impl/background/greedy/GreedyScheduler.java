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

package androidx.work.impl.background.greedy;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.State;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.model.WorkSpec;

/**
 * A greedy {@link Scheduler} that schedules unconstrained, non-timed work.  It intentionally does
 * not acquire any WakeLocks, instead trying to brute-force them as time allows before the process
 * gets killed.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GreedyScheduler implements Scheduler {

    private static final String TAG = "GreedyScheduler";

    private WorkManagerImpl mWorkManagerImpl;

    public GreedyScheduler(WorkManagerImpl workManagerImpl) {
        mWorkManagerImpl = workManagerImpl;
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            if (workSpec.getState() == State.ENQUEUED
                    && !workSpec.isPeriodic()
                    && workSpec.getInitialDelay() == 0L
                    && !workSpec.hasConstraints()) {
                Logger.debug(TAG, "Scheduling work ID %s", workSpec.getId());
                mWorkManagerImpl.startWork(workSpec.getId());
            }
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        Logger.debug(TAG, "Cancelling work ID %s", workSpecId);
        mWorkManagerImpl.stopWork(workSpecId);
    }
}
