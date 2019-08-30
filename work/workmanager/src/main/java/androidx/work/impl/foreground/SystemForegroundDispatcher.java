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
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.NotificationMetadata;
import androidx.work.impl.WorkManagerImpl;

/**
 * Handles requests for executing {@link androidx.work.WorkRequest}s on behalf of
 * {@link SystemForegroundService}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemForegroundDispatcher {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("SystemFgDispatcher");

    // keys
    private static final String KEY_NOTIFICATION = "KEY_NOTIFICATION";
    private static final String KEY_NOTIFICATION_ID = "KEY_NOTIFICATION_ID";
    private static final String KEY_NOTIFICATION_TYPE = "KEY_NOTIFICATION_TYPE";
    private static final String KEY_NOTIFICATION_TAG = "KEY_NOTIFICATION_TAG";

    // actions
    private static final String ACTION_START_FOREGROUND = "ACTION_START_FOREGROUND";
    private static final String ACTION_NOTIFY = "ACTION_NOTIFY";
    private static final String ACTION_STOP_FOREGROUND = "ACTION_STOP_FOREGROUND";

    private Context mContext;
    private WorkManagerImpl mWorkManagerImpl;

    @Nullable
    private Callback mCallback;

    SystemForegroundDispatcher(@NonNull Context context) {
        mContext = context;
        mWorkManagerImpl = WorkManagerImpl.getInstance(mContext);
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
        }
    }

    @MainThread
    void onDestroy() {
        mCallback = null;
    }

    @MainThread
    private void handleStartForeground(@NonNull Intent intent) {
        Logger.get().info(TAG, String.format("Started foreground service %s", intent));
    }

    @MainThread
    private void handleNotify(@NonNull Intent intent) {
        int notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0);
        int notificationType = intent.getIntExtra(KEY_NOTIFICATION_TYPE, 0);
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

    /**
     * The {@link Intent} is used to start a foreground {@link android.app.Service}.
     *
     * @param context   The application {@link Context}
     * @return The {@link Intent}
     */
    @NonNull
    public static Intent createStartForegroundIntent(@NonNull Context context) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        return intent;
    }

    /**
     * The {@link Intent} which is used to display a {@link Notification} via
     * {@link SystemForegroundService}.
     *o
     * @param context  The application {@link Context}
     * @param metadata The {@link NotificationMetadata}
     * @return The {@link Intent}
     */
    @NonNull
    public static Intent createNotifyIntent(
            @NonNull Context context,
            @NonNull NotificationMetadata metadata) {
        Intent intent = new Intent(context, SystemForegroundService.class);
        intent.setAction(ACTION_NOTIFY);
        intent.putExtra(KEY_NOTIFICATION_ID, metadata.getNotificationId());
        intent.putExtra(KEY_NOTIFICATION_TYPE, metadata.getNotificationType());
        intent.putExtra(KEY_NOTIFICATION, metadata.getNotification());
        if (!TextUtils.isEmpty(metadata.getNotificationTag())) {
            intent.putExtra(KEY_NOTIFICATION_TAG, metadata.getNotificationTag());
        }
        return intent;
    }

    /**
     * The {@link Intent} is used to start a foreground {@link android.app.Service}.
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
