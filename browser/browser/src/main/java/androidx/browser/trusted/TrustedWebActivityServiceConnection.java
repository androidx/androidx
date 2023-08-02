/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.trusted;

import android.app.Notification;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.customtabs.trusted.ITrustedWebActivityCallback;
import android.support.customtabs.trusted.ITrustedWebActivityService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * TrustedWebActivityServiceConnection is used by a Trusted Web Activity provider to wrap calls to
 * the {@link TrustedWebActivityService} in the client app.
 * All of these calls except {@link #getComponentName()} forward over IPC
 * to corresponding calls on {@link TrustedWebActivityService}, eg {@link #getSmallIconId()}
 * forwards to {@link TrustedWebActivityService#onGetSmallIconId()}.
 * <p>
 * These IPC calls are synchronous, though the {@link TrustedWebActivityService} method may hit the
 * disk. Therefore it is recommended to call them on a background thread (without StrictMode).
 */
public final class TrustedWebActivityServiceConnection {
    // Inputs.
    private static final String KEY_PLATFORM_TAG =
            "android.support.customtabs.trusted.PLATFORM_TAG";
    private static final String KEY_PLATFORM_ID =
            "android.support.customtabs.trusted.PLATFORM_ID";
    private static final String KEY_NOTIFICATION =
            "android.support.customtabs.trusted.NOTIFICATION";
    private static final String KEY_CHANNEL_NAME =
            "android.support.customtabs.trusted.CHANNEL_NAME";
    private static final String KEY_ACTIVE_NOTIFICATIONS =
            "android.support.customtabs.trusted.ACTIVE_NOTIFICATIONS";

    // Outputs.
    private static final String KEY_NOTIFICATION_SUCCESS =
            "android.support.customtabs.trusted.NOTIFICATION_SUCCESS";

    private final ITrustedWebActivityService mService;
    private final ComponentName mComponentName;

    TrustedWebActivityServiceConnection(@NonNull ITrustedWebActivityService service,
            @NonNull ComponentName componentName) {
        mService = service;
        mComponentName = componentName;
    }

    /**
     * Checks whether notifications are enabled.
     * @param channelName The name of the channel to check enabled status. Only used on Android O+.
     * @return Whether notifications or the notification channel is blocked for the client app.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    public boolean areNotificationsEnabled(@NonNull String channelName) throws RemoteException {
        Bundle args = new NotificationsEnabledArgs(channelName).toBundle();
        return ResultArgs.fromBundle(mService.areNotificationsEnabled(args)).success;
    }

    /**
     * Requests a notification be shown.
     * @param platformTag The tag to identify the notification.
     * @param platformId The id to identify the notification.
     * @param notification The notification.
     * @param channel The name of the channel in the Trusted Web Activity client app to display the
     *                notification on.
     * @return Whether notifications or the notification channel are blocked for the client app.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    public boolean notify(@NonNull String platformTag, int platformId,
            @NonNull Notification notification, @NonNull String channel) throws RemoteException {
        Bundle args = new NotifyNotificationArgs(platformTag, platformId, notification, channel)
                .toBundle();
        return ResultArgs.fromBundle(mService.notifyNotificationWithChannel(args)).success;
    }

    /**
     * Requests a notification be cancelled.
     * @param platformTag The tag to identify the notification.
     * @param platformId The id to identify the notification.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    public void cancel(@NonNull String platformTag, int platformId) throws RemoteException {
        Bundle args = new CancelNotificationArgs(platformTag, platformId).toBundle();
        mService.cancelNotification(args);
    }

    /**
     * Gets the notifications shown by the Trusted Web Activity client. Can only be called on
     * Android M and above.
     * @return An StatusBarNotification[] as a Parcelable[]. This is so this code can compile for
     *         Jellybean (even if it must not be called for pre-Marshmallow).
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     * @throws IllegalStateException If called on Android pre-M.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(Build.VERSION_CODES.M)
    @NonNull
    public Parcelable[] getActiveNotifications() throws RemoteException {
        Bundle notifications = mService.getActiveNotifications();
        return ActiveNotificationsArgs.fromBundle(notifications).notifications;
    }

    /**
     * Requests an Android resource id to be used for the notification small icon.
     * @return An Android resource id for the notification small icon. -1 if non found.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    public int getSmallIconId() throws RemoteException {
        return mService.getSmallIconId();
    }

    /**
     * Requests a bitmap of a small icon to be used for the notification
     * small icon. The bitmap is decoded on the side of Trusted Web Activity client using
     * the resource id from {@link TrustedWebActivityService#onGetSmallIconId}.
     * @return A {@link Bitmap} to be used for the small icon.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public Bitmap getSmallIconBitmap() throws RemoteException {
        return mService.getSmallIconBitmap()
                .getParcelable(TrustedWebActivityService.KEY_SMALL_ICON_BITMAP);
    }

    /**
     * Gets the {@link ComponentName} of the connected Trusted Web Activity client app.
     * @return The Trusted Web Activity client app component name.
     */
    @NonNull
    public ComponentName getComponentName() {
        return mComponentName;
    }

    /**
     * Passes a free-form command to the client.
     * {@link TrustedWebActivityService#onExtraCommand} will be called.
     * The client may not know how to deal with the command, in which case {@code null} may be
     * returned.
     *
     * @param commandName Name of the command to execute.
     * @param args        Arguments to the command.
     * @param callback    Callback that may be used to return data, depending on the command.
     * @return The result {@link Bundle}, or {@code null} if the command could not be executed.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws SecurityException If verification with the TrustedWebActivityService fails.
     */
    @SuppressWarnings("NullAway")  // TODO: b/142938599
    @Nullable
    public Bundle sendExtraCommand(@NonNull String commandName, @NonNull Bundle args,
            @Nullable TrustedWebActivityCallback callback) throws RemoteException {
        ITrustedWebActivityCallback callbackBinder = wrapCallback(callback);
        IBinder binder = callbackBinder == null ? null : callbackBinder.asBinder();
        return mService.extraCommand(commandName, args, binder);
    }

    static class NotifyNotificationArgs {
        public final String platformTag;
        public final int platformId;
        public final Notification notification;
        public final String channelName;

        NotifyNotificationArgs(String platformTag, int platformId,
                Notification notification, String channelName) {
            this.platformTag = platformTag;
            this.platformId = platformId;
            this.notification = notification;
            this.channelName = channelName;
        }

        @SuppressWarnings("deprecation")
        public static NotifyNotificationArgs fromBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_PLATFORM_TAG);
            ensureBundleContains(bundle, KEY_PLATFORM_ID);
            ensureBundleContains(bundle, KEY_NOTIFICATION);
            ensureBundleContains(bundle, KEY_CHANNEL_NAME);

            return new NotifyNotificationArgs(bundle.getString(KEY_PLATFORM_TAG),
                    bundle.getInt(KEY_PLATFORM_ID),
                    (Notification) bundle.getParcelable(KEY_NOTIFICATION),
                    bundle.getString(KEY_CHANNEL_NAME));
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putString(KEY_PLATFORM_TAG, platformTag);
            args.putInt(KEY_PLATFORM_ID, platformId);
            args.putParcelable(KEY_NOTIFICATION, notification);
            args.putString(KEY_CHANNEL_NAME, channelName);
            return args;
        }
    }

    static class CancelNotificationArgs {
        public final String platformTag;
        public final int platformId;

        CancelNotificationArgs(String platformTag, int platformId) {
            this.platformTag = platformTag;
            this.platformId = platformId;
        }

        public static CancelNotificationArgs fromBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_PLATFORM_TAG);
            ensureBundleContains(bundle, KEY_PLATFORM_ID);

            return new CancelNotificationArgs(bundle.getString(KEY_PLATFORM_TAG),
                    bundle.getInt(KEY_PLATFORM_ID));
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putString(KEY_PLATFORM_TAG, platformTag);
            args.putInt(KEY_PLATFORM_ID, platformId);
            return args;
        }
    }

    static class ResultArgs {
        public final boolean success;

        ResultArgs(boolean success) {
            this.success = success;
        }

        public static ResultArgs fromBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_NOTIFICATION_SUCCESS);
            return new ResultArgs(bundle.getBoolean(KEY_NOTIFICATION_SUCCESS));
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putBoolean(KEY_NOTIFICATION_SUCCESS, success);
            return args;
        }
    }

    static class ActiveNotificationsArgs {
        public final Parcelable[] notifications;

        ActiveNotificationsArgs(Parcelable[] notifications) {
            this.notifications = notifications;
        }

        @SuppressWarnings("deprecation")
        public static ActiveNotificationsArgs fromBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_ACTIVE_NOTIFICATIONS);
            return new ActiveNotificationsArgs(bundle.getParcelableArray(KEY_ACTIVE_NOTIFICATIONS));
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putParcelableArray(KEY_ACTIVE_NOTIFICATIONS, notifications);
            return args;
        }
    }

    static class NotificationsEnabledArgs {
        public final String channelName;

        NotificationsEnabledArgs(String channelName) {
            this.channelName = channelName;
        }

        public static NotificationsEnabledArgs fromBundle(Bundle bundle) {
            ensureBundleContains(bundle, KEY_CHANNEL_NAME);
            return new NotificationsEnabledArgs(bundle.getString(KEY_CHANNEL_NAME));
        }

        public Bundle toBundle() {
            Bundle args = new Bundle();
            args.putString(KEY_CHANNEL_NAME, channelName);
            return args;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void ensureBundleContains(Bundle args, String key) {
        if (args.containsKey(key)) return;
        throw new IllegalArgumentException("Bundle must contain " + key);
    }

    @Nullable
    private static ITrustedWebActivityCallback wrapCallback(
            @Nullable TrustedWebActivityCallback callback) {
        if (callback == null) return null;
        return new ITrustedWebActivityCallback.Stub() {
            @Override
            public void onExtraCallback(String callbackName, Bundle args)
                    throws RemoteException {
                callback.onExtraCallback(callbackName, args);
            }
        };
    }
}
