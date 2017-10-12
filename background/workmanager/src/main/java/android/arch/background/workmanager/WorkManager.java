/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import static android.arch.background.workmanager.Work.STATUS_BLOCKED;

import android.arch.background.workmanager.foreground.ForegroundProcessor;
import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.systemjob.SystemJobScheduler;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.os.Build;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WorkManager is a class used to enqueue persisted work that is guaranteed to run after its
 * constraints are met.
 */
public final class WorkManager {

    private static final String TAG = "WorkManager";

    private Context mContext;
    private WorkDatabase mWorkDatabase;
    private ExecutorService mEnqueueExecutor = Executors.newSingleThreadExecutor();
    private Scheduler mScheduler;

    private static WorkManager sInstance = null;

    /**
     * Creates/Retrieves the static instance of {@link WorkManager}
     *
     * @param context {@link Context} used to create static instance
     * @return {@link WorkManager} object
     */
    public static synchronized WorkManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WorkManager(context, false);
        }
        return sInstance;
    }

    @VisibleForTesting
    WorkManager(Context context, boolean useTestDatabase) {
        mContext = context.getApplicationContext();
        mWorkDatabase = WorkDatabase.create(mContext, useTestDatabase);
        if (Build.VERSION.SDK_INT >= 23) {
            mScheduler = new SystemJobScheduler(mContext);
        }
        new ForegroundProcessor(mContext, mWorkDatabase, mScheduler, ProcessLifecycleOwner.get());
    }

    /**
     * @return The {@link WorkDatabase} instance associated with this WorkManager.
     */
    // TODO(sumir): Fix public access.  Need it for SystemJobService, but these classes need to be
    // refactored.
    // @VisibleForTesting
    public WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    /**
     * @return The {@link Scheduler} associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     */
    public Scheduler getScheduler() {
        return mScheduler;
    }

    /**
     * Gets the {@link Work.WorkStatus} for a given work id.
     *
     * @param id The id of the {@link Work}.
     * @return A {@link LiveData} of the status.
     */
    public LiveData<Integer> getWorkStatus(String id) {
        return mWorkDatabase.workSpecDao().getWorkSpecLiveDataStatus(id);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param work One or more {@link Work} to enqueue
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input work
     */
    public WorkContinuation enqueue(Work... work) {
        return enqueue(work, null);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workBuilders One or more {@link Work.Builder} to enqueue; internally {@code build} is
     *                     called on each of them
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input workBuilders
     */
    public WorkContinuation enqueue(Work.Builder... workBuilders) {
        return enqueue(WorkContinuation.convertBuilderArrayToWorkArray(workBuilders), null);
    }

    /**
     * Enqueues one or more items for background processing.
     *
     * @param workerClasses One or more {@link Worker}s to enqueue; this is a convenience method
     *                      that makes a {@link Work} object with default arguments for each Worker
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input workerClasses
     */
    @SafeVarargs
    public final WorkContinuation enqueue(Class<? extends Worker>... workerClasses) {
        return enqueue(WorkContinuation.convertWorkerClassArrayToWorkArray(workerClasses), null);
    }

    WorkContinuation enqueue(Work[] work, String[] prerequisiteIds) {
        WorkContinuation workContinuation = new WorkContinuation(this, work);
        mEnqueueExecutor.execute(new EnqueueRunnable(work, prerequisiteIds));
        return workContinuation;
    }

    /**
     * A Runnable to enqueue a {@link Work} in the database.
     */
    private class EnqueueRunnable implements Runnable {
        private Work[] mWorkArray;
        private String[] mPrerequisiteIds;

        EnqueueRunnable(Work[] workArray, String[] prerequisiteIds) {
            mWorkArray = workArray;
            mPrerequisiteIds = prerequisiteIds;
        }

        @Override
        public void run() {
            mWorkDatabase.beginTransaction();
            try {
                boolean hasPrerequisite = (mPrerequisiteIds != null && mPrerequisiteIds.length > 0);
                for (Work work : mWorkArray) {
                    WorkSpec workSpec = work.getWorkSpec();
                    if (hasPrerequisite) {
                        workSpec.setStatus(STATUS_BLOCKED);
                    }
                    mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);

                    if (hasPrerequisite) {
                        for (String prerequisiteId : mPrerequisiteIds) {
                            Dependency dep = new Dependency(work.getId(), prerequisiteId);
                            mWorkDatabase.dependencyDao().insertDependency(dep);
                        }
                    }
                }
                mWorkDatabase.setTransactionSuccessful();

                // Schedule in the background if there are no prerequisites.  Foreground scheduling
                // happens automatically because we instantiated ForegroundProcessor earlier.
                // TODO(janclarin): Remove mScheduler != null check when Scheduler added for 23-.
                if (mScheduler != null && !hasPrerequisite) {
                    for (Work work : mWorkArray) {
                        mScheduler.schedule(work.getWorkSpec());
                    }
                }
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
    }
}

