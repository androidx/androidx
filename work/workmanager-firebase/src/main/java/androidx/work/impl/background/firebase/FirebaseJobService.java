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

package androidx.work.impl.background.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.impl.ExecutionListener;
import androidx.work.impl.WorkManagerImpl;

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
        String workSpecId = params.getTag();
        if (TextUtils.isEmpty(workSpecId)) {
            Log.e(TAG, "WorkSpec id not found!");
            return false;
        }

        Log.d(TAG, String.format("onStartJob for %s", workSpecId));
        synchronized (mJobParameters) {
            mJobParameters.put(workSpecId, params);
        }
        mWorkManagerImpl.startWork(workSpecId);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        String workSpecId = params.getTag();
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
        Log.d(TAG, String.format("%s executed on FirebaseJobDispatcher", workSpecId));
        JobParameters parameters;
        synchronized (mJobParameters) {
            parameters = mJobParameters.get(workSpecId);
        }
        if (parameters != null) {
            jobFinished(parameters, needsReschedule);
        }
    }
}
