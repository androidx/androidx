/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.misc;

import static androidx.car.app.sample.showcase.common.DeepLinkNotificationReceiver.INTENT_ACTION_CANCEL_RESERVATION;
import static androidx.car.app.sample.showcase.common.DeepLinkNotificationReceiver.INTENT_ACTION_PHONE;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Template;
import androidx.car.app.notification.CarAppExtender;
import androidx.car.app.sample.showcase.common.DeepLinkNotificationReceiver;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/** A simple screen that demonstrates how to use notifications in a car app. */
public final class NotificationDemoScreen extends Screen implements DefaultLifecycleObserver {

    private static final String NOTIFICATION_CHANNEL_ID = "channel_00";
    private static final CharSequence NOTIFICATION_CHANNEL_NAME = "Default Channel";
    private static final int NOTIFICATION_ID = 1001;

    private static final String NOTIFICATION_CHANNEL_HIGH_ID = "channel_01";
    private static final CharSequence NOTIFICATION_CHANNEL_HIGH_NAME = "High Channel";

    private static final String NOTIFICATION_CHANNEL_LOW_ID = "channel_02";
    private static final CharSequence NOTIFICATION_CHANNEL_LOW_NAME = "Low Channel";

    private static final String INTENT_ACTION_PRIMARY_PHONE =
            "androidx.car.app.sample.showcase.common.INTENT_ACTION_PRIMARY_PHONE";
    private static final String INTENT_ACTION_SECONDARY_PHONE =
            "androidx.car.app.sample.showcase.common.INTENT_ACTION_SECONDARY_PHONE";

    private static final int MSG_SEND_NOTIFICATION = 1;

    static final long NOTIFICATION_DELAY_IN_MILLIS = SECONDS.toMillis(1);
    final Handler mHandler = new Handler(Looper.getMainLooper(), new HandlerCallback());

    private final IconCompat mIcon = IconCompat.createWithResource(getCarContext(),
            R.drawable.ic_face_24px);
    private int mImportance = NotificationManager.IMPORTANCE_DEFAULT;
    private boolean mIsNavCategory = false;
    private boolean mSetOngoing = false;
    private int mNotificationCount = 0;

    /** A broadcast receiver that can show a toast message upon receiving a broadcast. */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CarToast.makeText(
                    getCarContext(),
                    "Triggered: " + intent.getAction(),
                    CarToast.LENGTH_SHORT)
                    .show();
        }
    };

    public NotificationDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        registerBroadcastReceiver();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        unregisterBroadcastReceiver();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        // Send a single notification with the settings configured by other buttons.
        listBuilder.addItem(
                new GridItem.Builder()
                        .setTitle("Send a notification")
                        .setImage(new CarIcon.Builder(mIcon).build())
                        .setOnClickListener(this::sendNotification)
                        .build());

        // Start a repeating notification with the settings configured by other buttons.
        listBuilder.addItem(
                new GridItem.Builder()
                        .setTitle("Start notifications")
                        .setImage(new CarIcon.Builder(mIcon).build())
                        .setOnClickListener(() -> mHandler.sendMessage(
                                mHandler.obtainMessage(MSG_SEND_NOTIFICATION)))
                        .build());

        // Stop the repeating notification and reset the count.
        listBuilder.addItem(
                new GridItem.Builder()
                        .setTitle("Stop notifications")
                        .setImage(new CarIcon.Builder(mIcon).build())
                        .setOnClickListener(() -> {
                            mHandler.removeMessages(MSG_SEND_NOTIFICATION);
                            NotificationManagerCompat.from(getCarContext()).cancelAll();
                            mNotificationCount = 0;
                        })
                        .build());

        // Configure the notification importance.
        listBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mIcon).build())
                        .setTitle("Importance")
                        .setText(getImportanceString())
                        .setOnClickListener(() -> {
                            setImportance();
                            invalidate();
                        })
                        .build());

        // Configure whether the notification's category is navigation.
        listBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mIcon).build())
                        .setTitle("Category")
                        .setText(getCategoryString())
                        .setOnClickListener(() -> {
                            mIsNavCategory = !mIsNavCategory;
                            invalidate();
                        })
                        .build());

        // Configure whether the notification is an ongoing notification.
        listBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mIcon).build())
                        .setTitle("Ongoing")
                        .setText(String.valueOf(mSetOngoing))
                        .setOnClickListener(() -> {
                            mSetOngoing = !mSetOngoing;
                            invalidate();
                        })
                        .build());

        return new GridTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Notification Demo")
                .setHeaderAction(Action.BACK)
                .build();
    }

    void sendNotification() {
        mNotificationCount++;
        String title = "Notification: " + getImportanceString() + ", " + mNotificationCount;
        String text = "Category: " + getCategoryString() + ", ongoing: " + mSetOngoing;

        switch (mImportance) {
            case NotificationManager.IMPORTANCE_HIGH:
                sendNotification(title, text, NOTIFICATION_CHANNEL_HIGH_ID,
                        NOTIFICATION_CHANNEL_HIGH_NAME, NOTIFICATION_ID,
                        NotificationManager.IMPORTANCE_HIGH);
                break;
            case NotificationManager.IMPORTANCE_DEFAULT:
                sendNotification(title, text, NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
                        NOTIFICATION_ID, NotificationManager.IMPORTANCE_DEFAULT);
                break;
            case NotificationManager.IMPORTANCE_LOW:
                sendNotification(title, text, NOTIFICATION_CHANNEL_LOW_ID,
                        NOTIFICATION_CHANNEL_LOW_NAME, NOTIFICATION_ID,
                        NotificationManager.IMPORTANCE_LOW);
                break;
            default:
                break;
        }
    }

    // Suppressing 'ObsoleteSdkInt' as this code is shared between APKs with different min SDK
    // levels
    @SuppressLint({"UnsafeNewApiCall", "ObsoleteSdkInt"})
    private void sendNotification(CharSequence title, CharSequence text, String channelId,
            CharSequence channelName, int notificationId, int importance) {
        NotificationManagerCompat notificationManagerCompat =
                NotificationManagerCompat.from(getCarContext());
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            channelId,
                            channelName,
                            importance);
            notificationManagerCompat.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder;
        builder = new NotificationCompat.Builder(getCarContext(), channelId);
        if (mIsNavCategory) {
            builder.setCategory(NotificationCompat.CATEGORY_NAVIGATION);
        }
        builder.setOngoing(mSetOngoing);

        Notification notification =
                builder.setSmallIcon(R.drawable.ic_bug_report_24px)
                        .setContentTitle(title + " (phone)")
                        .setContentText(text + " (phone)")
                        .setLargeIcon(
                                BitmapFactory.decodeResource(
                                        getCarContext().getResources(), R.drawable.ic_hi))
                        .addAction(
                                new NotificationCompat.Action.Builder(
                                        R.drawable.ic_face_24px,
                                        "Action1 (phone)",
                                        createPendingIntent(INTENT_ACTION_PRIMARY_PHONE))
                                        .build())
                        .addAction(
                                R.drawable.ic_commute_24px,
                                "Action2 (phone)",
                                createPendingIntent(INTENT_ACTION_SECONDARY_PHONE))
                        .extend(
                                new CarAppExtender.Builder()
                                        .setContentTitle(title)
                                        .setContentText(text)
                                        .setSmallIcon(R.drawable.ic_bug_report_24px)
                                        .setLargeIcon(
                                                BitmapFactory.decodeResource(
                                                        getCarContext().getResources(),
                                                        R.drawable.ic_hi))
                                        .addAction(
                                                R.drawable.ic_commute_24px,
                                                "Complete on Phone",
                                                createDeepLinkActionPendingIntent(
                                                        INTENT_ACTION_PHONE))
                                        .addAction(
                                                R.drawable.ic_face_24px,
                                                "Cancel",
                                                createDeepLinkActionPendingIntent(
                                                        INTENT_ACTION_CANCEL_RESERVATION))
                                        .build())
                        .build();

        notificationManagerCompat.notify(notificationId, notification);
    }

    private String getImportanceString() {
        switch (mImportance) {
            case NotificationManager.IMPORTANCE_HIGH:
                return "High";
            case NotificationManager.IMPORTANCE_DEFAULT:
                return "Default";
            case NotificationManager.IMPORTANCE_LOW:
                return "Low";
            default:
                return "Unknown";
        }
    }

    private String getCategoryString() {
        return mIsNavCategory ? "Navigation" : "None";
    }

    /**
     * Change the notification importance in a rotating sequence:
     * Low -> Default -> High -> Low...
     */
    private void setImportance() {
        switch (mImportance) {
            case NotificationManager.IMPORTANCE_HIGH:
                mImportance = NotificationManager.IMPORTANCE_LOW;
                break;
            case NotificationManager.IMPORTANCE_DEFAULT:
                mImportance = NotificationManager.IMPORTANCE_HIGH;
                break;
            case NotificationManager.IMPORTANCE_LOW:
                mImportance = NotificationManager.IMPORTANCE_DEFAULT;
                break;
            default:
                break;
        }
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTION_PRIMARY_PHONE);
        filter.addAction(INTENT_ACTION_SECONDARY_PHONE);

        getCarContext().registerReceiver(mBroadcastReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        getCarContext().unregisterReceiver(mBroadcastReceiver);
    }

    /** Returns a pending intent with the provided intent action. */
    private PendingIntent createDeepLinkActionPendingIntent(String intentAction) {
        Intent intent =
                new Intent(intentAction)
                        .setComponent(
                                new ComponentName(
                                        getCarContext(), DeepLinkNotificationReceiver.class));
        return PendingIntent.getBroadcast(getCarContext(), intentAction.hashCode(), intent, 0);
    }

    /** Returns a pending intent with the provided intent action. */
    private PendingIntent createPendingIntent(String intentAction) {
        Intent intent = new Intent(intentAction);
        return PendingIntent.getBroadcast(getCarContext(), intentAction.hashCode(), intent, 0);
    }

    final class HandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_SEND_NOTIFICATION) {
                sendNotification();
                mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(MSG_SEND_NOTIFICATION),
                        NOTIFICATION_DELAY_IN_MILLIS);
                return true;
            }
            return false;
        }
    }
}
