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

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A grouping of related notification channels. e.g., channels that all belong to a single account.
 *
 * Setters return {@code this} to allow chaining.
 *
 * This class doesn't do anything on older SDKs which don't support Notification Channels.
 */
public class NotificationChannelGroupCompat {
    // These fields are settable through the builder
    final String mId;
    CharSequence mName;
    String mDescription;

    // These fields are read-only
    private boolean mBlocked;
    private List<NotificationChannelCompat> mChannels = Collections.emptyList();

    /**
     * Builder class for {@link NotificationChannelGroupCompat} objects.
     */
    public static class Builder {
        final NotificationChannelGroupCompat mGroup;

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

    @RequiresApi(28)
    NotificationChannelGroupCompat(@NonNull NotificationChannelGroup group) {
        this(group, Collections.<NotificationChannel>emptyList());
    }

    @RequiresApi(26)
    NotificationChannelGroupCompat(@NonNull NotificationChannelGroup group,
            @NonNull List<NotificationChannel> allChannels) {
        this(Api26Impl.getId(group));
        // Populate all builder-editable fields
        mName = Api26Impl.getName(group);
        if (Build.VERSION.SDK_INT >= 28) {
            mDescription = Api28Impl.getDescription(group);
        }
        // Populate all read-only fields
        if (Build.VERSION.SDK_INT >= 28) {
            mBlocked = Api28Impl.isBlocked(group);
            mChannels = getChannelsCompat(Api26Impl.getChannels(group));
        } else {
            // On API 26 and 27, the NotificationChannelGroup.getChannels() method was broken,
            // so we collect this information from the full list of channels at construction.
            mChannels = getChannelsCompat(allChannels);
        }
    }

    @RequiresApi(26)
    private List<NotificationChannelCompat> getChannelsCompat(List<NotificationChannel> channels) {
        List<NotificationChannelCompat> channelsCompat = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            if (mId.equals(Api26Impl.getGroup(channel))) {
                channelsCompat.add(new NotificationChannelCompat(channel));
            }
        }
        return channelsCompat;
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
        NotificationChannelGroup group = Api26Impl.createNotificationChannelGroup(mId, mName);
        if (Build.VERSION.SDK_INT >= 28) {
            Api28Impl.setDescription(group, mDescription);
        }
        return group;
    }

    /**
     * Creates a {@link Builder} instance with all the writeable property values of this instance.
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(mId)
                .setName(mName)
                .setDescription(mDescription);
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

    /**
     * Returns whether or not notifications posted to {@link NotificationChannelCompat} belonging
     * to this group are blocked. This value is independent of
     * {@link NotificationManagerCompat#areNotificationsEnabled()} and
     * {@link NotificationChannelCompat#getImportance()}.
     *
     * <p>This value is always {@code false} before {@link Build.VERSION_CODES#P}
     *
     * <p>This is a read-only property which is only valid on instances fetched from the
     * {@link NotificationManagerCompat}.
     */
    public boolean isBlocked() {
        return mBlocked;
    }

    /**
     * Returns the list of channels that belong to this group.
     *
     * <p>This is a read-only property which is only valid on instances fetched from the
     * {@link NotificationManagerCompat}.
     */
    @NonNull
    public List<NotificationChannelCompat> getChannels() {
        return mChannels;
    }

    /**
     * A class for wrapping calls to {@link NotificationChannelGroupCompat} methods which
     * were added in API 26; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() { }

        @DoNotInline
        static NotificationChannelGroup createNotificationChannelGroup(String id,
                CharSequence name) {
            return new NotificationChannelGroup(id, name);
        }

        @DoNotInline
        static String getId(NotificationChannelGroup notificationChannelGroup) {
            return notificationChannelGroup.getId();
        }

        @DoNotInline
        static CharSequence getName(NotificationChannelGroup notificationChannelGroup) {
            return notificationChannelGroup.getName();
        }

        @DoNotInline
        static List<NotificationChannel> getChannels(
                NotificationChannelGroup notificationChannelGroup) {
            return notificationChannelGroup.getChannels();
        }

        @DoNotInline
        static String getGroup(NotificationChannel notificationChannel) {
            return notificationChannel.getGroup();
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationChannelGroupCompat} methods which
     * were added in API 28; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() { }

        @DoNotInline
        static boolean isBlocked(NotificationChannelGroup notificationChannelGroup) {
            return notificationChannelGroup.isBlocked();
        }

        @DoNotInline
        static String getDescription(NotificationChannelGroup notificationChannelGroup) {
            return notificationChannelGroup.getDescription();
        }

        @DoNotInline
        static void setDescription(NotificationChannelGroup notificationChannelGroup,
                String description) {
            notificationChannelGroup.setDescription(description);
        }
    }
}
