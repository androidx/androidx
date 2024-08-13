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

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE;

import android.Manifest;
import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleService;
import androidx.work.Logger;

/**
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemForegroundService extends LifecycleService implements
        SystemForegroundDispatcher.Callback {

    private static final String TAG = Logger.tagWithPrefix("SystemFgService");

    @Nullable
    private static SystemForegroundService sForegroundService = null;

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
        mNotificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mDispatcher = new SystemForegroundDispatcher(getApplicationContext());
        mDispatcher.setCallback(this);
    }

    @MainThread
    @Override
    public void stop() {
        mIsShutdown = true;
        Logger.get().debug(TAG, "Shutting down.");
        // No need to pass in startId; stopSelf() translates to stopSelf(-1) which is a hard stop
        // of all startCommands. This is the behavior we want.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        sForegroundService = null;
        stopSelf();
    }

    @Override
    public void onTimeout(int startId) {
        // On API devices 35 both overloads of onTimeout() are invoked so we do nothing on the
        // version introduced in API 34 since the newer version will be invoked. However, on API 34
        // devices only this version is invoked, and thus why both versions are overridden.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return;
        }
        mDispatcher.onTimeout(startId, FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        mDispatcher.onTimeout(startId, fgsType);
    }

    @MainThread
    @Override
    public void startForeground(
            final int notificationId,
            final int notificationType,
            @NonNull final Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31Impl.startForeground(SystemForegroundService.this, notificationId,
                    notification, notificationType);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.startForeground(SystemForegroundService.this, notificationId,
                    notification, notificationType);
        } else {
            startForeground(notificationId, notification);
        }
    }

    @MainThread
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void notify(final int notificationId, @NonNull final Notification notification) {
        mNotificationManager.notify(notificationId, notification);
    }

    @Override
    @MainThread
    public void cancelNotification(final int notificationId) {
        mNotificationManager.cancel(notificationId);
    }

    /**
     * @return The current instance of {@link SystemForegroundService}.
     */
    @Nullable
    public static SystemForegroundService getInstance() {
        return sForegroundService;
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static void startForeground(Service service, int id, Notification notification,
                int foregroundServiceType) {
            service.startForeground(id, notification, foregroundServiceType);
        }
    }

    @RequiresApi(31)
    static class Api31Impl {
        private Api31Impl() {
            // This class is not instantiable.
        }

        static void startForeground(Service service, int id, Notification notification,
                int foregroundServiceType) {
            try {
                service.startForeground(id, notification, foregroundServiceType);
            } catch (ForegroundServiceStartNotAllowedException exception) {
                // This should ideally never happen. But there a chance that this method
                // is called, and the app is no longer in a state where it's possible to start a
                // foreground service. WorkManager will eventually call stop() to clean up.
                Logger.get().warning(TAG, "Unable to start foreground service", exception);
            } catch (SecurityException exception) {
                // In the case that a foreground type prerequisites permission is revoked
                // and the service is restarted it will not be possible to start the
                // foreground service.
                Logger.get().warning(TAG, "Unable to start foreground service", exception);
            }
        }
    }
}
