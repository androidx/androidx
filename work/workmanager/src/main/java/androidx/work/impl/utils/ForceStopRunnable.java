/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl.utils;

import static android.app.AlarmManager.RTC_WAKEUP;
import static android.app.ApplicationExitInfo.REASON_USER_REQUESTED;
import static android.app.PendingIntent.FLAG_MUTABLE;
import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.impl.model.WorkSpec.SCHEDULE_NOT_REQUESTED_YET;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.ApplicationExitInfo;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteAccessPermException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteTableLockedException;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.BuildCompat;
import androidx.work.Configuration;
import androidx.work.InitializationExceptionHandler;
import androidx.work.Logger;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkDatabasePathHelper;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.model.WorkProgressDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager is restarted after an app was force stopped.
 * Alarms and Jobs get cancelled when an application is force-stopped. To reschedule, we
 * create a pending alarm that will not survive force stops.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ForceStopRunnable implements Runnable {

    private static final String TAG = Logger.tagWithPrefix("ForceStopRunnable");

    @VisibleForTesting
    static final String ACTION_FORCE_STOP_RESCHEDULE = "ACTION_FORCE_STOP_RESCHEDULE";
    @VisibleForTesting
    static final int MAX_ATTEMPTS = 3;

    // All our alarms are use request codes which are > 0.
    private static final int ALARM_ID = -1;
    private static final long BACKOFF_DURATION_MS = 300L;
    private static final long TEN_YEARS = TimeUnit.DAYS.toMillis(10 * 365);

    private final Context mContext;
    private final WorkManagerImpl mWorkManager;
    private int mRetryCount;

    public ForceStopRunnable(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        mContext = context.getApplicationContext();
        mWorkManager = workManager;
        mRetryCount = 0;
    }

    @Override
    public void run() {
        if (!multiProcessChecks()) {
            return;
        }

        while (true) {
            // Migrate the database to the no-backup directory if necessary.
            WorkDatabasePathHelper.migrateDatabase(mContext);
            // Clean invalid jobs attributed to WorkManager, and Workers that might have been
            // interrupted because the application crashed (RUNNING state).
            Logger.get().debug(TAG, "Performing cleanup operations.");
            try {
                forceStopRunnable();
                break;
            } catch (SQLiteCantOpenDatabaseException
                    | SQLiteDatabaseCorruptException
                    | SQLiteDatabaseLockedException
                    | SQLiteTableLockedException
                    | SQLiteConstraintException
                    | SQLiteAccessPermException exception) {
                mRetryCount++;
                if (mRetryCount >= MAX_ATTEMPTS) {
                    // ForceStopRunnable is usually the first thing that accesses a database
                    // (or an app's internal data directory). This means that weird
                    // PackageManager bugs are attributed to ForceStopRunnable, which is
                    // unfortunate. This gives the developer a better error
                    // message.
                    String message = "The file system on the device is in a bad state. "
                            + "WorkManager cannot access the app's internal data store.";
                    Logger.get().error(TAG, message, exception);
                    IllegalStateException throwable = new IllegalStateException(message, exception);
                    InitializationExceptionHandler exceptionHandler =
                            mWorkManager.getConfiguration().getExceptionHandler();
                    if (exceptionHandler != null) {
                        Logger.get().debug(TAG,
                                "Routing exception to the specified exception handler",
                                throwable);
                        exceptionHandler.handleException(throwable);
                        break;
                    } else {
                        throw throwable;
                    }
                } else {
                    long duration = mRetryCount * BACKOFF_DURATION_MS;
                    Logger.get()
                            .debug(TAG, String.format("Retrying after %s", duration), exception);
                    sleep(mRetryCount * BACKOFF_DURATION_MS);
                }
            }
        }
    }

    /**
     * @return {@code true} If the application was force stopped.
     */
    @VisibleForTesting
    public boolean isForceStopped() {
        // Alarms get cancelled when an app is force-stopped starting at Eclair MR1.
        // Cancelling of Jobs on force-stop was introduced in N-MR1 (SDK 25).
        // Even though API 23, 24 are probably safe, OEMs may choose to do
        // something different.
        try {
            int flags = FLAG_NO_CREATE;
            if (BuildCompat.isAtLeastS()) {
                flags |= FLAG_MUTABLE;
            }
            PendingIntent pendingIntent = getPendingIntent(mContext, flags);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // We no longer need the alarm.
                if (pendingIntent != null) {
                    pendingIntent.cancel();
                }
                ActivityManager activityManager =
                        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                List<ApplicationExitInfo> exitInfoList =
                        activityManager.getHistoricalProcessExitReasons(
                                null /* match caller uid */,
                                0, // ignore
                                0 // ignore
                        );

                if (exitInfoList != null && !exitInfoList.isEmpty()) {
                    for (int i = 0; i < exitInfoList.size(); i++) {
                        ApplicationExitInfo info = exitInfoList.get(i);
                        if (info.getReason() == REASON_USER_REQUESTED) {
                            return true;
                        }
                    }
                }
            } else if (pendingIntent == null) {
                setAlarm(mContext);
                return true;
            }
            return false;
        } catch (SecurityException | IllegalStateException exception) {
            // b/189975360 Some Samsung Devices seem to throw an IllegalStateException :( on API 30.

            // Setting Alarms on some devices fails due to OEM introduced bugs in AlarmManager.
            // When this happens, there is not much WorkManager can do, other can reschedule
            // everything.
            Logger.get().warning(TAG, "Ignoring exception", exception);
            return true;
        }
    }

    /**
     * Performs all the necessary steps to initialize {@link androidx.work.WorkManager}/
     */
    @VisibleForTesting
    public void forceStopRunnable() {
        boolean needsScheduling = cleanUp();
        if (shouldRescheduleWorkers()) {
            Logger.get().debug(TAG, "Rescheduling Workers.");
            mWorkManager.rescheduleEligibleWork();
            // Mark the jobs as migrated.
            mWorkManager.getPreferenceUtils().setNeedsReschedule(false);
        } else if (isForceStopped()) {
            Logger.get().debug(TAG, "Application was force-stopped, rescheduling.");
            mWorkManager.rescheduleEligibleWork();
        } else if (needsScheduling) {
            Logger.get().debug(TAG, "Found unfinished work, scheduling it.");
            Schedulers.schedule(
                    mWorkManager.getConfiguration(),
                    mWorkManager.getWorkDatabase(),
                    mWorkManager.getSchedulers());
        }
        mWorkManager.onForceStopRunnableCompleted();
    }

    /**
     * Performs cleanup operations like
     *
     * * Cancel invalid JobScheduler jobs.
     * * Reschedule previously RUNNING jobs.
     *
     * @return {@code true} if there are WorkSpecs that need rescheduling.
     */
    @VisibleForTesting
    public boolean cleanUp() {
        boolean needsReconciling = false;
        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            // Mitigation for faulty implementations of JobScheduler (b/134058261) and
            // Mitigation for a platform bug, which causes jobs to get dropped when binding to
            // SystemJobService fails.
            needsReconciling = SystemJobScheduler.reconcileJobs(mContext, mWorkManager);
        }
        // Reset previously unfinished work.
        WorkDatabase workDatabase = mWorkManager.getWorkDatabase();
        WorkSpecDao workSpecDao = workDatabase.workSpecDao();
        WorkProgressDao workProgressDao = workDatabase.workProgressDao();
        workDatabase.beginTransaction();
        boolean needsScheduling;
        try {
            List<WorkSpec> workSpecs = workSpecDao.getRunningWork();
            needsScheduling = workSpecs != null && !workSpecs.isEmpty();
            if (needsScheduling) {
                // Mark every instance of unfinished work with state = ENQUEUED and
                // SCHEDULE_NOT_REQUESTED_AT = -1 irrespective of its current state.
                // This is because the application might have crashed previously and we should
                // reschedule jobs that may have been running previously.
                // Also there is a chance that an application crash, happened during
                // onStartJob() and now no corresponding job now exists in JobScheduler.
                // To solve this, we simply force-reschedule all unfinished work.
                for (WorkSpec workSpec : workSpecs) {
                    workSpecDao.setState(ENQUEUED, workSpec.id);
                    workSpecDao.markWorkSpecScheduled(workSpec.id, SCHEDULE_NOT_REQUESTED_YET);
                }
            }
            workProgressDao.deleteAll();
            workDatabase.setTransactionSuccessful();
        } finally {
            workDatabase.endTransaction();
        }
        return needsScheduling || needsReconciling;
    }

    /**
     * @return {@code true} If we need to reschedule Workers.
     */
    @VisibleForTesting
    boolean shouldRescheduleWorkers() {
        return mWorkManager.getPreferenceUtils().getNeedsReschedule();
    }

    /**
     * @return {@code true} if we are allowed to run in the current app process.
     */
    @VisibleForTesting
    public boolean multiProcessChecks() {
        if (mWorkManager.getRemoteWorkManager() == null) {
            return true;
        }
        Logger.get().debug(TAG, "Found a remote implementation for WorkManager");
        Configuration configuration = mWorkManager.getConfiguration();
        boolean isDefaultProcess = ProcessUtils.isDefaultProcess(mContext, configuration);
        Logger.get().debug(TAG, String.format("Is default app process = %s", isDefaultProcess));
        return isDefaultProcess;
    }

    /**
     * Helps with backoff when exceptions occur during {@link androidx.work.WorkManager}
     * initialization.
     */
    @VisibleForTesting
    public void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignore) {
            // Nothing to do really.
        }
    }

    /**
     * @param flags The {@link PendingIntent} flags.
     * @return an instance of the {@link PendingIntent}.
     */
    private static PendingIntent getPendingIntent(Context context, int flags) {
        Intent intent = getIntent(context);
        return PendingIntent.getBroadcast(context, ALARM_ID, intent, flags);
    }

    /**
     * @return The instance of {@link Intent} used to keep track of force stops.
     */
    @VisibleForTesting
    static Intent getIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, ForceStopRunnable.BroadcastReceiver.class));
        intent.setAction(ACTION_FORCE_STOP_RESCHEDULE);
        return intent;
    }

    static void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Using FLAG_UPDATE_CURRENT, because we only ever want once instance of this alarm.
        int flags = FLAG_UPDATE_CURRENT;
        if (BuildCompat.isAtLeastS()) {
            flags |= FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = getPendingIntent(context, flags);
        long triggerAt = System.currentTimeMillis() + TEN_YEARS;
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.set(RTC_WAKEUP, triggerAt, pendingIntent);
            }
        }
    }

    /**
     * A {@link android.content.BroadcastReceiver} which takes care of recreating the
     * long lived alarm which helps track force stops for an application.  This is the target of the
     * alarm set by ForceStopRunnable in {@link #setAlarm(Context)}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class BroadcastReceiver extends android.content.BroadcastReceiver {
        private static final String TAG = Logger.tagWithPrefix("ForceStopRunnable$Rcvr");

        @Override
        public void onReceive(@NonNull Context context, @Nullable Intent intent) {
            // Our alarm somehow got triggered, so make sure we reschedule it.  This should really
            // never happen because we set it so far in the future.
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_FORCE_STOP_RESCHEDULE.equals(action)) {
                    Logger.get().verbose(
                            TAG,
                            "Rescheduling alarm that keeps track of force-stops.");
                    ForceStopRunnable.setAlarm(context);
                }
            }
        }
    }
}
