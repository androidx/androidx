/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationTestActivity extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();

        setContentView(R.layout.notification_test_activity);
    }

    public void pushNotification(@NonNull View v) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this.getApplicationContext());

        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0,
                new Intent(),
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this.getApplicationContext(), "CHANNEL_ID")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentTitle("Test App")
                .setContentText("Test Notification")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        int uniqueNotificationId = 0;
        notificationManager.notify(uniqueNotificationId, builder.build());
    }

    private void createNotificationChannel() {
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder("CHANNEL_ID",
                NotificationManager.IMPORTANCE_DEFAULT).setName("CHANNEL_NAME").build();
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this.getApplicationContext());
        notificationManager.createNotificationChannel(channel);
    }
}
