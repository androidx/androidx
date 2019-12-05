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

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
    private static final String KEY_NOTIFICATION_TAG = "KEY_NOTIFICATION_TAG";
    private static final String KEY_WORKSPEC_ID = "KEY_WORKSPEC_ID";

    // actions
    private static final String ACTION_START_FOREGROUND = "ACTION_START_FOREGROUND";
    private static final String ACTION_NOTIFY = "ACTION_NOTIFY";
    private static final String ACTION_STOP_FOREGROUND = "ACTION_STOP_FOREGROUND";
    private static final String ACTION_CANCEL_WORK = "ACTION_CANCEL_WORK";

    private Context mContext;
    private WorkManagerImpl mWorkManagerImpl;
    private final TaskExecutor mTaskExecutor;


    @SuppressWarnings("WeakerAccess") // Synthetic access
    final Object mLock;

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
        }
        if (removed) {
            // Stop tracking
            mConstraintsTracker.replace(mTrackedWorkSpecs);
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
        } else if (ACTION_STOP_FOREGROUND.equals(action)) {
            handleStop(intent);
        } else if (ACTION_NOTIFY.equals(action)) {
            handleNotify(intent);
        } else if (ACTION_CANCEL_WORK.equals(action)) {
            handleCancelWork(intent);
        }
    }

    @MainThread
    void onDestroy() {
        mCallback = null;
        mConstraintsTracker.reset();
        mWorkManagerImpl.getProcessor().removeExecutionListener(this);
    }

    @MainThread
    private void handleStartForeground(@NonNull Intent intent) {
        Logger.get().info(TAG, String.format("Started foreground service %s", intent));
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
                    }
                    mConstraintsTracker.replace(mTrackedWorkSpecs);
                }
            }
        });
    }

    @MainThread
    private void handleNotify(@NonNull Intent intent) {
        int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);
        int notificationType = intent.getIntExtra(KEY_FOREGROUND_SERVICE_TYPE, 0);
        String notificationTag = intent.getStringExtra(KEY_NOTIFICATION_TAG);
        Notification notification = intent.getParcelableExtra(KEY_NOTIFICATION);
        if (notification != null && mCallback != null) {
            mCallback.notify(notificationId, notificationType, notificationTag, notification);
        }
    }

    @MainThread
    private void handleStop(@NonNull Intent intent) {
        Logger.get().info(TAG, String.format("Stopping foreground service %s", intent));
        if (mCallback != null) {
            mCallback.stop();
        }
    }

    @MainThread
    private void handleCancelWork(@NonNull Intent intent) {
        Logger.get().info(TAG, String.format("Stopping foreground work for %s", intent));
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
                        String.format("Constraints unmet for WorkSpec %s", workSpecId));
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
            @NonNull String workSpecId) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_START_FOREGROUND);
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
        intent.setData(Uri.parse(String.format("workspec://%s", workSpecId)));
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
        intent.putExtra(KEY_NOTIFICATION_TAG, workSpecId);
        return intent;
    }

    /**
     * The {@link Intent} is used to stop a foreground {@link android.app.Service}.
     *
     * @param context   The application {@link Context}
     * @return The {@link Intent}
     */
    @NonNull
    public static Intent createStopForegroundIntent(@NonNull Context context) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_STOP_FOREGROUND);
        return intent;
    }

    /**
     * Used to notify that all pending commands are now completed.
     */
    interface Callback {
        /**
         * Used to update the {@link Notification}.
         */
        void notify(
                int notificationId,
                int notificationType,
                @Nullable String notificationTag,
                @NonNull Notification notification);

        /**
         * Used to stop the {@link SystemForegroundService}.
         */
        void stop();
    }
}
