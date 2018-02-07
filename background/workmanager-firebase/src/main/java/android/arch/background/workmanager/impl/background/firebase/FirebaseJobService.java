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

package android.arch.background.workmanager.impl.background.firebase;

import android.arch.background.workmanager.impl.ExecutionListener;
import android.arch.background.workmanager.impl.Processor;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.logger.Logger;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoked by {@link com.firebase.jobdispatcher.FirebaseJobDispatcher} to run work tasks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseJobService extends JobService implements ExecutionListener {
    private static final String TAG = "FirebaseJobService";
    private Processor mProcessor;
    private Map<String, JobParameters> mJobParameters = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance();
        mProcessor = workManagerImpl.getProcessor();
        mProcessor.addExecutionListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProcessor.removeExecutionListener(this);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        String workSpecId = params.getTag();
        if (TextUtils.isEmpty(workSpecId)) {
            Logger.error(TAG, "WorkSpec id not found!");
            return false;
        }
        Logger.debug(TAG, "%s started on FirebaseJobDispatcher", workSpecId);
        mJobParameters.put(workSpecId, params);

        mProcessor.startWork(workSpecId);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        String workSpecId = params.getTag();
        if (TextUtils.isEmpty(workSpecId)) {
            Logger.error(TAG, "WorkSpec id not found!");
            return false;
        }
        boolean isStopped = mProcessor.stopWork(workSpecId, true);
        Logger.debug(TAG, "onStopJob for %s; Processor.stopWork = ", workSpecId, isStopped);
        return isStopped;
    }

    @Override
    public void onExecuted(
            @NonNull String workSpecId,
            boolean isSuccessful,
            boolean needsReschedule) {
        Logger.debug(TAG, "%s executed on FirebaseJobDispatcher", workSpecId);
        JobParameters parameters = mJobParameters.get(workSpecId);
        jobFinished(parameters, needsReschedule);
    }
}
