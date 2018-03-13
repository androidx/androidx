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

import androidx.work.BaseWork;
import androidx.work.BlockingWorkManagerMethods;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Work;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;
import androidx.work.impl.background.greedy.GreedyScheduler;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.CancelWorkRunnable;
import androidx.work.impl.utils.ForceStopRunnable;
import androidx.work.impl.utils.LiveDataUtils;
import androidx.work.impl.utils.StartWorkRunnable;
import androidx.work.impl.utils.StopWorkRunnable;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A concrete implementation of {@link WorkManager}.
 *
 * @hide
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerImpl extends WorkManager implements BlockingWorkManagerMethods {

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

        // Checks for app force stops.
        mTaskExecutor.executeOnBackgroundThread(new ForceStopRunnable(context, this));
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
    public WorkContinuation beginWithName(
            @NonNull String name,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<Work> work) {
        return new WorkContinuationImpl(this, name, existingWorkPolicy, work);
    }

    @Override
    public void cancelWorkById(@NonNull String id) {
        mTaskExecutor.executeOnBackgroundThread(CancelWorkRunnable.forId(id, this));
    }

    @Override
    @WorkerThread
    public void cancelWorkByIdBlocking(@NonNull String id) {
        assertBackgroundThread("Cannot cancelWorkByIdBlocking on main thread!");
        CancelWorkRunnable.forId(id, this).run();
    }

    @Override
    public void cancelAllWorkWithTag(@NonNull final String tag) {
        mTaskExecutor.executeOnBackgroundThread(
                CancelWorkRunnable.forTag(tag, this));
    }

    @Override
    @WorkerThread
    public void cancelAllWorkWithTagBlocking(@NonNull String tag) {
        assertBackgroundThread("Cannot cancelAllWorkWithTagBlocking on main thread!");
        CancelWorkRunnable.forTag(tag, this).run();
    }

    @Override
    public void cancelAllWorkWithName(@NonNull String name) {
        mTaskExecutor.executeOnBackgroundThread(
                CancelWorkRunnable.forName(name, this));
    }

    @Override
    public void cancelAllWorkWithNameBlocking(@NonNull String name) {
        assertBackgroundThread("Cannot cancelAllWorkWithNameBlocking on main thread!");
        CancelWorkRunnable.forName(name, this).run();
    }

    @Override
    public LiveData<WorkStatus> getStatusById(@NonNull String id) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        final MediatorLiveData<WorkStatus> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(
                LiveDataUtils.dedupedLiveDataFor(
                        dao.getIdStateAndOutputsLiveDataForIds(Collections.singletonList(id))),
                new Observer<List<WorkSpec.WorkStatusPojo>>() {
                    @Override
                    public void onChanged(
                            @Nullable List<WorkSpec.WorkStatusPojo> workStatusPojos) {
                        WorkStatus workStatus = null;
                        if (workStatusPojos != null && workStatusPojos.size() > 0) {
                            workStatus = workStatusPojos.get(0).toWorkStatus();
                        }
                        mediatorLiveData.setValue(workStatus);
                    }
                });
        return mediatorLiveData;
    }

    @Override
    @WorkerThread
    public @Nullable WorkStatus getStatusByIdBlocking(@NonNull String id) {
        assertBackgroundThread("Cannot call getStatusByIdBlocking on main thread!");
        WorkSpec.WorkStatusPojo workStatusPojo =
                mWorkDatabase.workSpecDao().getIdStateAndOutputForId(id);
        if (workStatusPojo != null) {
            return workStatusPojo.toWorkStatus();
        } else {
            return null;
        }
    }

    @Override
    public LiveData<List<WorkStatus>> getStatusesByTag(@NonNull String tag) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        final MediatorLiveData<List<WorkStatus>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(
                LiveDataUtils.dedupedLiveDataFor(
                        workSpecDao.getIdStateAndOutputLiveDataForTag(tag)),
                new Observer<List<WorkSpec.WorkStatusPojo>>() {
                    @Override
                    public void onChanged(
                            @Nullable List<WorkSpec.WorkStatusPojo> workStatusPojos) {
                        List<WorkStatus> workStatuses = null;
                        if (workStatusPojos != null) {
                            workStatuses = new ArrayList<>(workStatusPojos.size());
                            for (WorkSpec.WorkStatusPojo workStatusPojo : workStatusPojos) {
                                workStatuses.add(workStatusPojo.toWorkStatus());
                            }
                        }
                        mediatorLiveData.setValue(workStatuses);
                    }
                });
        return mediatorLiveData;
    }

    @Override
    public List<WorkStatus> getStatusesByTagBlocking(@NonNull String tag) {
        assertBackgroundThread("Cannot call getStatusesByTagBlocking on main thread!");
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        List<WorkStatus> workStatuses = null;
        List<WorkSpec.WorkStatusPojo> workStatusPojos =
                workSpecDao.getIdStateAndOutputForTag(tag);
        if (workStatusPojos != null) {
            workStatuses = new ArrayList<>(workStatusPojos.size());
            for (WorkSpec.WorkStatusPojo workStatusPojo : workStatusPojos) {
                workStatuses.add(workStatusPojo.toWorkStatus());
            }
        }
        return workStatuses;
    }

    @Override
    public BlockingWorkManagerMethods blocking() {
        return this;
    }

    LiveData<List<WorkStatus>> getStatusesById(@NonNull List<String> workSpecIds) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        final MediatorLiveData<List<WorkStatus>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(
                LiveDataUtils.dedupedLiveDataFor(
                        dao.getIdStateAndOutputsLiveDataForIds(workSpecIds)),
                new Observer<List<WorkSpec.WorkStatusPojo>>() {
                    @Override
                    public void onChanged(
                            @Nullable List<WorkSpec.WorkStatusPojo> workStatusPojos) {
                        List<WorkStatus> workStatuses = null;
                        if (workStatusPojos != null) {
                            workStatuses = new ArrayList<>(workStatusPojos.size());
                            for (WorkSpec.WorkStatusPojo workStatusPojo : workStatusPojos) {
                                workStatuses.add(workStatusPojo.toWorkStatus());
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

    /**
     * Reschedules all the eligible work. Useful for cases like, app was force stopped or
     * BOOT_COMPLETED, TIMEZONE_CHANGED and TIME_SET for AlarmManager.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void rescheduleEligibleWork() {
        // Using getters here so we can use from a mocked instance
        // of WorkManagerImpl.
        List<WorkSpec> eligibleWorkSpecs = getWorkDatabase()
                .workSpecDao()
                .getEligibleWorkSpecs(Long.MAX_VALUE);

        // Delegate to the WorkManager's schedulers.
        for (Scheduler scheduler : getSchedulers()) {
            scheduler.schedule(eligibleWorkSpecs.toArray(new WorkSpec[0]));
        }
    }

    private void assertBackgroundThread(String errorMessage) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
