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

package androidx.car.app.samples.showcase.misc;

import static androidx.car.app.model.Action.BACK;
import static androidx.car.app.samples.showcase.DeepLinkNotificationReceiver.INTENT_ACTION_CANCEL_RESERVATION;
import static androidx.car.app.samples.showcase.DeepLinkNotificationReceiver.INTENT_ACTION_PHONE;

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

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.notification.CarAppExtender;
import androidx.car.app.samples.showcase.DeepLinkNotificationReceiver;
import androidx.car.app.samples.showcase.R;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/** Creates a screen that has an assortment of API demos. */
public final class MiscDemoScreen extends Screen implements DefaultLifecycleObserver {
    private static final String NOTIFICATION_CHANNEL_ID = "channel_00";
    private static final CharSequence NOTIFICATION_CHANNEL_NAME = "Default Channel";
    private static final int NOTIFICATION_ID = 1001;

    static final String MARKER = "MiscDemoScreen";

    private static final String INTENT_ACTION_PRIMARY_CAR =
            "com.showcase.INTENT_ACTION_PRIMARY_CAR";
    private static final String INTENT_ACTION_DELETE_CAR = "com.showcase.INTENT_ACTION_DELETE_CAR";
    private static final String INTENT_ACTION_PRIMARY_PHONE =
            "com.showcase.INTENT_ACTION_PRIMARY_PHONE";
    private static final String INTENT_ACTION_SECONDARY_PHONE =
            "com.showcase.INTENT_ACTION_SECONDARY_PHONE";
    private static final String INTENT_ACTION_TERTIARY_PHONE =
            "com.showcase.INTENT_ACTION_TERTIARY_PHONE";
    private static final String INTENT_ACTION_DELETE_PHONE =
            "com.showcase.INTENT_ACTION_DELETE_PHONE";

    private final BroadcastReceiver mBroadcastReceiver;

    public MiscDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        // Create a broadcast receiver that can show a toast message upon receiving a broadcast.
        mBroadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        CarToast.makeText(
                                getCarContext(),
                                "Triggered: " + intent.getAction(),
                                CarToast.LENGTH_SHORT)
                                .show();
                    }
                };
        getLifecycle().addObserver(this);
        setMarker(MARKER);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        registerBroadcastReceiver();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        getCarContext().unregisterReceiver(mBroadcastReceiver);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Send Notification")
                        .setOnClickListener(this::onClickSendNotification)
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Go-to-Phone Demo")
                        .setOnClickListener(
                                ParkedOnlyOnClickListener.create(
                                        () ->
                                                getScreenManager()
                                                        .push(
                                                                new GoToPhoneScreen(
                                                                        getCarContext()))))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("PopTo Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new PopToDemoScreen(getCarContext(), 0)))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Loading Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(new LoadingDemoScreen(getCarContext())))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Finish App Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new FinishAppScreen(
                                                                getCarContext(),
                                                                /** willPreseed= */
                                                                false)))
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle("Pre-seed the Screen backstack on next run Demo")
                        .setOnClickListener(
                                () ->
                                        getScreenManager()
                                                .push(
                                                        new FinishAppScreen(
                                                                getCarContext(),
                                                                /** willPreseed= */
                                                                true)))
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Misc Demos")
                .setHeaderAction(BACK)
                .build();
    }

    private void onClickSendNotification() {
        sendNotification("Notification title", "Notification text");
    }

    @SuppressLint("UnsafeNewApiCall")
    private void sendNotification(CharSequence title, CharSequence text) {
        NotificationManagerCompat notificationManagerCompat =
                NotificationManagerCompat.from(getCarContext());
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            NOTIFICATION_CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_DEFAULT);
            notificationManagerCompat.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(getCarContext(), NOTIFICATION_CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(getCarContext());
        }

        Notification notification =
                builder.setSmallIcon(R.drawable.ic_bug_report_24px)
                        .setContentTitle(title + " (phone)")
                        .setContentText(text + " (phone)")
                        .setLargeIcon(
                                BitmapFactory.decodeResource(
                                        getCarContext().getResources(), R.drawable.ic_hi))
                        .setContentIntent(createPendingIntent(INTENT_ACTION_PRIMARY_PHONE))
                        .setDeleteIntent(createPendingIntent(INTENT_ACTION_DELETE_PHONE))
                        .addAction(
                                new NotificationCompat.Action.Builder(
                                        R.drawable.ic_face_24px,
                                        "Action1 (phone)",
                                        createPendingIntent(INTENT_ACTION_SECONDARY_PHONE))
                                        .build())
                        .addAction(
                                R.drawable.ic_commute_24px,
                                "Action2 (phone)",
                                createPendingIntent(INTENT_ACTION_TERTIARY_PHONE))
                        .extend(
                                new CarAppExtender.Builder()
                                        .setContentTitle(title)
                                        .setContentText(text)
                                        .setSmallIcon(R.drawable.ic_bug_report_24px)
                                        .setLargeIcon(
                                                BitmapFactory.decodeResource(
                                                        getCarContext().getResources(),
                                                        R.drawable.ic_hi))
                                        .setContentIntent(
                                                createPendingIntent(INTENT_ACTION_PRIMARY_CAR))
                                        .setDeleteIntent(
                                                createPendingIntent(INTENT_ACTION_DELETE_CAR))
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

        notificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /** Registers a broadcast receiver with an intent filter. */
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTION_PRIMARY_CAR);
        filter.addAction(INTENT_ACTION_DELETE_CAR);
        filter.addAction(INTENT_ACTION_PRIMARY_PHONE);
        filter.addAction(INTENT_ACTION_SECONDARY_PHONE);
        filter.addAction(INTENT_ACTION_TERTIARY_PHONE);
        filter.addAction(INTENT_ACTION_DELETE_PHONE);

        getCarContext().registerReceiver(mBroadcastReceiver, filter);
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
}
