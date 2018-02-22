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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import androidx.work.State;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
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
public class GreedyScheduler implements Scheduler, WorkConstraintsCallback, ExecutionListener {

    private static final String TAG = "GreedyScheduler";

    private WorkManagerImpl mWorkManagerImpl;
    private WorkConstraintsTracker mWorkConstraintsTracker;
    private List<WorkSpec> mConstrainedWorkSpecs = new ArrayList<>();

    public GreedyScheduler(Context context, WorkManagerImpl workManagerImpl) {
        mWorkManagerImpl = workManagerImpl;
        mWorkConstraintsTracker = new WorkConstraintsTracker(context, this);
    }

    @VisibleForTesting
    public GreedyScheduler(WorkManagerImpl workManagerImpl,
            WorkConstraintsTracker workConstraintsTracker) {
        mWorkManagerImpl = workManagerImpl;
        mWorkManagerImpl.getProcessor().addExecutionListener(this);
        mWorkConstraintsTracker = workConstraintsTracker;
    }

    @Override
    public synchronized void schedule(WorkSpec... workSpecs) {
        int originalSize = mConstrainedWorkSpecs.size();

        for (WorkSpec workSpec : workSpecs) {
            if (workSpec.getState() == State.ENQUEUED
                    && !workSpec.isPeriodic()
                    && workSpec.getInitialDelay() == 0L) {
                if (workSpec.hasConstraints()) {
                    Logger.debug(TAG, "Starting tracking for %s", workSpec.getId());
                    mConstrainedWorkSpecs.add(workSpec);
                } else {
                    mWorkManagerImpl.startWork(workSpec.getId());
                }
            }
        }

        if (originalSize != mConstrainedWorkSpecs.size()) {
            mWorkConstraintsTracker.replace(mConstrainedWorkSpecs);
        }
    }

    @Override
    public synchronized void cancel(@NonNull String workSpecId) {
        Logger.debug(TAG, "Cancelling work ID %s", workSpecId);
        mWorkManagerImpl.stopWork(workSpecId);
        removeConstraintTrackingFor(workSpecId);
    }

    @Override
    public synchronized void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            Logger.debug(TAG, "Constraints met: Scheduling work ID %s", workSpecId);
            mWorkManagerImpl.startWork(workSpecId);
        }
    }

    @Override
    public synchronized void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            Logger.debug(TAG, "Constraints not met: Cancelling work ID %s", workSpecId);
            mWorkManagerImpl.stopWork(workSpecId);
        }
    }

    @Override
    public synchronized void onExecuted(@NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {
        removeConstraintTrackingFor(workSpecId);
    }

    private synchronized void removeConstraintTrackingFor(@NonNull String workSpecId) {
        for (int i = 0, size = mConstrainedWorkSpecs.size(); i < size; ++i) {
            if (mConstrainedWorkSpecs.get(i).getId().equals(workSpecId)) {
                Logger.debug(TAG, "Stopping tracking for %s", workSpecId);
                mConstrainedWorkSpecs.remove(i);
                mWorkConstraintsTracker.replace(mConstrainedWorkSpecs);
                break;
            }
        }
    }
}
