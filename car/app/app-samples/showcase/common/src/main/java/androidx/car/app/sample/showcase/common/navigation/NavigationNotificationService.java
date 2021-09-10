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

package androidx.car.app.sample.showcase.common.navigation;

import static androidx.car.app.sample.showcase.common.ShowcaseService.INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.notification.CarAppExtender;
import androidx.car.app.notification.CarNotificationManager;
import androidx.car.app.notification.CarPendingIntent;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.ShowcaseService;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * A simple foreground service that imitates a client routing service posting navigation
 * notifications.
 */
public final class NavigationNotificationService extends Service {
    private static final int MSG_SEND_NOTIFICATION = 1;
    private static final String NAV_NOTIFICATION_CHANNEL_ID = "nav_channel_00";
    private static final CharSequence NAV_NOTIFICATION_CHANNEL_NAME = "Navigation Channel";
    private static final int NAV_NOTIFICATION_ID = 10101;
    static final long NAV_NOTIFICATION_DELAY_IN_MILLIS = SECONDS.toMillis(1);

    /**
     * The number of notifications fired so far.
     *
     * <p>We use this number to post notifications with a repeating list of directions. See {@link
     * #getDirectionInfo(int)} for details.
     */
    int mNotificationCount = 0;

    /**
     * A handler that posts notifications when given the message request. See {@link
     * HandlerCallback} for details.
     */
    final Handler mHandler = new Handler(Looper.getMainLooper(), new HandlerCallback());

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        initNotifications(this);
        startForeground(NAV_NOTIFICATION_ID,
                getNavigationNotification(this, mNotificationCount).build());

        // Start updating the notification continuously.
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(MSG_SEND_NOTIFICATION), NAV_NOTIFICATION_DELAY_IN_MILLIS);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mHandler.removeMessages(MSG_SEND_NOTIFICATION);
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }

    /**
     * Initializes the notifications, if needed.
     *
     * <p>{@link NotificationManager#IMPORTANCE_HIGH} is needed to show the alerts on top of the car
     * screen. However, the rail widget at the bottom of the screen will show regardless of the
     * importance setting.
     */
    // Suppressing 'ObsoleteSdkInt' as this code is shared between APKs with different min SDK
    // levels
    @SuppressLint({"ObsoleteSdkInt"})
    private static void initNotifications(Context context) {
        NotificationChannelCompat navChannel =
                new NotificationChannelCompat.Builder(
                        NAV_NOTIFICATION_CHANNEL_ID,
                        NotificationManagerCompat.IMPORTANCE_HIGH)
                        .setName(NAV_NOTIFICATION_CHANNEL_NAME).build();
        CarNotificationManager.from(context).createNotificationChannel(navChannel);
    }

    /** Returns the navigation notification that corresponds to the given notification count. */
    static NotificationCompat.Builder getNavigationNotification(
            Context context, int notificationCount) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, NAV_NOTIFICATION_CHANNEL_ID);
        DirectionInfo directionInfo = getDirectionInfo(notificationCount);

        // Set an intent to open the car app. The app receives this intent when the user taps the
        // heads-up notification or the rail widget.
        PendingIntent pendingIntent = CarPendingIntent.getCarApp(
                context,
                INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP.hashCode(),
                new Intent(
                        INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP).setComponent(
                        new ComponentName(context,
                                ShowcaseService.class)).setData(
                        ShowcaseService.createDeepLinkUri(
                                INTENT_ACTION_NAV_NOTIFICATION_OPEN_APP)),
                0);

        return builder
                // This title, text, and icon will be shown in both phone and car screen. These
                // values can
                // be overridden in the extender below, to customize notifications in the car
                // screen.
                .setContentTitle(directionInfo.mTitle)
                .setContentText(directionInfo.mDistance)
                .setSmallIcon(directionInfo.mIcon)

                // The notification must be set to 'ongoing' and its category must be set to
                // CATEGORY_NAVIGATION in order to show it in the rail widget when the app is
                // navigating on
                // the background.
                // These values cannot be overridden in the extender.
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)

                // If set to true, the notification will only show the alert once in both phone and
                // car screen. This value cannot be overridden in the extender.
                .setOnlyAlertOnce(directionInfo.mOnlyAlertOnce)

                // This extender must be set in order to display the notification in the car screen.
                // The extender also allows various customizations, such as showing different title
                // or icon on the car screen.
                .extend(new CarAppExtender.Builder()
                        .setContentIntent(pendingIntent)
                        .build());
    }

    /**
     * A {@link Handler.Callback} used to process the message queue for the notification service.
     */
    final class HandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_SEND_NOTIFICATION) {
                Context context = NavigationNotificationService.this;
                CarNotificationManager.from(context).notify(NAV_NOTIFICATION_ID,
                        getNavigationNotification(context, mNotificationCount));
                mNotificationCount++;
                mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(MSG_SEND_NOTIFICATION),
                        NAV_NOTIFICATION_DELAY_IN_MILLIS);
                return true;
            }
            return false;
        }
    }

    /**
     * Returns a {@link DirectionInfo} that corresponds to the given notification count.
     *
     * <p>There are 5 directions, repeating in order. For each direction, the alert will only show
     * once, but the distance will update on every count on the rail widget.
     */
    private static DirectionInfo getDirectionInfo(int notificationCount) {
        DecimalFormat formatter = new DecimalFormat("#.##");
        formatter.setRoundingMode(RoundingMode.DOWN);
        int repeatingCount = notificationCount % 35;
        if (0 <= repeatingCount && repeatingCount < 10) {
            // Distance decreases from 1km to 0.1km
            String distance = formatter.format((10 - repeatingCount) * 0.1) + "km";
            return new DirectionInfo(
                    "Go Straight",
                    distance,
                    R.drawable.arrow_straight,
                    /* onlyAlertOnce= */ repeatingCount > 0);
        } else if (10 <= repeatingCount && repeatingCount < 20) {
            // Distance decreases from 5km to 0.5km
            String distance = formatter.format((20 - repeatingCount) * 0.5) + "km";
            return new DirectionInfo(
                    "Turn Right",
                    distance,
                    R.drawable.arrow_right_turn,
                    /* onlyAlertOnce= */ repeatingCount > 10);
        } else if (20 <= repeatingCount && repeatingCount < 25) {
            // Distance decreases from 200m to 40m
            String distance = formatter.format((25 - repeatingCount) * 40) + "m";
            return new DirectionInfo(
                    "Take 520",
                    distance,
                    R.drawable.ic_520,
                    /* onlyAlertOnce= */ repeatingCount > 20);
        } else {
            // Distance decreases from 1km to 0.1km
            String distance = formatter.format((35 - repeatingCount) * 0.1) + "km";
            return new DirectionInfo(
                    "Gas Station",
                    distance,
                    R.drawable.ic_local_gas_station_white_48dp,
                    repeatingCount > 25);
        }
    }

    /**
     * A container class that encapsulates the direction information to use in the notifications.
     */
    static final class DirectionInfo {
        @NonNull
        final String mTitle;
        @NonNull
        final String mDistance;
        final int mIcon;
        final boolean mOnlyAlertOnce;

        DirectionInfo(@NonNull String title, @NonNull String distance, int icon,
                boolean onlyAlertOnce) {
            this.mTitle = title;
            this.mDistance = distance;
            this.mIcon = icon;
            this.mOnlyAlertOnce = onlyAlertOnce;
        }
    }
}
