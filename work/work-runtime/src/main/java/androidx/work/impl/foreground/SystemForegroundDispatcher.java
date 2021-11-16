/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.foreground;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.ForegroundInfo;
import androidx.work.Logger;
import androidx.work.impl.ExecutionListener;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.WorkConstraintsCallback;
import androidx.work.impl.constraints.WorkConstraintsTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles requests for executing {@link androidx.work.WorkRequest}s on behalf of
 * {@link SystemForegroundService}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemForegroundDispatcher implements WorkConstraintsCallback, ExecutionListener {
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    static final String TAG = Logger.tagWithPrefix("SystemFgDispatcher");

    // keys
    private static final String KEY_NOTIFICATION = "KEY_NOTIFICATION";
    private static final String KEY_NOTIFICATION_ID = "KEY_NOTIFICATION_ID";
    private static final String KEY_FOREGROUND_SERVICE_TYPE = "KEY_FOREGROUND_SERVICE_TYPE";
    private static final String KEY_WORKSPEC_ID = "KEY_WORKSPEC_ID";

    // actions
    private static final String ACTION_START_FOREGROUND = "ACTION_START_FOREGROUND";
    private static final String ACTION_NOTIFY = "ACTION_NOTIFY";
    private static final String ACTION_CANCEL_WORK = "ACTION_CANCEL_WORK";
    private static final String ACTION_STOP_FOREGROUND = "ACTION_STOP_FOREGROUND";

    private Context mContext;
    private WorkManagerImpl mWorkManagerImpl;
    private final TaskExecutor mTaskExecutor;


    @SuppressWarnings("WeakerAccess") // Synthetic access
    final Object mLock;

    @SuppressWarnings("WeakerAccess") // Synthetic access
    String mCurrentForegroundWorkSpecId;

    @SuppressWarnings("WeakerAccess") // Synthetic access
    final Map<String, ForegroundInfo> mForegroundInfoById;

    @SuppressWarnings("WeakerAccess") // Synthetic access
    final Map<String, WorkSpec> mWorkSpecById;

    @SuppressWarnings("WeakerAccess") // Synthetic access
    final Set<WorkSpec> mTrackedWorkSpecs;

    @SuppressWarnings("WeakerAccess") // Synthetic access
    final WorkConstraintsTracker mConstraintsTracker;

    @Nullable
    private Callback mCallback;

    SystemForegroundDispatcher(@NonNull Context context) {
        mContext = context;
        mLock = new Object();
        mWorkManagerImpl = WorkManagerImpl.getInstance(mContext);
        mTaskExecutor = mWorkManagerImpl.getWorkTaskExecutor();
        mCurrentForegroundWorkSpecId = null;
        mForegroundInfoById = new LinkedHashMap<>();
        mTrackedWorkSpecs = new HashSet<>();
        mWorkSpecById = new HashMap<>();
        mConstraintsTracker = new WorkConstraintsTracker(mContext, mTaskExecutor, this);
        mWorkManagerImpl.getProcessor().addExecutionListener(this);
    }

    @VisibleForTesting
    SystemForegroundDispatcher(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManagerImpl,
            @NonNull WorkConstraintsTracker tracker) {

        mContext = context;
        mLock = new Object();
        mWorkManagerImpl = workManagerImpl;
        mTaskExecutor = mWorkManagerImpl.getWorkTaskExecutor();
        mCurrentForegroundWorkSpecId = null;
        mForegroundInfoById = new LinkedHashMap<>();
        mTrackedWorkSpecs = new HashSet<>();
        mWorkSpecById = new HashMap<>();
        mConstraintsTracker = tracker;
        mWorkManagerImpl.getProcessor().addExecutionListener(this);
    }

    @MainThread
    @Override
    public void onExecuted(@NonNull String workSpecId, boolean needsReschedule) {
        boolean removed = false;
        synchronized (mLock) {
            WorkSpec workSpec = mWorkSpecById.remove(workSpecId);
            if (workSpec != null) {
                removed = mTrackedWorkSpecs.remove(workSpec);
            }
            if (removed) {
                // Stop tracking constraints.
                mConstraintsTracker.replace(mTrackedWorkSpecs);
            }
        }

        ForegroundInfo removedInfo = mForegroundInfoById.remove(workSpecId);
        // Promote new notifications to the foreground if necessary.
        if (workSpecId.equals(mCurrentForegroundWorkSpecId)) {
            if (mForegroundInfoById.size() > 0) {
                // Find the next eligible ForegroundInfo
                // LinkedHashMap uses insertion order, so find the last one because that was
                // the most recent ForegroundInfo used. That way when different WorkSpecs share
                // notification ids, we still end up in a reasonably good place.
                Iterator<Map.Entry<String, ForegroundInfo>> iterator =
                        mForegroundInfoById.entrySet().iterator();

                Map.Entry<String, ForegroundInfo> entry = iterator.next();
                while (iterator.hasNext()) {
                    entry = iterator.next();
                }

                mCurrentForegroundWorkSpecId = entry.getKey();
                if (mCallback != null) {
                    ForegroundInfo info = entry.getValue();
                    mCallback.startForeground(
                            info.getNotificationId(),
                            info.getForegroundServiceType(),
                            info.getNotification());

                    // We used NotificationManager before to update notifications, so ensure
                    // that we reference count the Notification instance down by
                    // cancelling the notification.
                    mCallback.cancelNotification(info.getNotificationId());
                }
            }
        }
        // Keep track of the reference and use that when cancelling Notification. This is because
        // the work-testing library uses a direct executor and does *not* call this method
        // on the main thread.
        Callback callback = mCallback;
        if (removedInfo != null && callback != null) {
            // Explicitly decrement the reference count for the notification

            // We are doing this without having to wait for the handleStop() to clean up
            // Notifications. This is because the Processor stops foreground workers on the
            // dedicated task executor thread. Meanwhile Notifications are managed on the main
            // thread, so there is a chance that handleStop() fires before onExecuted() is called
            // on the main thread.
            Logger.get().debug(TAG,
                    "Removing Notification (id: " + removedInfo.getNotificationId() +
                            ", workSpecId: " + workSpecId +
                            ", notificationType: " + removedInfo.getForegroundServiceType());
            callback.cancelNotification(removedInfo.getNotificationId());
        }
    }

    @MainThread
    void setCallback(@NonNull Callback callback) {
        if (mCallback != null) {
            Logger.get().error(TAG, "A callback already exists.");
            return;
        }
        mCallback = callback;
    }

    WorkManagerImpl getWorkManager() {
        return mWorkManagerImpl;
    }

    void onStartCommand(@NonNull Intent intent) {
        String action = intent.getAction();
        if (ACTION_START_FOREGROUND.equals(action)) {
            handleStartForeground(intent);
            // Call handleNotify() which in turn calls startForeground() as part of handing this
            // command. This is important for some OEMs.
            handleNotify(intent);
        } else if (ACTION_NOTIFY.equals(action)) {
            handleNotify(intent);
        } else if (ACTION_CANCEL_WORK.equals(action)) {
            handleCancelWork(intent);
        } else if (ACTION_STOP_FOREGROUND.equals(action)) {
            handleStop(intent);
        }
    }

    @MainThread
    void onDestroy() {
        mCallback = null;
        synchronized (mLock) {
            mConstraintsTracker.reset();
        }
        mWorkManagerImpl.getProcessor().removeExecutionListener(this);
    }

    @MainThread
    private void handleStartForeground(@NonNull Intent intent) {
        Logger.get().info(TAG, "Started foreground service " + intent);
        final String workSpecId = intent.getStringExtra(KEY_WORKSPEC_ID);
        final WorkDatabase database = mWorkManagerImpl.getWorkDatabase();
        mTaskExecutor.executeOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                WorkSpec workSpec = database.workSpecDao().getWorkSpec(workSpecId);
                // Only track constraints if there are constraints that need to be tracked
                // (constraints are immutable)
                if (workSpec != null && workSpec.hasConstraints()) {
                    synchronized (mLock) {
                        mWorkSpecById.put(workSpecId, workSpec);
                        mTrackedWorkSpecs.add(workSpec);
                        mConstraintsTracker.replace(mTrackedWorkSpecs);
                    }
                }
            }
        });
    }

    @MainThread
    private void handleNotify(@NonNull Intent intent) {
        int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);
        int notificationType = intent.getIntExtra(KEY_FOREGROUND_SERVICE_TYPE, 0);
        String workSpecId = intent.getStringExtra(KEY_WORKSPEC_ID);
        Notification notification = intent.getParcelableExtra(KEY_NOTIFICATION);

        Logger.get().debug(TAG,
                "Notifying with (id:" + notificationId
                        + ", workSpecId: " + workSpecId
                        + ", notificationType :" + notificationType + ")");

        if (notification != null && mCallback != null) {
            // Keep track of this ForegroundInfo
            ForegroundInfo info = new ForegroundInfo(
                    notificationId, notification, notificationType);

            mForegroundInfoById.put(workSpecId, info);
            if (TextUtils.isEmpty(mCurrentForegroundWorkSpecId)) {
                // This is the current workSpecId which owns the Foreground lifecycle.
                mCurrentForegroundWorkSpecId = workSpecId;
                mCallback.startForeground(notificationId, notificationType, notification);
            } else {
                // Update notification
                mCallback.notify(notificationId, notification);
                // Update the notification in the foreground such that it's the union of
                // all current foreground service types if necessary.
                if (notificationType != FOREGROUND_SERVICE_TYPE_NONE
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    int foregroundServiceType = FOREGROUND_SERVICE_TYPE_NONE;
                    for (Map.Entry<String, ForegroundInfo> entry : mForegroundInfoById.entrySet()) {
                        ForegroundInfo foregroundInfo = entry.getValue();
                        foregroundServiceType |= foregroundInfo.getForegroundServiceType();
                    }
                    ForegroundInfo currentInfo =
                            mForegroundInfoById.get(mCurrentForegroundWorkSpecId);
                    if (currentInfo != null) {
                        mCallback.startForeground(
                                currentInfo.getNotificationId(),
                                foregroundServiceType,
                                currentInfo.getNotification()
                        );
                    }
                }
            }
        }
    }

    @MainThread
    void handleStop(@NonNull Intent intent) {
        Logger.get().info(TAG, "Stopping foreground service");
        if (mCallback != null) {
            mCallback.stop();
        }
    }

    @MainThread
    private void handleCancelWork(@NonNull Intent intent) {
        Logger.get().info(TAG, "Stopping foreground work for " + intent);
        String workSpecId = intent.getStringExtra(KEY_WORKSPEC_ID);
        if (workSpecId != null && !TextUtils.isEmpty(workSpecId)) {
            mWorkManagerImpl.cancelWorkById(UUID.fromString(workSpecId));
        }
    }

    @Override
    public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
        // Do nothing
    }

    @Override
    public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
        if (!workSpecIds.isEmpty()) {
            for (String workSpecId : workSpecIds) {
                Logger.get().debug(TAG,
                        "Constraints unmet for WorkSpec " + workSpecId);
                mWorkManagerImpl.stopForegroundWork(workSpecId);
            }
        }
    }

    /**
     * The {@link Intent} is used to start a foreground {@link android.app.Service}.
     *
     * @param context    The application {@link Context}
     * @param workSpecId The WorkSpec id of the Worker being executed in the context of the
     *                   foreground service
     * @return The {@link Intent}
     */
    @NonNull
    public static Intent createStartForegroundIntent(
            @NonNull Context context,
            @NonNull String workSpecId,
            @NonNull ForegroundInfo info) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        intent.putExtra(KEY_NOTIFICATION_ID, info.getNotificationId());
        intent.putExtra(KEY_FOREGROUND_SERVICE_TYPE, info.getForegroundServiceType());
        intent.putExtra(KEY_NOTIFICATION, info.getNotification());
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    /**
     * The {@link Intent} is used to cancel foreground work for a given {@link String} workSpecId.
     *
     * @param context    The application {@link Context}
     * @param workSpecId The WorkSpec id of the Worker being executed in the context of the
     *                   foreground service
     * @return The {@link Intent}
     */
    @NonNull
    public static Intent createCancelWorkIntent(
            @NonNull Context context,
            @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_CANCEL_WORK);
        // Set data to make it unique for filterEquals()
        intent.setData(Uri.parse("workspec://" + workSpecId));
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    /**
     * The {@link Intent} which is used to display a {@link Notification} via
     * {@link SystemForegroundService}.
     *
     * @param context    The application {@link Context}
     * @param workSpecId The {@link WorkSpec} id
     * @param info       The {@link ForegroundInfo}
     * @return The {@link Intent}
     */
    @NonNull
    public static Intent createNotifyIntent(
            @NonNull Context context,
            @NonNull String workSpecId,
            @NonNull ForegroundInfo info) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_NOTIFY);
        intent.putExtra(KEY_NOTIFICATION_ID, info.getNotificationId());
        intent.putExtra(KEY_FOREGROUND_SERVICE_TYPE, info.getForegroundServiceType());
        intent.putExtra(KEY_NOTIFICATION, info.getNotification());
        intent.putExtra(KEY_WORKSPEC_ID, workSpecId);
        return intent;
    }

    /**
     * The {@link Intent} which can be used to stop {@link SystemForegroundService}.
     *
     * @param context The application {@link Context}
     * @return The {@link Intent}
     */
    @NonNull
    public static Intent createStopForegroundIntent(@NonNull Context context) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_STOP_FOREGROUND);
        return  intent;
    }

    /**
     * Used to notify that all pending commands are now completed.
     */
    interface Callback {
        /**
         * An implementation of this callback should call
         * {@link android.app.Service#startForeground(int, Notification, int)}.
         */
        void startForeground(
                int notificationId,
                int notificationType,
                @NonNull Notification notification);

        /**
         * Used to update the {@link Notification}.
         */
        void notify(int notificationId, @NonNull Notification notification);

        /**
         * Used to cancel a {@link Notification}.
         */
        void cancelNotification(int notificationId);

        /**
         * Used to stop the {@link SystemForegroundService}.
         */
        void stop();
    }
}
