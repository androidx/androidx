/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.wear.ongoingactivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.LocusIdCompat;

import java.util.function.Predicate;

/**
 * Main class to access the Ongoing Activities API.
 *
 * It's created with the {@link Builder}. After it's created (and before building and
 * posting the {@link Notification}) {@link OngoingActivity#apply(Context)} apply} needs to be
 * called:
 * {@code
 * NotificationCompat.Builder builder = new NotificationCompat.Builder(context)....
 *
 * OngoingActivity ongoingActivity = new OngoingActivity.Builder(context, notificationId, builder);
 * ....
 * ongoingActivity.apply(context);
 *
 * notificationManager.notify(notificationId, builder.build());
 * }
 *
 * Afterward, {@link OngoingActivity#update(Context, OngoingActivityStatus) update} can be used to
 * update the status.
 *
 * If saving the {@link OngoingActivity} instance is not convenient, it can be recovered (after the
 * notification is posted) with {@link OngoingActivity#fromExistingOngoingActivity(Context)}
 */
@RequiresApi(24)
public final class OngoingActivity {
    private final int mNotificationId;
    private final NotificationCompat.Builder mNotificationBuilder;
    private final OngoingActivityData mData;

    private OngoingActivity(int notificationId,
            @NonNull NotificationCompat.Builder notificationBuilder,
            @NonNull OngoingActivityData data) {
        this.mNotificationId = notificationId;
        this.mNotificationBuilder = notificationBuilder;
        this.mData = data;
    }

    /**
     * Builder used to build an {@link OngoingActivity}
     */
    public static final class Builder {
        private final Context mContext;
        private final int mNotificationId;
        private final NotificationCompat.Builder mNotificationBuilder;
        private final OngoingActivityData mData = new OngoingActivityData();

        /**
         * Construct a new empty {@link Builder}, associated with the given notification.
         *
         * @param context             to be used during the life of this {@link Builder}, will
         *                            NOT pass a reference into the built {@link OngoingActivity}
         * @param notificationId      id that will be used to post the notification associated
         *                            with this Ongoing Activity
         * @param notificationBuilder builder for the notification associated with this Ongoing
         *                            Activity
         */
        public Builder(@NonNull Context context, int notificationId,
                @NonNull NotificationCompat.Builder notificationBuilder) {
            this.mContext = context;
            this.mNotificationId = notificationId;
            this.mNotificationBuilder = notificationBuilder;
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * {@link OngoingActivity}. For example, in the WatchFace.
         * Should be white with a transparent background, preferably an AnimatedVectorDrawable.
         */
        @NonNull
        public Builder setAnimatedIcon(@NonNull Icon animatedIcon) {
            mData.setAnimatedIcon(animatedIcon);
            return this;
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * {@link OngoingActivity}. For example, in the WatchFace.
         * Should be white with a transparent background, preferably an AnimatedVectorDrawable.
         */
        @NonNull
        public Builder setAnimatedIcon(@DrawableRes int animatedIcon) {
            mData.setAnimatedIcon(Icon.createWithResource(mContext, animatedIcon));
            return this;
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * {@link OngoingActivity}, for example in the WatchFace in ambient mode.
         * Should be white with a transparent background, preferably an VectorDrawable.
         */
        @NonNull
        public Builder setStaticIcon(@NonNull Icon staticIcon) {
            mData.setStaticIcon(staticIcon);
            return this;
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * {@link OngoingActivity}, for example in the WatchFace in ambient mode.
         * Should be white with a transparent background, preferably an VectorDrawable.
         */
        @NonNull
        public Builder setStaticIcon(@DrawableRes int staticIcon) {
            mData.setStaticIcon(Icon.createWithResource(mContext, staticIcon));
            return this;
        }

        /**
         * Set the initial status of this ongoing activity, the status may be displayed on the UI to
         * show progress of the Ongoing Activity.
         */
        @NonNull
        public Builder setStatus(@NonNull OngoingActivityStatus status) {
            mData.setStatus(status);
            return this;
        }

        /**
         * Set the intent to be used to go back to the activity when the user interacts with the
         * Ongoing Activity in other surfaces (for example, taps the Icon on the WatchFace)
         */
        @NonNull
        public Builder setTouchIntent(@NonNull PendingIntent touchIntent) {
            mData.setTouchIntent(touchIntent);
            return this;
        }

        /**
         * Set the corresponding LocusId of this {@link OngoingActivity}, this will be used by the
         * launcher to identify the corresponding launcher item and display it accordingly.
         */
        @NonNull
        public Builder setLocusId(@NonNull LocusIdCompat locusId) {
            mData.setLocusId(locusId);
            return this;
        }

        /**
         * Give an id to this {@link OngoingActivity}, as a way to reference it in
         * {@link OngoingActivity#fromExistingOngoingActivity(Context, int)}
         */
        @NonNull
        public Builder setOngoingActivityId(int ongoingActivityId) {
            mData.setOngoingActivityId(ongoingActivityId);
            return this;
        }

        /**
         * Combine all options provided and return a new {@link OngoingActivity} object.
         */
        @SuppressWarnings("SyntheticAccessor")
        @NonNull
        public OngoingActivity build() {
            return new OngoingActivity(mNotificationId, mNotificationBuilder, mData);
        }
    }

    /**
     * Notify the system that this activity should be shown as an Ongoing Activity.
     *
     * This will modify the notification builder associated with this Ongoing Activity, so needs
     * to be called before building and posting that notification.
     *
     * @param context May be used to access system services. A reference will not be kept after
     *                this call returns.
     */
    public void apply(@NonNull @SuppressWarnings("unused") Context context) {
        mData.extend(mNotificationBuilder);
    }

    /**
     * Update the status of this Ongoing Activity.
     *
     * Note that this may post the notification updated with the new information.
     *
     * @param context May be used to access system services. A reference will not be kept after
     *                this call returns.
     * @param status  The new status of this Ongoing Activity.
     */
    public void update(@NonNull Context context, @NonNull OngoingActivityStatus status) {
        mData.setStatus(status);
        Notification notification = mData.extendAndBuild(mNotificationBuilder);

        context.getSystemService(NotificationManager.class).notify(mNotificationId, notification);
    }

    /**
     * Convenience method for clients that don’t want to / can’t store the OngoingActivity
     * instance.
     *
     * @param context May be used to access system services. A reference will not be kept after
     *                this call returns.
     * @param filter  used to find the required {@link OngoingActivity}.
     * @return the Ongoing Activity or null if not found
     */
    @Nullable
    public static OngoingActivity fromExistingOngoingActivity(
            @NonNull Context context,
            @NonNull Predicate<OngoingActivityData> filter
    ) {
        StatusBarNotification[] notifications =
                context.getSystemService(NotificationManager.class).getActiveNotifications();
        for (StatusBarNotification statusBarNotification : notifications) {
            OngoingActivityData data = OngoingActivityData.create(
                    statusBarNotification.getNotification());
            if (data != null && filter.test(data)) {
                return new OngoingActivity(statusBarNotification.getId(),
                        new NotificationCompat.Builder(context,
                                statusBarNotification.getNotification()),
                        data);
            }
        }
        return null;
    }

    /**
     * Convenience method for clients that don’t want to / can’t store the OngoingActivity
     * instance.
     *
     * Note that if there is more than one Ongoing Activity active you have not guarantee
     * over which one you get, you need to use one of the other variations of this method.
     *
     * @param context May be used to access system services. A reference will not be kept after
     *                this call returns.
     * @return the Ongoing Activity or null if not found
     */
    @Nullable
    public static OngoingActivity fromExistingOngoingActivity(@NonNull Context context) {
        return fromExistingOngoingActivity(context, (data) -> true);
    }

    /**
     * Convenience method for clients that don’t want to / can’t store the OngoingActivity
     * instance.
     *
     * @param context           May be used to access system services. A reference will not be kept
     *                          after this call returns.
     * @param ongoingActivityId the id of the Ongoing Activity to retrieve, set in
     *                          {@link OngoingActivity.Builder#setOngoingActivityId(int)}
     * @return the Ongoing Activity or null if not found
     */
    @Nullable
    public static OngoingActivity fromExistingOngoingActivity(@NonNull Context context,
            int ongoingActivityId) {
        return fromExistingOngoingActivity(context,
                (data) -> data.getOngoingActivityId() == ongoingActivityId);
    }
}
