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

import android.app.Application;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import androidx.work.Logger;
import androidx.work.WorkerParameters;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.WorkManagerImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Service invoked by {@link android.app.job.JobScheduler} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class SystemJobService extends JobService implements ExecutionListener {
    private static final String TAG = "SystemJobService";
    private WorkManagerImpl mWorkManagerImpl;
    private final Map<String, JobParameters> mJobParameters = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkManagerImpl = WorkManagerImpl.getInstance();
        if (mWorkManagerImpl == null) {
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
                        + "ContentProvider#onCreate() or an Application#onCreate().");
            }
            Logger.get().warning(TAG, "Could not find WorkManager instance; this may be because an "
                    + "auto-backup is in progress. Ignoring JobScheduler commands for now. Please "
                    + "make sure that you are initializing WorkManager if you have manually "
                    + "disabled WorkManagerInitializer.");
        } else {
            mWorkManagerImpl.getProcessor().addExecutionListener(this);
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
    public boolean onStartJob(JobParameters params) {
        if (mWorkManagerImpl == null) {
            Logger.get().debug(TAG, "WorkManager is not initialized; requesting retry.");
            jobFinished(params, true);
            return false;
        }

        PersistableBundle extras = params.getExtras();
        String workSpecId = extras.getString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID);
        if (TextUtils.isEmpty(workSpecId)) {
            Logger.get().error(TAG, "WorkSpec id not found!");
            return false;
        }

        synchronized (mJobParameters) {
            if (mJobParameters.containsKey(workSpecId)) {
                // This condition may happen due to our workaround for an undesired behavior in API
                // 23.  See the documentation in {@link SystemJobScheduler#schedule}.
                Logger.get().debug(TAG, String.format(
                        "Job is already being executed by SystemJobService: %s", workSpecId));
                return false;
            }

            // We don't need to worry about the case where JobParams#isOverrideDeadlineExpired()
            // returns true. This is because JobScheduler ensures that for PeriodicWork, constraints
            // are actually met irrespective.

            Logger.get().debug(TAG, String.format("onStartJob for %s", workSpecId));
            mJobParameters.put(workSpecId, params);
        }

        WorkerParameters.RuntimeExtras runtimeExtras = null;
        if (Build.VERSION.SDK_INT >= 24) {
            runtimeExtras = new WorkerParameters.RuntimeExtras();
            if (params.getTriggeredContentUris() != null) {
                runtimeExtras.triggeredContentUris =
                        Arrays.asList(params.getTriggeredContentUris());
            }
            if (params.getTriggeredContentAuthorities() != null) {
                runtimeExtras.triggeredContentAuthorities =
                        Arrays.asList(params.getTriggeredContentAuthorities());
            }
            if (Build.VERSION.SDK_INT >= 28) {
                runtimeExtras.network = params.getNetwork();
            }
        }

        mWorkManagerImpl.startWork(workSpecId, runtimeExtras);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mWorkManagerImpl == null) {
            Logger.get().debug(TAG, "WorkManager is not initialized; requesting retry.");
            return true;
        }

        String workSpecId = params.getExtras().getString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID);
        if (TextUtils.isEmpty(workSpecId)) {
            Logger.get().error(TAG, "WorkSpec id not found!");
            return false;
        }

        Logger.get().debug(TAG, String.format("onStopJob for %s", workSpecId));

        synchronized (mJobParameters) {
            mJobParameters.remove(workSpecId);
        }
        mWorkManagerImpl.stopWork(workSpecId);
        return !mWorkManagerImpl.getProcessor().isCancelled(workSpecId);
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        Logger.get().debug(TAG, String.format("%s executed on JobScheduler", workSpecId));
        JobParameters parameters;
        synchronized (mJobParameters) {
            parameters = mJobParameters.remove(workSpecId);
        }
        if (parameters != null) {
            jobFinished(parameters, needsReschedule);
        }
    }
}
