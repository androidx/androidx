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

package androidx.work.test;


import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    private static final String TAG = "TestScheduler";

    private final Map<String, WorkSpec> mWorkSpecs;

    private static final Object sLock = new Object();

    TestScheduler() {
        mWorkSpecs = new HashMap<>();
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        if (workSpecs == null || workSpecs.length <= 0) {
            return;
        }

        synchronized (sLock) {
            for (WorkSpec workSpec : workSpecs) {
                mWorkSpecs.put(workSpec.id, workSpec);
            }
        }
        scheduleInternal(Arrays.asList(workSpecs), true);
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        synchronized (sLock) {
            WorkManagerImpl.getInstance().stopWork(workSpecId);
        }
    }

    /**
     * Tells the {@link TestScheduler} to pretend that all constraints on the {@link Worker} with
     * the given {@code workSpecId} are met.
     *
     * @param workSpecId The {@link Worker}'s id.
     */
    void setAllConstraintsMet(@NonNull UUID workSpecId) {
        synchronized (sLock) {
            WorkSpec workSpec = mWorkSpecs.get(workSpecId.toString());
            if (workSpec != null) {
                scheduleInternal(Collections.singletonList(workSpec), false);
            }
        }
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {

        synchronized (sLock) {
            mWorkSpecs.remove(workSpecId);
        }
    }

    private static void scheduleInternal(
            Collection<WorkSpec> workSpecs,
            boolean enforcingConstraints) {

        for (WorkSpec workSpec : workSpecs) {
            if (!enforcingConstraints || !workSpec.hasConstraints()) {
                if (workSpec.isPeriodic()) {
                    Log.w(TAG, String.format(
                            "Worker (%s) is Periodic. %s will only run once when testing.",
                            workSpec.id, workSpec.workerClassName));
                }

                if (workSpec.initialDelay > 0) {
                    Log.w(TAG, String.format(
                            "Worker (%s, %s) has an initial delay."
                                    + " This will be ignored when testing.",
                            workSpec.id, workSpec.workerClassName));
                }
                WorkManagerImpl.getInstance()
                        .startWork(workSpec.id);
            }
        }
    }
}
