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
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.IdGenerator;

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
    private final WorkManagerImpl mWorkManager;
    private final IdGenerator mIdGenerator;
    private final SystemJobInfoConverter mSystemJobInfoConverter;

    public SystemJobScheduler(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        this(context,
                workManager,
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE),
                new SystemJobInfoConverter(context));
    }

    @VisibleForTesting
    public SystemJobScheduler(
            Context context,
            WorkManagerImpl workManager,
            JobScheduler jobScheduler,
            SystemJobInfoConverter systemJobInfoConverter) {
        mWorkManager = workManager;
        mJobScheduler = jobScheduler;
        mIdGenerator = new IdGenerator(context);
        mSystemJobInfoConverter = systemJobInfoConverter;
    }

    @Override
    public void schedule(WorkSpec... workSpecs) {
        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();

        for (WorkSpec workSpec : workSpecs) {
            try {
                workDatabase.beginTransaction();

                SystemIdInfo info = workDatabase.systemIdInfoDao()
                        .getSystemIdInfo(workSpec.id);

                int jobId = info != null ? info.systemId : mIdGenerator.nextJobSchedulerIdWithRange(
                        mWorkManager.getConfiguration().getMinJobSchedulerID(),
                        mWorkManager.getConfiguration().getMaxJobSchedulerID());

                if (info == null) {
                    SystemIdInfo newSystemIdInfo = new SystemIdInfo(workSpec.id, jobId);
                    mWorkManager.getWorkDatabase()
                            .systemIdInfoDao()
                            .insertSystemIdInfo(newSystemIdInfo);
                }

                scheduleInternal(workSpec, jobId);

                // API 23 JobScheduler only kicked off jobs if there were at least two jobs in the
                // queue, even if the job constraints were met.  This behavior was considered
                // undesirable and later changed in Marshmallow MR1.  To match the new behavior,
                // we will double-schedule jobs on API 23 and de-dupe them
                // in SystemJobService as needed.
                if (Build.VERSION.SDK_INT == 23) {
                    int nextJobId = mIdGenerator.nextJobSchedulerIdWithRange(
                            mWorkManager.getConfiguration().getMinJobSchedulerID(),
                            mWorkManager.getConfiguration().getMaxJobSchedulerID());

                    scheduleInternal(workSpec, nextJobId);
                }

                workDatabase.setTransactionSuccessful();
            } finally {
                workDatabase.endTransaction();
            }
        }
    }

    /**
     * Schedules one job with JobScheduler.
     *
     * @param workSpec The {@link WorkSpec} to schedule with JobScheduler.
     */
    @VisibleForTesting
    public void scheduleInternal(WorkSpec workSpec, int jobId) {
        JobInfo jobInfo = mSystemJobInfoConverter.convert(workSpec, jobId);
        Log.d(TAG, String.format("Scheduling work ID %s Job ID %s", workSpec.id, jobId));
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

                    // Its safe to call this method twice.
                    mWorkManager.getWorkDatabase()
                            .systemIdInfoDao()
                            .removeSystemIdInfo(workSpecId);

                    mJobScheduler.cancel(jobInfo.getId());

                    // See comment in #schedule.
                    if (Build.VERSION.SDK_INT != 23) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Cancels all the jobs owned by {@link androidx.work.WorkManager} in {@link JobScheduler}.
     */
    public static void jobSchedulerCancelAll(@NonNull Context context) {
        JobScheduler jobScheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler != null) {
            List<JobInfo> jobInfos = jobScheduler.getAllPendingJobs();
            // Apparently this can be null on API 23?
            if (jobInfos != null) {
                for (JobInfo jobInfo : jobInfos) {
                    PersistableBundle extras = jobInfo.getExtras();
                    // This is a job scheduled by WorkManager.
                    if (extras.containsKey(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID)) {
                        jobScheduler.cancel(jobInfo.getId());
                    }
                }
            }
        }
    }
}
