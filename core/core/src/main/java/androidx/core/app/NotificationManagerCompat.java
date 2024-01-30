/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.INotificationSideChannel;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compatibility library for NotificationManager with fallbacks for older platforms.
 *
 * <p>To use this class, call the static function {@link #from} to get a
 * {@link NotificationManagerCompat} object, and then call one of its
 * methods to post or cancel notifications.
 */
public final class NotificationManagerCompat {
    private static final String TAG = "NotifManCompat";
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    /**
     * Notification extras key: if set to true, the posted notification should use
     * the side channel for delivery instead of using notification manager.
     */
    public static final String EXTRA_USE_SIDE_CHANNEL = "android.support.useSideChannel";

    /**
     * Intent action to register for on a service to receive side channel
     * notifications. The listening service must be in the same package as an enabled
     * {@link NotificationListenerService}.
     */
    public static final String ACTION_BIND_SIDE_CHANNEL =
            "android.support.BIND_NOTIFICATION_SIDE_CHANNEL";

    /**
     * Maximum sdk build version which needs support for side channeled notifications.
     * Currently the only needed use is for side channeling group children before KITKAT_WATCH.
     */
    static final int MAX_SIDE_CHANNEL_SDK_VERSION = 19;

    /** Base time delay for a side channel listener queue retry. */
    private static final int SIDE_CHANNEL_RETRY_BASE_INTERVAL_MS = 1000;
    /** Maximum retries for a side channel listener before dropping tasks. */
    private static final int SIDE_CHANNEL_RETRY_MAX_COUNT = 6;
    /** Hidden field Settings.Secure.ENABLED_NOTIFICATION_LISTENERS */
    private static final String SETTING_ENABLED_NOTIFICATION_LISTENERS =
            "enabled_notification_listeners";

    /** Cache of enabled notification listener components */
    private static final Object sEnabledNotificationListenersLock = new Object();
    @GuardedBy("sEnabledNotificationListenersLock")
    private static String sEnabledNotificationListeners;
    @GuardedBy("sEnabledNotificationListenersLock")
    private static Set<String> sEnabledNotificationListenerPackages = new HashSet<String>();

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    /** Lock for mutable static fields */
    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static SideChannelManager sSideChannelManager;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({INTERRUPTION_FILTER_UNKNOWN, INTERRUPTION_FILTER_ALL, INTERRUPTION_FILTER_PRIORITY,
            INTERRUPTION_FILTER_NONE, INTERRUPTION_FILTER_ALARMS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface InterruptionFilter {
    }

    /**
     * {@link #getCurrentInterruptionFilter() Interruption filter} constant -
     *     Normal interruption filter - no notifications are suppressed.
     */
    public static final int INTERRUPTION_FILTER_ALL = 1;

    /**
     * {@link #getCurrentInterruptionFilter() Interruption filter} constant -
     *     Priority interruption filter - all notifications are suppressed except those that match
     *     the priority criteria. Some audio streams are muted. See
     *     {@link Policy#priorityCallSenders}, {@link Policy#priorityCategories},
     *     {@link Policy#priorityMessageSenders} to define or query this criteria. Users can
     *     additionally specify packages that can bypass this interruption filter.
     */
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;

    /**
     * {@link #getCurrentInterruptionFilter() Interruption filter} constant -
     *     No interruptions filter - all notifications are suppressed and all audio streams (except
     *     those used for phone calls) and vibrations are muted.
     */
    public static final int INTERRUPTION_FILTER_NONE = 3;

    /**
     * {@link #getCurrentInterruptionFilter() Interruption filter} constant -
     *     Alarms only interruption filter - all notifications except those of category
     *     {@link Notification#CATEGORY_ALARM} are suppressed. Some audio streams are muted.
     */
    public static final int INTERRUPTION_FILTER_ALARMS = 4;

    /** {@link #getCurrentInterruptionFilter() Interruption filter} constant -
     *     returned when the value is unavailable for any reason.
     */
    public static final int INTERRUPTION_FILTER_UNKNOWN = 0;

    /**
     * Value signifying that the user has not expressed an importance.
     *
     * This value is for persisting preferences, and should never be associated with
     * an actual notification.
     */
    public static final int IMPORTANCE_UNSPECIFIED = -1000;

    /**
     * A notification with no importance: shows nowhere, is blocked.
     */
    public static final int IMPORTANCE_NONE = 0;

    /**
     * Min notification importance: only shows in the shade, below the fold.
     */
    public static final int IMPORTANCE_MIN = 1;

    /**
     * Low notification importance: shows everywhere, but is not intrusive.
     */
    public static final int IMPORTANCE_LOW = 2;

    /**
     * Default notification importance: shows everywhere, allowed to makes noise,
     * but does not visually intrude.
     */
    public static final int IMPORTANCE_DEFAULT = 3;

    /**
     * Higher notification importance: shows everywhere, allowed to makes noise and peek.
     */
    public static final int IMPORTANCE_HIGH = 4;

    /**
     * Highest notification importance: shows everywhere, allowed to makes noise, peek, and
     * use full screen intents.
     */
    public static final int IMPORTANCE_MAX = 5;

    /** Get a {@link NotificationManagerCompat} instance for a provided context. */
    @NonNull
    public static NotificationManagerCompat from(@NonNull Context context) {
        return new NotificationManagerCompat(context);
    }

    private NotificationManagerCompat(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    @VisibleForTesting
    NotificationManagerCompat(@NonNull NotificationManager notificationManager,
            @NonNull Context context) {
        mContext = context;
        mNotificationManager = notificationManager;
    }

    /**
     * Cancel a previously shown notification.
     *
     * @param id the ID of the notification
     */
    public void cancel(int id) {
        cancel(null, id);
    }

    /**
     * Cancel a previously shown notification.
     *
     * @param tag the string identifier of the notification.
     * @param id  the ID of the notification
     */
    public void cancel(@Nullable String tag, int id) {
        mNotificationManager.cancel(tag, id);
        if (Build.VERSION.SDK_INT <= MAX_SIDE_CHANNEL_SDK_VERSION) {
            pushSideChannelQueue(new CancelTask(mContext.getPackageName(), id, tag));
        }
    }

    /** Cancel all previously shown notifications. */
    public void cancelAll() {
        mNotificationManager.cancelAll();
        if (Build.VERSION.SDK_INT <= MAX_SIDE_CHANNEL_SDK_VERSION) {
            pushSideChannelQueue(new CancelTask(mContext.getPackageName()));
        }
    }

    /**
     * Post a notification to be shown in the status bar, stream, etc.
     *
     * @param id           the ID of the notification
     * @param notification the notification to post to the system
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public void notify(int id, @NonNull Notification notification) {
        notify(null, id, notification);
    }

    /**
     * Post a notification to be shown in the status bar, stream, etc.
     *
     * @param tag          the string identifier for a notification. Can be {@code null}.
     * @param id           the ID of the notification. The pair (tag, id) must be unique within
     *                     your app.
     * @param notification the notification to post to the system
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public void notify(@Nullable String tag, int id, @NonNull Notification notification) {
        if (useSideChannelForNotification(notification)) {
            pushSideChannelQueue(new NotifyTask(mContext.getPackageName(), id, tag, notification));
            // Cancel this notification in notification manager if it just transitioned to being
            // side channelled.
            mNotificationManager.cancel(tag, id);
        } else {
            mNotificationManager.notify(tag, id, notification);
        }
    }

    /**
     * Post a number of notifications, to be shown in the status bar, stream, etc.
     * Each notification will attempt to be posted in the order provided in the {@code
     * notificationWithIds} list. Each notification must have a provided id and may have a
     * provided tag.
     *
     * This is the preferred method for posting groups of notifications, to improve sound and
     * animation behavior.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public void notify(@NonNull List<NotificationWithIdAndTag> notificationWithIdAndTags) {
        final int notificationsSize = notificationWithIdAndTags.size();
        for (int i = 0; i < notificationsSize; i++) {
            NotificationWithIdAndTag notificationWithIdAndTag = notificationWithIdAndTags.get(i);
            notify(notificationWithIdAndTag.mTag, notificationWithIdAndTag.mId,
                    notificationWithIdAndTag.mNotification);
        }
    }

    /**
     * Helper class which encapsulates a Notification, its id, and optionally a tag, for use when
     * batch-posting a number of notifications.
     */
    public static class NotificationWithIdAndTag {
        final String mTag;
        final int mId;
        Notification mNotification;

        public NotificationWithIdAndTag(@Nullable String tag, int id,
                @NonNull Notification notification) {
            this.mTag = tag;
            this.mId = id;
            this.mNotification = notification;
        }

        public NotificationWithIdAndTag(int id, @NonNull Notification notification) {
            this(null, id, notification);
        }
    }

    /**
     * Recover a list of active notifications: ones that have been posted by the calling app that
     * have not yet been dismissed by the user or {@link #cancel(String, int)}ed by the app.
     *
     * <p><Each notification is embedded in a {@link StatusBarNotification} object, including the
     * original <code>tag</code> and <code>id</code> supplied to
     * {@link #notify(String, int, Notification) notify()}
     * (via {@link StatusBarNotification#getTag() getTag()} and
     * {@link StatusBarNotification#getId() getId()}) as well as a copy of the original
     * {@link Notification} object (via {@link StatusBarNotification#getNotification()}).
     * </p>
     * <p>From {@link Build.VERSION_CODES#Q}, will also return notifications you've posted as an
     * app's notification delegate via
     * {@link NotificationManager#notifyAsPackage(String, String, int, Notification)}.
     * </p>
     * <p>
     *     Returns an empty list on {@link Build.VERSION_CODES#LOLLIPOP_MR1} and earlier.
     * </p>
     *
     * @return A list of {@link StatusBarNotification}.
     */
    @NonNull
    public List<StatusBarNotification> getActiveNotifications() {
        if (Build.VERSION.SDK_INT >= 23) {
            return Api23Impl.getActiveNotifications(mNotificationManager);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns whether notifications from the calling package are not blocked.
     */
    public boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.areNotificationsEnabled(mNotificationManager);
        } else if (Build.VERSION.SDK_INT >= 19) {
            AppOpsManager appOps =
                    (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = mContext.getApplicationInfo();
            String pkg = mContext.getApplicationContext().getPackageName();
            int uid = appInfo.uid;
            try {
                Class<?> appOpsClass = Class.forName(AppOpsManager.class.getName());
                Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE,
                        Integer.TYPE, String.class);
                Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
                int value = (int) opPostNotificationValue.get(Integer.class);
                return ((int) checkOpNoThrowMethod.invoke(appOps, value, uid, pkg)
                        == AppOpsManager.MODE_ALLOWED);
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException
                    | InvocationTargetException | IllegalAccessException | RuntimeException e) {
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Returns the user specified importance for notifications from the calling package.
     *
     * @return An importance level, such as {@link #IMPORTANCE_DEFAULT}.
     */
    public int getImportance() {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.getImportance(mNotificationManager);
        } else {
            return IMPORTANCE_UNSPECIFIED;
        }
    }

    /**
     * Creates a notification channel that notifications can be posted to.
     *
     * This can also be used to restore a deleted channel and to update an existing channel's
     * name, description, group, and/or importance.
     *
     * <p>The importance of an existing channel will only be changed if the new importance is lower
     * than the current value and the user has not altered any settings on this channel.
     *
     * <p>The group of an existing channel will only be changed if the channel does not already
     * belong to a group.
     *
     * All other fields are ignored for channels that already exist.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param channel the channel to create.  Note that the created channel may differ from this
     *                value. If the provided channel is malformed, a RemoteException will be
     *                thrown.
     */
    public void createNotificationChannel(@NonNull NotificationChannel channel) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.createNotificationChannel(mNotificationManager, channel);
        }
    }

    /**
     * Creates a notification channel that notifications can be posted to.
     *
     * This can also be used to restore a deleted channel and to update an existing channel's
     * name, description, group, and/or importance.
     *
     * <p>The importance of an existing channel will only be changed if the new importance is lower
     * than the current value and the user has not altered any settings on this channel.
     *
     * <p>The group of an existing channel will only be changed if the channel does not already
     * belong to a group.
     *
     * All other fields are ignored for channels that already exist.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param channel the channel to create.  Note that the created channel may differ from this
     *                value. If the provided channel is malformed, a RemoteException will be
     *                thrown.
     */
    public void createNotificationChannel(@NonNull NotificationChannelCompat channel) {
        createNotificationChannel(channel.getNotificationChannel());
    }

    /**
     * Creates a group container for {@link NotificationChannel} objects.
     *
     * This can be used to rename an existing group.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param group The group to create
     */
    public void createNotificationChannelGroup(@NonNull NotificationChannelGroup group) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.createNotificationChannelGroup(mNotificationManager, group);
        }
    }

    /**
     * Creates a group container for {@link NotificationChannel} objects.
     *
     * This can be used to rename an existing group.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param group The group to create
     */
    public void createNotificationChannelGroup(@NonNull NotificationChannelGroupCompat group) {
        createNotificationChannelGroup(group.getNotificationChannelGroup());
    }

    /**
     * Creates multiple notification channels that different notifications can be posted to. See
     * {@link #createNotificationChannel(NotificationChannel)}.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param channels the list of channels to attempt to create.
     */
    public void createNotificationChannels(@NonNull List<NotificationChannel> channels) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.createNotificationChannels(mNotificationManager, channels);
        }
    }

    /**
     * Creates multiple notification channels that different notifications can be posted to. See
     * {@link #createNotificationChannel(NotificationChannelCompat)}.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param channels the list of channels to attempt to create.
     */
    public void createNotificationChannelsCompat(
            @NonNull List<NotificationChannelCompat> channels) {
        if (Build.VERSION.SDK_INT >= 26 && !channels.isEmpty()) {
            List<NotificationChannel> platformChannels = new ArrayList<>(channels.size());
            for (NotificationChannelCompat channel : channels) {
                platformChannels.add(channel.getNotificationChannel());
            }
            Api26Impl.createNotificationChannels(mNotificationManager, platformChannels);
        }
    }

    /**
     * Creates multiple notification channel groups. See
     * {@link #createNotificationChannelGroup(NotificationChannelGroup)}.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param groups The list of groups to create
     */
    public void createNotificationChannelGroups(@NonNull List<NotificationChannelGroup> groups) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.createNotificationChannelGroups(mNotificationManager, groups);
        }
    }

    /**
     * Creates multiple notification channel groups. See
     * {@link #createNotificationChannelGroup(NotificationChannelGroupCompat)}.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param groups The list of groups to create
     */
    public void createNotificationChannelGroupsCompat(
            @NonNull List<NotificationChannelGroupCompat> groups) {
        if (Build.VERSION.SDK_INT >= 26 && !groups.isEmpty()) {
            List<NotificationChannelGroup> platformGroups = new ArrayList<>(groups.size());
            for (NotificationChannelGroupCompat group : groups) {
                platformGroups.add(group.getNotificationChannelGroup());
            }
            Api26Impl.createNotificationChannelGroups(mNotificationManager, platformGroups);
        }
    }

    /**
     * Deletes the given notification channel.
     *
     * <p>If you {@link #createNotificationChannel(NotificationChannel) create} a new channel with
     * this same id, the deleted channel will be un-deleted with all of the same settings it
     * had before it was deleted.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     */
    public void deleteNotificationChannel(@NonNull String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.deleteNotificationChannel(mNotificationManager, channelId);
        }
    }

    /**
     * Deletes the given notification channel group, and all notification channels that
     * belong to it.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     */
    public void deleteNotificationChannelGroup(@NonNull String groupId) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.deleteNotificationChannelGroup(mNotificationManager, groupId);
        }
    }

    /**
     * Deletes notification channels for which ids are NOT given.
     *
     * This will NOT delete channels which are conversation children of the given channels.
     *
     * It doesn't do anything on older SDKs which don't support Notification Channels.
     *
     * @param channelIds the IDs of any channels which should NOT be deleted by this method.
     */
    public void deleteUnlistedNotificationChannels(@NonNull Collection<String> channelIds) {
        if (Build.VERSION.SDK_INT >= 26) {
            for (NotificationChannel channel :
                    Api26Impl.getNotificationChannels(mNotificationManager)) {
                if (channelIds.contains(Api26Impl.getId(channel))) {
                    continue;
                }
                if (Build.VERSION.SDK_INT >= 30
                        && channelIds.contains(Api30Impl.getParentChannelId(channel))) {
                    continue;
                }
                Api26Impl.deleteNotificationChannel(mNotificationManager,
                        Api26Impl.getId(channel));
            }
        }
    }

    /**
     * Returns the notification channel settings for a given channel id.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    @Nullable
    public NotificationChannel getNotificationChannel(@NonNull String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getNotificationChannel(mNotificationManager, channelId);
        }
        return null;
    }

    /**
     * Returns the notification channel settings for a given channel id.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    @Nullable
    public NotificationChannelCompat getNotificationChannelCompat(@NonNull String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = getNotificationChannel(channelId);
            if (channel != null) {
                return new NotificationChannelCompat(channel);
            }
        }
        return null;
    }

    /**
     * Returns the notification channel settings for a given channel and
     * {@link ShortcutInfo#getId() conversation id}.
     *
     * Returns the channel for the channelId on older SDKs which don't support Conversations.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    @Nullable
    public NotificationChannel getNotificationChannel(@NonNull String channelId,
            @NonNull String conversationId) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30Impl.getNotificationChannel(mNotificationManager, channelId,
                    conversationId);
        }
        return getNotificationChannel(channelId);
    }

    /**
     * Returns the notification channel settings for a given channel and
     * {@link ShortcutInfo#getId() conversation id}.
     *
     * Returns the channel for the channelId on older SDKs which don't support Conversations.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    @Nullable
    public NotificationChannelCompat getNotificationChannelCompat(@NonNull String channelId,
            @NonNull String conversationId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = getNotificationChannel(channelId, conversationId);
            if (channel != null) {
                return new NotificationChannelCompat(channel);
            }
        }
        return null;
    }

    /**
     * Returns the notification channel group settings for a given channel group id.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    @Nullable
    public NotificationChannelGroup getNotificationChannelGroup(@NonNull String channelGroupId) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getNotificationChannelGroup(mNotificationManager, channelGroupId);
        } else if (Build.VERSION.SDK_INT >= 26) {
            // find the group in list by its ID
            for (NotificationChannelGroup group : getNotificationChannelGroups()) {
                if (Api26Impl.getId(group).equals(channelGroupId)) return group;
            }
            // requested group doesn't exist
            return null;
        } else {
            return null;
        }
    }

    /**
     * Returns the notification channel group settings for a given channel group id.
     *
     * Returns {@code null} on older SDKs which don't support Notification Channels.
     */
    @Nullable
    public NotificationChannelGroupCompat getNotificationChannelGroupCompat(
            @NonNull String channelGroupId) {
        if (Build.VERSION.SDK_INT >= 28) {
            NotificationChannelGroup group = getNotificationChannelGroup(channelGroupId);
            if (group != null) {
                return new NotificationChannelGroupCompat(group);
            }
        } else if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannelGroup group = getNotificationChannelGroup(channelGroupId);
            if (group != null) {
                return new NotificationChannelGroupCompat(group, getNotificationChannels());
            }
        }
        return null;
    }

    /**
     * Returns all notification channels belonging to the calling app
     * or an empty list on older SDKs which don't support Notification Channels.
     */
    @NonNull
    public List<NotificationChannel> getNotificationChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getNotificationChannels(mNotificationManager);
        }
        return Collections.emptyList();
    }

    /**
     * Returns all notification channels belonging to the calling app
     * or an empty list on older SDKs which don't support Notification Channels.
     */
    @NonNull
    @SuppressWarnings("MixedMutabilityReturnType")
    public List<NotificationChannelCompat> getNotificationChannelsCompat() {
        if (Build.VERSION.SDK_INT >= 26) {
            List<NotificationChannel> channels = getNotificationChannels();
            if (!channels.isEmpty()) {
                List<NotificationChannelCompat> channelsCompat = new ArrayList<>(channels.size());
                for (NotificationChannel channel : channels) {
                    channelsCompat.add(new NotificationChannelCompat(channel));
                }
                return channelsCompat;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Returns all notification channel groups belonging to the calling app
     * or an empty list on older SDKs which don't support Notification Channels.
     */
    @NonNull
    public List<NotificationChannelGroup> getNotificationChannelGroups() {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getNotificationChannelGroups(mNotificationManager);
        }
        return Collections.emptyList();
    }

    /**
     * Returns all notification channel groups belonging to the calling app
     * or an empty list on older SDKs which don't support Notification Channels.
     */
    @NonNull
    @SuppressWarnings("MixedMutabilityReturnType")
    public List<NotificationChannelGroupCompat> getNotificationChannelGroupsCompat() {
        if (Build.VERSION.SDK_INT >= 26) {
            List<NotificationChannelGroup> groups = getNotificationChannelGroups();
            if (!groups.isEmpty()) {
                // Don't query getNotificationChannels() on API 28+ where it isn't needed
                List<NotificationChannel> allChannels = Build.VERSION.SDK_INT >= 28
                        ? Collections.<NotificationChannel>emptyList()
                        : getNotificationChannels();
                List<NotificationChannelGroupCompat> groupsCompat = new ArrayList<>(groups.size());
                for (NotificationChannelGroup group : groups) {
                    if (Build.VERSION.SDK_INT >= 28) {
                        groupsCompat.add(new NotificationChannelGroupCompat(group));
                    } else {
                        groupsCompat.add(new NotificationChannelGroupCompat(group, allChannels));
                    }
                }
                return groupsCompat;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get the set of packages that have an enabled notification listener component within them.
     */
    @NonNull
    public static Set<String> getEnabledListenerPackages(@NonNull Context context) {
        final String enabledNotificationListeners = Settings.Secure.getString(
                context.getContentResolver(),
                SETTING_ENABLED_NOTIFICATION_LISTENERS);
        synchronized (sEnabledNotificationListenersLock) {
            // Parse the string again if it is different from the last time this method was called.
            if (enabledNotificationListeners != null
                    && !enabledNotificationListeners.equals(sEnabledNotificationListeners)) {
                final String[] components = enabledNotificationListeners.split(":", -1);
                Set<String> packageNames = new HashSet<String>(components.length);
                for (String component : components) {
                    ComponentName componentName = ComponentName.unflattenFromString(component);
                    if (componentName != null) {
                        packageNames.add(componentName.getPackageName());
                    }
                }
                sEnabledNotificationListenerPackages = packageNames;
                sEnabledNotificationListeners = enabledNotificationListeners;
            }
            return sEnabledNotificationListenerPackages;
        }
    }

    /**
     * Returns whether the calling app can send fullscreen intents.
     *
     * <p>Fullscreen intents were introduced in Android
     * {@link android.os.Build.VERSION_CODES#HONEYCOMB}, where apps could always attach a full
     * screen intent to their notification via
     * {@link Notification.Builder#setFullScreenIntent(PendingIntent, boolean)}}.
     *
     * <p>Android {@link android.os.Build.VERSION_CODES#Q} introduced the
     * {@link android.Manifest.permission#USE_FULL_SCREEN_INTENT}
     * permission, where SystemUI will only show the full screen intent attached to a notification
     * if the permission is declared in the manifest.
     *
     * <p>Starting from Android {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE}, apps
     * may not have permission to use {@link android.Manifest.permission#USE_FULL_SCREEN_INTENT}. If
     * the FSI permission is denied, SystemUI will show the notification as an expanded heads up
     * notification on lockscreen.
     *
     * <p>To request access, add the {@link android.Manifest.permission#USE_FULL_SCREEN_INTENT}
     * permission to your manifest, and use
     * {@link android.provider.Settings#ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT} to send the user
     * to the settings page where they can grant your app the FSI permission.
     */
    public boolean canUseFullScreenIntent() {
        if (Build.VERSION.SDK_INT < 29) {
            return true;
        }
        if (Build.VERSION.SDK_INT < 34) {
            final int permissionState =
                    mContext.checkSelfPermission(Manifest.permission.USE_FULL_SCREEN_INTENT);
            return permissionState == PackageManager.PERMISSION_GRANTED;
        }
        return Api34Impl.canUseFullScreenIntent(mNotificationManager);
    }

    /**
     * Returns true if this notification should use the side channel for delivery.
     */
    private static boolean useSideChannelForNotification(Notification notification) {
        Bundle extras = NotificationCompat.getExtras(notification);
        return extras != null && extras.getBoolean(EXTRA_USE_SIDE_CHANNEL);
    }

    /**
     * Gets the current notification interruption filter.
     * <p>
     * The interruption filter defines which notifications are allowed to
     * interrupt the user (e.g. via sound &amp; vibration) and is applied
     * globally.
     */
    public @InterruptionFilter int getCurrentInterruptionFilter() {
        if (Build.VERSION.SDK_INT < 23) {
            // Prior to API 23, Interruption Filters were not implemented, so we return
            // unknown filter level.
            return INTERRUPTION_FILTER_UNKNOWN;
        }
        return Api23Impl.getCurrentInterruptionFilter(mNotificationManager);
    }

    /**
     * Push a notification task for distribution to notification side channels.
     */
    private void pushSideChannelQueue(Task task) {
        synchronized (sLock) {
            if (sSideChannelManager == null) {
                sSideChannelManager = new SideChannelManager(mContext.getApplicationContext());
            }
            sSideChannelManager.queueTask(task);
        }
    }

    /**
     * Helper class to manage a queue of pending tasks to send to notification side channel
     * listeners.
     */
    private static class SideChannelManager implements Handler.Callback, ServiceConnection {
        private static final int MSG_QUEUE_TASK = 0;
        private static final int MSG_SERVICE_CONNECTED = 1;
        private static final int MSG_SERVICE_DISCONNECTED = 2;
        private static final int MSG_RETRY_LISTENER_QUEUE = 3;

        private final Context mContext;
        private final HandlerThread mHandlerThread;
        private final Handler mHandler;
        private final Map<ComponentName, ListenerRecord> mRecordMap =
                new HashMap<ComponentName, ListenerRecord>();
        private Set<String> mCachedEnabledPackages = new HashSet<String>();

        SideChannelManager(Context context) {
            mContext = context;
            mHandlerThread = new HandlerThread("NotificationManagerCompat");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper(), this);
        }

        /**
         * Queue a new task to be sent to all listeners. This function can be called
         * from any thread.
         */
        public void queueTask(Task task) {
            mHandler.obtainMessage(MSG_QUEUE_TASK, task).sendToTarget();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_QUEUE_TASK:
                    handleQueueTask((Task) msg.obj);
                    return true;
                case MSG_SERVICE_CONNECTED:
                    ServiceConnectedEvent event = (ServiceConnectedEvent) msg.obj;
                    handleServiceConnected(event.componentName, event.iBinder);
                    return true;
                case MSG_SERVICE_DISCONNECTED:
                    handleServiceDisconnected((ComponentName) msg.obj);
                    return true;
                case MSG_RETRY_LISTENER_QUEUE:
                    handleRetryListenerQueue((ComponentName) msg.obj);
                    return true;
            }
            return false;
        }

        private void handleQueueTask(Task task) {
            updateListenerMap();
            for (ListenerRecord record : mRecordMap.values()) {
                record.taskQueue.add(task);
                processListenerQueue(record);
            }
        }

        private void handleServiceConnected(ComponentName componentName, IBinder iBinder) {
            ListenerRecord record = mRecordMap.get(componentName);
            if (record != null) {
                record.service = INotificationSideChannel.Stub.asInterface(iBinder);
                record.retryCount = 0;
                processListenerQueue(record);
            }
        }

        private void handleServiceDisconnected(ComponentName componentName) {
            ListenerRecord record = mRecordMap.get(componentName);
            if (record != null) {
                ensureServiceUnbound(record);
            }
        }

        private void handleRetryListenerQueue(ComponentName componentName) {
            ListenerRecord record = mRecordMap.get(componentName);
            if (record != null) {
                processListenerQueue(record);
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connected to service " + componentName);
            }
            mHandler.obtainMessage(MSG_SERVICE_CONNECTED,
                    new ServiceConnectedEvent(componentName, iBinder))
                    .sendToTarget();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Disconnected from service " + componentName);
            }
            mHandler.obtainMessage(MSG_SERVICE_DISCONNECTED, componentName).sendToTarget();
        }

        /**
         * Check the current list of enabled listener packages and update the records map
         * accordingly.
         */
        @SuppressWarnings("deprecation")
        private void updateListenerMap() {
            Set<String> enabledPackages = getEnabledListenerPackages(mContext);
            if (enabledPackages.equals(mCachedEnabledPackages)) {
                // Short-circuit when the list of enabled packages has not changed.
                return;
            }
            mCachedEnabledPackages = enabledPackages;
            List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentServices(
                    new Intent().setAction(ACTION_BIND_SIDE_CHANNEL), 0);
            Set<ComponentName> enabledComponents = new HashSet<ComponentName>();
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (!enabledPackages.contains(resolveInfo.serviceInfo.packageName)) {
                    continue;
                }
                ComponentName componentName = new ComponentName(
                        resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                if (resolveInfo.serviceInfo.permission != null) {
                    Log.w(TAG, "Permission present on component " + componentName
                            + ", not adding listener record.");
                    continue;
                }
                enabledComponents.add(componentName);
            }
            // Ensure all enabled components have a record in the listener map.
            for (ComponentName componentName : enabledComponents) {
                if (!mRecordMap.containsKey(componentName)) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Adding listener record for " + componentName);
                    }
                    mRecordMap.put(componentName, new ListenerRecord(componentName));
                }
            }
            // Remove listener records that are no longer for enabled components.
            Iterator<Map.Entry<ComponentName, ListenerRecord>> it =
                    mRecordMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ComponentName, ListenerRecord> entry = it.next();
                if (!enabledComponents.contains(entry.getKey())) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Removing listener record for " + entry.getKey());
                    }
                    ensureServiceUnbound(entry.getValue());
                    it.remove();
                }
            }
        }

        /**
         * Ensure we are already attempting to bind to a service, or start a new binding if not.
         *
         * @return Whether the service bind attempt was successful.
         */
        private boolean ensureServiceBound(ListenerRecord record) {
            if (record.bound) {
                return true;
            }
            Intent intent = new Intent(ACTION_BIND_SIDE_CHANNEL).setComponent(record.componentName);
            record.bound = mContext.bindService(intent, this, Service.BIND_AUTO_CREATE
                    | Service.BIND_WAIVE_PRIORITY);
            if (record.bound) {
                record.retryCount = 0;
            } else {
                Log.w(TAG, "Unable to bind to listener " + record.componentName);
                mContext.unbindService(this);
            }
            return record.bound;
        }

        /**
         * Ensure we have unbound from a service.
         */
        private void ensureServiceUnbound(ListenerRecord record) {
            if (record.bound) {
                mContext.unbindService(this);
                record.bound = false;
            }
            record.service = null;
        }

        /**
         * Schedule a delayed retry to communicate with a listener service.
         * After a maximum number of attempts (with exponential back-off), start
         * dropping pending tasks for this listener.
         */
        private void scheduleListenerRetry(ListenerRecord record) {
            if (mHandler.hasMessages(MSG_RETRY_LISTENER_QUEUE, record.componentName)) {
                return;
            }
            record.retryCount++;
            if (record.retryCount > SIDE_CHANNEL_RETRY_MAX_COUNT) {
                Log.w(TAG, "Giving up on delivering " + record.taskQueue.size() + " tasks to "
                        + record.componentName + " after " + record.retryCount + " retries");
                record.taskQueue.clear();
                return;
            }
            int delayMs = SIDE_CHANNEL_RETRY_BASE_INTERVAL_MS * (1 << (record.retryCount - 1));
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Scheduling retry for " + delayMs + " ms");
            }
            Message msg = mHandler.obtainMessage(MSG_RETRY_LISTENER_QUEUE, record.componentName);
            mHandler.sendMessageDelayed(msg, delayMs);
        }

        /**
         * Perform a processing step for a listener. First check the bind state, then attempt
         * to flush the task queue, and if an error is encountered, schedule a retry.
         */
        private void processListenerQueue(ListenerRecord record) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Processing component " + record.componentName + ", "
                        + record.taskQueue.size() + " queued tasks");
            }
            if (record.taskQueue.isEmpty()) {
                return;
            }
            if (!ensureServiceBound(record) || record.service == null) {
                // Ensure bind has started and that a service interface is ready to use.
                scheduleListenerRetry(record);
                return;
            }
            // Attempt to flush all items in the task queue.
            while (true) {
                Task task = record.taskQueue.peek();
                if (task == null) {
                    break;
                }
                try {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Sending task " + task);
                    }
                    task.send(record.service);
                    record.taskQueue.remove();
                } catch (DeadObjectException e) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Remote service has died: " + record.componentName);
                    }
                    break;
                } catch (RemoteException e) {
                    Log.w(TAG, "RemoteException communicating with " + record.componentName, e);
                    break;
                }
            }
            if (!record.taskQueue.isEmpty()) {
                // Some tasks were not sent, meaning an error was encountered, schedule a retry.
                scheduleListenerRetry(record);
            }
        }

        /** A per-side-channel-service listener state record */
        private static class ListenerRecord {
            final ComponentName componentName;
            /** Whether the service is currently bound to. */
            boolean bound = false;
            /** The service stub provided by onServiceConnected */
            INotificationSideChannel service;
            /** Queue of pending tasks to send to this listener service */
            ArrayDeque<Task> taskQueue = new ArrayDeque<>();
            /** Number of retries attempted while connecting to this listener service */
            int retryCount = 0;

            ListenerRecord(ComponentName componentName) {
                this.componentName = componentName;
            }
        }
    }

    private static class ServiceConnectedEvent {
        final ComponentName componentName;
        final IBinder iBinder;

        ServiceConnectedEvent(ComponentName componentName,
                final IBinder iBinder) {
            this.componentName = componentName;
            this.iBinder = iBinder;
        }
    }

    private interface Task {
        void send(INotificationSideChannel service) throws RemoteException;
    }

    private static class NotifyTask implements Task {
        final String packageName;
        final int id;
        final String tag;
        final Notification notif;

        NotifyTask(String packageName, int id, String tag, Notification notif) {
            this.packageName = packageName;
            this.id = id;
            this.tag = tag;
            this.notif = notif;
        }

        @Override
        public void send(INotificationSideChannel service) throws RemoteException {
            service.notify(packageName, id, tag, notif);
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("NotifyTask[");
            sb.append("packageName:").append(packageName);
            sb.append(", id:").append(id);
            sb.append(", tag:").append(tag);
            sb.append("]");
            return sb.toString();
        }
    }

    private static class CancelTask implements Task {
        final String packageName;
        final int id;
        final String tag;
        final boolean all;

        CancelTask(String packageName) {
            this.packageName = packageName;
            this.id = 0;
            this.tag = null;
            this.all = true;
        }

        CancelTask(String packageName, int id, String tag) {
            this.packageName = packageName;
            this.id = id;
            this.tag = tag;
            this.all = false;
        }

        @Override
        public void send(INotificationSideChannel service) throws RemoteException {
            if (all) {
                service.cancelAll(packageName);
            } else {
                service.cancel(packageName, id, tag);
            }
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("CancelTask[");
            sb.append("packageName:").append(packageName);
            sb.append(", id:").append(id);
            sb.append(", tag:").append(tag);
            sb.append(", all:").append(all);
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationManager} methods which
     * were added in API 23; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() { }

        @DoNotInline
        static List<StatusBarNotification> getActiveNotifications(
                NotificationManager notificationManager) {
            StatusBarNotification[] notifs = notificationManager.getActiveNotifications();
            if (notifs == null) {
                return new ArrayList<>();
            }
            return Arrays.asList(notifs);
        }

        @DoNotInline
        static int getCurrentInterruptionFilter(
                NotificationManager notificationManager) {
            return notificationManager.getCurrentInterruptionFilter();
        }
    }

    /**
     * A class for wrapping calls to {@link NotificationManager} methods which
     * were added in API 24; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() { }

        @DoNotInline
        static boolean areNotificationsEnabled(NotificationManager notificationManager) {
            return notificationManager.areNotificationsEnabled();
        }

        @DoNotInline
        static int getImportance(NotificationManager notificationManager) {
            return notificationManager.getImportance();
        }
    }

    /**
     * A class for wrapping calls to {@link Notification.Builder} methods which
     * were added in API 26; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void createNotificationChannel(NotificationManager notificationManager,
                NotificationChannel channel) {
            notificationManager.createNotificationChannel(channel);
        }

        @DoNotInline
        static NotificationChannel getNotificationChannel(NotificationManager notificationManager,
                String channelId) {
            return notificationManager.getNotificationChannel(channelId);
        }

        @DoNotInline
        static void createNotificationChannels(
                NotificationManager notificationManager, List<NotificationChannel> channels) {
            notificationManager.createNotificationChannels(channels);
        }

        @DoNotInline
        static List<NotificationChannel> getNotificationChannels(
                NotificationManager notificationManager) {
            return notificationManager.getNotificationChannels();
        }

        @DoNotInline
        static void createNotificationChannelGroup(NotificationManager notificationManager,
                NotificationChannelGroup group) {
            notificationManager.createNotificationChannelGroup(group);
        }

        @DoNotInline
        static void createNotificationChannelGroups(NotificationManager notificationManager,
                List<NotificationChannelGroup> groups) {
            notificationManager.createNotificationChannelGroups(groups);
        }

        @DoNotInline
        static List<NotificationChannelGroup> getNotificationChannelGroups(
                NotificationManager notificationManager) {
            return notificationManager.getNotificationChannelGroups();
        }

        @DoNotInline
        static void deleteNotificationChannel(NotificationManager notificationManager,
                String channelId) {
            notificationManager.deleteNotificationChannel(channelId);
        }

        @DoNotInline
        static void deleteNotificationChannelGroup(NotificationManager notificationManager,
                String groupId) {
            notificationManager.deleteNotificationChannelGroup(groupId);
        }


        @DoNotInline
        static String getId(NotificationChannel notificationChannel) {
            return notificationChannel.getId();
        }

        @DoNotInline
        static String getId(NotificationChannelGroup notificationChannelGroup) {
            return notificationChannelGroup.getId();
        }
    }

    /**
     * A class for wrapping calls to {@link Notification.Builder} methods which
     * were added in API 28; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() { }

        @DoNotInline
        static NotificationChannelGroup getNotificationChannelGroup(
                NotificationManager notificationManager, String channelGroupId) {
            return notificationManager.getNotificationChannelGroup(channelGroupId);
        }
    }

    /**
     * A class for wrapping calls to {@link Notification.Builder} methods which
     * were added in API 30; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() { }

        @DoNotInline
        static String getParentChannelId(NotificationChannel notificationChannel) {
            return notificationChannel.getParentChannelId();
        }

        @DoNotInline
        static NotificationChannel getNotificationChannel(NotificationManager notificationManager,
                String channelId, String conversationId) {
            return notificationManager.getNotificationChannel(channelId, conversationId);
        }
    }

    /**
     * A class for wrapping calls to {@link Notification.Builder} methods which
     * were added in API 34; these calls must be wrapped to avoid performance issues.
     * See the UnsafeNewApiCall lint rule for more details.
     */
    @RequiresApi(34)
    static class Api34Impl {
        private Api34Impl() { }

        @DoNotInline
        static boolean canUseFullScreenIntent(NotificationManager notificationManager) {
            return notificationManager.canUseFullScreenIntent();
        }
    }
}
