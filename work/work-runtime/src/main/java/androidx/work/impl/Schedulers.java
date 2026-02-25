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
import androidx.work.Constraints;
import androidx.work.Logger;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.background.systemjob.SystemJobService;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        if (schedulers == null || schedulers.isEmpty()) {
            return;
        }

        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        List<WorkSpec> eligibleWorkSpecsForLimitedSlots;
        List<WorkSpec> ineligibleWorkSpecsForLimitedSlots = new ArrayList<>();
        List<WorkSpec> allEligibleWorkSpecs;

        workDatabase.beginTransaction();
        try {
            List<WorkSpec> contentUriWorkSpecs = null;
            if (Build.VERSION.SDK_INT >= CONTENT_URI_TRIGGER_API_LEVEL) {
                contentUriWorkSpecs = workSpecDao.getEligibleWorkForSchedulingWithContentUris();
                markScheduled(workSpecDao, configuration.getClock(), contentUriWorkSpecs);
            }

            // Enqueued workSpecs when scheduling limits are NOT applicable.
            allEligibleWorkSpecs = workSpecDao.getAllEligibleWorkSpecsForScheduling(
                    MAX_GREEDY_SCHEDULER_LIMIT);

            if (!configuration.isRepresentativeJobsEnabled()) {
                // Enqueued workSpecs when scheduling limits are applicable.
                eligibleWorkSpecsForLimitedSlots = workSpecDao.getEligibleWorkForScheduling(
                        configuration.getMaxSchedulerLimit());
            } else {

                Set<WorkSpec> uniqueConstraintsPrioritySet =
                        getRepresentativeJobsPrioritizedWorkToSchedule(
                                workSpecDao.getAllUnblockedWork(),
                                configuration.getMaxSchedulerLimit());
                for (WorkSpec workSpec : workSpecDao.getScheduledWork()) {
                    // Remove workSpecs that are already scheduled from the priority set.
                    // Collect those that are no longer in the priority set since they should be
                    // unscheduled.
                    if (uniqueConstraintsPrioritySet.contains(workSpec)) {
                        uniqueConstraintsPrioritySet.remove(workSpec);
                    } else {
                        ineligibleWorkSpecsForLimitedSlots.add(workSpec);
                        workSpecDao.markWorkSpecScheduled(
                                workSpec.id, WorkSpec.SCHEDULE_NOT_REQUESTED_YET);
                    }
                }
                eligibleWorkSpecsForLimitedSlots = new ArrayList<>(uniqueConstraintsPrioritySet);
            }

            markScheduled(workSpecDao, configuration.getClock(), eligibleWorkSpecsForLimitedSlots);
            if (contentUriWorkSpecs != null) {
                eligibleWorkSpecsForLimitedSlots.addAll(contentUriWorkSpecs);
            }

            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }

        if (configuration.isRepresentativeJobsEnabled()) {
            // Cancel now-ineligible workers before scheduling the new set.
            // TODO(b/481357599): Add logging to detect how often this occurs.
            cancelWorkSpecsForLimitedSlots(ineligibleWorkSpecsForLimitedSlots, schedulers);
        }

        scheduleWorkSpecs(
                eligibleWorkSpecsForLimitedSlots, schedulers, /* forLimitedSlots= */ true);
        scheduleWorkSpecs(allEligibleWorkSpecs, schedulers, /* forLimitedSlots= */ false);
    }

    private static void scheduleWorkSpecs(
            @NonNull List<WorkSpec> workSpecs,
            @NonNull List<Scheduler> schedulers,
            boolean forLimitedSlots) {
        if (workSpecs.isEmpty()) {
            return;
        }

        WorkSpec[] workSpecsArray = new WorkSpec[workSpecs.size()];
        workSpecsArray = workSpecs.toArray(workSpecsArray);

        // Delegate to the underlying schedulers.
        for (Scheduler scheduler : schedulers) {
            if (scheduler.hasLimitedSchedulingSlots() == forLimitedSlots) {
                scheduler.schedule(workSpecsArray);
            }
        }
    }

    private static void cancelWorkSpecsForLimitedSlots(
            @NonNull List<WorkSpec> workSpecs, @NonNull List<Scheduler> schedulers) {
        for (Scheduler scheduler : schedulers) {
            if (!scheduler.hasLimitedSchedulingSlots()) {
                continue;
            }
            for (WorkSpec workSpec : workSpecs) {
                scheduler.cancel(workSpec.id);
            }
        }
    }

    /**
     * Generates a priority set of {@link WorkSpec}s, ensuring that a diverse set of constraints
     * are represented within the given {@code maxSlots}.
     *
     * The method prioritizes {@link WorkSpec}s in the following order:
     * <ol>
     *     <li>WorkSpecs with unique {@link Constraints}, sorted by their calculated next run time.
     *     This ensures that WorkSpecs with shorter delays are prioritized.</li>
     *     <li>If there are still available slots after including all unique constraint WorkSpecs,
     *     the remaining slots are filled with the earliest-to-run WorkSpecs that share constraints
     *     with those already selected.</li>
     * </ol>
     *
     * @param allEligibleWorkSpecs A list of all eligible {@link WorkSpec}s.
     * @param maxSlots The maximum number of WorkSpecs to include in the priority set.
     * @return A {@link Set} of {@link WorkSpec}s representing the prioritized selection.
     */
    private static @NonNull Set<WorkSpec> getRepresentativeJobsPrioritizedWorkToSchedule(
            @NonNull List<WorkSpec> allEligibleWorkSpecs, int maxSlots) {
        if (allEligibleWorkSpecs.size() <= maxSlots) {
            return new HashSet<>(allEligibleWorkSpecs);
        }

        Set<WorkSpec> representativeWork = new HashSet<>(maxSlots);
        List<WorkSpec> remainingList = new ArrayList<>(maxSlots);

        // Sort WorkSpec by minimum delay to ensure that workers with short delays are not starved
        // by workers with long delays.
        Collections.sort(
                allEligibleWorkSpecs,
                (a, b) -> Long.compare(a.calculateNextRunTime(), b.calculateNextRunTime()));

        Set<Constraints> seenConstraints = new HashSet<>(maxSlots);
        for (WorkSpec workSpec : allEligibleWorkSpecs) {
            // Only constraints are considered, initialDelay should be ignored when considering
            // uniqueness.
            if (!seenConstraints.contains(workSpec.constraints)) {
                representativeWork.add(workSpec);
                seenConstraints.add(workSpec.constraints);
            } else {
                remainingList.add(workSpec);
            }

            if (representativeWork.size() == maxSlots) {
                return representativeWork;
            }
        }

        if (remainingList.isEmpty()) {
            return representativeWork;
        }

        int remainingSlots = maxSlots - representativeWork.size();
        representativeWork.addAll(remainingList.subList(0, remainingSlots));
        return representativeWork;
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
