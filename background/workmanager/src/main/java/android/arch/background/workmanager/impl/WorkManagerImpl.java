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

package android.arch.background.workmanager.impl;

import static android.arch.background.workmanager.BaseWork.STATUS_BLOCKED;

import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.BaseWork;
import android.arch.background.workmanager.PeriodicWork;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkContinuation;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.impl.foreground.ForegroundProcessor;
import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkSpecDao;
import android.arch.background.workmanager.impl.model.WorkTag;
import android.arch.background.workmanager.impl.utils.BaseWorkHelper;
import android.arch.background.workmanager.impl.utils.CancelWorkRunnable;
import android.arch.background.workmanager.impl.utils.LiveDataUtils;
import android.arch.background.workmanager.impl.utils.PruneDatabaseRunnable;
import android.arch.background.workmanager.impl.utils.taskexecutor.TaskExecutor;
import android.arch.background.workmanager.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A concrete implementation of {@link WorkManager}.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerImpl extends WorkManager {

    private WorkDatabase mWorkDatabase;
    private TaskExecutor mTaskExecutor;
    private Processor mForegroundProcessor;
    private Scheduler mBackgroundScheduler;

    private static WorkManagerImpl sInstance = null;

    static synchronized void init(Context context, WorkManagerConfiguration configuration) {
        if (sInstance != null) {
            throw new IllegalStateException("Trying to initialize WorkManager twice!");
        }
        sInstance = new WorkManagerImpl(context, configuration);
    }


    /**
     * Retrieves the singleton instance of {@link WorkManagerImpl}.
     *
     * @return The singleton instance of {@link WorkManagerImpl}
     */
    public static synchronized WorkManagerImpl getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException(
                    "Accessing WorkManager before it has been initialized!");
        }
        return sInstance;
    }


    WorkManagerImpl(Context context, WorkManagerConfiguration configuration) {
        // TODO(janclarin): Move ForegroundProcessor and TaskExecutor to WorkManagerConfiguration.
        // TODO(janclarin): Remove context parameter.
        Context appContext = context.getApplicationContext();
        mWorkDatabase = configuration.getWorkDatabase();
        mForegroundProcessor = new ForegroundProcessor(
                appContext,
                mWorkDatabase,
                mBackgroundScheduler,
                ProcessLifecycleOwner.get());
        mBackgroundScheduler = configuration.getBackgroundScheduler();
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
     * @return The foreground {@link Processor} associated with this WorkManager.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Processor getForegroundProcessor() {
        return mForegroundProcessor;
    }

    /**
     * @return The {@link Scheduler} associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Scheduler getScheduler() {
        return mBackgroundScheduler;
    }

    @Override
    public LiveData<Integer> getStatusForId(@NonNull String id) {
        return LiveDataUtils.dedupedLiveDataFor(
                mWorkDatabase.workSpecDao().getWorkSpecLiveDataStatus(id));
    }

    @Override
    public LiveData<Arguments> getOutput(@NonNull String id) {
        return LiveDataUtils.dedupedLiveDataFor(mWorkDatabase.workSpecDao().getOutput(id));
    }

    @Override
    public WorkContinuation enqueue(Work... work) {
        return enqueue(work, null);
    }

    @SafeVarargs
    @Override
    public final WorkContinuation enqueue(Class<? extends Worker>... workerClasses) {
        return enqueue(BaseWorkHelper.convertWorkerClassArrayToWorkArray(workerClasses), null);
    }

    @Override
    public void enqueue(PeriodicWork... periodicWork) {
        mTaskExecutor.executeOnBackgroundThread(
                new EnqueueRunnable(periodicWork, null));
    }

    @Override
    public void cancelWorkForId(@NonNull String id) {
        mTaskExecutor.executeOnBackgroundThread(new CancelWorkRunnable(this, id, null));
    }

    @Override
    public void cancelAllWorkWithTag(@NonNull final String tag) {
        mTaskExecutor.executeOnBackgroundThread(new CancelWorkRunnable(this, null, tag));
    }

    @Override
    public void pruneDatabase() {
        mTaskExecutor.executeOnBackgroundThread(new PruneDatabaseRunnable(mWorkDatabase));
    }

    LiveData<Map<String, Integer>> getStatusesFor(@NonNull List<String> workSpecIds) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        final MediatorLiveData<Map<String, Integer>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(
                LiveDataUtils.dedupedLiveDataFor(dao.getWorkSpecStatuses(workSpecIds)),
                new Observer<List<WorkSpec.IdAndStatus>>() {
                    @Override
                    public void onChanged(@Nullable List<WorkSpec.IdAndStatus> idAndStatuses) {
                        if (idAndStatuses == null) {
                            return;
                        }

                        Map<String, Integer> idToStatusMap = new HashMap<>(idAndStatuses.size());
                        for (WorkSpec.IdAndStatus idAndStatus : idAndStatuses) {
                            idToStatusMap.put(idAndStatus.id, idAndStatus.status);
                        }
                        mediatorLiveData.setValue(idToStatusMap);
                    }
                });
        return mediatorLiveData;
    }

    WorkContinuation enqueue(Work[] work, String[] prerequisiteIds) {
        WorkContinuation workContinuation = new WorkContinuationImpl(this, work);
        mTaskExecutor.executeOnBackgroundThread(
                new EnqueueRunnable(work, prerequisiteIds));
        return workContinuation;
    }

    /**
     * A Runnable to enqueue a {@link Work} in the database.
     */
    private class EnqueueRunnable implements Runnable {

        private InternalWorkImpl[] mWorkArray;
        private String[] mPrerequisiteIds;

        EnqueueRunnable(BaseWork[] workArray, String[] prerequisiteIds) {
            mWorkArray = new InternalWorkImpl[workArray.length];
            for (int i = 0; i < workArray.length; ++i) {
                mWorkArray[i] = (InternalWorkImpl) workArray[i];
            }
            mPrerequisiteIds = prerequisiteIds;
        }

        @WorkerThread
        @Override
        public void run() {
            mWorkDatabase.beginTransaction();
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

                    mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);

                    if (hasPrerequisite) {
                        for (String prerequisiteId : mPrerequisiteIds) {
                            Dependency dep = new Dependency(work.getId(), prerequisiteId);
                            mWorkDatabase.dependencyDao().insertDependency(dep);
                        }
                    }

                    for (String tag : work.getTags()) {
                        mWorkDatabase.workTagDao().insert(new WorkTag(tag, work.getId()));
                    }
                }
                mWorkDatabase.setTransactionSuccessful();

                // Schedule in the background if there are no prerequisites.  Foreground scheduling
                // happens automatically because we instantiated ForegroundProcessor earlier.
                if (!hasPrerequisite) {
                    for (InternalWorkImpl work : mWorkArray) {
                        mBackgroundScheduler.schedule(work.getWorkSpec());
                    }
                }
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
    }

}
