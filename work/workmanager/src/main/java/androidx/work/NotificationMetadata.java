/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work;

import android.annotation.TargetApi;
import android.app.Notification;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Metadata used for surfacing a {@link android.app.Notification}.
 */
public final class NotificationMetadata {

    private final String mNotificationTag;
    private final int mNotificationId;
    private final int mNotificationType;
    private final Notification mNotification;

    // Synthetic access
    NotificationMetadata(@NonNull Builder builder) {
        mNotificationTag = builder.getNotificationTag();
        mNotificationId = builder.getNotificationId();
        mNotificationType = builder.getNotificationType();
        mNotification = builder.getNotification();
    }

    /**
     * @return The tag used to identify a {@link Notification}.
     */
    @Nullable
    public String getNotificationTag() {
        return mNotificationTag;
    }

    /**
     * @return The {@link Notification} id.
     */
    public int getNotificationId() {
        return mNotificationId;
    }

    /**
     * @return The Foreground service notification type
     */
    public int getNotificationType() {
        return mNotificationType;
    }

    /**
     * @return The user visible {@link Notification}
     */
    @NonNull
    public Notification getNotification() {
        return mNotification;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationMetadata that = (NotificationMetadata) o;

        if (mNotificationId != that.mNotificationId) return false;
        if (mNotificationType != that.mNotificationType) return false;
        if (mNotificationTag
                != null ? !mNotificationTag.equals(that.mNotificationTag)
                : that.mNotificationTag != null) {
            return false;
        }
        return mNotification != null ? mNotification.equals(that.mNotification)
                : that.mNotification == null;
    }

    @Override
    public int hashCode() {
        int result = mNotificationTag != null ? mNotificationTag.hashCode() : 0;
        result = 31 * result + mNotificationId;
        result = 31 * result + mNotificationType;
        result = 31 * result + (mNotification != null ? mNotification.hashCode() : 0);
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return "NotificationMetadata {"
                + "mNotificationId='" + getNotificationId() + '\''
                + "mNotificationType='" + getNotificationType() + '\''
                + ", mNotificationTag=" + getNotificationTag()
                + "}";
    }

    /**
     * A {@link NotificationMetadata} Builder.
     */
    public static final class Builder {

        @Nullable
        private String mNotificationTag;
        private int mNotificationId;
        private Notification mNotification;
        // default mNotificationType = 0
        private int mNotificationType;

        /**
         * Creates an instance of {@link NotificationMetadata.Builder}.
         *
         * @param notificationId The {@link Notification} id
         * @param notification   The user visible {@link Notification}
         */
        public Builder(@IntRange(from = 1) int notificationId, @NonNull Notification notification) {
            if (notificationId == 0) {
                throw new IllegalArgumentException("Notification id cannot be 0");
            }
            mNotificationId = notificationId;
            mNotification = notification;
        }

        /**
         * @return The tag used to identify a {@link Notification}
         *
         * @hide
         */
        @Nullable
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public String getNotificationTag() {
            return mNotificationTag;
        }

        /**
         * @param notificationTag The tag used to identify a {@link Notification}
         * @return The instance of {@link Builder} for chaining
         */
        @NonNull
        public Builder setNotificationTag(@NonNull String notificationTag) {
            mNotificationTag = notificationTag;
            return this;
        }

        /**
         * Sets the type of foreground service notification. For more information see
         * {@link android.app.Service#startForeground(int, Notification, int)}. The default
         * notification type is {@code 0}.
         *
         * @param notificationType The type of foreground service notification.
         * @return The instance of {@link Builder} for chaining
         */
        @TargetApi(29)
        @NonNull
        public Builder setNotificationType(int notificationType) {
            mNotificationType = notificationType;
            return this;
        }

        /**
         * @return The {@link Notification} id
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public int getNotificationId() {
            return mNotificationId;
        }

        /**
         * @return The Foreground service notification type
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public int getNotificationType() {
            return mNotificationType;
        }

        /**
         * @return The user visible {@link Notification}
         *
         * @hide
         */
        @NonNull
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Notification getNotification() {
            return mNotification;
        }

        /**
         * @return The {@link NotificationMetadata}
         */
        @NonNull
        public NotificationMetadata build() {
            return new NotificationMetadata(this);
        }
    }
}
