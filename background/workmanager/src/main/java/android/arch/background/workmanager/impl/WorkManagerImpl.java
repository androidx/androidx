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

import android.arch.background.workmanager.BaseWork;
import android.arch.background.workmanager.ExistingWorkPolicy;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkContinuation;
import android.arch.background.workmanager.WorkManager;
import android.arch.background.workmanager.WorkStatus;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkSpecDao;
import android.arch.background.workmanager.impl.utils.CancelWorkRunnable;
import android.arch.background.workmanager.impl.utils.LiveDataUtils;
import android.arch.background.workmanager.impl.utils.PruneDatabaseRunnable;
import android.arch.background.workmanager.impl.utils.StartWorkRunnable;
import android.arch.background.workmanager.impl.utils.StopWorkRunnable;
import android.arch.background.workmanager.impl.utils.taskexecutor.TaskExecutor;
import android.arch.background.workmanager.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A concrete implementation of {@link WorkManager}.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerImpl extends WorkManager {

    public static final int MAX_PRE_JOB_SCHEDULER_API_LEVEL = 23;
    public static final int MIN_JOB_SCHEDULER_API_LEVEL = 24;

    private WorkManagerConfiguration mConfiguration;
    private WorkDatabase mWorkDatabase;
    private TaskExecutor mTaskExecutor;
    private Scheduler mBackgroundScheduler;
    private Processor mProcessor;

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
        // TODO(janclarin): Remove context parameter.
        mConfiguration = configuration;
        mWorkDatabase = configuration.getWorkDatabase();
        mBackgroundScheduler = configuration.getBackgroundScheduler();
        mTaskExecutor = WorkManagerTaskExecutor.getInstance();
        mProcessor = new Processor(
                context.getApplicationContext(),
                mWorkDatabase,
                mBackgroundScheduler,
                configuration.getExecutorService());
    }

    /**
     * @return The {@link WorkDatabase} instance associated with this WorkManager.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    /**
     * @return The {@link Scheduler} associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Scheduler getBackgroundScheduler() {
        return mBackgroundScheduler;
    }

    /**
     * @return The {@link Processor} used to process background work.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Processor getProcessor() {
        return mProcessor;
    }

    /**
     * @return the {@link TaskExecutor} used by the instance of {@link WorkManager}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull TaskExecutor getTaskExecutor() {
        return mTaskExecutor;
    }

    @Override
    public void enqueue(@NonNull List<? extends BaseWork> baseWork) {
        new WorkContinuationImpl(this, baseWork).enqueue();
    }

    @Override
    public WorkContinuation beginWith(@NonNull List<Work> work) {
        return new WorkContinuationImpl(this, work);
    }

    @Override
    public WorkContinuation beginWithUniqueTag(
            @NonNull String tag,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<Work> work) {
        return new WorkContinuationImpl(this, tag, existingWorkPolicy, work);
    }

    @Override
    public void cancelWorkForId(@NonNull String id) {
        mTaskExecutor.executeOnBackgroundThread(new CancelWorkRunnable(this, id, null));
    }

    @Override
    @WorkerThread
    public void cancelWorkForIdSync(@NonNull String id) {
        assertBackgroundThread("Cannot cancelWorkForIdSync on main thread!");
        new CancelWorkRunnable(this, id, null).run();
    }

    @Override
    public void cancelAllWorkWithTag(@NonNull final String tag) {
        mTaskExecutor.executeOnBackgroundThread(new CancelWorkRunnable(this, null, tag));
    }

    @Override
    @WorkerThread
    public void cancelAllWorkWithTagSync(@NonNull String tag) {
        assertBackgroundThread("Cannot cancelAllWorkWithTagSync on main thread!");
        new CancelWorkRunnable(this, null, tag).run();
    }

    @Override
    public void pruneDatabase() {
        mTaskExecutor.executeOnBackgroundThread(new PruneDatabaseRunnable(mWorkDatabase));
    }

    @Override
    public LiveData<WorkStatus> getStatus(@NonNull String id) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        final MediatorLiveData<WorkStatus> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(
                LiveDataUtils.dedupedLiveDataFor(
                        dao.getIdStateAndOutputsLiveData(Collections.singletonList(id))),
                new Observer<List<WorkSpec.IdStateAndOutput>>() {
                    @Override
                    public void onChanged(
                            @Nullable List<WorkSpec.IdStateAndOutput> idStateAndOutputs) {
                        WorkStatus workStatus = null;
                        if (idStateAndOutputs != null && idStateAndOutputs.size() > 0) {
                            WorkSpec.IdStateAndOutput idStateAndOutput = idStateAndOutputs.get(0);
                            workStatus = new WorkStatus(
                                    idStateAndOutput.id,
                                    idStateAndOutput.state,
                                    idStateAndOutput.output);
                        }
                        mediatorLiveData.setValue(workStatus);
                    }
                });
        return mediatorLiveData;
    }

    @Override
    @WorkerThread
    public WorkStatus getStatusSync(@NonNull String id) {
        assertBackgroundThread("Cannot call getStatusSync on main thread!");
        WorkSpec.IdStateAndOutput idStateAndOutput =
                mWorkDatabase.workSpecDao().getIdStateAndOutput(id);
        return new WorkStatus(idStateAndOutput.id, idStateAndOutput.state, idStateAndOutput.output);
    }

    LiveData<List<WorkStatus>> getStatuses(@NonNull List<String> workSpecIds) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        final MediatorLiveData<List<WorkStatus>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(
                LiveDataUtils.dedupedLiveDataFor(dao.getIdStateAndOutputsLiveData(workSpecIds)),
                new Observer<List<WorkSpec.IdStateAndOutput>>() {
                    @Override
                    public void onChanged(
                            @Nullable List<WorkSpec.IdStateAndOutput> idStateAndOutputs) {
                        List<WorkStatus> workStatuses = null;
                        if (idStateAndOutputs != null) {
                            workStatuses = new ArrayList<>(idStateAndOutputs.size());
                            for (WorkSpec.IdStateAndOutput idStateAndOutput : idStateAndOutputs) {
                                workStatuses.add(new WorkStatus(
                                        idStateAndOutput.id,
                                        idStateAndOutput.state,
                                        idStateAndOutput.output));
                            }
                        }
                        mediatorLiveData.setValue(workStatuses);
                    }
                });
        return mediatorLiveData;
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to start
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startWork(String workSpecId) {
        mTaskExecutor.executeOnBackgroundThread(new StartWorkRunnable(this, workSpecId));
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to stop
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void stopWork(String workSpecId) {
        mTaskExecutor.executeOnBackgroundThread(new StopWorkRunnable(this, workSpecId));
    }

    private void assertBackgroundThread(String errorMessage) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
