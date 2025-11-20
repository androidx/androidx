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
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteTableLockedException;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.UserManagerCompat;
import androidx.core.util.Consumer;
import androidx.work.Configuration;
import androidx.work.Logger;
import androidx.work.WorkInfo;
import androidx.work.impl.Schedulers;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkDatabasePathHelper;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.background.systemjob.SystemJobScheduler;
import androidx.work.impl.model.WorkProgressDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager is restarted after an app was force stopped.
 * Alarms and Jobs get cancelled when an application is force-stopped. To reschedule, we
 * create a pending alarm that will not survive force stops.
 *
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
    private final PreferenceUtils mPreferenceUtils;
    private int mRetryCount;

    public ForceStopRunnable(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        mContext = context.getApplicationContext();
        mWorkManager = workManager;
        mPreferenceUtils = workManager.getPreferenceUtils();
        mRetryCount = 0;
    }

    @Override
    public void run() {
        try {
            if (!multiProcessChecks()) {
                return;
            }
            while (true) {

                try {
                    // Migrate the database to the no-backup directory if necessary.
                    // Migrations are not retry-able. So if something unexpected were to happen
                    // here, the best we can do is to hand things off to the
                    // InitializationExceptionHandler.
                    WorkDatabasePathHelper.migrateDatabase(mContext);
                } catch (SQLiteException sqLiteException) {
                    // This should typically never happen.
                    String message = "Unexpected SQLite exception during migrations";
                    Logger.get().error(TAG, message);
                    IllegalStateException exception =
                            new IllegalStateException(message, sqLiteException);
                    Consumer<Throwable> exceptionHandler =
                            mWorkManager.getConfiguration().getInitializationExceptionHandler();
                    if (exceptionHandler != null) {
                        exceptionHandler.accept(exception);
                        break;
                    } else {
                        throw exception;
                    }
                }

                // Clean invalid jobs attributed to WorkManager, and Workers that might have been
                // interrupted because the application crashed (RUNNING state).
                Logger.get().debug(TAG, "Performing cleanup operations.");
                try {
                    forceStopRunnable();
                    break;
                } catch (SQLiteAccessPermException
                         | SQLiteCantOpenDatabaseException
                         | SQLiteConstraintException
                         | SQLiteDatabaseCorruptException
                         | SQLiteDatabaseLockedException
                         | SQLiteDiskIOException
                         | SQLiteFullException
                         | SQLiteTableLockedException exception) {
                    mRetryCount++;
                    if (mRetryCount >= MAX_ATTEMPTS) {
                        // ForceStopRunnable is usually the first thing that accesses a database
                        // (or an app's internal data directory). This means that weird
                        // PackageManager bugs are attributed to ForceStopRunnable, which is
                        // unfortunate. This gives the developer a better error
                        // message.
                        String message;
                        if (UserManagerCompat.isUserUnlocked(mContext)) {
                            message = "The file system on the device is in a bad state. "
                                    + "WorkManager cannot access the app's internal data store.";
                        } else {
                            message = "WorkManager can't be accessed from direct boot, because "
                                    + "credential encrypted storage isn't accessible.\n"
                                    + "Don't access or initialise WorkManager from directAware "
                                    + "components. See "
                                    + "https://developer.android.com/training/articles/direct-boot";
                        }
                        Logger.get().error(TAG, message, exception);
                        IllegalStateException throwable = new IllegalStateException(message,
                                exception);
                        Consumer<Throwable> exceptionHandler =
                                mWorkManager.getConfiguration().getInitializationExceptionHandler();
                        if (exceptionHandler != null) {
                            Logger.get().debug(TAG,
                                    "Routing exception to the specified exception handler",
                                    throwable);
                            exceptionHandler.accept(throwable);
                            break;
                        } else {
                            throw throwable;
                        }
                    } else {
                        long duration = mRetryCount * BACKOFF_DURATION_MS;
                        Logger.get()
                                .debug(TAG, "Retrying after " + duration,
                                        exception);
                        sleep(mRetryCount * BACKOFF_DURATION_MS);
                    }
                }
            }
        } finally {
            mWorkManager.onForceStopRunnableCompleted();
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
            if (Build.VERSION.SDK_INT >= 31) {
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
                    long timestamp = mPreferenceUtils.getLastForceStopEventMillis();
                    for (int i = 0; i < exitInfoList.size(); i++) {
                        ApplicationExitInfo info = exitInfoList.get(i);
                        if (info.getReason() == REASON_USER_REQUESTED
                                && info.getTimestamp() >= timestamp) {
                            return true;
                        }
                    }
                }
            } else if (pendingIntent == null) {
                setAlarm(mContext);
                return true;
            }
            return false;
        } catch (SecurityException | IllegalArgumentException exception) {
            // b/189975360 Some Samsung Devices seem to throw an IllegalArgumentException :( on
            // API 30.

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
            // Update the last known force-stop event timestamp.
            mPreferenceUtils.setLastForceStopEventMillis(
                    mWorkManager.getConfiguration().getClock().currentTimeMillis());
        } else if (needsScheduling) {
            Logger.get().debug(TAG, "Found unfinished work, scheduling it.");
            Schedulers.schedule(
                    mWorkManager.getConfiguration(),
                    mWorkManager.getWorkDatabase(),
                    mWorkManager.getSchedulers());
        }
    }

    /**
     * Performs cleanup operations like
     *
     * * Cancel invalid JobScheduler jobs.
     * * Reschedule previously RUNNING jobs.
     *
     * @return {@code true} if there are WorkSpecs that need rescheduling.
     */
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    public boolean cleanUp() {
        // Mitigation for faulty implementations of JobScheduler (b/134058261) and
        // Mitigation for a platform bug, which causes jobs to get dropped when binding to
        // SystemJobService fails.
        boolean needsReconciling = SystemJobScheduler.reconcileJobs(mContext,
                mWorkManager.getWorkDatabase());
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
                    workSpecDao.setStopReason(workSpec.id, WorkInfo.STOP_REASON_UNKNOWN);
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
    public boolean shouldRescheduleWorkers() {
        return mWorkManager.getPreferenceUtils().getNeedsReschedule();
    }

    /**
     * @return {@code true} if we are allowed to run in the current app process.
     */
    @VisibleForTesting
    public boolean multiProcessChecks() {
        // Ideally we should really check if RemoteWorkManager.getInstance() is non-null.
        // But ForceStopRunnable causes a lot of multi-process contention on the underlying
        // SQLite datastore. Therefore we only initialize WorkManager in the default app-process.
        Configuration configuration = mWorkManager.getConfiguration();
        // Check if the application specified a default process name. If they did not, we want to
        // run ForceStopRunnable in every app process. This is safer for apps with multiple
        // processes. There is risk of SQLite contention and that might result in a crash, but an
        // actual crash is better than decreased throughput for WorkRequests.
        if (TextUtils.isEmpty(configuration.getDefaultProcessName())) {
            Logger.get().debug(TAG, "The default process name was not specified.");
            return true;
        }
        boolean isDefaultProcess = ProcessUtils.isDefaultProcess(mContext, configuration);
        Logger.get().debug(TAG, "Is default app process = " + isDefaultProcess);
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
        if (Build.VERSION.SDK_INT >= 31) {
            flags |= FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = getPendingIntent(context, flags);
        // OK to use System.currentTimeMillis() since this is intended only to keep the alarm
        // scheduled ~forever and shouldn't need WorkManager to be initialized to reschedule.
        long triggerAt = System.currentTimeMillis() + TEN_YEARS;
        if (alarmManager != null) {
            alarmManager.setExact(RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    /**
     * A {@link android.content.BroadcastReceiver} which takes care of recreating the
     * long lived alarm which helps track force stops for an application.  This is the target of the
     * alarm set by ForceStopRunnable in {@link #setAlarm(Context)}.
     *
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
