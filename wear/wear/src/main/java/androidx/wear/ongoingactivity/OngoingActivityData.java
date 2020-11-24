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
import android.app.PendingIntent;
import android.graphics.drawable.Icon;

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
    private final Icon mAnimatedIcon;

    @NonNull
    @ParcelField(value = 2)
    private final Icon mStaticIcon;

    @Nullable
    @ParcelField(value = 3, defaultValue = "null")
    private OngoingActivityStatus mStatus;

    @NonNull
    @ParcelField(value = 4)
    private final PendingIntent mTouchIntent;

    @Nullable
    @ParcelField(value = 5, defaultValue = "null")
    private final LocusIdCompat mLocusId;

    @ParcelField(value = 6, defaultValue = "-1")
    private final int mOngoingActivityId;

    OngoingActivityData(
            @Nullable Icon animatedIcon,
            @NonNull Icon staticIcon,
            @Nullable OngoingActivityStatus status,
            @NonNull PendingIntent touchIntent,
            @Nullable LocusIdCompat locusId,
            int ongoingActivityId
    ) {
        mAnimatedIcon = animatedIcon;
        mStaticIcon = staticIcon;
        mStatus = status;
        mTouchIntent = touchIntent;
        mLocusId = locusId;
        mOngoingActivityId = ongoingActivityId;
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
     * Applies defaults from the notification for information not provided as part of the
     * {@link OngoingActivity}.
     *
     * @param notification the notification that may contain information about a Ongoing
     *                     Activity.
     * @return the data, or null of the notification doesn't contain Ongoing Activity data.
     */
    @Nullable
    public static OngoingActivityData create(@NonNull Notification notification) {
        return ParcelUtils.getVersionedParcelable(notification.extras, EXTRA_ONGOING_ACTIVITY);
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
     * {@link OngoingActivity}. For example in the WatchFace in ambient mode.
     */
    @NonNull
    public Icon getStaticIcon() {
        return mStaticIcon;
    }

    /**
     * Get the status of this ongoing activity, the status may be displayed on the UI to
     * show progress of the Ongoing Activity.
     */
    @Nullable
    public OngoingActivityStatus getStatus() {
        return mStatus;
    }

    /**
     * Get the intent to be used to go back to the activity when the user interacts with the
     * Ongoing Activity in other surfaces (for example, taps the Icon on the WatchFace)
     */
    @NonNull
    public PendingIntent getTouchIntent() {
        return mTouchIntent;
    }

    /**
     * Get the LocusId of this {@link OngoingActivity}, this can be used by the launcher to
     * identify the corresponding launcher item and display it accordingly.
     */
    @Nullable
    public LocusIdCompat getLocusId() {
        return mLocusId;
    }

    /**
     * Give the id to this {@link OngoingActivity}, as a way to reference it in
     * [fromExistingOngoingActivity]
     */
    public int getOngoingActivityId() {
        return mOngoingActivityId;
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

