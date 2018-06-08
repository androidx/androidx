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

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.impl.ExecutionListener;
import androidx.work.impl.Extras;
import androidx.work.impl.WorkManagerImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoked by {@link android.app.job.JobScheduler} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TargetApi(WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class SystemJobService extends JobService implements ExecutionListener {
    private static final String TAG = "SystemJobService";
    private WorkManagerImpl mWorkManagerImpl;
    private final Map<String, JobParameters> mJobParameters = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkManagerImpl = WorkManagerImpl.getInstance();
        mWorkManagerImpl.getProcessor().addExecutionListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWorkManagerImpl.getProcessor().removeExecutionListener(this);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle extras = params.getExtras();
        String workSpecId = extras.getString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID);
        if (TextUtils.isEmpty(workSpecId)) {
            Log.e(TAG, "WorkSpec id not found!");
            return false;
        }

        synchronized (mJobParameters) {
            if (mJobParameters.containsKey(workSpecId)) {
                // This condition may happen due to our workaround for an undesired behavior in API
                // 23.  See the documentation in {@link SystemJobScheduler#schedule}.
                Log.d(TAG, String.format(
                        "Job is already being executed by SystemJobService: %s", workSpecId));
                return false;
            }

            boolean isPeriodic = extras.getBoolean(SystemJobInfoConverter.EXTRA_IS_PERIODIC, false);
            if (isPeriodic && params.isOverrideDeadlineExpired()) {
                Log.d(TAG, String.format(
                        "Override deadline expired for id %s. Retry requested", workSpecId));
                jobFinished(params, true);
                return false;
            }

            Log.d(TAG, String.format("onStartJob for %s", workSpecId));
            mJobParameters.put(workSpecId, params);
        }

        Extras.RuntimeExtras runtimeExtras = null;
        if (Build.VERSION.SDK_INT >= 24) {
            runtimeExtras = new Extras.RuntimeExtras();
            if (params.getTriggeredContentUris() != null
                    || params.getTriggeredContentAuthorities() != null) {
                runtimeExtras.triggeredContentUris = params.getTriggeredContentUris();
                runtimeExtras.triggeredContentAuthorities =
                        params.getTriggeredContentAuthorities();
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
        String workSpecId = params.getExtras().getString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID);
        if (TextUtils.isEmpty(workSpecId)) {
            Log.e(TAG, "WorkSpec id not found!");
            return false;
        }

        Log.d(TAG, String.format("onStopJob for %s", workSpecId));

        synchronized (mJobParameters) {
            mJobParameters.remove(workSpecId);
        }
        mWorkManagerImpl.stopWork(workSpecId);
        return !mWorkManagerImpl.getProcessor().isCancelled(workSpecId);
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {
        Log.d(TAG, String.format("%s executed on JobScheduler", workSpecId));
        JobParameters parameters;
        synchronized (mJobParameters) {
            parameters = mJobParameters.get(workSpecId);
        }
        if (parameters != null) {
            jobFinished(parameters, needsReschedule);
        }
    }
}
