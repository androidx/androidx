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

import static androidx.work.impl.Scheduler.MAX_GREEDY_SCHEDULER_LIMIT;
import static androidx.work.impl.utils.PackageManagerHelper.setComponentEnabled;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.impl.background.systemalarm.SystemAlarmScheduler;
import androidx.work.impl.background.systemalarm.SystemAlarmService;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.background.systemjob.SystemJobService;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.List;

/**
 * Helper methods for {@link Scheduler}s.
 *
 * Helps schedule {@link androidx.work.impl.model.WorkSpec}s while enforcing
 * {@link Scheduler#MAX_SCHEDULER_LIMIT}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Schedulers {

    public static final String GCM_SCHEDULER = "androidx.work.impl.background.gcm.GcmScheduler";
    private static final String TAG = Logger.tagWithPrefix("Schedulers");

    /**
     * Schedules {@link WorkSpec}s while honoring the {@link Scheduler#MAX_SCHEDULER_LIMIT}.
     *
     * @param workDatabase The {@link WorkDatabase}.
     * @param schedulers   The {@link List} of {@link Scheduler}s to delegate to.
     */
    public static void schedule(
            @NonNull Configuration configuration,
            @NonNull WorkDatabase workDatabase,
            List<Scheduler> schedulers) {
        if (schedulers == null || schedulers.size() == 0) {
            return;
        }

        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        List<WorkSpec> eligibleWorkSpecsForLimitedSlots;
        List<WorkSpec> allEligibleWorkSpecs;

        workDatabase.beginTransaction();
        try {
            // Enqueued workSpecs when scheduling limits are applicable.
            eligibleWorkSpecsForLimitedSlots = workSpecDao.getEligibleWorkForScheduling(
                    configuration.getMaxSchedulerLimit());

            // Enqueued workSpecs when scheduling limits are NOT applicable.
            allEligibleWorkSpecs = workSpecDao.getAllEligibleWorkSpecsForScheduling(
                    MAX_GREEDY_SCHEDULER_LIMIT);

            if (eligibleWorkSpecsForLimitedSlots != null
                    && eligibleWorkSpecsForLimitedSlots.size() > 0) {
                long now = System.currentTimeMillis();

                // Mark all the WorkSpecs as scheduled.
                // Calls to Scheduler#schedule() could potentially result in more schedules
                // on a separate thread. Therefore, this needs to be done first.
                for (WorkSpec workSpec : eligibleWorkSpecsForLimitedSlots) {
                    workSpecDao.markWorkSpecScheduled(workSpec.id, now);
                }
            }
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }

        if (eligibleWorkSpecsForLimitedSlots != null
                && eligibleWorkSpecsForLimitedSlots.size() > 0) {

            WorkSpec[] eligibleWorkSpecsArray =
                    new WorkSpec[eligibleWorkSpecsForLimitedSlots.size()];
            eligibleWorkSpecsArray =
                    eligibleWorkSpecsForLimitedSlots.toArray(eligibleWorkSpecsArray);

            // Delegate to the underlying schedulers.
            for (Scheduler scheduler : schedulers) {
                if (scheduler.hasLimitedSchedulingSlots()) {
                    scheduler.schedule(eligibleWorkSpecsArray);
                }
            }
        }

        if (allEligibleWorkSpecs != null && allEligibleWorkSpecs.size() > 0) {
            WorkSpec[] enqueuedWorkSpecsArray = new WorkSpec[allEligibleWorkSpecs.size()];
            enqueuedWorkSpecsArray = allEligibleWorkSpecs.toArray(enqueuedWorkSpecsArray);
            // Delegate to the underlying schedulers.
            for (Scheduler scheduler : schedulers) {
                if (!scheduler.hasLimitedSchedulingSlots()) {
                    scheduler.schedule(enqueuedWorkSpecsArray);
                }
            }
        }
    }

    @NonNull
    static Scheduler createBestAvailableBackgroundScheduler(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManager) {

        Scheduler scheduler;

        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            scheduler = new SystemJobScheduler(context, workManager);
            setComponentEnabled(context, SystemJobService.class, true);
            Logger.get().debug(TAG, "Created SystemJobScheduler and enabled SystemJobService");
        } else {
            scheduler = tryCreateGcmBasedScheduler(context);
            if (scheduler == null) {
                scheduler = new SystemAlarmScheduler(context);
                setComponentEnabled(context, SystemAlarmService.class, true);
                Logger.get().debug(TAG, "Created SystemAlarmScheduler");
            }
        }
        return scheduler;
    }

    @Nullable
    private static Scheduler tryCreateGcmBasedScheduler(@NonNull Context context) {
        try {
            Class<?> klass = Class.forName(GCM_SCHEDULER);
            Scheduler scheduler =
                    (Scheduler) klass.getConstructor(Context.class).newInstance(context);
            Logger.get().debug(TAG, "Created " + GCM_SCHEDULER);
            return scheduler;
        } catch (Throwable throwable) {
            Logger.get().debug(TAG, "Unable to create GCM Scheduler", throwable);
            return null;
        }
    }

    private Schedulers() {
    }
}
