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

package androidx.work.testing;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Worker;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A test scheduler that schedules unconstrained, non-timed workers. It intentionally does
 * not acquire any WakeLocks, instead trying to brute-force them as time allows before the process
 * gets killed.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TestScheduler implements Scheduler, ExecutionListener {

    private final Context mContext;
    private final Map<String, InternalWorkState> mPendingWorkStates;
    private final Map<String, InternalWorkState> mTerminatedWorkStates;

    TestScheduler(@NonNull Context context) {
        mContext = context;
        mPendingWorkStates = new HashMap<>();
        mTerminatedWorkStates = new HashMap<>();
    }

    @Override
    public boolean hasLimitedSchedulingSlots() {
        return true;
    }

    @Override
    public void schedule(@NonNull WorkSpec... workSpecs) {
        if (workSpecs == null || workSpecs.length <= 0) {
            return;
        }

        List<String> workSpecIdsToSchedule = new ArrayList<>(workSpecs.length);
        for (WorkSpec workSpec : workSpecs) {
            if (!mPendingWorkStates.containsKey(workSpec.id)) {
                mPendingWorkStates.put(workSpec.id, new InternalWorkState(mContext, workSpec));
            }
            workSpecIdsToSchedule.add(workSpec.id);
        }
        scheduleInternal(workSpecIdsToSchedule);
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        InternalWorkState internalWorkState = mPendingWorkStates.get(workSpecId);
        // We don't need to keep track of cancelled workSpecs. This is because subsequent calls
        // to enqueue() will no-op because insertWorkSpec in WorkDatabase has a conflict
        // policy of @Ignore. So TestScheduler will _never_ be asked to schedule those
        // WorkSpecs.
        WorkManagerImpl.getInstance(mContext).stopWork(workSpecId);
        if (internalWorkState != null && !internalWorkState.mWorkSpec.isPeriodic()) {
            // Don't remove PeriodicWorkRequests from the list of pending work states.
            // This is because we keep track of mPeriodDelayMet for PeriodicWorkRequests.
            // `mPeriodDelayMet` is set to `false` when `onExecuted()` is called as a result of a
            // successful run or a cancellation. That way subsequent calls to schedule() no-op
            // until a developer explicitly calls setPeriodDelayMet().
            mPendingWorkStates.remove(workSpecId);
        }
    }

    /**
     * Tells the {@link TestScheduler} to pretend that all constraints on the {@link Worker} with
     * the given {@code workSpecId} are met.
     *
     * @param workSpecId The {@link Worker}'s id
     * @throws IllegalArgumentException if {@code workSpecId} is not enqueued
     */
    void setAllConstraintsMet(@NonNull UUID workSpecId) {
        String id = workSpecId.toString();
        if (!mTerminatedWorkStates.containsKey(id)) {
            InternalWorkState internalWorkState = mPendingWorkStates.get(id);
            if (internalWorkState == null) {
                throw new IllegalArgumentException(
                        "Work with id " + workSpecId + " is not enqueued!");
            }
            internalWorkState.mConstraintsMet = true;
            scheduleInternal(Collections.singletonList(workSpecId.toString()));
        }
    }

    /**
     * Tells the {@link TestScheduler} to pretend that the initial delay on the {@link Worker} with
     * the given {@code workSpecId} are met.
     *
     * @param workSpecId The {@link Worker}'s id
     * @throws IllegalArgumentException if {@code workSpecId} is not enqueued
     */
    void setInitialDelayMet(@NonNull UUID workSpecId) {
        String id = workSpecId.toString();
        if (!mTerminatedWorkStates.containsKey(id)) {
            InternalWorkState internalWorkState = mPendingWorkStates.get(id);
            if (internalWorkState == null) {
                throw new IllegalArgumentException(
                        "Work with id " + workSpecId + " is not enqueued!");
            }
            internalWorkState.mInitialDelayMet = true;
            scheduleInternal(Collections.singletonList(workSpecId.toString()));
        }
    }

    /**
     * Tells the {@link TestScheduler} to pretend that the periodic delay on the {@link Worker} with
     * the given {@code workSpecId} are met.
     *
     * @param workSpecId The {@link Worker}'s id
     * @throws IllegalArgumentException if {@code workSpecId} is not enqueued
     */
    void setPeriodDelayMet(@NonNull UUID workSpecId) {
        String id = workSpecId.toString();
        InternalWorkState internalWorkState = mPendingWorkStates.get(id);
        if (internalWorkState == null) {
            throw new IllegalArgumentException(
                    "Work with id " + workSpecId + " is not enqueued!");
        }
        internalWorkState.mPeriodDelayMet = true;
        scheduleInternal(Collections.singletonList(workSpecId.toString()));
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        InternalWorkState internalWorkState = mPendingWorkStates.get(workSpecId);
        if (internalWorkState != null) {
            if (internalWorkState.mWorkSpec.isPeriodic()) {
                internalWorkState.reset();
            } else {
                mTerminatedWorkStates.put(workSpecId, internalWorkState);
                mPendingWorkStates.remove(workSpecId);
            }
        }
    }

    private void scheduleInternal(Collection<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            InternalWorkState internalWorkState = mPendingWorkStates.get(workSpecId);
            if (internalWorkState.isRunnable()) {
                WorkManagerImpl.getInstance(mContext).startWork(workSpecId);
            }
        }
    }

    /**
     * A class to keep track of a WorkRequest's internal state.
     */
    private static class InternalWorkState {

        @NonNull Context mContext;
        @NonNull WorkSpec mWorkSpec;
        boolean mConstraintsMet;
        boolean mInitialDelayMet;
        boolean mPeriodDelayMet;

        InternalWorkState(@NonNull Context context, @NonNull WorkSpec workSpec) {
            mContext = context;
            mWorkSpec = workSpec;
            mConstraintsMet = !mWorkSpec.hasConstraints();
            mInitialDelayMet = (mWorkSpec.initialDelay == 0L);
            mPeriodDelayMet = true;
        }

        void reset() {
            mConstraintsMet = !mWorkSpec.hasConstraints();
            mPeriodDelayMet = !mWorkSpec.isPeriodic();
            if (mWorkSpec.isPeriodic()) {
                // Reset the startTime to simulate the first run of PeriodicWork.
                // Otherwise WorkerWrapper de-dupes runs of PeriodicWork to 1 execution per interval
                WorkManagerImpl workManager = WorkManagerImpl.getInstance(mContext);
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                workDatabase.workSpecDao().setPeriodStartTime(mWorkSpec.id, 0);
            }
        }

        boolean isRunnable() {
            return (mConstraintsMet && mInitialDelayMet && mPeriodDelayMet);
        }
    }
}
