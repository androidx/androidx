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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.util.Preconditions;

/**
 * A representation of settings that apply to a collection of similarly themed notifications.
 *
 * Setters return {@code this} to allow chaining.
 *
 * This class doesn't do anything on older SDKs which don't support Notification Channels.
 */
public class NotificationChannelCompat {
    /**
     * The id of the default channel for an app. This id is reserved by the system. All
     * notifications posted from apps targeting {@link android.os.Build.VERSION_CODES#N_MR1} or
     * earlier without a notification channel specified are posted to this channel.
     */
    public static final String DEFAULT_CHANNEL_ID = "miscellaneous";

    private static final boolean DEFAULT_SHOW_BADGE = true;
    private static final int DEFAULT_LIGHT_COLOR = 0;

    // These fields are settable theough the builder
    @NonNull
    final String mId;
    CharSequence mName;
    int mImportance;
    String mDescription;
    String mGroupId;
    boolean mShowBadge = DEFAULT_SHOW_BADGE;
    Uri mSound = Settings.System.DEFAULT_NOTIFICATION_URI;
    AudioAttributes mAudioAttributes;
    boolean mLights;
    int mLightColor = DEFAULT_LIGHT_COLOR;
    boolean mVibrationEnabled;
    long[] mVibrationPattern;
    String mParentId;
    String mConversationId;

    // These fields are read-only
    private boolean mBypassDnd;
    private int mLockscreenVisibility;
    private boolean mCanBubble;
    private boolean mImportantConversation;

    /**
     * Builder class for {@link NotificationChannelCompat} objects.
     */
    public static class Builder {
        private final NotificationChannelCompat mChannel;

        /**
         * Creates a notification channel builder.
         *
         * @param id         The id of the channel. Must be unique per package. The value may be
         *                   truncated if it is too long.
         * @param importance The importance of the channel. This controls how interruptive
         *                   notifications posted to this channel are.
         */
        public Builder(@NonNull String id, int importance) {
            mChannel = new NotificationChannelCompat(id, importance);
        }

        /**
         * Sets the user visible name of this channel.
         *
         * You can rename this channel when the system locale changes by listening for the
         * {@link Intent#ACTION_LOCALE_CHANGED} broadcast.
         *
         * <p>The recommended maximum length is 40 characters; the value may be truncated if it
         * is too long.
         */
        @NonNull
        public Builder setName(@Nullable CharSequence name) {
            mChannel.mName = name;
            return this;
        }

        /**
         * Sets the level of interruption of this notification channel.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)}.
         *
         * @param importance the amount the user should be interrupted by notifications from this
         *                   channel.
         */
        @NonNull
        public Builder setImportance(int importance) {
            mChannel.mImportance = importance;
            return this;
        }

        /**
         * Sets the user visible description of this channel.
         *
         * <p>The recommended maximum length is 300 characters; the value may be truncated if it is
         * too long.
         */
        @NonNull
        public Builder setDescription(@Nullable String description) {
            mChannel.mDescription = description;
            return this;
        }

        /**
         * Sets what group this channel belongs to.
         *
         * Group information is only used for presentation, not for behavior.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)},
         * unless the channel is not currently part of a group.
         *
         * @param groupId the id of a group created by
         *                {@link NotificationManagerCompat#createNotificationChannelGroup}.
         */
        @NonNull
        public Builder setGroup(@Nullable String groupId) {
            mChannel.mGroupId = groupId;
            return this;
        }

        /**
         * Sets whether notifications posted to this channel can appear as application icon badges
         * in a Launcher.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)}.
         *
         * @param showBadge true if badges should be allowed to be shown.
         */
        @NonNull
        public Builder setShowBadge(boolean showBadge) {
            mChannel.mShowBadge = showBadge;
            return this;
        }

        /**
         * Sets the sound that should be played for notifications posted to this channel and its
         * audio attributes. Notification channels with an {@link #setImportance(int)}
         * importance} of
         * at least {@link NotificationManagerCompat#IMPORTANCE_DEFAULT} should have a sound.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)}.
         */
        @NonNull
        public Builder setSound(@Nullable Uri sound, @Nullable AudioAttributes audioAttributes) {
            mChannel.mSound = sound;
            mChannel.mAudioAttributes = audioAttributes;
            return this;
        }

        /**
         * Sets whether notifications posted to this channel should display notification lights,
         * on devices that support that feature.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)}.
         */
        @NonNull
        public Builder setLightsEnabled(boolean lights) {
            mChannel.mLights = lights;
            return this;
        }

        /**
         * Sets the notification light color for notifications posted to this channel, if lights are
         * {@link #setLightsEnabled(boolean) enabled} on this channel and the device supports that
         * feature.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)}.
         */
        @NonNull
        public Builder setLightColor(int argb) {
            mChannel.mLightColor = argb;
            return this;
        }

        /**
         * Sets whether notification posted to this channel should vibrate. The vibration pattern
         * can be set with {@link #setVibrationPattern(long[])}.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)}.
         */
        @NonNull
        public Builder setVibrationEnabled(boolean vibration) {
            mChannel.mVibrationEnabled = vibration;
            return this;
        }

        /**
         * Sets the vibration pattern for notifications posted to this channel. If the provided
         * pattern is valid (non-null, non-empty), will {@link #setVibrationEnabled(boolean)} enable
         * vibration} as well. Otherwise, vibration will be disabled.
         *
         * Only modifiable before the channel is submitted to
         * {@link NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)}.
         */
        @NonNull
        public Builder setVibrationPattern(@Nullable long[] vibrationPattern) {
            mChannel.mVibrationEnabled = vibrationPattern != null && vibrationPattern.length > 0;
            mChannel.mVibrationPattern = vibrationPattern;
            return this;
        }

        /**
         * Sets this channel as being conversation-centric. Different settings and functionality may
         * be exposed for conversation-centric channels.
         *
         * Calling this on SDKs that do not support conversations will have no effect on the
         * built channel.  As a result, this channel will not be linked to the channel with the
         * parentChannelId.  That means that this channel must be published to directly to be used;
         * it cannot be published to by publishing to the parentChannelId with the shortcutId.
         *
         * @param parentChannelId The {@link #getId()} id} of the generic channel that notifications
         *                        of this type would be posted to in absence of a specific
         *                        conversation id. For example, if this channel represents
         *                        'Messages from Person A', the parent channel would be 'Messages.'
         * @param conversationId  The {@link ShortcutInfoCompat#getId()} of the shortcut
         *                        representing this channel's conversation.
         */
        @NonNull
        public Builder setConversationId(@NonNull String parentChannelId,
                @NonNull String conversationId) {
            if (Build.VERSION.SDK_INT >= 30) {
                mChannel.mParentId = parentChannelId;
                mChannel.mConversationId = conversationId;
            }
            return this;
        }

        /**
         * Creates a {@link NotificationChannelCompat} instance.
         */
        @NonNull
        public NotificationChannelCompat build() {
            return mChannel;
        }
    }

    NotificationChannelCompat(@NonNull String id, int importance) {
        mId = Preconditions.checkNotNull(id);
        mImportance = importance;
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioAttributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
        }
    }

    @RequiresApi(26)
    NotificationChannelCompat(@NonNull NotificationChannel channel) {
        this(channel.getId(), channel.getImportance());
        // Populate all builder-editable fields
        mName = channel.getName();
        mDescription = channel.getDescription();
        mGroupId = channel.getGroup();
        mShowBadge = channel.canShowBadge();
        mSound = channel.getSound();
        mAudioAttributes = channel.getAudioAttributes();
        mLights = channel.shouldShowLights();
        mLightColor = channel.getLightColor();
        mVibrationEnabled = channel.shouldVibrate();
        mVibrationPattern = channel.getVibrationPattern();
        if (Build.VERSION.SDK_INT >= 30) {
            mParentId = channel.getParentChannelId();
            mConversationId = channel.getConversationId();
        }
        // Populate all read-only fields
        mBypassDnd = channel.canBypassDnd();
        mLockscreenVisibility = channel.getLockscreenVisibility();
        if (Build.VERSION.SDK_INT >= 29) {
            mCanBubble = channel.canBubble();
        }
        if (Build.VERSION.SDK_INT >= 30) {
            mImportantConversation = channel.isImportantConversation();
        }
    }

    /**
     * Gets the platform notification channel object.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    NotificationChannel getNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return null;
        }
        NotificationChannel channel = new NotificationChannel(mId, mName, mImportance);
        channel.setDescription(mDescription);
        channel.setGroup(mGroupId);
        channel.setShowBadge(mShowBadge);
        channel.setSound(mSound, mAudioAttributes);
        channel.enableLights(mLights);
        channel.setLightColor(mLightColor);
        channel.setVibrationPattern(mVibrationPattern);
        channel.enableVibration(mVibrationEnabled);
        if (Build.VERSION.SDK_INT >= 30 && mParentId != null && mConversationId != null) {
            channel.setConversationId(mParentId, mConversationId);
        }
        return channel;
    }

    /**
     * Creates a {@link Builder} instance with all the writeable property values of this instance.
     */
    @NonNull
    public Builder toBuilder() {
        return new Builder(mId, mImportance)
                .setName(mName)
                .setDescription(mDescription)
                .setGroup(mGroupId)
                .setShowBadge(mShowBadge)
                .setSound(mSound, mAudioAttributes)
                .setLightsEnabled(mLights)
                .setLightColor(mLightColor)
                .setVibrationEnabled(mVibrationEnabled)
                .setVibrationPattern(mVibrationPattern)
                .setConversationId(mParentId, mConversationId);
    }

    /**
     * Returns the id of this channel.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the user visible name of this channel.
     */
    @Nullable
    public CharSequence getName() {
        return mName;
    }

    /**
     * Returns the user visible description of this channel.
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the user specified importance e.g. {@link NotificationManagerCompat#IMPORTANCE_LOW}
     * for notifications posted to this channel. Note: This value might be >
     * {@link NotificationManagerCompat#IMPORTANCE_NONE}, but notifications posted to this channel
     * will not be shown to the user if the parent {@link NotificationChannelGroup} or app is
     * blocked.
     * See {@link NotificationChannelGroup#isBlocked()} and
     * {@link NotificationManagerCompat#areNotificationsEnabled()}.
     */
    public int getImportance() {
        return mImportance;
    }

    /**
     * Returns the notification sound for this channel.
     */
    @Nullable
    public Uri getSound() {
        return mSound;
    }

    /**
     * Returns the audio attributes for sound played by notifications posted to this channel.
     */
    @Nullable
    public AudioAttributes getAudioAttributes() {
        return mAudioAttributes;
    }

    /**
     * Returns whether notifications posted to this channel trigger notification lights.
     */
    public boolean shouldShowLights() {
        return mLights;
    }

    /**
     * Returns the notification light color for notifications posted to this channel. Irrelevant
     * unless {@link #shouldShowLights()}.
     */
    public int getLightColor() {
        return mLightColor;
    }

    /**
     * Returns whether notifications posted to this channel always vibrate.
     */
    public boolean shouldVibrate() {
        return mVibrationEnabled;
    }

    /**
     * Returns the vibration pattern for notifications posted to this channel. Will be ignored if
     * vibration is not enabled ({@link #shouldVibrate()}.
     */
    @Nullable
    public long[] getVibrationPattern() {
        return mVibrationPattern;
    }

    /**
     * Returns whether notifications posted to this channel can appear as badges in a Launcher
     * application.
     *
     * Note that badging may be disabled for other reasons.
     */
    public boolean canShowBadge() {
        return mShowBadge;
    }

    /**
     * Returns what group this channel belongs to.
     *
     * This is used only for visually grouping channels in the UI.
     */
    @Nullable
    public String getGroup() {
        return mGroupId;
    }

    /**
     * Returns the {@link #getId() id} of the parent notification channel to this channel, if it's
     * a conversation related channel.
     * See {@link Builder#setConversationId(String, String)}.
     */
    @Nullable
    public String getParentChannelId() {
        return mParentId;
    }

    /**
     * Returns the {@link ShortcutInfoCompat#getId() id} of the conversation backing this channel,
     * if it's associated with a conversation.
     * See {@link Builder#setConversationId(String, String)}.
     */
    @Nullable
    public String getConversationId() {
        return mConversationId;
    }

    /**
     * Whether or not notifications posted to this channel can bypass the Do Not Disturb
     * {@link android.app.NotificationManager#INTERRUPTION_FILTER_PRIORITY} mode.
     *
     * <p>This is a read-only property which is only valid on instances fetched from the
     * {@link NotificationManagerCompat}.
     */
    public boolean canBypassDnd() {
        return mBypassDnd;
    }

    /**
     * Returns whether or not notifications posted to this channel are shown on the lockscreen
     * in full or redacted form.
     *
     * <p>This is a read-only property which is only valid on instances fetched from the
     * {@link NotificationManagerCompat}.
     */
    @NotificationCompat.NotificationVisibility
    public int getLockscreenVisibility() {
        return mLockscreenVisibility;
    }

    /**
     * Returns whether notifications posted to this channel are allowed to display outside of the
     * notification shade, in a floating window on top of other apps.
     *
     * <p>This is a read-only property which is only valid on instances fetched from the
     * {@link NotificationManagerCompat}.
     */
    public boolean canBubble() {
        return mCanBubble;
    }

    /**
     * Whether or not notifications in this conversation are considered important.
     *
     * <p>Important conversations may get special visual treatment, and might be able to bypass DND.
     *
     * <p>This is only valid for channels that represent conversations, that is, those with a valid
     * {@link #getConversationId() conversation id}.
     *
     * <p>This is a read-only property which is only valid on instances fetched from the
     * {@link NotificationManagerCompat}.
     */
    public boolean isImportantConversation() {
        return mImportantConversation;
    }

}
