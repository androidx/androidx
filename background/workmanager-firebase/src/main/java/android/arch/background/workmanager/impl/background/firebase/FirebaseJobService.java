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
import android.arch.background.workmanager.impl.WorkDatabase;
import android.arch.background.workmanager.impl.WorkManagerImpl;
import android.arch.background.workmanager.impl.background.BackgroundProcessor;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

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
    private BackgroundProcessor mProcessor;
    private Map<String, JobParameters> mJobParameters = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        WorkManagerImpl workManagerImpl = WorkManagerImpl.getInstance();
        WorkDatabase database = workManagerImpl.getWorkDatabase();
        mProcessor = new BackgroundProcessor(
                context,
                database,
                workManagerImpl.getBackgroundScheduler(),
                workManagerImpl.getBackgroundExecutorService(),
                this);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        String workSpecId = params.getTag();
        if (TextUtils.isEmpty(workSpecId)) {
            Log.e(TAG, "WorkSpec id not found!");
            return false;
        }
        Log.d(TAG, workSpecId + " started on FirebaseJobDispatcher");
        mJobParameters.put(workSpecId, params);

        mProcessor.process(workSpecId);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        String workSpecId = params.getTag();
        if (TextUtils.isEmpty(workSpecId)) {
            Log.e(TAG, "WorkSpec id not found!");
            return false;
        }
        boolean cancelled = mProcessor.cancel(workSpecId, true);
        Log.d(TAG, "onStopJob for " + workSpecId + "; Processor.cancel = " + cancelled);
        return cancelled;
    }

    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        Log.d(TAG, workSpecId + " executed on FirebaseJobDispatcher");
        JobParameters parameters = mJobParameters.get(workSpecId);
        jobFinished(parameters, needsReschedule);
    }
}
