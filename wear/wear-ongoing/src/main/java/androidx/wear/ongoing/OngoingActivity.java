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
package androidx.wear.ongoing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.LocusIdCompat;
import androidx.core.util.Preconditions;

import java.util.function.Predicate;

/**
 * Main class to access the Ongoing Activities API.
 *
 * It's created with the {@link Builder}. After it's created (and before building and
 * posting the {@link Notification}) {@link OngoingActivity#apply(Context)} apply needs to be
 * called:
 *
 * <pre>{@code
 * NotificationCompat.Builder builder = new NotificationCompat.Builder(context)....
 *
 * OngoingActivity ongoingActivity = new OngoingActivity.Builder(context, notificationId, builder);
 * ....
 * ongoingActivity.apply(context);
 *
 * notificationManager.notify(notificationId, builder.build());
 * }</pre>
 *
 * Note that if a Notification with that id was previously posted it will be replaced. If you
 * need more than one Notification with the same ID you can use a String tag to differentiate
 * them in both the {@link Builder#Builder(Context, String, int, NotificationCompat.Builder)} and
 * {@link NotificationManager#notify(String, int, Notification)}
 * <p>
 * Afterward, {@link OngoingActivity#update(Context, Status) update} can be used to
 * update the status.
 * <p>
 * If saving the {@link OngoingActivity} instance is not convenient, it can be recovered (after the
 * notification is posted) with {@link OngoingActivity#recoverOngoingActivity(Context)}
 */
@RequiresApi(24)
public final class OngoingActivity {
    @Nullable
    private final String mTag;
    private final int mNotificationId;
    @Nullable
    private final NotificationCompat.Builder mNotificationBuilder;
    private final OngoingActivityData mData;

    private OngoingActivity(@Nullable String tag,
            int notificationId,
            @NonNull NotificationCompat.Builder notificationBuilder,
            @NonNull OngoingActivityData data) {
        this.mTag = tag;
        this.mNotificationId = notificationId;
        this.mNotificationBuilder = notificationBuilder;
        this.mData = data;
    }

    // Used when reconstructing an OngoingActivity form a bundle.
    OngoingActivity(@NonNull OngoingActivityData data) {
        this.mTag = null;
        this.mNotificationId = 0;
        this.mNotificationBuilder = null;
        this.mData = data;
    }

    /**
     * Builder used to build an {@link OngoingActivity}
     */
    public static final class Builder {
        private final Context mContext;
        private final int mNotificationId;
        private final String mTag;
        private final NotificationCompat.Builder mNotificationBuilder;

        // Ongoing Activity Data
        private Icon mAnimatedIcon;
        private Icon mStaticIcon;
        private Status mStatus;
        private PendingIntent mTouchIntent;
        private LocusIdCompat mLocusId;
        private int mOngoingActivityId = DEFAULT_ID;
        private String mCategory;
        private String mTitle;

        static final int DEFAULT_ID = -1;

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
            this(context, null, notificationId, notificationBuilder);
        }

        /**
         * Construct a new empty {@link Builder}, associated with the given notification.
         *
         * @param context             to be used during the life of this {@link Builder}, will
         *                            NOT pass a reference into the built {@link OngoingActivity}
         * @param tag                 tag that will be used to post the notification associated
         *                            with this Ongoing Activity
         * @param notificationId      id that will be used to post the notification associated
         *                            with this Ongoing Activity
         * @param notificationBuilder builder for the notification associated with this Ongoing
         *                            Activity
         */
        public Builder(@NonNull Context context, @NonNull String tag, int notificationId,
                @NonNull NotificationCompat.Builder notificationBuilder) {
            this.mContext = context;
            this.mTag = tag;
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
            mAnimatedIcon = animatedIcon;
            return this;
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * {@link OngoingActivity}. For example, in the WatchFace.
         * Should be white with a transparent background, preferably an AnimatedVectorDrawable.
         */
        @NonNull
        public Builder setAnimatedIcon(@DrawableRes int animatedIcon) {
            mAnimatedIcon = Icon.createWithResource(mContext, animatedIcon);
            return this;
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * {@link OngoingActivity}, for example in the WatchFace in ambient mode.
         * Should be white with a transparent background, preferably an VectorDrawable.
         */
        @NonNull
        public Builder setStaticIcon(@NonNull Icon staticIcon) {
            mStaticIcon = staticIcon;
            return this;
        }

        /**
         * Set the animated icon that can be used on some surfaces to represent this
         * {@link OngoingActivity}, for example in the WatchFace in ambient mode.
         * Should be white with a transparent background, preferably an VectorDrawable.
         */
        @NonNull
        public Builder setStaticIcon(@DrawableRes int staticIcon) {
            mStaticIcon = Icon.createWithResource(mContext, staticIcon);
            return this;
        }

        /**
         * Set the initial status of this ongoing activity, the status may be displayed on the UI to
         * show progress of the Ongoing Activity.
         */
        @NonNull
        public Builder setStatus(@NonNull Status status) {
            mStatus = status;
            return this;
        }

        /**
         * Set the intent to be used to go back to the activity when the user interacts with the
         * Ongoing Activity in other surfaces (for example, taps the Icon on the WatchFace)
         */
        @NonNull
        public Builder setTouchIntent(@NonNull PendingIntent touchIntent) {
            mTouchIntent = touchIntent;
            return this;
        }

        /**
         * Set the corresponding LocusId of this {@link OngoingActivity}, this will be used by the
         * launcher to identify the corresponding launcher item and display it accordingly.
         */
        @NonNull
        public Builder setLocusId(@NonNull LocusIdCompat locusId) {
            mLocusId = locusId;
            return this;
        }

        /**
         * Give an id to this {@link OngoingActivity}, as a way to reference it in
         * {@link OngoingActivity#recoverOngoingActivity(Context, int)}
         */
        @NonNull
        public Builder setOngoingActivityId(int ongoingActivityId) {
            mOngoingActivityId = ongoingActivityId;
            return this;
        }

        /**
         * Set the category of this {@link OngoingActivity}.
         * <p>
         * Must be one of the predefined notification categories (see the {@code CATEGORY_*}
         * constants in {@link NotificationCompat}) that best describes this
         * {@link OngoingActivity}. This may be used by the system to prioritize it.
         */
        @NonNull
        public Builder setCategory(@NonNull String category) {
            mCategory = category;
            return this;
        }

        /**
         * Sets the Title of this {@link OngoingActivity}, this could be used by the launcher to
         * override the app's title.
         */
        @NonNull
        public Builder setTitle(@NonNull String title) {
            mTitle = title;
            return this;
        }

        /**
         * Combine all options provided and the information in the notification if needed,
         * return a new {@link OngoingActivity} object.
         *
         * @throws IllegalArgumentException if the static icon or the touch intent are not provided.
         */
        @SuppressWarnings("SyntheticAccessor")
        @NonNull
        public OngoingActivity build() {
            Notification notification = mNotificationBuilder.build();
            Icon staticIcon = mStaticIcon == null ? notification.getSmallIcon() : mStaticIcon;
            if (staticIcon == null) {
                throw new IllegalArgumentException("Static icon should be specified.");
            }

            PendingIntent touchIntent = mTouchIntent == null
                    ? notification.contentIntent
                    : mTouchIntent;
            if (touchIntent == null) {
                throw new IllegalArgumentException("Touch intent should be specified.");
            }

            OngoingActivityStatus status = mStatus == null ? null : mStatus.toVersionedParcelable();
            if (status == null) {
                String text = notification.extras.getString(Notification.EXTRA_TEXT);
                if (text != null) {
                    status = Status.forPart(new Status.TextPart(text))
                        .toVersionedParcelable();
                }
            }

            LocusIdCompat locusId = mLocusId;
            if (locusId == null &&  Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                locusId = Api29Impl.getLocusId(notification);
            }

            String category = mCategory == null ? notification.category : mCategory;

            return new OngoingActivity(mTag, mNotificationId, mNotificationBuilder,
                    new OngoingActivityData(
                        mAnimatedIcon,
                        staticIcon,
                        status,
                        touchIntent,
                        locusId == null ? null : locusId.getId(),
                        mOngoingActivityId,
                        category,
                        SystemClock.elapsedRealtime(),
                        mTitle
                    ));
        }
    }

    /**
     * Get the notificationId of the notification associated with this {@link OngoingActivity}.
     */
    public int getNotificationId() {
        return mNotificationId;
    }

    /**
     * Get the tag of the notification associated with this {@link OngoingActivity}, or null if
     * there is none.
     */
    @Nullable
    public String getTag() {
        return mTag;
    }

    /**
     * Get the animated icon that can be used on some surfaces to represent this
     * {@link OngoingActivity}. For example, in the WatchFace.
     */
    @Nullable
    public Icon getAnimatedIcon() {
        return mData.getAnimatedIcon();
    }

    /**
     * Get the static icon that can be used on some surfaces to represent this
     * {@link OngoingActivity}. For example in the WatchFace in ambient mode. If not set, returns
     *  the small icon of the corresponding Notification.
     */
    @NonNull
    public Icon getStaticIcon() {
        return mData.getStaticIcon();
    }

    /**
     * Get the status of this ongoing activity, the status may be displayed on the UI to
     * show progress of the Ongoing Activity. If not set, returns the content text of the
     * corresponding Notification.
     */
    @Nullable
    public Status getStatus() {
        return mData.getStatus() == null ? null :
                Status.fromVersionedParcelable(mData.getStatus());
    }

    /**
     * Get the intent to be used to go back to the activity when the user interacts with the
     * Ongoing Activity in other surfaces (for example, taps the Icon on the WatchFace). If not
     * set, returns the touch intent of the corresponding Notification.
     */
    @NonNull
    public PendingIntent getTouchIntent() {
        return mData.getTouchIntent();
    }

    /**
     * Get the LocusId of this {@link OngoingActivity}, this can be used by the launcher to
     * identify the corresponding launcher item and display it accordingly. If not set, returns
     * the one in the corresponding Notification.
     */
    @Nullable
    public LocusIdCompat getLocusId() {
        return mData.getLocusId();
    }

    /**
     * Get the id to this {@link OngoingActivity}. This id is used to reference it in
     * {@link #recoverOngoingActivity(Context, int)}
     */
    public int getOngoingActivityId() {
        return mData.getOngoingActivityId();
    }

    /**
     * Get the Category of this {@link OngoingActivity} if set, otherwise the category of the
     * corresponding notification.
     */
    @Nullable
    public String getCategory() {
        return mData.getCategory();
    }

    /**
     * Get the time (in {@link SystemClock#elapsedRealtime()} time) the OngoingActivity was built.
     */
    public long getTimestamp() {
        return mData.getTimestamp();
    }

    /**
     * Get the title of this {@link OngoingActivity} if set.
     */
    @Nullable
    public String getTitle() {
        return mData.getTitle();
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
        Preconditions.checkNotNull(mNotificationBuilder);
        SerializationHelper.extend(mNotificationBuilder, mData);
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
    public void update(@NonNull Context context, @NonNull Status status) {
        Preconditions.checkNotNull(mNotificationBuilder);
        mData.setStatus(status.toVersionedParcelable());
        Notification notification = SerializationHelper.extendAndBuild(mNotificationBuilder, mData);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (mTag == null) {
            manager.notify(mNotificationId, notification);
        } else {
            manager.notify(mTag, mNotificationId, notification);
        }
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
    public static OngoingActivity recoverOngoingActivity(
            @NonNull Context context,
            @NonNull Predicate<OngoingActivity> filter
    ) {
        StatusBarNotification[] notifications =
                context.getSystemService(NotificationManager.class).getActiveNotifications();
        for (StatusBarNotification statusBarNotification : notifications) {
            OngoingActivityData data =
                    SerializationHelper.createInternal(statusBarNotification.getNotification());
            if (data != null) {
                OngoingActivity oa = new OngoingActivity(
                        statusBarNotification.getTag(),
                        statusBarNotification.getId(),
                        new NotificationCompat.Builder(context,
                                statusBarNotification.getNotification()),
                        data);
                if (filter.test(oa)) {
                    return oa;
                }
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
    public static OngoingActivity recoverOngoingActivity(@NonNull Context context) {
        return recoverOngoingActivity(context, (data) -> true);
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
    public static OngoingActivity recoverOngoingActivity(@NonNull Context context,
            int ongoingActivityId) {
        return recoverOngoingActivity(context,
                (data) -> data.getOngoingActivityId() == ongoingActivityId);
    }


    // Inner class required to avoid VFY errors during class init.
    @RequiresApi(29)
    static class Api29Impl {
        // Avoid instantiation.
        private Api29Impl() {
        }

        @Nullable
        private static LocusIdCompat getLocusId(@NonNull Notification notification) {
            return notification.getLocusId() != null
                    ? LocusIdCompat.toLocusIdCompat(notification.getLocusId()) : null;
        }
    }
}
