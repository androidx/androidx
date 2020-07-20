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

package androidx.core.app;

import android.app.NotificationChannelGroup;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

/**
 * A grouping of related notification channels. e.g., channels that all belong to a single account.
 *
 * Setters return {@code this} to allow chaining.
 *
 * This class doesn't do anything on older SDKs which don't support Notification Channels.
 */
public class NotificationChannelGroupCompat {
    final String mId;
    CharSequence mName;
    String mDescription;

    /**
     * Builder class for {@link NotificationChannelGroupCompat} objects.
     */
    public static class Builder {
        private final NotificationChannelGroupCompat mGroup;

        /**
         * Creates a notification channel group.
         *
         * @param id The id of the group. Must be unique per package.
         *           The value may be truncated if it is too long.
         */
        public Builder(@NonNull String id) {
            mGroup = new NotificationChannelGroupCompat(id);
        }

        /**
         * Sets the user visible name of this group.
         *
         * You can rename this group when the system locale changes by listening for the
         * {@link Intent#ACTION_LOCALE_CHANGED} broadcast.
         *
         * <p>The recommended maximum length is 40 characters; the value may be truncated if it
         * is too long.
         */
        @NonNull
        public Builder setName(@Nullable CharSequence name) {
            mGroup.mName = name;
            return this;
        }

        /**
         * Sets the user visible description of this group.
         *
         * <p>The recommended maximum length is 300 characters; the value may be truncated if it
         * is too
         * long.
         */
        @NonNull
        public Builder setDescription(@Nullable String description) {
            mGroup.mDescription = description;
            return this;
        }

        /**
         * Creates a {@link NotificationChannelGroupCompat} instance.
         */
        @NonNull
        public NotificationChannelGroupCompat build() {
            return mGroup;
        }
    }

    NotificationChannelGroupCompat(@NonNull String id) {
        mId = Preconditions.checkNotNull(id);
    }

    /**
     * Gets the platform notification channel group object.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    NotificationChannelGroup getNotificationChannelGroup() {
        if (Build.VERSION.SDK_INT < 26) {
            return null;
        }
        NotificationChannelGroup group = new NotificationChannelGroup(mId, mName);
        if (Build.VERSION.SDK_INT >= 28) {
            group.setDescription(mDescription);
        }
        return group;
    }

    /**
     * Gets the id of the group.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Gets the user visible name of the group.
     */
    @Nullable
    public CharSequence getName() {
        return mName;
    }

    /**
     * Gets the user visible description of the group.
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }
}
