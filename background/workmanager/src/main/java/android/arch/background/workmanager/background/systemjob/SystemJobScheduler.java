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
package android.arch.background.workmanager.background.systemjob;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.arch.background.workmanager.impl.Scheduler;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.List;

/**
 * A class that schedules work using {@link android.app.job.JobScheduler}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TargetApi(23)
public class SystemJobScheduler implements Scheduler {
    private static final String TAG = "SystemJobScheduler";
    private JobScheduler mJobScheduler;
    private SystemJobInfoConverter mSystemJobInfoConverter;

    public SystemJobScheduler(Context context) {
        mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        mSystemJobInfoConverter = new SystemJobInfoConverter(context);
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            JobInfo jobInfo = mSystemJobInfoConverter.convert(workSpec);
            Log.d(TAG, "Scheduling work, ID: " + workSpec.getId() + " Job ID: " + jobInfo.getId());
            mJobScheduler.schedule(jobInfo);
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        // Note: despite what the word "pending" and the associated Javadoc might imply, this is
        // actually a list of all unfinished jobs that JobScheduler knows about for the current
        // process.
        List<JobInfo> allJobInfos = mJobScheduler.getAllPendingJobs();
        for (JobInfo jobInfo : allJobInfos) {
            if (workSpecId.equals(
                    jobInfo.getExtras().getString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID))) {
                mJobScheduler.cancel(jobInfo.getId());
                return;
            }
        }
    }
}
