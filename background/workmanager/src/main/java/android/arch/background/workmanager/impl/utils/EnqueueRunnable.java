/*
 * Copyright 2017 The Android Open Source Project
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

import android.arch.background.workmanager.BaseWork;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.impl.InternalWorkImpl;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkTag;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * A {@link Runnable} to enqueue a {@link Work} in the database.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EnqueueRunnable implements Runnable {

    private static final String TAG = "EnqueueRunnable";

    private WorkManagerImpl mWorkManagerImpl;
    private InternalWorkImpl[] mWorkArray;
    private String[] mPrerequisiteIds;
    private String mUniqueTag;
    @WorkManager.ExistingWorkPolicy private int mExistingWorkPolicy;

    public EnqueueRunnable(WorkManagerImpl workManagerImpl,
            @NonNull BaseWork[] workArray,
            String[] prerequisiteIds,
            String uniqueTag,
            @WorkManager.ExistingWorkPolicy int existingWorkPolicy) {
        mWorkManagerImpl = workManagerImpl;
        mWorkArray = new InternalWorkImpl[workArray.length];
        for (int i = 0; i < workArray.length; ++i) {
            mWorkArray[i] = (InternalWorkImpl) workArray[i];
        }
        mPrerequisiteIds = prerequisiteIds;
        mUniqueTag = uniqueTag;
        mExistingWorkPolicy = existingWorkPolicy;
    }

    @WorkerThread
    @Override
    public void run() {
        WorkDatabase workDatabase = mWorkManagerImpl.getWorkDatabase();
        workDatabase.beginTransaction();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            boolean hasPrerequisite = (mPrerequisiteIds != null && mPrerequisiteIds.length > 0);

            if (hasPrerequisite) {
                // If there are prerequisites, make sure they actually exist before enqueuing
                // anything.  Prerequisites may not exist if we are using unique tags, because the
                // chain of work could have been wiped out already.
                for (String id : mPrerequisiteIds) {
                    if (workDatabase.workSpecDao().getWorkSpec(id) == null) {
                        Log.e(TAG, "Prerequisite " + id + " doesn't exist; not enqueuing");
                        return;
                    }
                }
            }

            boolean hasUniqueTag = !TextUtils.isEmpty(mUniqueTag);
            if (hasUniqueTag && !hasPrerequisite) {
                List<String> existingWorkSpecIds =
                        workDatabase.workSpecDao().getWorkSpecIdsForTag(mUniqueTag);
                if (!existingWorkSpecIds.isEmpty()) {
                    if (mExistingWorkPolicy == WorkManager.KEEP_EXISTING_WORK) {
                        return;
                    }

                    // Cancel all of these workers.
                    CancelWorkRunnable cancelWorkRunnable =
                            new CancelWorkRunnable(mWorkManagerImpl, null, mUniqueTag);
                    cancelWorkRunnable.run();
                    // And delete all the database records.
                    workDatabase.workSpecDao().delete(existingWorkSpecIds);
                }
            }

            for (InternalWorkImpl work : mWorkArray) {
                WorkSpec workSpec = work.getWorkSpec();

                if (hasPrerequisite) {
                    workSpec.setStatus(STATUS_BLOCKED);
                } else {
                    // Set scheduled times only for work without prerequisites. Dependent work
                    // will set their scheduled times when they are unblocked.
                    workSpec.setPeriodStartTime(currentTimeMillis);
                }

                workDatabase.workSpecDao().insertWorkSpec(workSpec);

                if (hasPrerequisite) {
                    for (String prerequisiteId : mPrerequisiteIds) {
                        Dependency dep = new Dependency(work.getId(), prerequisiteId);
                        workDatabase.dependencyDao().insertDependency(dep);
                    }
                }

                for (String tag : work.getTags()) {
                    workDatabase.workTagDao().insert(new WorkTag(tag, work.getId()));
                }

                // Enforce that the unique tag is always present.
                if (hasUniqueTag) {
                    workDatabase.workTagDao().insert(new WorkTag(mUniqueTag, work.getId()));
                }
            }
            workDatabase.setTransactionSuccessful();

            // Schedule in the background if there are no prerequisites.  Foreground scheduling
            // happens automatically if a ForegroundProcessor is available.
            if (!hasPrerequisite) {
                for (InternalWorkImpl work : mWorkArray) {
                    mWorkManagerImpl.getBackgroundScheduler().schedule(work.getWorkSpec());
                }
            }
        } finally {
            workDatabase.endTransaction();
        }
    }
}
