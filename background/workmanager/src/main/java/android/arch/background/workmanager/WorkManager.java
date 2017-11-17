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
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.background.workmanager.utils.BaseWorkHelper;
import android.arch.background.workmanager.utils.LiveDataUtils;
import android.arch.background.workmanager.utils.taskexecutor.TaskExecutor;
import android.arch.background.workmanager.utils.taskexecutor.WorkManagerTaskExecutor;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.List;

/**
 * WorkManager is a class used to enqueue persisted work that is guaranteed to run after its
 * constraints are met.
 */
public final class WorkManager {
    private static final String TAG = "WorkManager";

    private Context mContext;
    private WorkDatabase mWorkDatabase;
    private TaskExecutor mTaskExecutor;
    private Processor mForegroundProcessor;
    private Scheduler mBackgroundScheduler;

    private static WorkManager sInstance = null;

    static synchronized void init(Context context) {
        if (sInstance != null) {
            throw new IllegalStateException("Trying to initialize WorkManager twice!");
        }
        sInstance = new WorkManager(context.getApplicationContext(), false);
    }

    /**
     * Retrieves the singleton instance of {@link WorkManager}.
     *
     * @return The singleton instance of {@link WorkManager}
     */
    public static synchronized WorkManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException(
                    "Accessing WorkManager before it has been initialized!");
        }
        return sInstance;
    }

    WorkManager(Context context, boolean useTestDatabase) {
        mContext = context.getApplicationContext();
        mWorkDatabase = WorkDatabase.create(mContext, useTestDatabase);
        mForegroundProcessor = new ForegroundProcessor(
                mContext,
                mWorkDatabase,
                mBackgroundScheduler,
                ProcessLifecycleOwner.get());
        mBackgroundScheduler = SchedulerHelper.getBackgroundScheduler(mContext);
        mTaskExecutor = WorkManagerTaskExecutor.getInstance();
    }

    /**
     * @return The {@link WorkDatabase} instance associated with this WorkManager.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    /**
     * @return The {@link Scheduler} associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Scheduler getScheduler() {
        return mBackgroundScheduler;
    }

    /**
     * Gets the {@link BaseWork.WorkStatus} for a given work id.
     *
     * @param id The id of the {@link Work}.
     * @return A {@link LiveData} of the status.
     */
    public LiveData<Integer> getWorkStatus(String id) {
        return LiveDataUtils.dedupedLiveDataFor(
                mWorkDatabase.workSpecDao().getWorkSpecLiveDataStatus(id));
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
     * @param workerClasses One or more {@link Worker}s to enqueue; this is a convenience method
     *                      that makes a {@link Work} object with default arguments for each Worker
     * @return A {@link WorkContinuation} that allows further chaining, depending on all of the
     *         input workerClasses
     */
    @SafeVarargs
    public final WorkContinuation enqueue(Class<? extends Worker>... workerClasses) {
        return enqueue(BaseWorkHelper.convertWorkerClassArrayToWorkArray(workerClasses), null);
    }

    /**
     * Enqueues one or more periodic work items for background processing.
     *
     * @param periodicWork One or more {@link PeriodicWork} to enqueue
     */
    public void enqueue(PeriodicWork... periodicWork) {
        mTaskExecutor.executeOnBackgroundThread(new EnqueueRunnable(periodicWork, null));
    }

    /**
     * Cancels all work with the given tag, regardless of the current state of the work.
     * Note that cancellation is a best-effort policy and work that is already executing may
     * continue to run.
     *
     * @param tag The tag used to identify the work
     */
    public void cancelAllWorkWithTag(@NonNull final String tag) {
        mTaskExecutor.executeOnBackgroundThread(new CancelWorkWithTagRunnable(tag, false));
    }

    /**
     * Cancels all work with the given tag prefix, regardless of the current state of the work.
     * Note that cancellation is a best-effort policy and work that is already executing may
     * continue to run.
     *
     * @param tagPrefix The tag prefix used to identify the work
     */
    public void cancelAllWorkWithTagPrefix(@NonNull final String tagPrefix) {
        mTaskExecutor.executeOnBackgroundThread(new CancelWorkWithTagRunnable(tagPrefix, true));
    }

    WorkContinuation enqueue(Work[] work, String[] prerequisiteIds) {
        WorkContinuation workContinuation = new WorkContinuation(this, work);
        mTaskExecutor.executeOnBackgroundThread(new EnqueueRunnable(work, prerequisiteIds));
        return workContinuation;
    }

    /**
     * A Runnable to enqueue a {@link Work} in the database.
     */
    private class EnqueueRunnable implements Runnable {
        private BaseWork[] mWorkArray;
        private String[] mPrerequisiteIds;

        EnqueueRunnable(BaseWork[] workArray, String[] prerequisiteIds) {
            mWorkArray = workArray;
            mPrerequisiteIds = prerequisiteIds;
        }

        @WorkerThread
        @Override
        public void run() {
            mWorkDatabase.beginTransaction();
            try {
                boolean hasPrerequisite = (mPrerequisiteIds != null && mPrerequisiteIds.length > 0);
                for (BaseWork work : mWorkArray) {
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
                // TODO(janclarin): Remove mBackgroundScheduler != null check when Scheduler added
                // for 23-.
                if (mBackgroundScheduler != null && !hasPrerequisite) {
                    for (BaseWork work : mWorkArray) {
                        mBackgroundScheduler.schedule(work.getWorkSpec());
                    }
                }
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
    }

    /**
     * A Runnable to cancel work with a given tag.
     */
    private class CancelWorkWithTagRunnable implements Runnable {

        private String mTag;
        private boolean mIsPrefix;

        CancelWorkWithTagRunnable(@NonNull String tag, boolean isPrefix) {
            mTag = tag;
            mIsPrefix = isPrefix;
        }

        @WorkerThread
        @Override
        public void run() {
            mWorkDatabase.beginTransaction();
            try {
                WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();

                List<String> workSpecIds =
                        mIsPrefix
                                ? workSpecDao.getUnfinishedWorkWithTagPrefix(mTag)
                                : workSpecDao.getUnfinishedWorkWithTag(mTag);
                for (String workSpecId : workSpecIds) {
                    mForegroundProcessor.cancel(workSpecId, true);
                    if (mBackgroundScheduler != null) {
                        mBackgroundScheduler.cancel(workSpecId);
                    }
                    workSpecDao.delete(workSpecId);
                }

                mWorkDatabase.setTransactionSuccessful();
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
    }
}

