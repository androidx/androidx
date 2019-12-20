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

import static android.content.Context.JOB_SCHEDULER_SERVICE;

import static androidx.work.impl.background.systemjob.SystemJobInfoConverter.EXTRA_WORK_SPEC_ID;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A class that schedules work using {@link android.app.job.JobScheduler}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class SystemJobScheduler implements Scheduler {

    private static final String TAG = Logger.tagWithPrefix("SystemJobScheduler");

    private final Context mContext;
    private final JobScheduler mJobScheduler;
    private final WorkManagerImpl mWorkManager;
    private final SystemJobInfoConverter mSystemJobInfoConverter;

    public SystemJobScheduler(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        this(context,
                workManager,
                (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE),
                new SystemJobInfoConverter(context));
    }

    @VisibleForTesting
    public SystemJobScheduler(
            Context context,
            WorkManagerImpl workManager,
            JobScheduler jobScheduler,
            SystemJobInfoConverter systemJobInfoConverter) {
        mContext = context;
        mWorkManager = workManager;
        mJobScheduler = jobScheduler;
        mSystemJobInfoConverter = systemJobInfoConverter;
    }

    @Override
    public void schedule(@NonNull WorkSpec... workSpecs) {
        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();
        IdGenerator idGenerator = new IdGenerator(workDatabase);

        for (WorkSpec workSpec : workSpecs) {
            workDatabase.beginTransaction();
            try {
                WorkSpec currentDbWorkSpec = workDatabase.workSpecDao().getWorkSpec(workSpec.id);
                if (currentDbWorkSpec == null) {
                    Logger.get().warning(
                            TAG,
                            "Skipping scheduling " + workSpec.id
                                    + " because it's no longer in the DB");

                    // Marking this transaction as successful, as we don't want this transaction
                    // to affect transactions for unrelated WorkSpecs.
                    workDatabase.setTransactionSuccessful();
                    continue;
                } else if (currentDbWorkSpec.state != WorkInfo.State.ENQUEUED) {
                    Logger.get().warning(
                            TAG,
                            "Skipping scheduling " + workSpec.id
                                    + " because it is no longer enqueued");

                    // Marking this transaction as successful, as we don't want this transaction
                    // to affect transactions for unrelated WorkSpecs.
                    workDatabase.setTransactionSuccessful();
                    continue;
                }

                SystemIdInfo info = workDatabase.systemIdInfoDao()
                        .getSystemIdInfo(workSpec.id);

                int jobId = info != null ? info.systemId : idGenerator.nextJobSchedulerIdWithRange(
                        mWorkManager.getConfiguration().getMinJobSchedulerId(),
                        mWorkManager.getConfiguration().getMaxJobSchedulerId());

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
                    // Get pending jobIds that might be currently being used.
                    // This is useful only for API 23, because we double schedule jobs.
                    List<Integer> jobIds = getPendingJobIds(mContext, mJobScheduler, workSpec.id);

                    // jobIds can be null if getPendingJobIds() throws an Exception.
                    // When this happens this will not setup a second job, and hence might delay
                    // execution, but it's better than crashing the app.
                    if (jobIds != null) {
                        // Remove the jobId which has been used from the list of eligible jobIds.
                        int index = jobIds.indexOf(jobId);
                        if (index >= 0) {
                            jobIds.remove(index);
                        }

                        int nextJobId;
                        if (!jobIds.isEmpty()) {
                            // Use the next eligible jobId
                            nextJobId = jobIds.get(0);
                        } else {
                            // Create a new jobId
                            nextJobId = idGenerator.nextJobSchedulerIdWithRange(
                                    mWorkManager.getConfiguration().getMinJobSchedulerId(),
                                    mWorkManager.getConfiguration().getMaxJobSchedulerId());
                        }
                        scheduleInternal(workSpec, nextJobId);
                    }
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
        Logger.get().debug(
                TAG,
                String.format("Scheduling work ID %s Job ID %s", workSpec.id, jobId));
        try {
            mJobScheduler.schedule(jobInfo);
        } catch (IllegalStateException e) {
            // This only gets thrown if we exceed 100 jobs.  Let's figure out if WorkManager is
            // responsible for all these jobs.
            List<JobInfo> jobs = getPendingJobs(mContext, mJobScheduler);
            int numWorkManagerJobs = jobs != null ? jobs.size() : 0;

            String message = String.format(Locale.getDefault(),
                    "JobScheduler 100 job limit exceeded.  We count %d WorkManager "
                            + "jobs in JobScheduler; we have %d tracked jobs in our DB; "
                            + "our Configuration limit is %d.",
                    numWorkManagerJobs,
                    mWorkManager.getWorkDatabase().workSpecDao().getScheduledWork().size(),
                    mWorkManager.getConfiguration().getMaxSchedulerLimit());

            Logger.get().error(TAG, message);

            // Rethrow a more verbose exception.
            throw new IllegalStateException(message, e);
        } catch (Throwable throwable) {
            // OEM implementation bugs in JobScheduler cause the app to crash. Avoid crashing.
            Logger.get().error(TAG, String.format("Unable to schedule %s", workSpec), throwable);
        }
    }

    @Override
    public void cancel(@NonNull String workSpecId) {
        List<Integer> jobIds = getPendingJobIds(mContext, mJobScheduler, workSpecId);
        if (jobIds != null && !jobIds.isEmpty()) {
            for (int jobId : jobIds) {
                cancelJobById(mJobScheduler, jobId);
            }

            // Drop the relevant system ids.
            mWorkManager.getWorkDatabase()
                .systemIdInfoDao()
                .removeSystemIdInfo(workSpecId);
        }
    }

    @Override
    public boolean hasLimitedSchedulingSlots() {
        return true;
    }

    private static void cancelJobById(@NonNull JobScheduler jobScheduler, int id) {
        try {
            jobScheduler.cancel(id);
        } catch (Throwable throwable) {
            // OEM implementation bugs in JobScheduler can cause the app to crash.
            Logger.get().error(TAG,
                    String.format(
                            Locale.getDefault(),
                            "Exception while trying to cancel job (%d)",
                            id),
                    throwable);
        }
    }

    /**
     * Cancels all the jobs owned by {@link androidx.work.WorkManager} in {@link JobScheduler}.
     *
     * @param context The {@link Context} for the {@link JobScheduler}
     */
    public static void cancelAll(@NonNull Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            List<JobInfo> jobs = getPendingJobs(context, jobScheduler);
            if (jobs != null && !jobs.isEmpty()) {
                for (JobInfo jobInfo : jobs) {
                    cancelJobById(jobScheduler, jobInfo.getId());
                }
            }
        }
    }

    /**
     * Cancels invalid jobs owned by WorkManager.  This iterates all the jobs set for our
     * {@link SystemJobService} but with invalid extras.  These jobs are invalid (inactionable on
     * our part) but occupy slots in JobScheduler.  This method is meant to help mitigate problems
     * like b/134058261, where we have faulty implementations of JobScheduler.
     *
     * @param context The {@link Context} for the {@link JobScheduler}
     */
    public static void cancelInvalidJobs(@NonNull Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            List<JobInfo> jobs = getPendingJobs(context, jobScheduler);
            if (jobs != null && !jobs.isEmpty()) {
                for (JobInfo jobInfo : jobs) {
                    if (getWorkSpecIdFromJobInfo(jobInfo) == null) {
                        cancelJobById(jobScheduler, jobInfo.getId());
                    }
                }
            }
        }
    }

    @Nullable
    private static List<JobInfo> getPendingJobs(
            @NonNull Context context,
            @NonNull JobScheduler jobScheduler) {
        List<JobInfo> pendingJobs = null;
        try {
            // Note: despite what the word "pending" and the associated Javadoc might imply, this is
            // actually a list of all unfinished jobs that JobScheduler knows about for the current
            // process.
            pendingJobs = jobScheduler.getAllPendingJobs();
        } catch (Throwable exception) {
            // OEM implementation bugs in JobScheduler cause the app to crash. Avoid crashing.
            Logger.get().error(TAG, "getAllPendingJobs() is not reliable on this device.",
                    exception);
        }

        if (pendingJobs == null) {
            return null;
        }

        // Filter jobs that belong to WorkManager.
        List<JobInfo> filtered = new ArrayList<>(pendingJobs.size());
        ComponentName jobServiceComponent = new ComponentName(context, SystemJobService.class);
        for (JobInfo jobInfo : pendingJobs) {
            if (jobServiceComponent.equals(jobInfo.getService())) {
                filtered.add(jobInfo);
            }
        }
        return filtered;
    }

    /**
     * Always wrap a call to getAllPendingJobs(), schedule() and cancel() with a try catch as there
     * are platform bugs with several OEMs in API 23, which cause this method to throw Exceptions.
     *
     * For reference: b/133556574, b/133556809, b/133556535
     */
    @Nullable
    private static List<Integer> getPendingJobIds(
            @NonNull Context context,
            @NonNull JobScheduler jobScheduler,
            @NonNull String workSpecId) {

        List<JobInfo> jobs = getPendingJobs(context, jobScheduler);
        if (jobs == null) {
            return null;
        }

        // We have at most 2 jobs per WorkSpec
        List<Integer> jobIds = new ArrayList<>(2);

        for (JobInfo jobInfo : jobs) {
            if (workSpecId.equals(getWorkSpecIdFromJobInfo(jobInfo))) {
                jobIds.add(jobInfo.getId());
            }
        }

        return jobIds;
    }

    @SuppressWarnings("ConstantConditions")
    private static @Nullable String getWorkSpecIdFromJobInfo(@NonNull JobInfo jobInfo) {
        PersistableBundle extras = jobInfo.getExtras();
        try {
            if (extras != null && extras.containsKey(EXTRA_WORK_SPEC_ID)) {
                return extras.getString(EXTRA_WORK_SPEC_ID);
            }
        } catch (NullPointerException e) {
            // b/138364061: BaseBundle.mMap seems to be null in some cases here.  Ignore and return
            // null.
        }
        return null;
    }
}