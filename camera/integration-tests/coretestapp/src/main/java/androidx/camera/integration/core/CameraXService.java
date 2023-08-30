/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.core;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

/**
 * A service used to test background UseCases binding and camera operations.
 */
public class CameraXService extends LifecycleService {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID_SERVICE_INFO = "channel_service_info";

    private final IBinder mBinder = new CameraXServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        makeForeground();
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return mBinder;
    }

    private void makeForeground() {
        createNotificationChannel();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,
                CHANNEL_ID_SERVICE_INFO)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = Api26Impl.newNotificationChannel(
                    CHANNEL_ID_SERVICE_INFO,
                    getString(R.string.camerax_service),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            Api26Impl.createNotificationChannel(getNotificationManager(), serviceChannel);
        }
    }

    @NonNull
    private NotificationManager getNotificationManager() {
        return checkNotNull(ContextCompat.getSystemService(this, NotificationManager.class));
    }

    @RequiresApi(26)
    static class Api26Impl {

        private Api26Impl() {
        }

        /** @noinspection SameParameterValue */
        @DoNotInline
        @NonNull
        static NotificationChannel newNotificationChannel(@NonNull String id,
                @NonNull CharSequence name, int importance) {
            return new NotificationChannel(id, name, importance);
        }

        @DoNotInline
        static void createNotificationChannel(@NonNull NotificationManager manager,
                @NonNull NotificationChannel channel) {
            manager.createNotificationChannel(channel);
        }
    }

    class CameraXServiceBinder extends Binder {
        @NonNull
        CameraXService getService() {
            return CameraXService.this;
        }
    }
}
