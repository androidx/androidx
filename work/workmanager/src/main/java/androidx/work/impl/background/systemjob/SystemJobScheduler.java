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
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import java.util.List;

/**
 * A class that schedules work using {@link android.app.job.JobScheduler}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TargetApi(WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class SystemJobScheduler implements Scheduler {

    private static final String TAG = "SystemJobScheduler";

    private final JobScheduler mJobScheduler;
    private final SystemJobInfoConverter mSystemJobInfoConverter;

    public SystemJobScheduler(@NonNull Context context, @NonNull Configuration configuration) {
        this((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE),
                new SystemJobInfoConverter(context, configuration));
    }

    @VisibleForTesting
    public SystemJobScheduler(
            JobScheduler jobScheduler,
            SystemJobInfoConverter systemJobInfoConverter) {
        mJobScheduler = jobScheduler;
        mSystemJobInfoConverter = systemJobInfoConverter;
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        for (WorkSpec workSpec : workSpecs) {
            scheduleInternal(workSpec);

            // API 23 JobScheduler only kicked off jobs if there were at least two jobs in the
            // queue, even if the job constraints were met.  This behavior was considered
            // undesirable and later changed in Marshmallow MR1.  To match the new behavior, we will
            // double-schedule jobs on API 23 and dedupe them in SystemJobService as needed.
            if (Build.VERSION.SDK_INT == 23) {
                scheduleInternal(workSpec);
            }
        }
    }

    /**
     * Schedules one job with JobScheduler.
     *
     * @param workSpec The {@link WorkSpec} to schedule with JobScheduler.
     */
    @VisibleForTesting
    public void scheduleInternal(WorkSpec workSpec) {
        JobInfo jobInfo = mSystemJobInfoConverter.convert(workSpec);
        Log.d(TAG, String.format("Scheduling work ID %s Job ID %s", workSpec.id, jobInfo.getId()));
        mJobScheduler.schedule(jobInfo);
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        // Note: despite what the word "pending" and the associated Javadoc might imply, this is
        // actually a list of all unfinished jobs that JobScheduler knows about for the current
        // process.
        List<JobInfo> allJobInfos = mJobScheduler.getAllPendingJobs();
        if (allJobInfos != null) {  // Apparently this CAN be null on API 23?
            for (JobInfo jobInfo : allJobInfos) {
                if (workSpecId.equals(
                        jobInfo.getExtras().getString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID))) {
                    mJobScheduler.cancel(jobInfo.getId());

                    // See comment in #schedule.
                    if (Build.VERSION.SDK_INT != 23) {
                        return;
                    }
                }
            }
        }
    }
}
