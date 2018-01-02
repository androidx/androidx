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

import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.PeriodicWork;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkContinuation;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.Worker;
import android.arch.background.workmanager.impl.foreground.ForegroundProcessor;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkSpecDao;
import android.arch.background.workmanager.impl.utils.BaseWorkHelper;
import android.arch.background.workmanager.impl.utils.CancelWorkRunnable;
import android.arch.background.workmanager.impl.utils.EnqueueRunnable;
import android.arch.background.workmanager.impl.utils.LiveDataUtils;
import android.arch.background.workmanager.impl.utils.PruneDatabaseRunnable;
import android.arch.background.workmanager.impl.utils.taskexecutor.TaskExecutor;
import android.arch.background.workmanager.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * A concrete implementation of {@link WorkManager}.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerImpl extends WorkManager {

    private WorkManagerConfiguration mConfiguration;
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
        mConfiguration = configuration;
        mWorkDatabase = configuration.getWorkDatabase();
        mForegroundProcessor = new ForegroundProcessor(
                appContext,
                mWorkDatabase,
                mBackgroundScheduler,
                configuration.getForegroundLifecycleOwner(),
                configuration.getForegroundExecutorService());
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
    public @NonNull Scheduler getBackgroundScheduler() {
        return mBackgroundScheduler;
    }

    /**
     * @return The {@link ExecutorService} for background {@link Processor}s.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull ExecutorService getBackgroundExecutorService() {
        return mConfiguration.getBackgroundExecutorService();
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
    public WorkContinuation enqueue(@NonNull Work... work) {
        return enqueue(work, null);
    }

    @SafeVarargs
    @Override
    public final WorkContinuation enqueue(@NonNull Class<? extends Worker>... workerClasses) {
        return enqueue(BaseWorkHelper.convertWorkerClassArrayToWorkArray(workerClasses), null);
    }

    @Override
    public void enqueue(@NonNull PeriodicWork... periodicWork) {
        mTaskExecutor.executeOnBackgroundThread(
                new EnqueueRunnable(this, periodicWork, null));
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

    WorkContinuation enqueue(@NonNull Work[] work, String[] prerequisiteIds) {
        WorkContinuation workContinuation = new WorkContinuationImpl(this, work);
        mTaskExecutor.executeOnBackgroundThread(
                new EnqueueRunnable(this, work, prerequisiteIds));
        return workContinuation;
    }

}
