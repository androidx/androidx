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
import android.arch.background.workmanager.impl.InternalWorkImpl;
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkTag;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

/**
 * A {@link Runnable} to enqueue a {@link Work} in the database.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EnqueueRunnable implements Runnable {

    private WorkManagerImpl mWorkManagerImpl;
    private InternalWorkImpl[] mWorkArray;
    private String[] mPrerequisiteIds;

    public EnqueueRunnable(WorkManagerImpl workManagerImpl,
            @NonNull BaseWork[] workArray,
            String[] prerequisiteIds) {
        mWorkManagerImpl = workManagerImpl;
        mWorkArray = new InternalWorkImpl[workArray.length];
        for (int i = 0; i < workArray.length; ++i) {
            mWorkArray[i] = (InternalWorkImpl) workArray[i];
        }
        mPrerequisiteIds = prerequisiteIds;
    }

    @WorkerThread
    @Override
    public void run() {
        WorkDatabase workDatabase = mWorkManagerImpl.getWorkDatabase();
        workDatabase.beginTransaction();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            boolean hasPrerequisite = (mPrerequisiteIds != null && mPrerequisiteIds.length > 0);

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
            }
            workDatabase.setTransactionSuccessful();

            // Schedule in the background if there are no prerequisites.  Foreground scheduling
            // happens automatically if a ForegroundProcessor is available.
            if (!hasPrerequisite) {
                for (InternalWorkImpl work : mWorkArray) {
                    mWorkManagerImpl.getScheduler().schedule(work.getWorkSpec());
                }
            }
        } finally {
            workDatabase.endTransaction();
        }
    }
}
