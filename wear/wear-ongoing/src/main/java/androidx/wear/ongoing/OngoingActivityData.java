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
import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.LocusIdCompat;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * This class is used internally by the library to represent the data of an OngoingActivity.
 */
@VersionedParcelize
public class OngoingActivityData implements VersionedParcelable {
    @Nullable
    @ParcelField(value = 1, defaultValue = "null")
    Icon mAnimatedIcon;

    @NonNull
    @ParcelField(value = 2)
    Icon mStaticIcon;

    @Nullable
    @ParcelField(value = 3, defaultValue = "null")
    OngoingActivityStatus mStatus;

    @NonNull
    @ParcelField(value = 4)
    PendingIntent mTouchIntent;

    @Nullable
    @ParcelField(value = 5, defaultValue = "null")
    String mLocusId;

    @ParcelField(value = 6, defaultValue = "-1")
    int mOngoingActivityId;

    @Nullable
    @ParcelField(value = 7, defaultValue = "null")
    String mCategory;

    @ParcelField(value = 8)
    long mTimestamp;

    // Required by VersionedParcelable
    OngoingActivityData() {
    }

    OngoingActivityData(
            @Nullable Icon animatedIcon,
            @NonNull Icon staticIcon,
            @Nullable OngoingActivityStatus status,
            @NonNull PendingIntent touchIntent,
            @Nullable String locusId,
            int ongoingActivityId,
            @Nullable String category,
            long timestamp
    ) {
        mAnimatedIcon = animatedIcon;
        mStaticIcon = staticIcon;
        mStatus = status;
        mTouchIntent = touchIntent;
        mLocusId = locusId;
        mOngoingActivityId = ongoingActivityId;
        mCategory = category;
        mTimestamp = timestamp;
    }

    @NonNull
    NotificationCompat.Builder extend(@NonNull NotificationCompat.Builder builder) {
        ParcelUtils.putVersionedParcelable(builder.getExtras(), EXTRA_ONGOING_ACTIVITY,
                this);
        return builder;
    }

    @NonNull Notification extendAndBuild(@NonNull NotificationCompat.Builder builder) {
        Notification notification = extend(builder).build();
        // TODO(http://b/169394642): Undo this if/when the bug is fixed.
        notification.extras.putBundle(
                EXTRA_ONGOING_ACTIVITY,
                builder.getExtras().getBundle(EXTRA_ONGOING_ACTIVITY)
        );
        return notification;
    }

    /**
     * Checks if the given notification represents an ongoing activity.
     */
    public static boolean hasOngoingActivity(@NonNull Notification notification) {
        return notification.extras.getBundle(EXTRA_ONGOING_ACTIVITY) != null;
    }

    /**
     * Deserializes the {@link OngoingActivityData} from a notification.
     *
     * @param notification the notification that may contain information about a Ongoing
     *                     Activity.
     * @return the data, or null of the notification doesn't contain Ongoing Activity data.
     */
    @Nullable
    public static OngoingActivityData create(@NonNull Notification notification) {
        return create(notification.extras);
    }

    /**
     * Deserializes the {@link OngoingActivityData} from a Bundle.
     *
     * @param bundle the bundle that may contain information about a Ongoing Activity.
     * @return the data, or null of the Bundle doesn't contain Ongoing Activity data.
     */
    @Nullable
    public static OngoingActivityData create(@NonNull Bundle bundle) {
        return ParcelUtils.getVersionedParcelable(bundle, EXTRA_ONGOING_ACTIVITY);
    }


    /**
     * Copies an Ongoing Activity information from a bundle to another, without deserializing
     * and serializing (Note that Bundle instance is shared, not copied and deserializing the
     * Ongoing activity information somewhere else negates the advantages of using this)
     *
     * @param sourceBundle The bundle to get the Ongoing Activity data from
     * @param destinationBundle The bundle to put the Ongoing Activity data into.
     */
    public static void copy(@NonNull Bundle sourceBundle, @NonNull Bundle destinationBundle) {
        destinationBundle.putBundle(EXTRA_ONGOING_ACTIVITY,
                sourceBundle.getBundle(EXTRA_ONGOING_ACTIVITY));
    }

    /**
     * Get the animated icon that can be used on some surfaces to represent this
     * {@link OngoingActivity}. For example, in the WatchFace.
     */
    @Nullable
    public Icon getAnimatedIcon() {
        return mAnimatedIcon;
    }

    /**
     * Get the static icon that can be used on some surfaces to represent this
     * {@link OngoingActivity}. For example in the WatchFace in ambient mode. If not set, returns
     *  the small icon of the corresponding Notification.
     */
    @NonNull
    public Icon getStaticIcon() {
        return mStaticIcon;
    }

    /**
     * Get the status of this ongoing activity, the status may be displayed on the UI to
     * show progress of the Ongoing Activity. If not set, returns the content text of the
     * corresponding Notification.
     */
    @Nullable
    public OngoingActivityStatus getStatus() {
        return mStatus;
    }

    /**
     * Get the intent to be used to go back to the activity when the user interacts with the
     * Ongoing Activity in other surfaces (for example, taps the Icon on the WatchFace). If not
     * set, returns the touch intent of the corresponding Notification.
     */
    @NonNull
    public PendingIntent getTouchIntent() {
        return mTouchIntent;
    }

    /**
     * Get the LocusId of this {@link OngoingActivity}, this can be used by the launcher to
     * identify the corresponding launcher item and display it accordingly. If not set, returns
     * the one in the corresponding Notification.
     */
    @Nullable
    public LocusIdCompat getLocusId() {
        return mLocusId == null ? null : new LocusIdCompat(mLocusId);
    }

    /**
     * Give the id to this {@link OngoingActivity}, as a way to reference it in
     * [fromExistingOngoingActivity]
     */
    public int getOngoingActivityId() {
        return mOngoingActivityId;
    }

    /**
     * Get the Category of this {@link OngoingActivity} if set, otherwise the category of the
     * corresponding notification.
     */
    @Nullable
    public String getCategory() {
        return mCategory;
    }

    /**
     * Get the time (in {@link SystemClock#elapsedRealtime()} time) the OngoingActivity was built.
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    // Status is mutable, by the library.
    void setStatus(@NonNull OngoingActivityStatus status) {
        mStatus = status;
    }

    /** Notification action extra which contains ongoing activity extensions */
    private static final String EXTRA_ONGOING_ACTIVITY =
            "android.wearable.ongoingactivities.EXTENSIONS";

    static final int DEFAULT_ID = -1;
}

