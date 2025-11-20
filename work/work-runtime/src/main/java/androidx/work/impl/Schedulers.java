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
import static androidx.work.impl.WorkManagerImpl.CONTENT_URI_TRIGGER_API_LEVEL;
import static androidx.work.impl.utils.PackageManagerHelper.setComponentEnabled;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.work.Clock;
import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.background.systemjob.SystemJobService;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Helper methods for {@link Scheduler}s.
 *
 * Helps schedule {@link androidx.work.impl.model.WorkSpec}s while enforcing
 * {@link Scheduler#MAX_SCHEDULER_LIMIT}s.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Schedulers {
    private static final String TAG = Logger.tagWithPrefix("Schedulers");

    /**
     * Make sure that once worker has run its dependants are run.
     */
    public static void registerRescheduling(
            @NonNull List<Scheduler> schedulers,
            @NonNull Processor processor,
            @NonNull Executor executor,
            @NonNull WorkDatabase workDatabase,
            @NonNull Configuration configuration) {
        processor.addExecutionListener((id, needsReschedule) -> {
            executor.execute(() -> {
                // Try to schedule any newly-unblocked workers, and workers requiring rescheduling
                // (such as periodic work using AlarmManager). This code runs after runWorker()
                // because it should happen in its own transaction.

                // Cancel this work in other schedulers. For example, if this work was
                // handled by GreedyScheduler, we should make sure JobScheduler is informed
                // that it should remove this job and AlarmManager should remove all related alarms.
                for (Scheduler scheduler : schedulers) {
                    scheduler.cancel(id.getWorkSpecId());
                }
                Schedulers.schedule(configuration, workDatabase, schedulers);
            });
        });
    }

    /**
     * Schedules {@link WorkSpec}s while honoring the {@link Scheduler#MAX_SCHEDULER_LIMIT}.
     *
     * @param workDatabase The {@link WorkDatabase}.
     * @param schedulers   The {@link List} of {@link Scheduler}s to delegate to.
     */
    public static void schedule(
            @NonNull Configuration configuration,
            @NonNull WorkDatabase workDatabase,
            @Nullable List<Scheduler> schedulers) {
        if (schedulers == null || schedulers.size() == 0) {
            return;
        }

        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        List<WorkSpec> eligibleWorkSpecsForLimitedSlots;
        List<WorkSpec> allEligibleWorkSpecs;

        workDatabase.beginTransaction();
        try {
            List<WorkSpec> contentUriWorkSpecs = null;
            if (Build.VERSION.SDK_INT >= CONTENT_URI_TRIGGER_API_LEVEL) {
                contentUriWorkSpecs = workSpecDao.getEligibleWorkForSchedulingWithContentUris();
                markScheduled(workSpecDao, configuration.getClock(), contentUriWorkSpecs);
            }

            // Enqueued workSpecs when scheduling limits are applicable.
            eligibleWorkSpecsForLimitedSlots = workSpecDao.getEligibleWorkForScheduling(
                    configuration.getMaxSchedulerLimit());
            markScheduled(workSpecDao, configuration.getClock(), eligibleWorkSpecsForLimitedSlots);
            if (contentUriWorkSpecs != null) {
                eligibleWorkSpecsForLimitedSlots.addAll(contentUriWorkSpecs);
            }

            // Enqueued workSpecs when scheduling limits are NOT applicable.
            allEligibleWorkSpecs = workSpecDao.getAllEligibleWorkSpecsForScheduling(
                    MAX_GREEDY_SCHEDULER_LIMIT);
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }

        if (eligibleWorkSpecsForLimitedSlots.size() > 0) {

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

        if (allEligibleWorkSpecs.size() > 0) {
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

    static @NonNull Scheduler createBestAvailableBackgroundScheduler(@NonNull Context context,
            @NonNull WorkDatabase workDatabase, Configuration configuration) {

        Scheduler scheduler = new SystemJobScheduler(context, workDatabase, configuration);
        setComponentEnabled(context, SystemJobService.class, true);
        Logger.get().debug(TAG, "Created SystemJobScheduler and enabled SystemJobService");
        return scheduler;
    }

    private Schedulers() {
    }

    private static void markScheduled(WorkSpecDao dao, Clock clock, List<WorkSpec> workSpecs) {
        if (workSpecs.size() > 0) {
            long now = clock.currentTimeMillis();

            // Mark all the WorkSpecs as scheduled.
            // Calls to Scheduler#schedule() could potentially result in more schedules
            // on a separate thread. Therefore, this needs to be done first.
            for (WorkSpec workSpec : workSpecs) {
                dao.markWorkSpecScheduled(workSpec.id, now);
            }
        }
    }
}
