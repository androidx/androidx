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

package android.arch.background.workmanager.impl.utils;

import static android.arch.background.workmanager.BaseWork.STATUS_BLOCKED;
import static android.arch.background.workmanager.BaseWork.STATUS_SUCCEEDED;

import android.arch.background.workmanager.BaseWork;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.impl.InternalWorkImpl;
import android.arch.background.workmanager.impl.WorkContinuationImpl;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkTag;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

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
        WorkManagerImpl workManagerImpl = mWorkContinuation.getWorkManagerImpl();
        WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();
        workDatabase.beginTransaction();
        try {
            processContinuation(mWorkContinuation);
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }
    }

    private static void processContinuation(@NonNull WorkContinuationImpl workContinuation) {
        WorkContinuationImpl parent = workContinuation.getParent();
        if (parent != null) {
            // When chaining off a completed continuation we need to pay
            // attention to parents that may have been marked as enqueued before.
            if (!parent.isEnqueued()) {
                processContinuation(parent);
            } else {
                Log.w(TAG,
                        String.format(
                                "Already enqueued work ids (%s).",
                                TextUtils.join(", ", parent.getIds())));
            }

        }
        enqueueContinuation(workContinuation);
    }

    private static void enqueueContinuation(@NonNull WorkContinuationImpl workContinuation) {
        WorkContinuationImpl parent = workContinuation.getParent();
        String[] prerequisiteIds = parent != null ? parent.getIds() : null;

        enqueueWorkWithPrerequisites(
                workContinuation.getWorkManagerImpl(),
                workContinuation.getWork(),
                prerequisiteIds,
                workContinuation.getUniqueTag(),
                workContinuation.getExistingWorkPolicy());

        workContinuation.markEnqueued();
    }

    /**
     * Enqueues the {@link WorkSpec}'s while keeping track of the prerequisites.
     */
    private static void enqueueWorkWithPrerequisites(
            WorkManagerImpl workManagerImpl,
            @NonNull BaseWork[] workArray,
            String[] prerequisiteIds,
            String uniqueTag,
            @WorkManager.ExistingWorkPolicy int existingWorkPolicy) {

        long currentTimeMillis = System.currentTimeMillis();
        WorkDatabase workDatabase = workManagerImpl.getWorkDatabase();

        InternalWorkImpl[] workImplArray = new InternalWorkImpl[workArray.length];
        for (int i = 0; i < workArray.length; ++i) {
            workImplArray[i] = (InternalWorkImpl) workArray[i];
        }

        boolean hasPrerequisite = (prerequisiteIds != null && prerequisiteIds.length > 0);
        boolean hasCompletedAllPrerequisites = true;

        if (hasPrerequisite) {
            // If there are prerequisites, make sure they actually exist before enqueuing
            // anything.  Prerequisites may not exist if we are using unique tags, because the
            // chain of work could have been wiped out already.
            for (String id : prerequisiteIds) {
                WorkSpec prerequisiteWorkSpec = workDatabase.workSpecDao().getWorkSpec(id);
                if (prerequisiteWorkSpec == null) {
                    Log.e(TAG, "Prerequisite " + id + " doesn't exist; not enqueuing");
                    return;
                }
                hasCompletedAllPrerequisites &=
                        (prerequisiteWorkSpec.getStatus() == STATUS_SUCCEEDED);
            }
        }

        boolean hasUniqueTag = !TextUtils.isEmpty(uniqueTag);
        if (hasUniqueTag && !hasPrerequisite) {
            List<String> existingWorkSpecIds =
                    workDatabase.workSpecDao().getWorkSpecIdsForTag(uniqueTag);
            if (!existingWorkSpecIds.isEmpty()) {
                if (existingWorkPolicy == WorkManager.KEEP_EXISTING_WORK) {
                    return;
                }

                // Cancel all of these workers.
                CancelWorkRunnable cancelWorkRunnable =
                        new CancelWorkRunnable(workManagerImpl, null, uniqueTag);
                cancelWorkRunnable.run();
                // And delete all the database records.
                workDatabase.workSpecDao().delete(existingWorkSpecIds);
            }
        }

        for (InternalWorkImpl work : workImplArray) {
            WorkSpec workSpec = work.getWorkSpec();

            if (hasPrerequisite && !hasCompletedAllPrerequisites) {
                workSpec.setStatus(STATUS_BLOCKED);
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

            // Enforce that the unique tag is always present.
            if (hasUniqueTag) {
                workDatabase.workTagDao().insert(new WorkTag(uniqueTag, work.getId()));
            }
        }

        // TODO(rahulrav@) It's dangerous to ask the background scheduler to schedule things.
        // Revisit this and see if there is a better way to do this, after the transaction
        // has settled.

        // Schedule in the background if there are no prerequisites.  Foreground scheduling
        // happens automatically if a ForegroundProcessor is available.
        if (!hasPrerequisite) {
            for (InternalWorkImpl work : workImplArray) {
                workManagerImpl.getBackgroundScheduler().schedule(work.getWorkSpec());
            }
        }
    }
}
