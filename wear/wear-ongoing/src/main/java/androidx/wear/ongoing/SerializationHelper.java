/*
 * Copyright 2021 The Android Open Source Project
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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;
import androidx.versionedparcelable.ParcelUtils;

/**
 * Class used to manage Ongoing Activity information as part of a Bundle or Notification.
 */
public class SerializationHelper {
    // Utility class, avoid instantiation
    private SerializationHelper() {
    }

    /**
     * Checks if the given notification contains information of an ongoing activity.
     */
    public static boolean hasOngoingActivity(@NonNull Notification notification) {
        return notification.extras.getBundle(EXTRA_ONGOING_ACTIVITY) != null;
    }

    /**
     * Deserializes the {@link OngoingActivity} from a notification.
     *
     * @param notification the notification that may contain information about a Ongoing
     *                     Activity.
     * @return the data, or null of the notification doesn't contain Ongoing Activity data.
     */
    @Nullable
    public static OngoingActivity create(@NonNull Notification notification) {
        return create(notification.extras);
    }

    /**
     * Deserializes the {@link OngoingActivity} from a Bundle.
     *
     * @param bundle the bundle that may contain information about a Ongoing Activity.
     * @return the data, or null of the Bundle doesn't contain Ongoing Activity data.
     */
    @Nullable
    public static OngoingActivity create(@NonNull Bundle bundle) {
        OngoingActivityData data = createInternal(bundle);
        return data == null ? null : new OngoingActivity(data);
    }

    /**
     * Copies an Ongoing Activity information from a bundle to another, without deserializing
     * and serializing (this needs to be done before accessing the source Bundle)
     *
     * @param sourceBundle The bundle to get the Ongoing Activity data from
     * @param destinationBundle The bundle to put the Ongoing Activity data into.
     */
    public static void copy(@NonNull Bundle sourceBundle, @NonNull Bundle destinationBundle) {
        destinationBundle.putBundle(EXTRA_ONGOING_ACTIVITY,
                new Bundle(sourceBundle.getBundle(EXTRA_ONGOING_ACTIVITY)));
    }

    /**
     * Add the information from the given OngoingActivityData into the notification builder.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    static NotificationCompat.Builder extend(@NonNull NotificationCompat.Builder builder,
            @NonNull OngoingActivityData data) {
        ParcelUtils.putVersionedParcelable(builder.getExtras(), EXTRA_ONGOING_ACTIVITY,
                data);
        return builder;
    }

    /**
     * Add the information from the given OngoingActivityData into the notification builder
     * and build the notification.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    static Notification extendAndBuild(@NonNull NotificationCompat.Builder builder,
            @NonNull OngoingActivityData data) {
        Notification notification = extend(builder, data).build();
        // TODO(http://b/169394642): Undo this if/when the bug is fixed.
        notification.extras.putBundle(
                EXTRA_ONGOING_ACTIVITY,
                builder.getExtras().getBundle(EXTRA_ONGOING_ACTIVITY)
        );
        return notification;
    }

    /**
     * Deserialize a OngoingActivityData from a notification.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    static OngoingActivityData createInternal(@NonNull Notification notification) {
        return createInternal(notification.extras);
    }

    /**
     * Deserialize a OngoingActivityData from a bundle.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    static OngoingActivityData createInternal(@NonNull Bundle bundle) {
        return ParcelUtils.getVersionedParcelable(bundle, EXTRA_ONGOING_ACTIVITY);
    }

    /** Notification action extra which contains ongoing activity extensions */
    private static final String EXTRA_ONGOING_ACTIVITY =
            "android.wearable.ongoingactivities.EXTENSIONS";
}
