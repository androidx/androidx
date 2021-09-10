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

package androidx.car.app.notification;

import static androidx.car.app.model.CarColor.TYPE_BLUE;
import static androidx.car.app.model.CarColor.TYPE_CUSTOM;
import static androidx.car.app.model.CarColor.TYPE_DEFAULT;
import static androidx.car.app.model.CarColor.TYPE_GREEN;
import static androidx.car.app.model.CarColor.TYPE_PRIMARY;
import static androidx.car.app.model.CarColor.TYPE_RED;
import static androidx.car.app.model.CarColor.TYPE_SECONDARY;
import static androidx.car.app.model.CarColor.TYPE_YELLOW;
import static androidx.car.app.utils.CommonUtils.isAutomotiveOS;

import static java.util.Objects.requireNonNull;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.R;
import androidx.car.app.model.CarColor;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationChannelGroupCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A manager for car apps to send notifications.
 *
 * This class wraps a {@link NotificationManagerCompat} to manage the actual sending of the
 * {@link Notification}.
 */
public final class CarNotificationManager {
    @NonNull
    private final Context mContext;
    @NonNull
    private final NotificationManagerCompat mNotificationManagerCompat;
    @ColorInt
    @Nullable
    private final Integer mPrimaryColor;
    @ColorInt
    @Nullable
    private final Integer mPrimaryColorDark;
    @ColorInt
    @Nullable
    private final Integer mSecondaryColor;
    @ColorInt
    @Nullable
    private final Integer mSecondaryColorDark;

    /**
     * Returns a {@link CarNotificationManager} instance for a provided context.
     *
     * @throws NullPointerException if {@code context} is {@code null}
     */
    @NonNull
    public static CarNotificationManager from(@NonNull Context context) {
        return new CarNotificationManager(requireNonNull(context));
    }

    /**
     * Cancels a previously shown notification.
     *
     * @see NotificationManagerCompat#cancel(int)
     */
    public void cancel(int id) {
        mNotificationManagerCompat.cancel(id);
    }

    /**
     * Cancels a previously shown notification.
     *
     * @see NotificationManagerCompat#cancel(String, int)
     */
    public void cancel(@Nullable String tag, int id) {
        mNotificationManagerCompat.cancel(tag, id);
    }

    /**
     * Cancels all previously shown notifications.
     *
     * @see NotificationManagerCompat#cancelAll()
     */
    public void cancelAll() {
        mNotificationManagerCompat.cancelAll();
    }

    /**
     * Posts a notification.
     *
     * <p>Callers are expected to extend the notification with CarAppExtender to ensure the
     * notification will show up on all car environments. This method will extend the
     * notification if it is not already extended for the car using a CarAppExtender.
     *
     * @throws NullPointerException if {@code notification} is {@code null}
     *
     * @see NotificationManagerCompat#notify(int, Notification)
     */
    public void notify(int id, @NonNull NotificationCompat.Builder notification) {
        mNotificationManagerCompat.notify(id, updateForCar(requireNonNull(notification)));
    }

    /**
     * Post a notification to be shown in the status bar, stream, etc.
     *
     * <p>Callers are expected to extend the notification with CarAppExtender to ensure the
     * notification will show up on all car environments. This method will extend the
     * notification if it is not already extended for the car using a CarAppExtender.
     *
     * @param tag          the string identifier for a notification. Can be {@code null}.
     * @param id           the ID of the notification. The pair (tag, id) must be unique within
     *                     your app.
     * @param notification the notification to post to the system
     *
     * @throws NullPointerException if notification is {@code null}
     */
    public void notify(@Nullable String tag, int id,
            @NonNull NotificationCompat.Builder notification) {
        mNotificationManagerCompat.notify(tag, id, updateForCar(requireNonNull(notification)));
    }

    /**
     * Returns whether notifications from the calling package are not blocked.
     *
     * @see NotificationManagerCompat#areNotificationsEnabled()
     */
    public boolean areNotificationsEnabled() {
        return mNotificationManagerCompat.areNotificationsEnabled();
    }

    /**
     * Returns the user specified importance for notifications from the calling package.
     *
     * @see NotificationManagerCompat#getImportance()
     */
    public int getImportance() {
        return mNotificationManagerCompat.getImportance();
    }

    /**
     * Creates a notification channel that notifications can be posted to.
     *
     * @throws NullPointerException if {@code channel} is {@code null}
     *
     * @see NotificationManagerCompat#createNotificationChannel(NotificationChannelCompat)
     */
    public void createNotificationChannel(@NonNull NotificationChannelCompat channel) {
        mNotificationManagerCompat.createNotificationChannel(requireNonNull(channel));
    }

    /**
     * Creates a group container for {@link NotificationChannel} objects.
     *
     * @throws NullPointerException if {@code group} is {@code null}
     *
     * @see NotificationManagerCompat#createNotificationChannelGroup(NotificationChannelGroupCompat)
     */
    public void createNotificationChannelGroup(@NonNull NotificationChannelGroupCompat group) {
        mNotificationManagerCompat.createNotificationChannelGroup(requireNonNull(group));
    }

    /**
     * Creates multiple notification channels that different notifications can be posted to. See
     * {@link #createNotificationChannel(NotificationChannelCompat)}.
     *
     * @throws NullPointerException if {@code channels} is {@code null}
     *
     * @see NotificationManagerCompat#createNotificationChannelsCompat(List)
     */
    public void createNotificationChannels(
            @NonNull List<NotificationChannelCompat> channels) {
        mNotificationManagerCompat.createNotificationChannelsCompat(requireNonNull(channels));
    }

    /**
     * Creates multiple notification channel groups. See
     * {@link #createNotificationChannelGroup(NotificationChannelGroupCompat)}.
     *
     * @throws NullPointerException if {@code groups} is {@code null}
     *
     * @see NotificationManagerCompat#createNotificationChannelGroupsCompat(List)
     */
    public void createNotificationChannelGroups(
            @NonNull List<NotificationChannelGroupCompat> groups) {
        mNotificationManagerCompat.createNotificationChannelGroupsCompat(requireNonNull(groups));
    }

    /**
     * Deletes the given notification channel.
     *
     * @throws NullPointerException if {@code channelId} is {@code null}
     *
     * @see NotificationManagerCompat#deleteNotificationChannel(String)
     */
    public void deleteNotificationChannel(@NonNull String channelId) {
        mNotificationManagerCompat.deleteNotificationChannel(requireNonNull(channelId));
    }

    /**
     * Deletes the given notification channel group, and all notification channels that
     * belong to it.
     *
     * @throws NullPointerException if {@code groupId} is {@code null}
     *
     * @see NotificationManagerCompat#deleteNotificationChannelGroup(String)
     */
    public void deleteNotificationChannelGroup(@NonNull String groupId) {
        mNotificationManagerCompat.deleteNotificationChannelGroup(requireNonNull(groupId));
    }

    /**
     * Deletes notification channels for which ids are NOT given.
     *
     * @throws NullPointerException if {@code channelIds} is {@code null}
     *
     * @see NotificationManagerCompat#deleteUnlistedNotificationChannels(Collection)
     */
    public void deleteUnlistedNotificationChannels(@NonNull Collection<String> channelIds) {
        mNotificationManagerCompat.deleteUnlistedNotificationChannels(requireNonNull(channelIds));
    }

    /**
     * Returns the notification channel settings for a given channel id.
     *
     * @throws NullPointerException if {@code channelId} is {@code null}
     *
     * @see NotificationManagerCompat#getNotificationChannelCompat(String)
     */
    @Nullable
    public NotificationChannelCompat getNotificationChannel(@NonNull String channelId) {
        return mNotificationManagerCompat.getNotificationChannelCompat(requireNonNull(channelId));
    }

    /**
     * Returns the notification channel settings for a given channel and
     * {@link ShortcutInfo#getId() conversation id}.
     *
     * @throws NullPointerException if either {@code channelId} of {@code conversationId} are {@code
     *                              null}
     *
     * @see NotificationManagerCompat#getNotificationChannelCompat(String, String)
     */
    @Nullable
    public NotificationChannelCompat getNotificationChannel(@NonNull String channelId,
            @NonNull String conversationId) {
        return mNotificationManagerCompat.getNotificationChannelCompat(requireNonNull(channelId),
                requireNonNull(conversationId));
    }

    /**
     * Returns the notification channel group settings for a given channel group id.
     *
     * @throws NullPointerException if {@code channelGroupId} is {@code null}
     *
     * @see NotificationManagerCompat#getNotificationChannelGroupCompat(String)
     */
    @Nullable
    public NotificationChannelGroupCompat getNotificationChannelGroup(
            @NonNull String channelGroupId) {
        return mNotificationManagerCompat.getNotificationChannelGroupCompat(
                requireNonNull(channelGroupId));
    }

    /**
     * Returns all notification channels belonging to the calling app
     * or an empty list on older SDKs which don't support Notification Channels.
     *
     * @see NotificationManagerCompat#getNotificationChannelsCompat()
     */
    @NonNull
    public List<NotificationChannelCompat> getNotificationChannels() {
        return mNotificationManagerCompat.getNotificationChannelsCompat();
    }

    /**
     * Returns all notification channel groups belonging to the calling app
     * or an empty list on older SDKs which don't support Notification Channels.
     *
     * @see NotificationManagerCompat#getNotificationChannelGroupsCompat()
     */
    @NonNull
    public List<NotificationChannelGroupCompat> getNotificationChannelGroups() {
        return mNotificationManagerCompat.getNotificationChannelGroupsCompat();
    }

    /**
     * Get the set of packages that have an enabled notification listener component within them.
     *
     * @throws NullPointerException if {@code context} is {@code null}
     *
     * @see NotificationManagerCompat#getEnabledListenerPackages(Context)
     */
    @NonNull
    public static Set<String> getEnabledListenerPackages(@NonNull Context context) {
        return NotificationManagerCompat.getEnabledListenerPackages(requireNonNull(context));
    }

    @VisibleForTesting
    @NonNull
    Notification updateForCar(@NonNull NotificationCompat.Builder notification) {
        if (isAutomotiveOS(mContext)) {
            return updateForAutomotive(notification);
        } else if (!CarAppExtender.isExtended(notification.build())) {
            notification.extend(new CarAppExtender.Builder().build());
        }
        return notification.build();
    }

    private Notification updateForAutomotive(@NonNull NotificationCompat.Builder notification) {
        if (Build.VERSION.SDK_INT < 29) {
            throw new UnsupportedOperationException(
                    "Not supported for Automotive OS before API 29.");
        }

        CarAppExtender carAppExtender = new CarAppExtender(notification.build());

        // CarAppExtender only supports adding Icon to Notification.Action via resource.
        // To convert the Notification.Action in the extender back to NotificationCompat.Action,
        // we need to rely on the Icon.getResId() API which is added in API 28. The other ctors
        // for NotificationCompat.Action would throw an exception because the Icon was created
        // without a res package. See CarAppExtender.Builder.addAction(...) for reference.
        Api29Impl.convertActionsToCompatActions(notification, carAppExtender.getActions());

        CarColor color = carAppExtender.getColor();
        if (color != null) {
            @ColorInt Integer colorInt = getColorInt(color);
            if (colorInt != null) {
                notification.setColorized(true);
                notification.setColor(colorInt);
            }
        }

        PendingIntent contentIntent = carAppExtender.getContentIntent();
        if (contentIntent != null) {
            notification.setContentIntent(contentIntent);
        }

        CharSequence contentTitle = carAppExtender.getContentTitle();
        if (contentTitle != null) {
            notification.setContentTitle(contentTitle);
        }

        CharSequence contentText = carAppExtender.getContentText();
        if (contentText != null) {
            notification.setContentText(contentText);
        }

        PendingIntent deleteIntent = carAppExtender.getDeleteIntent();
        if (deleteIntent != null) {
            notification.setDeleteIntent(deleteIntent);
        }

        String channelId = carAppExtender.getChannelId();
        if (channelId != null) {
            notification.setChannelId(channelId);
        }

        Bitmap largeIcon = carAppExtender.getLargeIcon();
        if (largeIcon != null) {
            notification.setLargeIcon(largeIcon);
        }

        int smallIconRes = carAppExtender.getSmallIcon();
        if (smallIconRes != Resources.ID_NULL) {
            notification.setSmallIcon(smallIconRes);
        }

        return notification.build();
    }

    @VisibleForTesting
    @ColorInt
    @Nullable
    Integer getColorInt(CarColor carColor) {
        boolean isDarkMode =
                (mContext.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        switch (carColor.getType()) {
            case TYPE_CUSTOM:
                return isDarkMode ? carColor.getColorDark() : carColor.getColor();
            case TYPE_PRIMARY:
                return isDarkMode ? mPrimaryColorDark : mPrimaryColor;
            case TYPE_SECONDARY:
                return isDarkMode ? mSecondaryColorDark : mSecondaryColor;
            case TYPE_RED:
                return mContext.getColor(R.color.carColorRed);
            case TYPE_GREEN:
                return mContext.getColor(R.color.carColorGreen);
            case TYPE_BLUE:
                return mContext.getColor(R.color.carColorBlue);
            case TYPE_YELLOW:
                return mContext.getColor(R.color.carColorYellow);
            case TYPE_DEFAULT:
            default:
                return null;
        }
    }

    @StyleRes
    private static int loadThemeId(Context context) {
        int theme = Resources.ID_NULL;
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return theme;
        }

        if (applicationInfo.metaData != null) {
            theme = applicationInfo.metaData.getInt("androidx.car.app.theme");
        }

        return theme;
    }

    @ColorInt
    @Nullable
    private static Integer getColor(int resId, Resources.Theme appTheme) {
        @ColorInt Integer color = null;
        if (resId != Resources.ID_NULL) {
            int[] attr = {resId};
            TypedArray ta = appTheme.obtainStyledAttributes(attr);
            color = ta.getColor(0, 0);
            ta.recycle();
        }
        return color;
    }

    private CarNotificationManager(@NonNull Context context) {
        mContext = requireNonNull(context);
        mNotificationManagerCompat = NotificationManagerCompat.from(context);

        Context themeableContext = mContext.createConfigurationContext(
                context.getResources().getConfiguration());

        @StyleRes int themeId = loadThemeId(context);
        if (themeId != Resources.ID_NULL) {
            themeableContext.setTheme(themeId);
        }

        Resources.Theme theme = themeableContext.getTheme();
        Resources themedResources = theme.getResources();

        int carColorPrimary = themedResources.getIdentifier("carColorPrimary", "attr",
                context.getPackageName());
        mPrimaryColor = getColor(carColorPrimary, theme);

        int carColorPrimaryDark = themedResources.getIdentifier("carColorPrimaryDark", "attr",
                context.getPackageName());
        mPrimaryColorDark = getColor(carColorPrimaryDark, theme);

        int carColorSecondary = themedResources.getIdentifier("carColorSecondary", "attr",
                context.getPackageName());
        mSecondaryColor = getColor(carColorSecondary, theme);

        int carColorSecondaryDark = themedResources.getIdentifier("carColorSecondaryDark", "attr",
                context.getPackageName());
        mSecondaryColorDark = getColor(carColorSecondaryDark, theme);
    }

    @RequiresApi(28)
    private static final class Api29Impl {
        /**
         * Convert the list of {@link Notification.Action} to {@link NotificationCompat.Action} and
         * add them to the input {@code notification}.
         */
        @DoNotInline
        static void convertActionsToCompatActions(@NonNull NotificationCompat.Builder notification,
                @NonNull List<Notification.Action> actions) {
            if (actions.isEmpty()) {
                return;
            }

            notification.clearActions();
            for (Notification.Action action : actions) {
                notification.addAction(fromAndroidAction(action));
            }
        }

        /**
         * Creates an {@link NotificationCompat.Action} from the {@code action} provided.
         *
         * <p>This is copied from {@link NotificationCompat.Action}'s fromAndroidAction method which
         * is not visible outside the package.
         */
        private static NotificationCompat.Action fromAndroidAction(
                @NonNull Notification.Action action) {
            return new NotificationCompat.Action(action.getIcon() == null ? 0 :
                    action.getIcon().getResId(),
                    action.title,
                    action.actionIntent);
        }

        private Api29Impl() {
        }
    }
}
