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

import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import androidx.work.BaseWork;
import androidx.work.BlockingWorkManager;
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
public class WorkManagerImpl extends WorkManager implements BlockingWorkManager {

    public static final int MAX_PRE_JOB_SCHEDULER_API_LEVEL = 22;
    public static final int MIN_JOB_SCHEDULER_API_LEVEL = 23;

    private WorkDatabase mWorkDatabase;
    private TaskExecutor mTaskExecutor;
    private List<Scheduler> mSchedulers;
    private Processor mProcessor;

    private static WorkManagerImpl sDelegatedInstance = null;
    private static WorkManagerImpl sDefaultInstance = null;

    /**
     * Retrieves the singleton instance of {@link WorkManagerImpl}.
     *
     * @return The singleton instance of {@link WorkManagerImpl}
     */
    public static synchronized WorkManagerImpl getInstance() {
        if (sDelegatedInstance != null) {
            return sDelegatedInstance;
        }

        return sDefaultInstance;
    }

    /**
     * @param delegate The delegate for {@link WorkManagerImpl} for testing; {@code null} to use the
     *                 default instance
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static synchronized void setDelegate(WorkManagerImpl delegate) {
        sDelegatedInstance = delegate;
    }

    /**
     * Initializes the singleton instance of {@link WorkManagerImpl}.
     *
     * @param context A {@link Context} object for configuration purposes.  Internally, this class
     *                will call {@link Context#getApplicationContext()}, so you may safely pass in
     *                any Context without risking a memory leak.
     * @return The singleton instance of {@link WorkManagerImpl}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static synchronized WorkManagerImpl initialize(Context context) {
        if (sDelegatedInstance == null) {
            context = context.getApplicationContext();
            if (sDefaultInstance == null) {
                sDefaultInstance =
                        new WorkManagerImpl(context, new WorkManagerConfiguration(context));
            }
            sDelegatedInstance = sDefaultInstance;
        }
        return sDelegatedInstance;
    }

    /**
     * Create an instance of {@link WorkManagerImpl}.
     * @param context The application {@link Context}
     * @param configuration The {@link WorkManagerConfiguration} configuration.
     *
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerImpl(Context context, WorkManagerConfiguration configuration) {
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
    public void cancelAllWorkByTag(@NonNull final String tag) {
        mTaskExecutor.executeOnBackgroundThread(
                CancelWorkRunnable.forTag(tag, this));
    }

    @Override
    @WorkerThread
    public void cancelAllWorkByTagBlocking(@NonNull String tag) {
        assertBackgroundThread("Cannot cancelAllWorkByTagBlocking on main thread!");
        CancelWorkRunnable.forTag(tag, this).run();
    }

    @Override
    public void cancelAllWorkByName(@NonNull String name) {
        mTaskExecutor.executeOnBackgroundThread(
                CancelWorkRunnable.forName(name, this));
    }

    @Override
    public void cancelAllWorkByNameBlocking(@NonNull String name) {
        assertBackgroundThread("Cannot cancelAllWorkByNameBlocking on main thread!");
        CancelWorkRunnable.forName(name, this).run();
    }

    @Override
    public LiveData<WorkStatus> getStatusById(@NonNull String id) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                dao.getWorkStatusPojoLiveDataForIds(Collections.singletonList(id));
        return LiveDataUtils.dedupedMappedLiveDataFor(inputLiveData,
                new Function<List<WorkSpec.WorkStatusPojo>, WorkStatus>() {
                    @Override
                    public WorkStatus apply(List<WorkSpec.WorkStatusPojo> input) {
                        WorkStatus workStatus = null;
                        if (input != null && input.size() > 0) {
                            workStatus = input.get(0).toWorkStatus();
                        }
                        return workStatus;
                    }
                });
    }

    @Override
    @WorkerThread
    public @Nullable WorkStatus getStatusByIdBlocking(@NonNull String id) {
        assertBackgroundThread("Cannot call getStatusByIdBlocking on main thread!");
        WorkSpec.WorkStatusPojo workStatusPojo =
                mWorkDatabase.workSpecDao().getWorkStatusPojoForId(id);
        if (workStatusPojo != null) {
            return workStatusPojo.toWorkStatus();
        } else {
            return null;
        }
    }

    @Override
    public LiveData<List<WorkStatus>> getStatusesByTag(@NonNull String tag) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                workSpecDao.getWorkStatusPojoLiveDataForTag(tag);
        return LiveDataUtils.dedupedMappedLiveDataFor(inputLiveData, WorkSpec.WORK_STATUS_MAPPER);
    }

    @Override
    public List<WorkStatus> getStatusesByTagBlocking(@NonNull String tag) {
        assertBackgroundThread("Cannot call getStatusesByTagBlocking on main thread!");
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        List<WorkSpec.WorkStatusPojo> input = workSpecDao.getWorkStatusPojoForTag(tag);
        return WorkSpec.WORK_STATUS_MAPPER.apply(input);
    }

    @Override
    public LiveData<List<WorkStatus>> getStatusesByName(@NonNull String name) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                workSpecDao.getWorkStatusPojoLiveDataForName(name);
        return LiveDataUtils.dedupedMappedLiveDataFor(inputLiveData, WorkSpec.WORK_STATUS_MAPPER);
    }

    @Override
    public List<WorkStatus> getStatusesByNameBlocking(@NonNull String name) {
        assertBackgroundThread("Cannot call getStatusesByNameBlocking on main thread!");
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        List<WorkSpec.WorkStatusPojo> input = workSpecDao.getWorkStatusPojoForName(name);
        return WorkSpec.WORK_STATUS_MAPPER.apply(input);
    }

    @Override
    public BlockingWorkManager blocking() {
        return this;
    }

    LiveData<List<WorkStatus>> getStatusesById(@NonNull List<String> workSpecIds) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkStatusPojo>> inputLiveData =
                dao.getWorkStatusPojoLiveDataForIds(workSpecIds);
        return LiveDataUtils.dedupedMappedLiveDataFor(inputLiveData, WorkSpec.WORK_STATUS_MAPPER);
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to start
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startWork(String workSpecId) {
        startWork(workSpecId, null);
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to start
     * @param runtimeExtras The {@link RuntimeExtras} associated with this work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startWork(String workSpecId, RuntimeExtras runtimeExtras) {
        mTaskExecutor.executeOnBackgroundThread(
                new StartWorkRunnable(this, workSpecId, runtimeExtras));
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
        // Reset scheduled state.
        getWorkDatabase().workSpecDao().resetScheduledState();

        // Delegate to the WorkManager's schedulers.
        // Using getters here so we can use from a mocked instance
        // of WorkManagerImpl.
        Schedulers.schedule(getWorkDatabase(), getSchedulers());
    }

    private void assertBackgroundThread(String errorMessage) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
