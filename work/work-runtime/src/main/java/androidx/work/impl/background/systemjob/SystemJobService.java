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

package androidx.work.impl.background.systemjob;

import static android.app.job.JobParameters.STOP_REASON_APP_STANDBY;
import static android.app.job.JobParameters.STOP_REASON_BACKGROUND_RESTRICTION;
import static android.app.job.JobParameters.STOP_REASON_CANCELLED_BY_APP;
import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW;
import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CHARGING;
import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY;
import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_DEVICE_IDLE;
import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW;
import static android.app.job.JobParameters.STOP_REASON_DEVICE_STATE;
import static android.app.job.JobParameters.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED;
import static android.app.job.JobParameters.STOP_REASON_PREEMPT;
import static android.app.job.JobParameters.STOP_REASON_QUOTA;
import static android.app.job.JobParameters.STOP_REASON_SYSTEM_PROCESSING;
import static android.app.job.JobParameters.STOP_REASON_TIMEOUT;
import static android.app.job.JobParameters.STOP_REASON_UNDEFINED;
import static android.app.job.JobParameters.STOP_REASON_USER;

import static androidx.work.impl.background.systemjob.SystemJobInfoConverter.EXTRA_WORK_SPEC_GENERATION;
import static androidx.work.impl.background.systemjob.SystemJobInfoConverter.EXTRA_WORK_SPEC_ID;

import android.app.Application;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.PersistableBundle;

import androidx.annotation.MainThread;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.WorkerParameters;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Processor;
import androidx.work.impl.StartStopToken;
import androidx.work.impl.StartStopTokens;
import androidx.work.impl.WorkLauncher;
import androidx.work.impl.WorkLauncherImpl;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkGenerationalId;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Service invoked by {@link JobScheduler} to run work tasks.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemJobService extends JobService implements ExecutionListener {
    private static final String TAG = Logger.tagWithPrefix("SystemJobService");
    private WorkManagerImpl mWorkManagerImpl;
    private final Map<WorkGenerationalId, JobParameters> mJobParameters = new HashMap<>();
    private final StartStopTokens mStartStopTokens = StartStopTokens.create(false);
    private WorkLauncher mWorkLauncher;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mWorkManagerImpl = WorkManagerImpl.getInstance(getApplicationContext());
            Processor processor = mWorkManagerImpl.getProcessor();
            mWorkLauncher = new WorkLauncherImpl(processor,
                    mWorkManagerImpl.getWorkTaskExecutor());
            processor.addExecutionListener(this);
        } catch (IllegalStateException e) {
            // This can occur if...
            // 1. The app is performing an auto-backup.  Prior to O, JobScheduler could erroneously
            //    try to send commands to JobService in this state (b/32180780).  Since neither
            //    Application#onCreate nor ContentProviders have run, WorkManager won't be
            //    initialized.  In this case, we should ignore all JobScheduler commands and tell it
            //    to retry.
            // 2. The app is not performing auto-backup.  WorkManagerInitializer has been disabled
            //    but WorkManager is not manually initialized in Application#onCreate.  This is a
            //    developer error and we should throw an Exception.
            if (!Application.class.equals(getApplication().getClass())) {
                // During auto-backup, we don't get a custom Application subclass.  This code path
                // indicates we are either performing auto-backup or the user never used a custom
                // Application class (or both).
                throw new IllegalStateException("WorkManager needs to be initialized via a "
                        + "ContentProvider#onCreate() or an Application#onCreate().", e);
            }
            Logger.get().warning(TAG, "Could not find WorkManager instance; this may be because "
                    + "an auto-backup is in progress. Ignoring JobScheduler commands for now. "
                    + "Please make sure that you are initializing WorkManager if you have manually "
                    + "disabled WorkManagerInitializer.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWorkManagerImpl != null) {
            mWorkManagerImpl.getProcessor().removeExecutionListener(this);
        }
    }

    @Override
    public boolean onStartJob(@NonNull JobParameters params) {
        assertMainThread("onStartJob");
        if (mWorkManagerImpl == null) {
            Logger.get().debug(TAG, "WorkManager is not initialized; requesting retry.");
            jobFinished(params, true);
            return false;
        }

        WorkGenerationalId workGenerationalId = workGenerationalIdFromJobParameters(params);
        if (workGenerationalId == null) {
            Logger.get().error(TAG, "WorkSpec id not found!");
            return false;
        }

        if (mJobParameters.containsKey(workGenerationalId)) {
            // This condition may happen due to our workaround for an undesired behavior in API
            // 23.  See the documentation in {@link SystemJobScheduler#schedule}.
            Logger.get().debug(TAG, "Job is already being executed by SystemJobService: "
                    + workGenerationalId);
            return false;
        }

        // We don't need to worry about the case where JobParams#isOverrideDeadlineExpired()
        // returns true. This is because JobScheduler ensures that for PeriodicWork, constraints
        // are actually met irrespective.

        Logger.get().debug(TAG, "onStartJob for " + workGenerationalId);
        mJobParameters.put(workGenerationalId, params);

        WorkerParameters.RuntimeExtras runtimeExtras = null;
        if (Build.VERSION.SDK_INT >= 24) {
            runtimeExtras = new WorkerParameters.RuntimeExtras();
            if (Api24Impl.getTriggeredContentUris(params) != null) {
                runtimeExtras.triggeredContentUris =
                        Arrays.asList(Api24Impl.getTriggeredContentUris(params));
            }
            if (Api24Impl.getTriggeredContentAuthorities(params) != null) {
                runtimeExtras.triggeredContentAuthorities =
                        Arrays.asList(Api24Impl.getTriggeredContentAuthorities(params));
            }
            if (Build.VERSION.SDK_INT >= 28) {
                runtimeExtras.network = Api28Impl.getNetwork(params);
            }
        }

        // It is important that we return true, and hang on this onStartJob() request.
        // The call to startWork() may no-op because the WorkRequest could have been picked up
        // by the GreedyScheduler, and was already being executed. GreedyScheduler does not
        // handle retries, and the Processor notifies all Schedulers about an intent to reschedule.
        // In such cases, we rely on SystemJobService to ask for a reschedule by calling
        // jobFinished(params, true) in onExecuted(...);
        // For more information look at b/123211993
        mWorkLauncher.startWork(mStartStopTokens.tokenFor(workGenerationalId), runtimeExtras);
        return true;
    }

    @Override
    public boolean onStopJob(@NonNull JobParameters params) {
        assertMainThread("onStopJob");
        if (mWorkManagerImpl == null) {
            Logger.get().debug(TAG, "WorkManager is not initialized; requesting retry.");
            return true;
        }

        WorkGenerationalId workGenerationalId = workGenerationalIdFromJobParameters(params);
        if (workGenerationalId == null) {
            Logger.get().error(TAG, "WorkSpec id not found!");
            return false;
        }

        Logger.get().debug(TAG, "onStopJob for " + workGenerationalId);

        mJobParameters.remove(workGenerationalId);
        StartStopToken runId = mStartStopTokens.remove(workGenerationalId);
        if (runId != null) {
            int stopReason;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                stopReason = Api31Impl.getStopReason(params);
            } else {
                stopReason = WorkInfo.STOP_REASON_UNKNOWN;
            }
            //
            mWorkLauncher.stopWorkWithReason(runId, stopReason);
        }
        return !mWorkManagerImpl.getProcessor().isCancelled(workGenerationalId.getWorkSpecId());
    }

    @MainThread
    @Override
    public void onExecuted(@NonNull WorkGenerationalId id, boolean needsReschedule) {
        assertMainThread("onExecuted");
        Logger.get().debug(TAG, id.getWorkSpecId() + " executed on JobScheduler");
        JobParameters parameters = mJobParameters.remove(id);
        mStartStopTokens.remove(id);
        if (parameters != null) {
            jobFinished(parameters, needsReschedule);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static @Nullable WorkGenerationalId workGenerationalIdFromJobParameters(
            @NonNull JobParameters parameters
    ) {
        try {
            PersistableBundle extras = parameters.getExtras();
            if (extras != null && extras.containsKey(EXTRA_WORK_SPEC_ID)) {
                return new WorkGenerationalId(extras.getString(EXTRA_WORK_SPEC_ID),
                        extras.getInt(EXTRA_WORK_SPEC_GENERATION));
            }
        } catch (NullPointerException e) {
            // b/138441699: BaseBundle.getString sometimes throws an NPE.  Ignore and return null.
        }
        return null;
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static Uri[] getTriggeredContentUris(JobParameters jobParameters) {
            return jobParameters.getTriggeredContentUris();
        }

        static String[] getTriggeredContentAuthorities(JobParameters jobParameters) {
            return jobParameters.getTriggeredContentAuthorities();
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static Network getNetwork(JobParameters jobParameters) {
            return jobParameters.getNetwork();
        }
    }

    @RequiresApi(31)
    static class Api31Impl {
        private Api31Impl() {
            // This class is not instantiable.
        }

        static int getStopReason(JobParameters jobParameters) {
            return stopReason(jobParameters.getStopReason());
        }
    }

    // making sure that we return only values that WorkManager is aware of.
    static int stopReason(int jobReason) {
        int reason;
        switch (jobReason) {
            case STOP_REASON_APP_STANDBY:
            case STOP_REASON_BACKGROUND_RESTRICTION:
            case STOP_REASON_CANCELLED_BY_APP:
            case STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW:
            case STOP_REASON_CONSTRAINT_CHARGING:
            case STOP_REASON_CONSTRAINT_CONNECTIVITY:
            case STOP_REASON_CONSTRAINT_DEVICE_IDLE:
            case STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW:
            case STOP_REASON_DEVICE_STATE:
            case STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED:
            case STOP_REASON_PREEMPT:
            case STOP_REASON_QUOTA:
            case STOP_REASON_SYSTEM_PROCESSING:
            case STOP_REASON_TIMEOUT:
            case STOP_REASON_UNDEFINED:
            case STOP_REASON_USER:
                reason = jobReason;
                break;
            default:
                reason = WorkInfo.STOP_REASON_UNKNOWN;
        }
        return reason;
    }

    private static void assertMainThread(String methodName) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background"
                    + " thread");
        }
    }
}
