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

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.work.BaseWork;
import androidx.work.ExistingWorkPolicy;
import androidx.work.State;
import androidx.work.impl.InternalWorkImpl;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.logger.Logger;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkName;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTag;

/**
 * Manages the enqueuing of a {@link WorkContinuationImpl}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EnqueueRunnable implements Runnable {

    private static final String TAG = "EnqueueRunnable";

    private final WorkContinuationImpl mWorkContinuation;
    private final List<InternalWorkImpl> mWorkToBeScheduled;

    public EnqueueRunnable(@NonNull WorkContinuationImpl workContinuation) {
        mWorkContinuation = workContinuation;
        mWorkToBeScheduled = new ArrayList<>();
    }

    @Override
    public void run() {
        addToDatabase();
        scheduleWorkInBackground();
    }

    /**
     * Adds the {@link WorkSpec}'s to the datastore, parent first.
     * Schedules work on the background scheduler, if transaction is successful.
     */
    @VisibleForTesting
    public void addToDatabase() {
        WorkManagerImpl workManagerImpl = mWorkContinuation.getWorkManagerImpl();
        WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
        workDatabase.beginTransaction();
        try {
            processContinuation(mWorkContinuation, mWorkToBeScheduled);
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }
    }

    /**
     * Schedules work on the background scheduler.
     */
    @VisibleForTesting
    public void scheduleWorkInBackground() {
        // Schedule in the background. This list contains work that does not have prerequisites.
        for (Scheduler scheduler : mWorkContinuation.getWorkManagerImpl().getSchedulers()) {
            for (InternalWorkImpl work : mWorkToBeScheduled) {
                scheduler.schedule(work.getWorkSpec());
            }
        }
    }

    private static void processContinuation(
            @NonNull WorkContinuationImpl workContinuation,
            @NonNull List<InternalWorkImpl> workToBeScheduled) {

        List<WorkContinuationImpl> parents = workContinuation.getParents();
        if (parents != null) {
            for (WorkContinuationImpl parent : parents) {
                // When chaining off a completed continuation we need to pay
                // attention to parents that may have been marked as enqueued before.
                if (!parent.isEnqueued()) {
                    processContinuation(parent, workToBeScheduled);
                } else {
                    Logger.warn(TAG, "Already enqueued work ids (%s).",
                            TextUtils.join(", ", parent.getIds()));
                }
            }
        }
        enqueueContinuation(workContinuation, workToBeScheduled);
    }

    private static void enqueueContinuation(
            @NonNull WorkContinuationImpl workContinuation,
            @NonNull List<InternalWorkImpl> workToBeScheduled) {

        List<WorkContinuationImpl> parents = workContinuation.getParents();
        Set<String> prerequisiteIds = new HashSet<>();
        if (parents != null) {
            for (WorkContinuationImpl parent : parents) {
                prerequisiteIds.addAll(parent.getIds());
            }
        }

        enqueueWorkWithPrerequisites(
                workContinuation.getWorkManagerImpl(),
                workContinuation.getWork(),
                prerequisiteIds.toArray(new String[0]),
                workContinuation.getName(),
                workContinuation.getExistingWorkPolicy(),
                workToBeScheduled);

        workContinuation.markEnqueued();
    }

    /**
     * Enqueues the {@link WorkSpec}'s while keeping track of the prerequisites.
     */
    private static void enqueueWorkWithPrerequisites(
            WorkManagerImpl workManagerImpl,
            @NonNull List<? extends BaseWork> workList,
            String[] prerequisiteIds,
            String name,
            ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<InternalWorkImpl> workToBeScheduled) {

        long currentTimeMillis = System.currentTimeMillis();
        WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();

        List<InternalWorkImpl> workImplList = new ArrayList<>(workList.size());
        for (int i = 0; i < workList.size(); ++i) {
            workImplList.add((InternalWorkImpl) workList.get(i));
        }

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
                    Logger.error(TAG, "Prerequisite %s doesn't exist; not enqueuing", id);
                    return;
                }

                State prerequisiteState = prerequisiteWorkSpec.getState();
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
                                return;
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

        for (InternalWorkImpl work : workImplList) {
            WorkSpec workSpec = work.getWorkSpec();

            if (hasPrerequisite && !hasCompletedAllPrerequisites) {
                if (hasFailedPrerequisites) {
                    workSpec.setState(FAILED);
                } else if (hasCancelledPrerequisites) {
                    workSpec.setState(CANCELLED);
                } else {
                    workSpec.setState(BLOCKED);
                }
            } else {
                // Set scheduled times only for work without prerequisites. Dependent work
                // will set their scheduled times when they are unblocked.
                workSpec.setPeriodStartTime(currentTimeMillis);
            }

            workDatabase.workSpecDao().insertWorkSpec(workSpec);

            if (hasPrerequisite) {
                for (String prerequisiteId : prerequisiteIds) {
                    Dependency dep = new Dependency(work.getId(), prerequisiteId);
                    workDatabase.dependencyDao().insertDependency(dep);
                }
            }

            for (String tag : work.getTags()) {
                workDatabase.workTagDao().insert(new WorkTag(tag, work.getId()));
            }

            if (isNamed) {
                workDatabase.workNameDao().insert(new WorkName(name, work.getId()));
            }
        }

        // If there are no prerequisites (or they have all been completed), then add then to the
        // list of work items that can be scheduled in the background. The actual scheduling is
        // typically done after the transaction has settled.
        if (!hasPrerequisite || hasCompletedAllPrerequisites) {
            workToBeScheduled.addAll(workImplList);
        }
    }
}
