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

package android.support.v4.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobServiceEngine;
import android.app.job.JobWorkItem;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * Implementation of a JobServiceEngine for interaction with JobIntentService.
 */
@RequiresApi(26)
final class JobServiceEngineImpl extends JobServiceEngine
        implements JobIntentService.CompatJobEngine {
    static final String TAG = "JobServiceEngineImpl";

    static final boolean DEBUG = false;

    final JobIntentService mService;
    JobParameters mParams;

    static final class JobWorkEnqueuer extends JobIntentService.WorkEnqueuer {
        private final JobInfo mJobInfo;
        private final JobScheduler mJobScheduler;

        JobWorkEnqueuer(Context context, Class cls, int jobId) {
            super(context, cls);
            ensureJobId(jobId);
            JobInfo.Builder b = new JobInfo.Builder(jobId, mComponentName);
            mJobInfo = b.setOverrideDeadline(0).build();
            mJobScheduler = (JobScheduler) context.getApplicationContext().getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
        }

        @Override
        void enqueueWork(Intent work) {
            if (DEBUG) Log.d(TAG, "Enqueueing work: " + work);
            mJobScheduler.enqueue(mJobInfo, new JobWorkItem(work));
        }
    }

    final class WrapperWorkItem implements JobIntentService.GenericWorkItem {
        final JobWorkItem mJobWork;

        WrapperWorkItem(JobWorkItem jobWork) {
            mJobWork = jobWork;
        }

        @Override
        public Intent getIntent() {
            return mJobWork.getIntent();
        }

        @Override
        public void complete() {
            mParams.completeWork(mJobWork);
        }
    }

    JobServiceEngineImpl(JobIntentService service) {
        super(service);
        mService = service;
    }

    @Override
    public IBinder compatGetBinder() {
        return getBinder();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if (DEBUG) Log.d(TAG, "onStartJob: " + params);
        mParams = params;
        // We can now start dequeuing work!
        mService.ensureProcessorRunningLocked();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (DEBUG) Log.d(TAG, "onStartJob: " + params);
        return mService.onStopCurrentWork();
    }

    /**
     * Dequeue some work.
     */
    @Override
    public JobIntentService.GenericWorkItem dequeueWork() {
        JobWorkItem work = mParams.dequeueWork();
        if (work != null) {
            return new WrapperWorkItem(work);
        } else {
            return null;
        }
    }
}
