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
import static androidx.work.ExistingWorkPolicy.KEEP;
import static androidx.work.State.BLOCKED;
import static androidx.work.State.CANCELLED;
import static androidx.work.State.ENQUEUED;
import static androidx.work.State.FAILED;
import static androidx.work.State.RUNNING;
import static androidx.work.State.SUCCEEDED;
import static androidx.work.impl.workers.ConstraintTrackingWorker.ARGUMENT_CLASS_NAME;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.State;
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
import androidx.work.impl.model.WorkTag;
import androidx.work.impl.workers.ConstraintTrackingWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages the enqueuing of a {@link WorkContinuationImpl}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EnqueueRunnable implements Runnable {

    private static final String TAG = "EnqueueRunnable";

    private final WorkContinuationImpl mWorkContinuation;

    public EnqueueRunnable(@NonNull WorkContinuationImpl workContinuation) {
        mWorkContinuation = workContinuation;
    }

    @Override
    public void run() {
        if (mWorkContinuation.hasCycles()) {
            throw new IllegalStateException(
                    String.format("WorkContinuation has cycles (%s)", mWorkContinuation));
        }
        boolean needsScheduling = addToDatabase();
        if (needsScheduling) {
            scheduleWorkInBackground();
        }
    }

    /**
     * Adds the {@link WorkSpec}'s to the datastore, parent first.
     * Schedules work on the background scheduler, if transaction is successful.
     */
    @VisibleForTesting
    public boolean addToDatabase() {
        WorkManagerImpl workManagerImpl = mWorkContinuation.getWorkManagerImpl();
        WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
        workDatabase.beginTransaction();
        try {
            boolean needsScheduling = processContinuation(mWorkContinuation);
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
    public void scheduleWorkInBackground() {
        WorkManagerImpl workManager = mWorkContinuation.getWorkManagerImpl();
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
                    Log.w(TAG, String.format("Already enqueued work ids (%s).",
                            TextUtils.join(", ", parent.getIds())));
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

        long currentTimeMillis = System.currentTimeMillis();
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
                    Log.e(TAG, String.format("Prerequisite %s doesn't exist; not enqueuing", id));
                    return false;
                }

                State prerequisiteState = prerequisiteWorkSpec.state;
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
                if (existingWorkPolicy == APPEND) {
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
                    CancelWorkRunnable.forName(name, workManagerImpl).run();
                    // And delete all the database records.
                    WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                    for (WorkSpec.IdAndState idAndState : existingWorkSpecIdAndStates) {
                        workSpecDao.delete(idAndState.id);
                    }
                }
            }
        }

        boolean needsScheduling = false;
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
                // Set scheduled times only for work without prerequisites. Dependent work
                // will set their scheduled times when they are unblocked.
                workSpec.periodStartTime = currentTimeMillis;
            }

            if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 25) {
                tryDelegateConstrainedWorkSpec(workSpec);
            }

            // If we have one WorkSpec with an enqueued state, then we need to schedule.
            if (workSpec.state == ENQUEUED) {
                needsScheduling = true;
            }

            workDatabase.workSpecDao().insertWorkSpec(workSpec);

            if (hasPrerequisite) {
                for (String prerequisiteId : prerequisiteIds) {
                    Dependency dep = new Dependency(work.getStringId(), prerequisiteId);
                    workDatabase.dependencyDao().insertDependency(dep);
                }
            }

            for (String tag : work.getTags()) {
                workDatabase.workTagDao().insert(new WorkTag(tag, work.getStringId()));
            }

            if (isNamed) {
                workDatabase.workNameDao().insert(new WorkName(name, work.getStringId()));
            }
        }
        return needsScheduling;
    }

    private static void tryDelegateConstrainedWorkSpec(WorkSpec workSpec) {
        // requiresBatteryNotLow and requiresStorageNotLow require API 26 for JobScheduler.
        // Delegate to ConstraintTrackingWorker between API 23-25.
        Constraints constraints = workSpec.constraints;
        if (constraints.requiresBatteryNotLow() || constraints.requiresStorageNotLow()) {
            String workerClassName = workSpec.workerClassName;
            Data.Builder builder = new Data.Builder();
            // Copy all arguments
            builder.putAll(workSpec.input)
                    .putString(ARGUMENT_CLASS_NAME, workerClassName);
            workSpec.workerClassName = ConstraintTrackingWorker.class.getName();
            workSpec.input = builder.build();
        }
    }
}
