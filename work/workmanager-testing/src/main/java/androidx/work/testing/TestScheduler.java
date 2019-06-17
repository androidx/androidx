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
    private final Map<String, InternalWorkState> mInternalWorkStates;

    private static final Object sLock = new Object();

    TestScheduler(@NonNull Context context) {
        mContext = context;
        mInternalWorkStates = new HashMap<>();
    }

    @Override
    public void schedule(@NonNull WorkSpec... workSpecs) {
        if (workSpecs == null || workSpecs.length <= 0) {
            return;
        }

        synchronized (sLock) {
            List<String> workSpecIdsToSchedule = new ArrayList<>(workSpecs.length);
            for (WorkSpec workSpec : workSpecs) {
                if (!mInternalWorkStates.containsKey(workSpec.id)) {
                    mInternalWorkStates.put(workSpec.id, new InternalWorkState(mContext, workSpec));
                }
                workSpecIdsToSchedule.add(workSpec.id);
            }
            scheduleInternal(workSpecIdsToSchedule);
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        synchronized (sLock) {
            WorkManagerImpl.getInstance(mContext).stopWork(workSpecId);
            mInternalWorkStates.remove(workSpecId);
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
        synchronized (sLock) {
            InternalWorkState internalWorkState = mInternalWorkStates.get(workSpecId.toString());
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
        synchronized (sLock) {
            InternalWorkState internalWorkState = mInternalWorkStates.get(workSpecId.toString());
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
        synchronized (sLock) {
            InternalWorkState internalWorkState = mInternalWorkStates.get(workSpecId.toString());
            if (internalWorkState == null) {
                throw new IllegalArgumentException(
                        "Work with id " + workSpecId + " is not enqueued!");
            }
            internalWorkState.mPeriodDelayMet = true;
            scheduleInternal(Collections.singletonList(workSpecId.toString()));
        }
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {

        synchronized (sLock) {
            InternalWorkState internalWorkState = mInternalWorkStates.get(workSpecId);
            if (internalWorkState.mWorkSpec.isPeriodic()) {
                internalWorkState.reset();
            } else {
                mInternalWorkStates.remove(workSpecId);
            }
        }
    }

    private void scheduleInternal(Collection<String> workSpecIds) {
        for (String workSpecId : workSpecIds) {
            InternalWorkState internalWorkState = mInternalWorkStates.get(workSpecId);
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
