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

package androidx.work.impl;

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

import androidx.work.BaseWork;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Work;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;
import androidx.work.impl.background.greedy.GreedyScheduler;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.CancelWorkRunnable;
import androidx.work.impl.utils.LiveDataUtils;
import androidx.work.impl.utils.StartWorkRunnable;
import androidx.work.impl.utils.StopWorkRunnable;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;

/**
 * A concrete implementation of {@link WorkManager}.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerImpl extends WorkManager {

    public static final int MAX_PRE_JOB_SCHEDULER_API_LEVEL = 22;
    public static final int MIN_JOB_SCHEDULER_API_LEVEL = 23;

    private WorkDatabase mWorkDatabase;
    private TaskExecutor mTaskExecutor;
    private List<Scheduler> mSchedulers;
    private Processor mProcessor;

    private static WorkManagerImpl sInstance = null;

    /**
     * Retrieves the singleton instance of {@link WorkManagerImpl}.
     *
     * @param context A {@link Context} object for configuration purposes.  Internally, this class
     *                will call {@link Context#getApplicationContext()}, so you may safely pass in
     *                any Context without risking a memory leak.
     * @return The singleton instance of {@link WorkManagerImpl}
     */
    public static synchronized WorkManagerImpl getInstance(Context context) {
        if (sInstance == null) {
            context = context.getApplicationContext();
            sInstance = new WorkManagerImpl(context, new WorkManagerConfiguration(context));
        }
        return sInstance;
    }

    WorkManagerImpl(Context context, WorkManagerConfiguration configuration) {
        mWorkDatabase = configuration.getWorkDatabase();

        mSchedulers = new ArrayList<>();
        mSchedulers.add(configuration.getBackgroundScheduler());
        mSchedulers.add(new GreedyScheduler(context, this));

        mTaskExecutor = WorkManagerTaskExecutor.getInstance();
        mProcessor = new Processor(
                context,
                mWorkDatabase,
                mSchedulers,
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
     * @return The {@link Scheduler}s associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull List<Scheduler> getSchedulers() {
        return mSchedulers;
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
    public void cancelWorkById(@NonNull String id) {
        mTaskExecutor.executeOnBackgroundThread(new CancelWorkRunnable(this, id, null));
    }

    @Override
    @WorkerThread
    public void cancelWorkByIdSync(@NonNull String id) {
        assertBackgroundThread("Cannot cancelWorkByIdSync on main thread!");
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
    public @Nullable WorkStatus getStatusSync(@NonNull String id) {
        assertBackgroundThread("Cannot call getStatusSync on main thread!");
        WorkSpec.IdStateAndOutput idStateAndOutput =
                mWorkDatabase.workSpecDao().getIdStateAndOutput(id);
        if (idStateAndOutput != null) {
            return new WorkStatus(
                    idStateAndOutput.id,
                    idStateAndOutput.state,
                    idStateAndOutput.output);
        } else {
            return null;
        }
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
