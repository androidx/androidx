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

import static androidx.work.ListenableFutureKt.executeAsync;
import static androidx.work.impl.UnfinishedWorkListenerKt.maybeLaunchUnfinishedWorkListener;
import static androidx.work.impl.WorkManagerImplExtKt.createWorkManager;
import static androidx.work.impl.WorkManagerImplExtKt.createWorkManagerScope;
import static androidx.work.impl.WorkerUpdater.enqueueUniquelyNamedPeriodic;
import static androidx.work.impl.foreground.SystemForegroundDispatcher.createCancelWorkIntent;
import static androidx.work.impl.model.RawWorkInfoDaoKt.getWorkInfoPojosFlow;
import static androidx.work.impl.model.WorkSpecDaoKt.getWorkStatusPojoFlowDataForIds;
import static androidx.work.impl.model.WorkSpecDaoKt.getWorkStatusPojoFlowForName;
import static androidx.work.impl.model.WorkSpecDaoKt.getWorkStatusPojoFlowForTag;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.StopReason;
import androidx.work.TracerKt;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import androidx.work.impl.background.systemalarm.RescheduleReceiver;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.RawWorkInfoDao;
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.CancelWorkRunnable;
import androidx.work.impl.utils.ForceStopRunnable;
import androidx.work.impl.utils.LiveDataUtils;
import androidx.work.impl.utils.PreferenceUtils;
import androidx.work.impl.utils.PruneWorkRunnableKt;
import androidx.work.impl.utils.RawQueries;
import androidx.work.impl.utils.StatusRunnable;
import androidx.work.impl.utils.StopWorkRunnable;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.multiprocess.RemoteWorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Unit;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * A concrete implementation of {@link WorkManager}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerImpl extends WorkManager {

    private static final String TAG = Logger.tagWithPrefix("WorkManagerImpl");

    public static final int CONTENT_URI_TRIGGER_API_LEVEL = 24;
    public static final String REMOTE_WORK_MANAGER_CLIENT =
            "androidx.work.multiprocess.RemoteWorkManagerClient";

    private Context mContext;
    private Configuration mConfiguration;
    private WorkDatabase mWorkDatabase;
    private TaskExecutor mWorkTaskExecutor;
    private List<Scheduler> mSchedulers;
    private Processor mProcessor;
    private PreferenceUtils mPreferenceUtils;
    private boolean mForceStopRunnableCompleted = false;
    private BroadcastReceiver.PendingResult mRescheduleReceiverResult;
    private volatile RemoteWorkManager mRemoteWorkManager;
    private final Trackers mTrackers;
    /**
     * Job for the scope of the whole WorkManager
     */
    private final CoroutineScope mWorkManagerScope;
    private static WorkManagerImpl sDelegatedInstance = null;
    private static WorkManagerImpl sDefaultInstance = null;
    private static final Object sLock = new Object();

    /**
     * @param delegate The delegate for {@link WorkManagerImpl} for testing; {@code null} to use the
     *                 default instance
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
     *
     */
    @SuppressWarnings("deprecation")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static boolean isInitialized() {
        WorkManagerImpl instance = getInstance();
        return instance != null;
    }

    /**
     * Retrieves the singleton instance of {@link WorkManagerImpl}.
     *
     * @param context A context for on-demand initialization.
     * @return The singleton instance of {@link WorkManagerImpl}
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
     * @param context       A {@link Context} object for configuration purposes. Internally, this
     *                      class will call {@link Context#getApplicationContext()}, so you may
     *                      safely pass in any Context without risking a memory leak.
     * @param configuration The {@link Configuration} for used to set up WorkManager.
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
                    sDefaultInstance = createWorkManager(context, configuration);
                }
                sDelegatedInstance = sDefaultInstance;
            }
        }
    }

    /**
     * Create an instance of {@link WorkManagerImpl}.
     *
     * @param context          The application {@link Context}
     * @param configuration    The {@link Configuration} configuration
     * @param workTaskExecutor The {@link TaskExecutor} for running "processing" jobs, such as
     *                         enqueueing, scheduling, cancellation, etc.
     * @param workDatabase     The {@link WorkDatabase} instance
     * @param processor        The {@link Processor} instance
     * @param trackers         Trackers
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration,
            @NonNull TaskExecutor workTaskExecutor,
            @NonNull WorkDatabase workDatabase,
            @NonNull List<Scheduler> schedulers,
            @NonNull Processor processor,
            @NonNull Trackers trackers) {
        context = context.getApplicationContext();
        // Check for direct boot mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Api24Impl.isDeviceProtectedStorage(
                context)) {
            throw new IllegalStateException("Cannot initialize WorkManager in direct boot mode");
        }
        Logger.setLogger(new Logger.LogcatLogger(configuration.getMinimumLoggingLevel()));
        mContext = context;
        mWorkTaskExecutor = workTaskExecutor;
        mWorkDatabase = workDatabase;
        mProcessor = processor;
        mTrackers = trackers;
        mConfiguration = configuration;
        mSchedulers = schedulers;
        mWorkManagerScope = createWorkManagerScope(mWorkTaskExecutor);
        mPreferenceUtils = new PreferenceUtils(mWorkDatabase);
        Schedulers.registerRescheduling(schedulers, mProcessor,
                workTaskExecutor.getSerialTaskExecutor(), mWorkDatabase, configuration);
        // Checks for app force stops.
        mWorkTaskExecutor.executeOnTaskThread(new ForceStopRunnable(context, this));
        maybeLaunchUnfinishedWorkListener(mWorkManagerScope, mContext, configuration, workDatabase);
    }

    /**
     * @return The application {@link Context} associated with this WorkManager.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Context getApplicationContext() {
        return mContext;
    }

    /**
     * @return The {@link WorkDatabase} instance associated with this WorkManager.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull WorkDatabase getWorkDatabase() {
        return mWorkDatabase;
    }

    /**
     * @return workmanager's CoroutineScope
     */
    @NonNull CoroutineScope getWorkManagerScope() {
        return mWorkManagerScope;
    }

    /**
     * @return The {@link Configuration} instance associated with this WorkManager.
     */
    @Override
    public @NonNull Configuration getConfiguration() {
        return mConfiguration;
    }

    /**
     * @return The {@link Scheduler}s associated with this WorkManager based on the device's
     * capabilities, SDK version, etc.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull List<Scheduler> getSchedulers() {
        return mSchedulers;
    }

    /**
     * @return The {@link Processor} used to process background work.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Processor getProcessor() {
        return mProcessor;
    }

    /**
     * @return the {@link TaskExecutor} used by the instance of {@link WorkManager}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull TaskExecutor getWorkTaskExecutor() {
        return mWorkTaskExecutor;
    }

    /**
     * @return the {@link PreferenceUtils} used by the instance of {@link WorkManager}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull PreferenceUtils getPreferenceUtils() {
        return mPreferenceUtils;
    }

    /**
     * @return the {@link Trackers} used by {@link WorkManager}
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Trackers getTrackers() {
        return mTrackers;
    }

    @Override
    public @NonNull Operation enqueue(
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
    public @NonNull WorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> requests) {
        if (requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "beginWith needs at least one OneTimeWorkRequest.");
        }
        return new WorkContinuationImpl(this, requests);
    }

    @Override
    public @NonNull WorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> requests) {
        if (requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "beginUniqueWork needs at least one OneTimeWorkRequest.");
        }
        return new WorkContinuationImpl(this, uniqueWorkName, existingWorkPolicy, requests);
    }

    @Override
    public @NonNull Operation enqueueUniqueWork(@NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> requests) {
        return new WorkContinuationImpl(this, uniqueWorkName,
                existingWorkPolicy, requests).enqueue();
    }

    @Override
    public @NonNull Operation enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest request) {
        if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.UPDATE) {
            return enqueueUniquelyNamedPeriodic(this, uniqueWorkName, request);
        }
        return createWorkContinuationForUniquePeriodicWork(
                uniqueWorkName,
                existingPeriodicWorkPolicy,
                request)
                .enqueue();
    }

    /**
     * Creates a {@link WorkContinuation} for the given unique {@link PeriodicWorkRequest}.
     */
    public @NonNull WorkContinuationImpl createWorkContinuationForUniquePeriodicWork(
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
        return CancelWorkRunnable.forId(id, this);
    }

    @Override
    public @NonNull Operation cancelAllWorkByTag(final @NonNull String tag) {
        return CancelWorkRunnable.forTag(tag, this);
    }

    @Override
    public @NonNull Operation cancelUniqueWork(@NonNull String uniqueWorkName) {
        return CancelWorkRunnable.forName(uniqueWorkName, this);
    }

    @Override
    public @NonNull Operation cancelAllWork() {
        return CancelWorkRunnable.forAll(this);
    }

    @Override
    public @NonNull PendingIntent createCancelPendingIntent(@NonNull UUID id) {
        Intent intent = createCancelWorkIntent(mContext, id.toString());
        int flags = FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) {
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
        final PreferenceUtils preferenceUtils = mPreferenceUtils;
        return executeAsync(mWorkTaskExecutor.getSerialTaskExecutor(),
                "getLastCancelAllTimeMillis", preferenceUtils::getLastCancelAllTimeMillis);
    }

    @Override
    public @NonNull Operation pruneWork() {
        return PruneWorkRunnableKt.pruneWork(mWorkDatabase, mConfiguration, mWorkTaskExecutor);
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
    public @NonNull Flow<WorkInfo> getWorkInfoByIdFlow(@NonNull UUID id) {
        return getWorkStatusPojoFlowDataForIds(getWorkDatabase().workSpecDao(), id);
    }

    @Override
    public @NonNull ListenableFuture<WorkInfo> getWorkInfoById(@NonNull UUID id) {
        return StatusRunnable.forUUID(mWorkDatabase, mWorkTaskExecutor, id);
    }

    @Override
    public @NonNull Flow<List<WorkInfo>> getWorkInfosByTagFlow(@NonNull String tag) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        return getWorkStatusPojoFlowForTag(workSpecDao,
                mWorkTaskExecutor.getTaskCoroutineDispatcher(), tag);
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
        return StatusRunnable.forTag(mWorkDatabase, mWorkTaskExecutor, tag);
    }

    @Override
    public @NonNull LiveData<List<WorkInfo>> getWorkInfosForUniqueWorkLiveData(
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
    public @NonNull Flow<List<WorkInfo>> getWorkInfosForUniqueWorkFlow(
            @NonNull String uniqueWorkName) {
        WorkSpecDao workSpecDao = mWorkDatabase.workSpecDao();
        return getWorkStatusPojoFlowForName(workSpecDao,
                mWorkTaskExecutor.getTaskCoroutineDispatcher(), uniqueWorkName);
    }

    @Override
    public @NonNull ListenableFuture<List<WorkInfo>> getWorkInfosForUniqueWork(
            @NonNull String uniqueWorkName) {
        return StatusRunnable.forUniqueWork(mWorkDatabase, mWorkTaskExecutor, uniqueWorkName);
    }

    @Override
    public @NonNull LiveData<List<WorkInfo>> getWorkInfosLiveData(
            @NonNull WorkQuery workQuery) {
        RawWorkInfoDao rawWorkInfoDao = mWorkDatabase.rawWorkInfoDao();
        LiveData<List<WorkSpec.WorkInfoPojo>> inputLiveData =
                rawWorkInfoDao.getWorkInfoPojosLiveData(
                        RawQueries.toRawQuery(workQuery));
        return LiveDataUtils.dedupedMappedLiveDataFor(
                inputLiveData,
                WorkSpec.WORK_INFO_MAPPER,
                mWorkTaskExecutor);
    }

    @Override
    public @NonNull Flow<List<WorkInfo>> getWorkInfosFlow(@NonNull WorkQuery workQuery) {
        RawWorkInfoDao rawWorkInfoDao = mWorkDatabase.rawWorkInfoDao();
        return getWorkInfoPojosFlow(rawWorkInfoDao, mWorkTaskExecutor.getTaskCoroutineDispatcher(),
                RawQueries.toRawQuery(workQuery));
    }

    @Override
    public @NonNull ListenableFuture<List<WorkInfo>> getWorkInfos(@NonNull WorkQuery workQuery) {
        return StatusRunnable.forWorkQuerySpec(mWorkDatabase, mWorkTaskExecutor, workQuery);
    }

    @Override
    public @NonNull ListenableFuture<UpdateResult> updateWork(@NonNull WorkRequest request) {
        return WorkerUpdater.updateWorkImpl(this, request);
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
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @Nullable RemoteWorkManager getRemoteWorkManager() {
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
     * @param id The {@link WorkSpec} id to stop when running in the context of a
     *           foreground service.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void stopForegroundWork(@NonNull WorkGenerationalId id, @StopReason int reason) {
        mWorkTaskExecutor.executeOnTaskThread(new StopWorkRunnable(mProcessor,
                new StartStopToken(id), true, reason));
    }

    /**
     * Reschedules all the eligible work. Useful for cases like, app was force stopped or
     * BOOT_COMPLETED, TIMEZONE_CHANGED and TIME_SET for AlarmManager.
     */
    public void rescheduleEligibleWork() {
        // Delegate to the getter so mocks continue to work when testing.
        Configuration configuration = getConfiguration();
        TracerKt.traced(configuration.getTracer(), "ReschedulingWork", () -> {
            // This gives us an easy way to clear persisted work state, and then reschedule work
            // that WorkManager is aware of. Ideally, we do something similar for other
            // persistent schedulers.
            SystemJobScheduler.cancelAllInAllNamespaces(getApplicationContext());
            // Reset scheduled state.
            getWorkDatabase().workSpecDao().resetScheduledState();

            // Delegate to the WorkManager's schedulers.
            // Using getters here so we can use from a mocked instance
            // of WorkManagerImpl.
            Schedulers.schedule(getConfiguration(), getWorkDatabase(), getSchedulers());
            return Unit.INSTANCE;
        });
    }

    /**
     * A way for {@link ForceStopRunnable} to tell {@link WorkManagerImpl} that it has completed.
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
     * {@link RescheduleReceiver}
     * after a call to {@link BroadcastReceiver#goAsync()}. Once {@link ForceStopRunnable} is done,
     * we can safely call {@link BroadcastReceiver.PendingResult#finish()}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setReschedulePendingResult(
            BroadcastReceiver.@NonNull PendingResult rescheduleReceiverResult) {
        synchronized (sLock) {
            // if we have two broadcast in the row, finish old one and use new one
            if (mRescheduleReceiverResult != null) {
                mRescheduleReceiverResult.finish();
            }
            mRescheduleReceiverResult = rescheduleReceiverResult;
            if (mForceStopRunnableCompleted) {
                mRescheduleReceiverResult.finish();
                mRescheduleReceiverResult = null;
            }
        }
    }

    /**
     * Cancels WorkManager Scope and closes the datastore.
     */
    public void closeDatabase() {
        WorkManagerImplExtKt.close(this);
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

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static boolean isDeviceProtectedStorage(Context context) {
            return context.isDeviceProtectedStorage();
        }
    }
}
