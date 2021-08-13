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

import static android.app.PendingIntent.FLAG_MUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.text.TextUtils.isEmpty;

import static androidx.work.impl.foreground.SystemForegroundDispatcher.createCancelWorkIntent;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.core.os.BuildCompat;
import androidx.lifecycle.LiveData;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.R;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import androidx.work.impl.background.greedy.GreedyScheduler;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.model.RawWorkInfoDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.CancelWorkRunnable;
import androidx.work.impl.utils.ForceStopRunnable;
import androidx.work.impl.utils.LiveDataUtils;
import androidx.work.impl.utils.PreferenceUtils;
import androidx.work.impl.utils.PruneWorkRunnable;
import androidx.work.impl.utils.RawQueries;
import androidx.work.impl.utils.StartWorkRunnable;
import androidx.work.impl.utils.StatusRunnable;
import androidx.work.impl.utils.StopWorkRunnable;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import androidx.work.multiprocess.RemoteWorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A concrete implementation of {@link WorkManager}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerImpl extends WorkManager {

    private static final String TAG = Logger.tagWithPrefix("WorkManagerImpl");
    public static final int MAX_PRE_JOB_SCHEDULER_API_LEVEL = 22;
    public static final int MIN_JOB_SCHEDULER_API_LEVEL = 23;
    public static final String REMOTE_WORK_MANAGER_CLIENT =
            "androidx.work.multiprocess.RemoteWorkManagerClient";

    private Context mContext;
    private Configuration mConfiguration;
    private WorkDatabase mWorkDatabase;
    private TaskExecutor mWorkTaskExecutor;
    private List<Scheduler> mSchedulers;
    private Processor mProcessor;
    private PreferenceUtils mPreferenceUtils;
    private boolean mForceStopRunnableCompleted;
    private BroadcastReceiver.PendingResult mRescheduleReceiverResult;
    private volatile RemoteWorkManager mRemoteWorkManager;

    private static WorkManagerImpl sDelegatedInstance = null;
    private static WorkManagerImpl sDefaultInstance = null;
    private static final Object sLock = new Object();


    /**
     * @param delegate The delegate for {@link WorkManagerImpl} for testing; {@code null} to use the
     *                 default instance
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void setDelegate(@Nullable WorkManagerImpl delegate) {
        synchronized (sLock) {
            sDelegatedInstance = delegate;
        }
    }

    /**
     * Retrieves the singleton instance of {@link WorkManagerImpl}.
     *
     * @return The singleton instance of {@link WorkManagerImpl}
     * @hide
     * @deprecated Call {@link WorkManagerImpl#getInstance(Context)} instead.
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("NullableProblems")
    public static @Nullable WorkManagerImpl getInstance() {
        synchronized (sLock) {
            if (sDelegatedInstance != null) {
                return sDelegatedInstance;
            }

            return sDefaultInstance;
        }
    }

    /**
     * Retrieves the singleton instance of {@link WorkManagerImpl}.
     *
     * @param context A context for on-demand initialization.
     * @return The singleton instance of {@link WorkManagerImpl}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static @NonNull WorkManagerImpl getInstance(@NonNull Context context) {
        synchronized (sLock) {
            WorkManagerImpl instance = getInstance();
            if (instance == null) {
                Context appContext = context.getApplicationContext();
                if (appContext instanceof Configuration.Provider) {
                    initialize(
                            appContext,
                            ((Configuration.Provider) appContext).getWorkManagerConfiguration());
                    instance = getInstance(appContext);
                } else {
                    throw new IllegalStateException("WorkManager is not initialized properly.  You "
                            + "have explicitly disabled WorkManagerInitializer in your manifest, "
                            + "have not manually called WorkManager#initialize at this point, and "
                            + "your Application does not implement Configuration.Provider.");
                }
            }

            return instance;
        }
    }

    /**
     * Initializes the singleton instance of {@link WorkManagerImpl}.  You should only do this if
     * you want to use a custom {@link Configuration} object and have disabled
     * WorkManagerInitializer.
     *
     * @param context A {@link Context} object for configuration purposes. Internally, this class
     *                will call {@link Context#getApplicationContext()}, so you may safely pass in
     *                any Context without risking a memory leak.
     * @param configuration The {@link Configuration} for used to set up WorkManager.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void initialize(@NonNull Context context, @NonNull Configuration configuration) {
        synchronized (sLock) {
            if (sDelegatedInstance != null && sDefaultInstance != null) {
                throw new IllegalStateException("WorkManager is already initialized.  Did you "
                        + "try to initialize it manually without disabling "
                        + "WorkManagerInitializer? See "
                        + "WorkManager#initialize(Context, Configuration) or the class level "
                        + "Javadoc for more information.");
            }

            if (sDelegatedInstance == null) {
                context = context.getApplicationContext();
                if (sDefaultInstance == null) {
                    sDefaultInstance = new WorkManagerImpl(
                            context,
                            configuration,
                            new WorkManagerTaskExecutor(configuration.getTaskExecutor()));
                }
                sDelegatedInstance = sDefaultInstance;
            }
        }
    }

    /**
     * Create an instance of {@link WorkManagerImpl}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor) {
        this(context,
                configuration,
                workTaskExecutor,
                context.getResources().getBoolean(R.bool.workmanager_test_configuration));
    }

    /**
     * Create an instance of {@link WorkManagerImpl}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @param useTestDatabase {@code true} If using an in-memory test database
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            boolean useTestDatabase) {
        this(context,
                configuration,
                workTaskExecutor,
                WorkDatabase.create(
                        context.getApplicationContext(),
                        workTaskExecutor.getBackgroundExecutor(),
                        useTestDatabase)
        );
    }

    /**
     * Create an instance of {@link WorkManagerImpl}.
     *
     * @param context          The application {@link Context}
     * @param configuration    The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @param database         The {@link WorkDatabase}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase database) {
        Context applicationContext = context.getApplicationContext();
        Logger.setLogger(new Logger.LogcatLogger(configuration.getMinimumLoggingLevel()));
        List<Scheduler> schedulers =
                createSchedulers(applicationContext, configuration, workTaskExecutor);
        Processor processor = new Processor(
                context,
                configuration,
                workTaskExecutor,
                database,
                schedulers);
        internalInit(context, configuration, workTaskExecutor, database, schedulers, processor);
    }

    /**
     * Create an instance of {@link WorkManagerImpl}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @param workDatabase The {@link WorkDatabase} instance
     * @param processor The {@link Processor} instance
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase workDatabase,
            @NonNull List<Scheduler> schedulers,
            @NonNull Processor processor) {
        internalInit(context, configuration, workTaskExecutor, workDatabase, schedulers, processor);
    }

    /**
     * @return The application {@link Context} associated with this WorkManager.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Context getApplicationContext() {
        return mContext;
    }

    /**
     * @return The {@link WorkDatabase} instance associated with this WorkManager.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    /**
     * @return The {@link Configuration} instance associated with this WorkManager.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
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
    public @NonNull TaskExecutor getWorkTaskExecutor() {
        return mWorkTaskExecutor;
    }

    /**
     * @return the {@link PreferenceUtils} used by the instance of {@link WorkManager}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull PreferenceUtils getPreferenceUtils() {
        return mPreferenceUtils;
    }

    @Override
    @NonNull
    public Operation enqueue(
            @NonNull List<? extends WorkRequest> requests) {

        // This error is not being propagated as part of the Operation, as we want the
        // app to crash during development. Having no workRequests is always a developer error.
        if (requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "enqueue needs at least one WorkRequest.");
        }
        return new WorkContinuationImpl(this, requests).enqueue();
    }

    @Override
    public @NonNull WorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work) {
        if (work.isEmpty()) {
            throw new IllegalArgumentException(
                    "beginWith needs at least one OneTimeWorkRequest.");
        }
        return new WorkContinuationImpl(this, work);
    }

    @Override
    public @NonNull WorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        if (work.isEmpty()) {
            throw new IllegalArgumentException(
                    "beginUniqueWork needs at least one OneTimeWorkRequest.");
        }
        return new WorkContinuationImpl(this, uniqueWorkName, existingWorkPolicy, work);
    }

    @NonNull
    @Override
    public Operation enqueueUniqueWork(@NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        return new WorkContinuationImpl(this, uniqueWorkName, existingWorkPolicy, work).enqueue();
    }

    @Override
    @NonNull
    public Operation enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork) {

        return createWorkContinuationForUniquePeriodicWork(
                uniqueWorkName,
                existingPeriodicWorkPolicy,
                periodicWork)
                .enqueue();
    }

    /**
     * Creates a {@link WorkContinuation} for the given unique {@link PeriodicWorkRequest}.
     */
    @NonNull
    public WorkContinuationImpl createWorkContinuationForUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork) {
        ExistingWorkPolicy existingWorkPolicy;
        if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.KEEP) {
            existingWorkPolicy = ExistingWorkPolicy.KEEP;
        } else {
            existingWorkPolicy = ExistingWorkPolicy.REPLACE;
        }
        return new WorkContinuationImpl(
                this,
                uniqueWorkName,
                existingWorkPolicy,
                Collections.singletonList(periodicWork));
    }

    @Override
    public @NonNull Operation cancelWorkById(@NonNull UUID id) {
        CancelWorkRunnable runnable = CancelWorkRunnable.forId(id, this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getOperation();
    }

    @Override
    public @NonNull Operation cancelAllWorkByTag(@NonNull final String tag) {
        CancelWorkRunnable runnable = CancelWorkRunnable.forTag(tag, this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getOperation();
    }

    @Override
    @NonNull
    public Operation cancelUniqueWork(@NonNull String uniqueWorkName) {
        CancelWorkRunnable runnable = CancelWorkRunnable.forName(uniqueWorkName, this, true);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getOperation();
    }

    @Override
    public @NonNull Operation cancelAllWork() {
        CancelWorkRunnable runnable = CancelWorkRunnable.forAll(this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getOperation();
    }

    @NonNull
    @Override
    public PendingIntent createCancelPendingIntent(@NonNull UUID id) {
        Intent intent = createCancelWorkIntent(mContext, id.toString());
        int flags = FLAG_UPDATE_CURRENT;
        if (BuildCompat.isAtLeastS()) {
            flags |= FLAG_MUTABLE;
        }
        return PendingIntent.getService(mContext, 0, intent, flags);
    }

    @Override
    public @NonNull LiveData<Long> getLastCancelAllTimeMillisLiveData() {
        return mPreferenceUtils.getLastCancelAllTimeMillisLiveData();
    }

    @Override
    public @NonNull ListenableFuture<Long> getLastCancelAllTimeMillis() {
        final SettableFuture<Long> future = SettableFuture.create();
        // Avoiding synthetic accessors.
        final PreferenceUtils preferenceUtils = mPreferenceUtils;
        mWorkTaskExecutor.executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(preferenceUtils.getLastCancelAllTimeMillis());
                } catch (Throwable throwable) {
                    future.setException(throwable);
                }
            }
        });
        return future;
    }

    @Override
    public @NonNull Operation pruneWork() {
        PruneWorkRunnable runnable = new PruneWorkRunnable(this);
        mWorkTaskExecutor.executeOnBackgroundThread(runnable);
        return runnable.getOperation();
    }

    @Override
    public @NonNull LiveData<WorkInfo> getWorkInfoByIdLiveData(@NonNull UUID id) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkInfoPojo>> inputLiveData =
                dao.getWorkStatusPojoLiveDataForIds(Collections.singletonList(id.toString()));
        return LiveDataUtils.dedupedMappedLiveDataFor(inputLiveData,
                new Function<List<WorkSpec.WorkInfoPojo>, WorkInfo>() {
                    @Override
                    public WorkInfo apply(List<WorkSpec.WorkInfoPojo> input) {
                        WorkInfo workInfo = null;
                        if (input != null && input.size() > 0) {
                            workInfo = input.get(0).toWorkInfo();
                        }
                        return workInfo;
                    }
                },
                mWorkTaskExecutor);
    }

    @Override
    public @NonNull ListenableFuture<WorkInfo> getWorkInfoById(@NonNull UUID id) {
        StatusRunnable<WorkInfo> runnable = StatusRunnable.forUUID(this, id);
        mWorkTaskExecutor.getBackgroundExecutor().execute(runnable);
        return runnable.getFuture();
    }

    @Override
    public @NonNull LiveData<List<WorkInfo>> getWorkInfosByTagLiveData(@NonNull String tag) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkInfoPojo>> inputLiveData =
                workSpecDao.getWorkStatusPojoLiveDataForTag(tag);
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_INFO_MAPPER,
                mWorkTaskExecutor);
    }

    @Override
    public @NonNull ListenableFuture<List<WorkInfo>> getWorkInfosByTag(@NonNull String tag) {
        StatusRunnable<List<WorkInfo>> runnable = StatusRunnable.forTag(this, tag);
        mWorkTaskExecutor.getBackgroundExecutor().execute(runnable);
        return runnable.getFuture();
    }

    @Override
    @NonNull
    public LiveData<List<WorkInfo>> getWorkInfosForUniqueWorkLiveData(
            @NonNull String uniqueWorkName) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkInfoPojo>> inputLiveData =
                workSpecDao.getWorkStatusPojoLiveDataForName(uniqueWorkName);
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_INFO_MAPPER,
                mWorkTaskExecutor);
    }

    @Override
    @NonNull
    public ListenableFuture<List<WorkInfo>> getWorkInfosForUniqueWork(
            @NonNull String uniqueWorkName) {
        StatusRunnable<List<WorkInfo>> runnable =
                StatusRunnable.forUniqueWork(this, uniqueWorkName);
        mWorkTaskExecutor.getBackgroundExecutor().execute(runnable);
        return runnable.getFuture();
    }

    @NonNull
    @Override
    public LiveData<List<WorkInfo>> getWorkInfosLiveData(
            @NonNull WorkQuery workQuery) {
        RawWorkInfoDao rawWorkInfoDao = mWorkDatabase.rawWorkInfoDao();
        LiveData<List<WorkSpec.WorkInfoPojo>> inputLiveData =
                rawWorkInfoDao.getWorkInfoPojosLiveData(
                        RawQueries.workQueryToRawQuery(workQuery));
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_INFO_MAPPER,
                mWorkTaskExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<List<WorkInfo>> getWorkInfos(
            @NonNull WorkQuery workQuery) {
        StatusRunnable<List<WorkInfo>> runnable =
                StatusRunnable.forWorkQuerySpec(this, workQuery);
        mWorkTaskExecutor.getBackgroundExecutor().execute(runnable);
        return runnable.getFuture();
    }

    LiveData<List<WorkInfo>> getWorkInfosById(@NonNull List<String> workSpecIds) {
        WorkSpecDao dao = mWorkDatabase.workSpecDao();
        LiveData<List<WorkSpec.WorkInfoPojo>> inputLiveData =
                dao.getWorkStatusPojoLiveDataForIds(workSpecIds);
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_INFO_MAPPER,
                mWorkTaskExecutor);
    }

    /**
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public RemoteWorkManager getRemoteWorkManager() {
        if (mRemoteWorkManager == null) {
            synchronized (sLock) {
                if (mRemoteWorkManager == null) {
                    // Initialize multi-process support.
                    tryInitializeMultiProcessSupport();
                    if (mRemoteWorkManager == null && !isEmpty(
                            mConfiguration.getDefaultProcessName())) {
                        String message = "Invalid multiprocess configuration. Define an "
                                + "`implementation` dependency on :work:work-multiprocess library";
                        throw new IllegalStateException(message);
                    }
                }
            }
        }
        return mRemoteWorkManager;
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to start
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startWork(@NonNull String workSpecId) {
        startWork(workSpecId, null);
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to start
     * @param runtimeExtras The {@link WorkerParameters.RuntimeExtras} associated with this work
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void startWork(
            @NonNull String workSpecId,
            @Nullable WorkerParameters.RuntimeExtras runtimeExtras) {
        mWorkTaskExecutor
                .executeOnBackgroundThread(
                        new StartWorkRunnable(this, workSpecId, runtimeExtras));
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to stop
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void stopWork(@NonNull String workSpecId) {
        mWorkTaskExecutor.executeOnBackgroundThread(new StopWorkRunnable(this, workSpecId, false));
    }

    /**
     * @param workSpecId The {@link WorkSpec} id to stop when running in the context of a
     *                   foreground service.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void stopForegroundWork(@NonNull String workSpecId) {
        mWorkTaskExecutor.executeOnBackgroundThread(new StopWorkRunnable(this, workSpecId, true));
    }

    /**
     * Reschedules all the eligible work. Useful for cases like, app was force stopped or
     * BOOT_COMPLETED, TIMEZONE_CHANGED and TIME_SET for AlarmManager.
     *
     * @hide
     */
    public void rescheduleEligibleWork() {
        // TODO (rahulrav@) Make every scheduler do its own cancelAll().
        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            SystemJobScheduler.cancelAll(getApplicationContext());
        }

        // Reset scheduled state.
        getWorkDatabase().workSpecDao().resetScheduledState();

        // Delegate to the WorkManager's schedulers.
        // Using getters here so we can use from a mocked instance
        // of WorkManagerImpl.
        Schedulers.schedule(getConfiguration(), getWorkDatabase(), getSchedulers());
    }

    /**
     * A way for {@link ForceStopRunnable} to tell {@link WorkManagerImpl} that it has completed.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onForceStopRunnableCompleted() {
        synchronized (sLock) {
            mForceStopRunnableCompleted = true;
            if (mRescheduleReceiverResult != null) {
                mRescheduleReceiverResult.finish();
                mRescheduleReceiverResult = null;
            }
        }
    }

    /**
     * This method is invoked by
     * {@link androidx.work.impl.background.systemalarm.RescheduleReceiver}
     * after a call to {@link BroadcastReceiver#goAsync()}. Once {@link ForceStopRunnable} is done,
     * we can safely call {@link BroadcastReceiver.PendingResult#finish()}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setReschedulePendingResult(
            @NonNull BroadcastReceiver.PendingResult rescheduleReceiverResult) {
        synchronized (sLock) {
            mRescheduleReceiverResult = rescheduleReceiverResult;
            if (mForceStopRunnableCompleted) {
                mRescheduleReceiverResult.finish();
                mRescheduleReceiverResult = null;
            }
        }
    }

    /**
     * Initializes an instance of {@link WorkManagerImpl}.
     *
     * @param context The application {@link Context}
     * @param configuration The {@link Configuration} configuration
     * @param workDatabase The {@link WorkDatabase} instance
     * @param schedulers The {@link List} of {@link Scheduler}s to use
     * @param processor The {@link Processor} instance
     */
    private void internalInit(@NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase workDatabase,
            @NonNull List<Scheduler> schedulers,
            @NonNull Processor processor) {

        context = context.getApplicationContext();
        mContext = context;
        mConfiguration = configuration;
        mWorkTaskExecutor = workTaskExecutor;
        mWorkDatabase = workDatabase;
        mSchedulers = schedulers;
        mProcessor = processor;
        mPreferenceUtils = new PreferenceUtils(workDatabase);
        mForceStopRunnableCompleted = false;

        // Check for direct boot mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context.isDeviceProtectedStorage()) {
            throw new IllegalStateException("Cannot initialize WorkManager in direct boot mode");
        }

        // Checks for app force stops.
        mWorkTaskExecutor.executeOnBackgroundThread(new ForceStopRunnable(context, this));
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public List<Scheduler> createSchedulers(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor taskExecutor) {

        return Arrays.asList(
                Schedulers.createBestAvailableBackgroundScheduler(context, this),
                // Specify the task executor directly here as this happens before internalInit.
                // GreedyScheduler creates ConstraintTrackers and controllers eagerly.
                new GreedyScheduler(context, configuration, taskExecutor, this));
    }

    /**
     * Tries to find a multi-process safe implementation for  {@link WorkManager}.
     */
    private void tryInitializeMultiProcessSupport() {
        try {
            Class<?> klass = Class.forName(REMOTE_WORK_MANAGER_CLIENT);
            mRemoteWorkManager = (RemoteWorkManager) klass.getConstructor(
                    Context.class, WorkManagerImpl.class
            ).newInstance(mContext, this);
        } catch (Throwable throwable) {
            Logger.get().debug(TAG, "Unable to initialize multi-process support", throwable);
        }
    }
}
