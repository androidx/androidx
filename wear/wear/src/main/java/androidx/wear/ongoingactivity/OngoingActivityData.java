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
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.LocusIdCompat;

/**
 * This class is used internally by the library to represent the data of an OngoingActivity.
 */
public class OngoingActivityData {
    @Nullable
    private Icon mAnimatedIcon = null;
    @Nullable
    private Icon mStaticIcon = null;
    @Nullable
    private OngoingActivityStatus mStatus = null;
    @Nullable
    private PendingIntent mTouchIntent = null;
    @Nullable
    private LocusIdCompat mLocusId = null;
    private int mOngoingActivityId = DEFAULT_ID;

    OngoingActivityData() {
    }

    @NonNull
    NotificationCompat.Builder extend(@NonNull NotificationCompat.Builder builder) {
        Bundle bundle = new Bundle();
        if (mAnimatedIcon != null) {
            bundle.putParcelable(KEY_ANIMATED_ICON, mAnimatedIcon);
        }
        if (mStaticIcon != null) {
            bundle.putParcelable(KEY_STATIC_ICON, mStaticIcon);
        }
        if (mStatus != null) {
            mStatus.extend(bundle);
        }
        if (mTouchIntent != null) {
            bundle.putParcelable(KEY_TOUCH_INTENT, mTouchIntent);
        }
        if (mLocusId != null) {
            builder.setLocusId(mLocusId);
        }
        if (mOngoingActivityId != DEFAULT_ID) {
            bundle.putInt(KEY_ID, mOngoingActivityId);
        }
        builder.getExtras().putBundle(EXTRA_ONGOING_ACTIVITY, bundle);
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

    @Nullable private static <T> T safeGetParcelableOrNull(@NonNull Bundle bundle,
            @NonNull String key, @NonNull Class<T> targetClass) {
        Parcelable obj = bundle.getParcelable(key);
        return targetClass.isInstance(obj) ? targetClass.cast(obj) : null;
    }

    /**
     * Deserializes the [OngoingActivityData] from a notification.
     */
    @Nullable
    @SuppressWarnings("SyntheticAccessor")
    static OngoingActivityData createInternal(@NonNull Notification notification) {
        Bundle bundle = notification.extras.getBundle(EXTRA_ONGOING_ACTIVITY);
        if (bundle != null) {
            OngoingActivityData data = new OngoingActivityData();
            data.mAnimatedIcon = safeGetParcelableOrNull(bundle, KEY_ANIMATED_ICON, Icon.class);
            data.mStaticIcon = safeGetParcelableOrNull(bundle, KEY_STATIC_ICON, Icon.class);
            data.mStatus = OngoingActivityStatus.create(bundle);
            data.mTouchIntent = safeGetParcelableOrNull(bundle, KEY_TOUCH_INTENT,
                    PendingIntent.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                data.mLocusId = Api29Impl.getLocusId(notification);
            }
            data.mOngoingActivityId = bundle.getInt(KEY_ID, DEFAULT_ID);
            return data;
        }
        return null;
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
        OngoingActivityData data = createInternal(notification);
        if (data != null) {
            if (data.getAnimatedIcon() == null) {
                data.setAnimatedIcon(notification.getSmallIcon());
            }
            if (data.getStaticIcon() == null) {
                data.setStaticIcon(notification.getSmallIcon());
            }
            if (data.getStatus() == null) {
                String text = notification.extras.getString(Notification.EXTRA_TEXT);
                if (text != null) {
                    data.setStatus(new TextOngoingActivityStatus(text));
                }
            }
            if (data.getTouchIntent() == null) {
                data.setTouchIntent(notification.contentIntent);
            }
        }
        return data;
    }

    // Inner class required to avoid VFY errors during class init.
    @RequiresApi(29)
    private static class Api29Impl {
        // Avoid instantiation.
        private Api29Impl() {
        }

        @Nullable
        private static LocusIdCompat getLocusId(@NonNull Notification notification) {
            return notification.getLocusId() != null
                    ? LocusIdCompat.toLocusIdCompat(notification.getLocusId()) : null;
        }
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
    @Nullable
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
    @Nullable
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

    /* Package private setters */
    void setAnimatedIcon(@Nullable Icon animatedIcon) {
        this.mAnimatedIcon = animatedIcon;
    }

    void setStaticIcon(@Nullable Icon staticIcon) {
        this.mStaticIcon = staticIcon;
    }

    void setStatus(@Nullable OngoingActivityStatus status) {
        this.mStatus = status;
    }

    void setTouchIntent(@Nullable PendingIntent touchIntent) {
        this.mTouchIntent = touchIntent;
    }

    void setLocusId(@Nullable LocusIdCompat locusId) {
        this.mLocusId = locusId;
    }

    void setOngoingActivityId(int ongoingActivityId) {
        this.mOngoingActivityId = ongoingActivityId;
    }

    /** Notification action extra which contains ongoing activity extensions */
    private static final String EXTRA_ONGOING_ACTIVITY =
            "android.wearable.ongoingactivities.EXTENSIONS";

    // Keys within EXTRA_ONGOING_ACTIVITY_EXTENDER for ongoing activities options.
    private static final String KEY_ANIMATED_ICON = "animatedIcon";
    private static final String KEY_STATIC_ICON = "staticIcon";
    private static final String KEY_TOUCH_INTENT = "touchIntent";
    private static final String KEY_ID = "id";

    private static final int DEFAULT_ID = -1;
}

