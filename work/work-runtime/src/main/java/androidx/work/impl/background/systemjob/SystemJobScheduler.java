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

import static androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST;
import static androidx.work.impl.background.systemjob.JobSchedulerExtKt.createErrorMessage;
import static androidx.work.impl.background.systemjob.JobSchedulerExtKt.getSafePendingJobs;
import static androidx.work.impl.background.systemjob.JobSchedulerExtKt.getWmJobScheduler;
import static androidx.work.impl.background.systemjob.SystemJobInfoConverter.EXTRA_WORK_SPEC_GENERATION;
import static androidx.work.impl.background.systemjob.SystemJobInfoConverter.EXTRA_WORK_SPEC_ID;
import static androidx.work.impl.model.SystemIdInfoKt.systemIdInfo;
import static androidx.work.impl.model.WorkSpec.SCHEDULE_NOT_REQUESTED_YET;
import static androidx.work.impl.model.WorkSpecKt.generationalId;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Consumer;
import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.WorkGenerationalId;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.IdGenerator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A class that schedules work using {@link android.app.job.JobScheduler}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemJobScheduler implements Scheduler {

    private static final String TAG = Logger.tagWithPrefix("SystemJobScheduler");

    private final Context mContext;
    private final JobScheduler mJobScheduler;
    private final SystemJobInfoConverter mSystemJobInfoConverter;

    private final WorkDatabase mWorkDatabase;
    private final Configuration mConfiguration;

    public SystemJobScheduler(@NonNull Context context, @NonNull WorkDatabase workDatabase,
            @NonNull Configuration configuration) {
        this(context,
                workDatabase,
                configuration,
                getWmJobScheduler(context),
                new SystemJobInfoConverter(context, configuration.getClock(),
                        configuration.isMarkingJobsAsImportantWhileForeground())
        );
    }

    @VisibleForTesting
    public SystemJobScheduler(
            @NonNull Context context,
            @NonNull WorkDatabase workDatabase,
            @NonNull Configuration configuration,
            @NonNull JobScheduler jobScheduler,
            @NonNull SystemJobInfoConverter systemJobInfoConverter) {
        mContext = context;
        mJobScheduler = jobScheduler;
        mSystemJobInfoConverter = systemJobInfoConverter;
        mWorkDatabase = workDatabase;
        mConfiguration = configuration;
    }

    @Override
    public void schedule(WorkSpec @NonNull ... workSpecs) {
        IdGenerator idGenerator = new IdGenerator(mWorkDatabase);

        for (WorkSpec workSpec : workSpecs) {
            mWorkDatabase.beginTransaction();
            try {
                WorkSpec currentDbWorkSpec = mWorkDatabase.workSpecDao().getWorkSpec(workSpec.id);
                if (currentDbWorkSpec == null) {
                    Logger.get().warning(
                            TAG,
                            "Skipping scheduling " + workSpec.id
                                    + " because it's no longer in the DB");

                    // Marking this transaction as successful, as we don't want this transaction
                    // to affect transactions for unrelated WorkSpecs.
                    mWorkDatabase.setTransactionSuccessful();
                    continue;
                } else if (currentDbWorkSpec.state != WorkInfo.State.ENQUEUED) {
                    Logger.get().warning(
                            TAG,
                            "Skipping scheduling " + workSpec.id
                                    + " because it is no longer enqueued");

                    // Marking this transaction as successful, as we don't want this transaction
                    // to affect transactions for unrelated WorkSpecs.
                    mWorkDatabase.setTransactionSuccessful();
                    continue;
                }
                WorkGenerationalId generationalId = generationalId(workSpec);
                SystemIdInfo info = mWorkDatabase.systemIdInfoDao().getSystemIdInfo(generationalId);

                int jobId = info != null ? info.systemId : idGenerator.nextJobSchedulerIdWithRange(
                        mConfiguration.getMinJobSchedulerId(),
                        mConfiguration.getMaxJobSchedulerId());

                if (info == null) {
                    SystemIdInfo newSystemIdInfo = systemIdInfo(generationalId, jobId);
                    mWorkDatabase.systemIdInfoDao().insertSystemIdInfo(newSystemIdInfo);
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
                                    mConfiguration.getMinJobSchedulerId(),
                                    mConfiguration.getMaxJobSchedulerId());
                        }
                        scheduleInternal(workSpec, nextJobId);
                    }
                }
                mWorkDatabase.setTransactionSuccessful();
            } finally {
                mWorkDatabase.endTransaction();
            }
        }
    }

    /**
     * Schedules one job with JobScheduler.
     *
     * @param workSpec The {@link WorkSpec} to schedule with JobScheduler.
     */
    @VisibleForTesting
    public void scheduleInternal(@NonNull WorkSpec workSpec, int jobId) {
        JobInfo jobInfo = mSystemJobInfoConverter.convert(workSpec, jobId);
        Logger.get().debug(
                TAG,
                "Scheduling work ID " + workSpec.id + "Job ID " + jobId);
        try {
            int result = mJobScheduler.schedule(jobInfo);
            if (result == JobScheduler.RESULT_FAILURE) {
                Logger.get().warning(TAG, "Unable to schedule work ID " + workSpec.id);
                if (workSpec.expedited
                        && workSpec.outOfQuotaPolicy == RUN_AS_NON_EXPEDITED_WORK_REQUEST) {
                    // Falling back to a non-expedited job.
                    workSpec.expedited = false;
                    String message = String.format(
                            "Scheduling a non-expedited job (work ID %s)", workSpec.id);
                    Logger.get().debug(TAG, message);
                    scheduleInternal(workSpec, jobId);
                }
            }
        } catch (IllegalStateException e) {
            // This only gets thrown if we exceed 100 jobs.  Let's figure out if WorkManager is
            // responsible for all these jobs.
            String message = createErrorMessage(mContext, mWorkDatabase, mConfiguration);
            Logger.get().error(TAG, message);

            IllegalStateException schedulingException = new IllegalStateException(message, e);
            // If a SchedulingExceptionHandler is defined, let the app handle the scheduling
            // exception.
            Consumer<Throwable> handler = mConfiguration.getSchedulingExceptionHandler();
            if (handler != null) {
                handler.accept(schedulingException);
            } else {
                // Rethrow a more verbose exception.
                throw schedulingException;
            }

        } catch (Throwable throwable) {
            // OEM implementation bugs in JobScheduler cause the app to crash. Avoid crashing.
            Logger.get().error(TAG, "Unable to schedule " + workSpec, throwable);
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
            mWorkDatabase.systemIdInfoDao().removeSystemIdInfo(workSpecId);
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
    public static void cancelAllInAllNamespaces(@NonNull Context context) {
        // on API 34+ at first we cancel everything in our own namespace.
        if (Build.VERSION.SDK_INT >= 34) {
            JobScheduler namespacedScheduler = getWmJobScheduler(context);
            namespacedScheduler.cancelAll();
        }

        // on API 34+ we still cancel our jobs in the default namespace, because
        // there can be jobs scheduled by older version of library in the default namespace.
        // On the previous APIs there is no namespaces, so we cancel our jobs in the only one
        // global JobScheduler.
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        List<JobInfo> jobs = getPendingJobs(context, jobScheduler);
        if (jobs != null && !jobs.isEmpty()) {
            for (JobInfo jobInfo : jobs) {
                cancelJobById(jobScheduler, jobInfo.getId());
            }
        }
    }

    /**
     * Returns <code>true</code> if the list of jobs in {@link JobScheduler} are out of sync with
     * what {@link androidx.work.WorkManager} expects to see.
     * <p>
     * If {@link JobScheduler} knows about things {@link androidx.work.WorkManager} does not know
     * know about (or does not care about), cancel them.
     * <p>
     * If {@link androidx.work.WorkManager} does not see backing jobs in {@link JobScheduler} for
     * expected {@link WorkSpec}s, reset the {@code scheduleRequestedAt} bit, so that jobs can be
     * rescheduled.
     *
     * @param context      The application {@link Context}
     * @param workDatabase The {@link WorkDatabase} instance
     * @return <code>true</code> if jobs need to be reconciled.
     */
    public static boolean reconcileJobs(
            @NonNull Context context,
            @NonNull WorkDatabase workDatabase) {
        // reconcile only in the namespaced jobscheduler.
        // all the work is explicitly migrated to namespace on API 34+.
        JobScheduler jobScheduler = getWmJobScheduler(context);
        List<JobInfo> jobs = getPendingJobs(context, jobScheduler);
        List<String> workManagerWorkSpecs =
                workDatabase.systemIdInfoDao().getWorkSpecIds();

        int jobSize = jobs != null ? jobs.size() : 0;
        Set<String> jobSchedulerWorkSpecs = new HashSet<>(jobSize);
        if (jobs != null && !jobs.isEmpty()) {
            for (JobInfo jobInfo : jobs) {
                WorkGenerationalId id = getWorkGenerationalIdFromJobInfo(jobInfo);
                if (id != null) {
                    jobSchedulerWorkSpecs.add(id.getWorkSpecId());
                } else {
                    // Cancels invalid jobs owned by WorkManager.
                    // These jobs are invalid (in-actionable on our part) but occupy slots in
                    // JobScheduler. This is meant to help mitigate problems like b/134058261,
                    // where we have faulty implementations of JobScheduler.
                    cancelJobById(jobScheduler, jobInfo.getId());
                }
            }
        }
        boolean needsReconciling = false;
        for (String workSpecId : workManagerWorkSpecs) {
            if (!jobSchedulerWorkSpecs.contains(workSpecId)) {
                Logger.get().debug(TAG, "Reconciling jobs");
                needsReconciling = true;
                break;
            }
        }

        if (needsReconciling) {
            workDatabase.beginTransaction();
            try {
                WorkSpecDao workSpecDao = workDatabase.workSpecDao();
                for (String workSpecId : workManagerWorkSpecs) {
                    // Mark every WorkSpec instance with SCHEDULE_NOT_REQUESTED_AT = -1
                    // so that it can be picked up by JobScheduler again. This is required
                    // because from WorkManager's perspective this job was actually scheduled
                    // (but subsequently dropped). For this job to be picked up by schedulers
                    // observing scheduling limits this bit needs to be reset.
                    workSpecDao.markWorkSpecScheduled(workSpecId, SCHEDULE_NOT_REQUESTED_YET);
                }
                workDatabase.setTransactionSuccessful();
            } finally {
                workDatabase.endTransaction();
            }
        }
        return needsReconciling;
    }

    static @Nullable List<JobInfo> getPendingJobs(
            @NonNull Context context,
            @NonNull JobScheduler jobScheduler) {
        List<JobInfo> pendingJobs = getSafePendingJobs(jobScheduler);
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
    private static @Nullable List<Integer> getPendingJobIds(
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
            WorkGenerationalId id = getWorkGenerationalIdFromJobInfo(jobInfo);
            if (id != null && workSpecId.equals(id.getWorkSpecId())) {
                jobIds.add(jobInfo.getId());
            }
        }

        return jobIds;
    }

    private static @Nullable WorkGenerationalId getWorkGenerationalIdFromJobInfo(
            @NonNull JobInfo jobInfo) {
        PersistableBundle extras = jobInfo.getExtras();
        try {
            if (extras != null && extras.containsKey(EXTRA_WORK_SPEC_ID)) {
                int generation = extras.getInt(EXTRA_WORK_SPEC_GENERATION, 0);
                return new WorkGenerationalId(extras.getString(EXTRA_WORK_SPEC_ID), generation);
            }
        } catch (NullPointerException e) {
            // b/138364061: BaseBundle.mMap seems to be null in some cases here.  Ignore and return
            // null.
        }
        return null;
    }
}