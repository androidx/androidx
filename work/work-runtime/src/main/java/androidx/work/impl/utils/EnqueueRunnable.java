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

import static androidx.work.ExistingWorkPolicy.APPEND;
import static androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE;
import static androidx.work.ExistingWorkPolicy.KEEP;
import static androidx.work.WorkInfo.State.BLOCKED;
import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static androidx.work.impl.utils.EnqueueUtilsKt.checkContentUriTriggerWorkerLimits;
import static androidx.work.impl.utils.EnqueueUtilsKt.wrapInConstraintTrackingWorkerIfNeeded;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.WorkRequest;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkName;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Manages the enqueuing of a {@link WorkContinuationImpl}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EnqueueRunnable {

    private EnqueueRunnable() {
    }

    private static final String TAG = Logger.tagWithPrefix("EnqueueRunnable");

    /**
     * Enqueues the given workContinuation.
     */
    public static void enqueue(@NonNull WorkContinuationImpl workContinuation) {
        if (workContinuation.hasCycles()) {
            throw new IllegalStateException(
                    "WorkContinuation has cycles (" + workContinuation + ")");
        }
        boolean needsScheduling = addToDatabase(workContinuation);
        if (needsScheduling) {
            scheduleWorkInBackground(workContinuation);
        }
    }

    /**
     * Adds the {@link WorkSpec}'s to the datastore, parent first.
     * Schedules work on the background scheduler, if transaction is successful.
     */
    @VisibleForTesting
    @SuppressWarnings("deprecation")
    public static boolean addToDatabase(@NonNull WorkContinuationImpl workContinuation) {
        WorkManagerImpl workManagerImpl = workContinuation.getWorkManagerImpl();
        WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
        workDatabase.beginTransaction();
        try {
            checkContentUriTriggerWorkerLimits(workDatabase,
                    workManagerImpl.getConfiguration(), workContinuation);
            boolean needsScheduling = processContinuation(workContinuation);
            workDatabase.setTransactionSuccessful();
            return needsScheduling;
        } finally {
            workDatabase.endTransaction();
        }
    }

    /**
     * Schedules work on the background scheduler.
     */
    @VisibleForTesting
    public static void scheduleWorkInBackground(@NonNull WorkContinuationImpl workContinuation) {
        WorkManagerImpl workManager = workContinuation.getWorkManagerImpl();
        Schedulers.schedule(
                workManager.getConfiguration(),
                workManager.getWorkDatabase(),
                workManager.getSchedulers());
    }

    private static boolean processContinuation(@NonNull WorkContinuationImpl workContinuation) {
        boolean needsScheduling = false;
        List<WorkContinuationImpl> parents = workContinuation.getParents();
        if (parents != null) {
            for (WorkContinuationImpl parent : parents) {
                // When chaining off a completed continuation we need to pay
                // attention to parents that may have been marked as enqueued before.
                if (!parent.isEnqueued()) {
                    needsScheduling |= processContinuation(parent);
                } else {
                    Logger.get().warning(TAG,
                            "Already enqueued work ids (" +
                                    TextUtils.join(", ", parent.getIds()) + ")");
                }
            }
        }
        needsScheduling |= enqueueContinuation(workContinuation);
        return needsScheduling;
    }

    private static boolean enqueueContinuation(@NonNull WorkContinuationImpl workContinuation) {
        Set<String> prerequisiteIds = WorkContinuationImpl.prerequisitesFor(workContinuation);

        boolean needsScheduling = enqueueWorkWithPrerequisites(
                workContinuation.getWorkManagerImpl(),
                workContinuation.getWork(),
                prerequisiteIds.toArray(new String[0]),
                workContinuation.getName(),
                workContinuation.getExistingWorkPolicy());

        workContinuation.markEnqueued();
        return needsScheduling;
    }

    /**
     * Enqueues the {@link WorkSpec}'s while keeping track of the prerequisites.
     *
     * @return {@code true} If there is any scheduling to be done.
     */
    private static boolean enqueueWorkWithPrerequisites(
            WorkManagerImpl workManagerImpl,
            @NonNull List<? extends WorkRequest> workList,
            String[] prerequisiteIds,
            String name,
            ExistingWorkPolicy existingWorkPolicy) {

        boolean needsScheduling = false;

        long currentTimeMillis = workManagerImpl.getConfiguration().getClock().currentTimeMillis();
        WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();

        boolean hasPrerequisite = (prerequisiteIds != null && prerequisiteIds.length > 0);
        boolean hasCompletedAllPrerequisites = true;
        boolean hasFailedPrerequisites = false;
        boolean hasCancelledPrerequisites = false;

        if (hasPrerequisite) {
            // If there are prerequisites, make sure they actually exist before enqueuing
            // anything.  Prerequisites may not exist if we are using unique tags, because the
            // chain of work could have been wiped out already.
            for (String id : prerequisiteIds) {
                WorkSpec prerequisiteWorkSpec = workDatabase.workSpecDao().getWorkSpec(id);
                if (prerequisiteWorkSpec == null) {
                    Logger.get().error(TAG, "Prerequisite " + id + " doesn't exist; not enqueuing");
                    return false;
                }

                WorkInfo.State prerequisiteState = prerequisiteWorkSpec.state;
                hasCompletedAllPrerequisites &= (prerequisiteState == SUCCEEDED);
                if (prerequisiteState == FAILED) {
                    hasFailedPrerequisites = true;
                } else if (prerequisiteState == CANCELLED) {
                    hasCancelledPrerequisites = true;
                }
            }
        }

        boolean isNamed = !TextUtils.isEmpty(name);

        // We only apply existing work policies for unique tag sequences that are the beginning of
        // chains.
        boolean shouldApplyExistingWorkPolicy = isNamed && !hasPrerequisite;
        if (shouldApplyExistingWorkPolicy) {
            // Get everything with the unique tag.
            List<WorkSpec.IdAndState> existingWorkSpecIdAndStates =
                    workDatabase.workSpecDao().getWorkSpecIdAndStatesForName(name);

            if (!existingWorkSpecIdAndStates.isEmpty()) {
                // If appending, these are the new prerequisites.
                if (existingWorkPolicy == APPEND || existingWorkPolicy == APPEND_OR_REPLACE) {
                    DependencyDao dependencyDao = workDatabase.dependencyDao();
                    List<String> newPrerequisiteIds = new ArrayList<>();
                    for (WorkSpec.IdAndState idAndState : existingWorkSpecIdAndStates) {
                        if (!dependencyDao.hasDependents(idAndState.id)) {
                            hasCompletedAllPrerequisites &= (idAndState.state == SUCCEEDED);
                            if (idAndState.state == FAILED) {
                                hasFailedPrerequisites = true;
                            } else if (idAndState.state == CANCELLED) {
                                hasCancelledPrerequisites = true;
                            }
                            newPrerequisiteIds.add(idAndState.id);
                        }
                    }
                    if (existingWorkPolicy == APPEND_OR_REPLACE) {
                        if (hasCancelledPrerequisites || hasFailedPrerequisites) {
                            // Delete all WorkSpecs with this name
                            WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                            List<WorkSpec.IdAndState> idAndStates =
                                    workSpecDao.getWorkSpecIdAndStatesForName(name);
                            for (WorkSpec.IdAndState idAndState : idAndStates) {
                                workSpecDao.delete(idAndState.id);
                            }
                            // Treat this as a new chain of work.
                            newPrerequisiteIds = Collections.emptyList();
                            hasCancelledPrerequisites = false;
                            hasFailedPrerequisites = false;
                        }
                    }
                    prerequisiteIds = newPrerequisiteIds.toArray(prerequisiteIds);
                    hasPrerequisite = (prerequisiteIds.length > 0);
                } else {
                    // If we're keeping existing work, make sure to do so only if something is
                    // enqueued or running.
                    if (existingWorkPolicy == KEEP) {
                        for (WorkSpec.IdAndState idAndState : existingWorkSpecIdAndStates) {
                            if (idAndState.state == ENQUEUED || idAndState.state == RUNNING) {
                                return false;
                            }
                        }
                    }

                    // Cancel all of these workers.
                    // Don't allow rescheduling in CancelWorkRunnable because it will happen inside
                    // the current transaction.  We want it to happen separately to avoid race
                    // conditions (see ag/4502245, which tries to avoid work trying to run before
                    // it's actually been committed to the database).
                    CancelWorkRunnable.forNameInline(name, workManagerImpl);
                    // Because we cancelled some work but didn't allow rescheduling inside
                    // CancelWorkRunnable, we need to make sure we do schedule work at the end of
                    // this runnable.
                    needsScheduling = true;

                    // And delete all the database records.
                    WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                    for (WorkSpec.IdAndState idAndState : existingWorkSpecIdAndStates) {
                        workSpecDao.delete(idAndState.id);
                    }
                }
            }
        }

        for (WorkRequest work : workList) {
            WorkSpec workSpec = work.getWorkSpec();

            if (hasPrerequisite && !hasCompletedAllPrerequisites) {
                if (hasFailedPrerequisites) {
                    workSpec.state = FAILED;
                } else if (hasCancelledPrerequisites) {
                    workSpec.state = CANCELLED;
                } else {
                    workSpec.state = BLOCKED;
                }
            } else {
                // Set scheduled times only for work without prerequisites.
                // Dependent work will set their scheduled times when they are
                // unblocked.
                workSpec.lastEnqueueTime = currentTimeMillis;
            }

            // If we have one WorkSpec with an enqueued state, then we need to schedule.
            if (workSpec.state == ENQUEUED) {
                needsScheduling = true;
            }

            workDatabase.workSpecDao().insertWorkSpec(
                    wrapInConstraintTrackingWorkerIfNeeded(
                            workManagerImpl.getSchedulers(),
                            workSpec
                    )
            );

            if (hasPrerequisite) {
                for (String prerequisiteId : prerequisiteIds) {
                    Dependency dep = new Dependency(work.getStringId(), prerequisiteId);
                    workDatabase.dependencyDao().insertDependency(dep);
                }
            }

            workDatabase.workTagDao().insertTags(work.getStringId(), work.getTags());
            if (isNamed) {
                workDatabase.workNameDao().insert(new WorkName(name, work.getStringId()));
            }
        }
        return needsScheduling;
    }
}
