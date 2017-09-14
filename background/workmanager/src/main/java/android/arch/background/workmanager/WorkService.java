/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

/**
 * Service invoked by {@link android.app.job.JobScheduler} to run work tasks.
 */
@TargetApi(21)
public class WorkService extends JobService {

    private static final String TAG = "WorkService";

    @Override
    public boolean onStartJob(JobParameters params) {
        int jobId = params.getJobId();
        Log.d(TAG, jobId + " scheduled on JobScheduler");
        // TODO(janclarin): Schedule work with instance of WorkExecutionManager.
        // TODO(janclarin): Call jobFinished after task is completed.
        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        int jobId = params.getJobId();
        // TODO(janclarin): Cancel work with instance of WorkExecutionManager.
        Log.d(TAG, jobId + " stopped");
        return false;
    }
}
