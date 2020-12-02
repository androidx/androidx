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
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleService;
import androidx.work.Logger;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemForegroundService extends LifecycleService implements
        SystemForegroundDispatcher.Callback {

    private static final String TAG = Logger.tagWithPrefix("SystemFgService");

    @Nullable
    private static SystemForegroundService sForegroundService = null;

    private Handler mHandler;
    private boolean mIsShutdown;

    // Synthetic access
    SystemForegroundDispatcher mDispatcher;
    // Synthetic access
    NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        sForegroundService = this;
        initializeDispatcher();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (mIsShutdown) {
            Logger.get().info(TAG,
                    "Re-initializing SystemForegroundService after a request to shut-down.");

            // Destroy the old dispatcher to complete it's lifecycle.
            mDispatcher.onDestroy();
            // Create a new dispatcher to setup a new lifecycle.
            initializeDispatcher();
            // Set mIsShutdown to false, to correctly accept new commands.
            mIsShutdown = false;
        }

        if (intent != null) {
            mDispatcher.onStartCommand(intent);
        }

        // If the service were to crash, we want all unacknowledged Intents to get redelivered.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispatcher.onDestroy();
    }

    @MainThread
    private void initializeDispatcher() {
        mHandler = new Handler(Looper.getMainLooper());
        mNotificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mDispatcher = new SystemForegroundDispatcher(getApplicationContext());
        mDispatcher.setCallback(this);
    }

    @MainThread
    @Override
    public void stop() {
        mIsShutdown = true;
        Logger.get().debug(TAG, "All commands completed.");
        // No need to pass in startId; stopSelf() translates to stopSelf(-1) which is a hard stop
        // of all startCommands. This is the behavior we want.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        sForegroundService = null;
        stopSelf();
    }

    @Override
    public void startForeground(
            final int notificationId,
            final int notificationType,
            @NonNull final Notification notification) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(notificationId, notification, notificationType);
                } else {
                    startForeground(notificationId, notification);
                }
            }
        });
    }

    @Override
    public void notify(final int notificationId, @NonNull final Notification notification) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.notify(notificationId, notification);
            }
        });
    }

    @Override
    public void cancelNotification(final int notificationId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.cancel(notificationId);
            }
        });
    }

    /**
     * @return The current instance of {@link SystemForegroundService}.
     */
    @Nullable
    public static SystemForegroundService getInstance() {
        return sForegroundService;
    }
}
