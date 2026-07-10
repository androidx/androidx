/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.notifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.compose.remote.integration.view.demos.R;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * Builder class to send Remote Compose notifications.
 */
@SuppressLint("RestrictedApiAndroidX")
public class RemoteNotification {
    private static final String TAG = "RemoteNotification";
    private static int sNotificationIdCounter = 1000;

    private final Context mContext;
    private byte[] mDocumentBytes;
    private String mChannelId = "remote_compose_notification_channel";
    private String mChannelName = "Remote Compose Notifications";
    private String mChannelDescription = "Channel for Remote Compose demos";
    private int mNotificationId = -1;
    private int mSmallIcon = R.drawable.ic_launcher_background;
    private PendingIntent mPendingIntent;
    private boolean mAutoCancel = true;
    private String mContentTitle;
    private String mContentText;
    private int mPriority = NotificationCompat.PRIORITY_DEFAULT;

    public RemoteNotification(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Set the Remote Compose document as a byte array.
     */
    public @NonNull RemoteNotification doc(byte @NonNull [] bytes) {
        mDocumentBytes = bytes;
        return this;
    }

    /**
     * Set the Remote Compose document.
     */
    @SuppressLint("RestrictedApi")
    public @NonNull RemoteNotification doc(@NonNull RemoteDocument document) {
        byte[] bytes = document.getDocument().getBuffer().getBuffer().getBuffer();
        int size = document.getDocument().getBuffer().getBuffer().getSize();
        if (bytes.length > size) {
            bytes = Arrays.copyOf(bytes, size);
        }
        mDocumentBytes = bytes;
        return this;
    }

    /**
     * Set the notification channel ID.
     */
    public @NonNull RemoteNotification channelId(@NonNull String channelId) {
        mChannelId = channelId;
        return this;
    }

    /**
     * Set the notification channel name.
     */
    public @NonNull RemoteNotification channelName(@NonNull String channelName) {
        mChannelName = channelName;
        return this;
    }

    /**
     * Set the notification channel description.
     */
    public @NonNull RemoteNotification channelDescription(@NonNull String description) {
        mChannelDescription = description;
        return this;
    }

    /**
     * Set the notification ID. If not set, a unique ID will be generated.
     */
    public @NonNull RemoteNotification notificationId(int id) {
        mNotificationId = id;
        return this;
    }

    /**
     * Set the small icon for the notification.
     */
    public @NonNull RemoteNotification smallIcon(int iconRes) {
        mSmallIcon = iconRes;
        return this;
    }

    /**
     * Set the content intent for the notification.
     */
    public @NonNull RemoteNotification contentIntent(@NonNull PendingIntent intent) {
        mPendingIntent = intent;
        return this;
    }

    /**
     * Set whether the notification should be automatically canceled when clicked.
     */
    public @NonNull RemoteNotification autoCancel(boolean autoCancel) {
        mAutoCancel = autoCancel;
        return this;
    }

    /**
     * Set the title of the notification.
     */
    public @NonNull RemoteNotification title(@NonNull String title) {
        mContentTitle = title;
        return this;
    }

    /**
     * Set the text content of the notification.
     */
    public @NonNull RemoteNotification text(@NonNull String text) {
        mContentText = text;
        return this;
    }

    /**
     * Set the priority of the notification.
     */
    public @NonNull RemoteNotification priority(int priority) {
        mPriority = priority;
        return this;
    }

    /**
     * Send the notification.
     */
    public void send() {
        if (Build.VERSION.SDK_INT < 35) { // Build.VERSION_CODES.VANILLA_ICE_CREAM
            Log.w(TAG, "Notification with DrawInstructions requires API 35 (Vanilla Ice Cream)");
            return;
        }

        if (mDocumentBytes == null) {
            Log.e(TAG, "Cannot send notification: document bytes are null");
            return;
        }

        try {
            createNotificationChannel();

            RemoteViews.DrawInstructions drawInstruction = new RemoteViews.DrawInstructions.Builder(
                    List.of(mDocumentBytes)).build();
            RemoteViews remoteViews = new RemoteViews(drawInstruction);

            if (mPendingIntent == null) {
                Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(
                        mContext.getPackageName());
                if (intent != null) {
                    mPendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                            PendingIntent.FLAG_IMMUTABLE);
                }
            }

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext, mChannelId)
                    .setSmallIcon(mSmallIcon)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(remoteViews)
                    .setPriority(mPriority)
                    .setAutoCancel(mAutoCancel);

            if (mContentTitle != null) {
                builder.setContentTitle(mContentTitle);
            }
            if (mContentText != null) {
                builder.setContentText(mContentText);
            }
            if (mPendingIntent != null) {
                builder.setContentIntent(mPendingIntent);
            }

            NotificationManagerCompat
                    notificationManager = NotificationManagerCompat.from(mContext);
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission not granted for notifications");
                return;
            }

            int id = mNotificationId != -1 ? mNotificationId : sNotificationIdCounter++;
            notificationManager.notify(id, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification", e);
        }
    }

    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(mChannelId, mChannelName,
                importance);
        channel.setDescription(mChannelDescription);

        NotificationManager notificationManager = mContext.getSystemService(
                NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}
