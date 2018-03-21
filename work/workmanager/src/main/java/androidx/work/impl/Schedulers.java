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

package androidx.work.impl;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.List;

/**
 * Helps schedule {@link androidx.work.impl.model.WorkSpec}s while enforcing
 * {@link Scheduler#MAX_SCHEDULER_LIMIT}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Schedulers {

    /**
     * Schedules {@link WorkSpec}s while honoring the {@link Scheduler#MAX_SCHEDULER_LIMIT}.
     *
     * @param workDatabase The {@link WorkDatabase}.
     * @param schedulers   The {@link List} of {@link Scheduler}s to delegate to.
     */
    public static void schedule(
            @NonNull WorkDatabase workDatabase,
            List<Scheduler> schedulers) {

        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        List<WorkSpec> eligibleWorkSpecs = workSpecDao.getEligibleWorkForScheduling();
        scheduleInternal(workDatabase, schedulers, eligibleWorkSpecs);
    }

    private static void scheduleInternal(
            @NonNull WorkDatabase workDatabase,
            List<Scheduler> schedulers,
            List<WorkSpec> workSpecs) {

        if (workSpecs == null || schedulers == null) {
            return;
        }

        long now = System.currentTimeMillis();
        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        // Mark all the WorkSpecs as scheduled.
        // Calls to Scheduler#schedule() could potentially result in more schedules
        // on a separate thread. Therefore, this needs to be done first.
        workDatabase.beginTransaction();
        try {
            for (WorkSpec workSpec : workSpecs) {
                workSpecDao.markWorkSpecScheduled(workSpec.id, now);
            }
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }
        WorkSpec[] eligibleWorkSpecsArray = workSpecs.toArray(new WorkSpec[0]);
        // Delegate to the underlying scheduler.
        for (Scheduler scheduler : schedulers) {
            scheduler.schedule(eligibleWorkSpecsArray);
        }
    }
}
