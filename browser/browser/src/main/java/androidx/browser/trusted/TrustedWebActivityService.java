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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.customtabs.trusted.ITrustedWebActivityService;

import androidx.annotation.BinderThread;
import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.browser.trusted.TrustedWebActivityServiceConnection.ActiveNotificationsArgs;
import androidx.browser.trusted.TrustedWebActivityServiceConnection.CancelNotificationArgs;
import androidx.browser.trusted.TrustedWebActivityServiceConnection.NotificationsEnabledArgs;
import androidx.browser.trusted.TrustedWebActivityServiceConnection.NotifyNotificationArgs;
import androidx.browser.trusted.TrustedWebActivityServiceConnection.ResultArgs;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;

/**
 * The TrustedWebActivityService lives in a client app and serves requests from a Trusted Web
 * Activity provider. At present it only serves requests to do with notifications.
 * <p>
 * When the provider receives a notification from a scope that is associated with a Trusted Web
 * Activity client app, it will attempt to connect to a TrustedWebActivityService and forward calls.
 * This allows the client app to display the notifications itself, meaning it is attributable to the
 * client app and is managed by notification permissions of the client app, not the provider.
 * <p>
 * TrustedWebActivityService is usable as it is, by adding the following to your AndroidManifest:
 *
 * <pre>
 * {@code
 * <service
 *     android:name="androidx.browser.trusted.TrustedWebActivityService"
 *     android:enabled="true"
 *     android:exported="true">
 *
 *     <meta-data android:name="android.support.customtabs.trusted.SMALL_ICON"
 *         android:resource="@drawable/ic_notification_icon" />
 *
 *     <intent-filter>
 *         <action android:name="android.support.customtabs.trusted.TRUSTED_WEB_ACTIVITY_SERVICE"/>
 *         <category android:name="android.intent.category.DEFAULT"/>
 *     </intent-filter>
 * </service>
 * }
 * </pre>
 *
 * The SMALL_ICON resource should point to a drawable to be used for the notification's small icon.
 * <p>
 * Alternatively for greater customization, TrustedWebActivityService can be extended and
 * overridden. In this case the manifest entry should be updated to point to the extending class.
 * <p>
 * As this is an AIDL Service, calls may come in from different Binder threads, so overriding
 * implementations need to be thread safe [1].
 * <p>
 * For security, the TrustedWebActivityService will check that whatever connects to it matches the
 * {@link Token} stored in the {@link TokenStore} returned by {@link #getTokenStore}.
 * This is because we don't want to allow any app on the users device to connect to this Service
 * be able to make it display notifications.
 *
 * [1]: https://developer.android.com/guide/components/aidl.html
 */
public abstract class TrustedWebActivityService extends Service {
    /** An Intent Action used by the provider to find the TrustedWebActivityService or subclass. */
    @SuppressLint({
            "ActionValue",  // This value was being used before being moved into AndroidX.
            "ServiceName",  // This variable is an Action, but Metalava thinks it's a Service.
    })
    public static final String ACTION_TRUSTED_WEB_ACTIVITY_SERVICE =
            "android.support.customtabs.trusted.TRUSTED_WEB_ACTIVITY_SERVICE";

    /** The Android Manifest meta-data name to specify a small icon id to use. */
    public static final String META_DATA_NAME_SMALL_ICON =
            "android.support.customtabs.trusted.SMALL_ICON";

    /**
     * The key to use to store a Bitmap to return from the {@link #onGetSmallIconBitmap()} method.
     */
    public static final String KEY_SMALL_ICON_BITMAP =
            "android.support.customtabs.trusted.SMALL_ICON_BITMAP";

    /**
     * The key to use to store a boolean in the returns bundle of {@link #onExtraCommand} method,
     * to indicate whether the command is executed successfully.
     */
    public static final String KEY_SUCCESS = "androidx.browser.trusted.SUCCESS";

    /** Used as a return value of {@link #onGetSmallIconId} when the icon is not provided. */
    public static final int SMALL_ICON_NOT_SET = -1;

    private NotificationManager mNotificationManager;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mVerifiedUid = -1;

    private final ITrustedWebActivityService.Stub mBinder =
            new ITrustedWebActivityService.Stub() {
        @Override
        public Bundle areNotificationsEnabled(Bundle bundle) {
            checkCaller();

            NotificationsEnabledArgs args = NotificationsEnabledArgs.fromBundle(bundle);
            boolean result =
                    TrustedWebActivityService.this.onAreNotificationsEnabled(args.channelName);

            return new ResultArgs(result).toBundle();
        }

        @Override
        public Bundle notifyNotificationWithChannel(Bundle bundle) {
            checkCaller();

            NotifyNotificationArgs args = NotifyNotificationArgs.fromBundle(bundle);

            boolean success = TrustedWebActivityService.this.onNotifyNotificationWithChannel(
                    args.platformTag, args.platformId, args.notification, args.channelName);

            return new ResultArgs(success).toBundle();
        }

        @Override
        public void cancelNotification(Bundle bundle) {
            checkCaller();

            CancelNotificationArgs args = CancelNotificationArgs.fromBundle(bundle);

            TrustedWebActivityService.this.onCancelNotification(args.platformTag, args.platformId);
        }

        @Override
        public Bundle getActiveNotifications() {
            checkCaller();

            return new ActiveNotificationsArgs(
                    TrustedWebActivityService.this.onGetActiveNotifications()).toBundle();
        }

        @Override
        public int getSmallIconId() {
            checkCaller();

            return TrustedWebActivityService.this.onGetSmallIconId();
        }

        @Override
        public Bundle getSmallIconBitmap() {
            checkCaller();

            return TrustedWebActivityService.this.onGetSmallIconBitmap();
        }

        @SuppressWarnings("NullAway")  // TODO: b/142938599
        @Override
        public Bundle extraCommand(String commandName, Bundle args, IBinder callback) {
            checkCaller();

            return TrustedWebActivityService.this.onExtraCommand(commandName, args,
                    TrustedWebActivityCallbackRemote.fromBinder(callback));
        }

        private void checkCaller() {
            if (mVerifiedUid == -1) {
                String[] packages = getPackageManager().getPackagesForUid(getCallingUid());

                if (packages == null) {
                    packages = new String[]{};
                }

                Token verifiedProvider = getTokenStore().load();
                PackageManager pm = getPackageManager();

                if (verifiedProvider != null) {
                    for (String packageName : packages) {
                        if (verifiedProvider.matches(packageName, pm)) {
                            mVerifiedUid = getCallingUid();
                            break;
                        }
                    }
                }
            }

            if (mVerifiedUid == getCallingUid()) return;

            throw new SecurityException("Caller is not verified as Trusted Web Activity provider.");
        }
    };

    /**
     * Called by the system when the service is first created. Do not call this method directly.
     * Overrides must call {@code super.onCreate()}.
     */
    @Override
    @CallSuper
    @MainThread
    public void onCreate() {
        super.onCreate();
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Checks whether notifications are enabled.
     * @param channelName The name of the notification channel to be used on Android O+.
     * @return Whether notifications are enabled.
     */
    @BinderThread
    public boolean onAreNotificationsEnabled(@NonNull String channelName) {
        ensureOnCreateCalled();

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;

        return NotificationApiHelperForO.isChannelEnabled(mNotificationManager,
                channelNameToId(channelName));
    }

    /**
     * Displays a notification.
     * @param platformTag The notification tag, see
     *                    {@link NotificationManager#notify(String, int, Notification)}.
     * @param platformId The notification id, see
     *                   {@link NotificationManager#notify(String, int, Notification)}.
     * @param notification The notification to be displayed, constructed by the provider.
     * @param channelName The name of the notification channel that the notification should be
     *                    displayed on. This method gets or creates a channel from the name and
     *                    modifies the notification to use that channel.
     * @return Whether the notification was successfully displayed (the channel/app may be blocked
     *         by the user).
     */
    @BinderThread
    public boolean onNotifyNotificationWithChannel(@NonNull String platformTag, int platformId,
            @NonNull Notification notification, @NonNull String channelName) {
        ensureOnCreateCalled();

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = channelNameToId(channelName);
            notification = NotificationApiHelperForO.copyNotificationOntoChannel(this,
                    mNotificationManager, notification, channelId, channelName);

            if (!NotificationApiHelperForO.isChannelEnabled(mNotificationManager, channelId)) {
                return false;
            }
        }

        mNotificationManager.notify(platformTag, platformId, notification);
        return true;
    }

    /**
     * Cancels a notification.
     * @param platformTag The notification tag, see
     *                    {@link NotificationManager#cancel(String, int)}.
     * @param platformId The notification id, see
     *                   {@link NotificationManager#cancel(String, int)}.
     */
    @BinderThread
    public void onCancelNotification(@NonNull String platformTag, int platformId) {
        ensureOnCreateCalled();
        mNotificationManager.cancel(platformTag, platformId);
    }

    /**
     * Returns a list of active notifications, essentially calling
     * NotificationManager#getActiveNotifications. The default implementation does not work on
     * pre-Android M.
     * @return An array of StatusBarNotifications as Parcelables.
     *
     * @hide
     */
    @NonNull
    @BinderThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Parcelable[] onGetActiveNotifications() {
        ensureOnCreateCalled();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return NotificationApiHelperForM.getActiveNotifications(mNotificationManager);
        }
        throw new IllegalStateException("onGetActiveNotifications cannot be called pre-M.");
    }

    /**
     * Returns a Bundle containing a bitmap to be use as the small icon for any notifications.
     * @return A Bundle that may contain a Bitmap contained with key {@link #KEY_SMALL_ICON_BITMAP}.
     *         The bundle may be empty if the client app does not provide a small icon.
     */
    @BinderThread
    public @NonNull Bundle onGetSmallIconBitmap() {
        int id = onGetSmallIconId();
        Bundle bundle = new Bundle();
        if (id == SMALL_ICON_NOT_SET) {
            return bundle;
        }
        bundle.putParcelable(KEY_SMALL_ICON_BITMAP,
                BitmapFactory.decodeResource(getResources(), id));
        return bundle;
    }

    /**
     * Returns the Android resource id of a drawable to be used for the small icon of the
     * notification. This is called by the provider as it is constructing the notification so a
     * complete notification can be passed to the client.
     *
     * Default behaviour looks for meta-data with the name {@link #META_DATA_NAME_SMALL_ICON} in
     * service section of the manifest.
     * @return A resource id for the small icon, or {@link #SMALL_ICON_NOT_SET} if not found.
     */
    @BinderThread
    @SuppressWarnings("deprecation")
    public int onGetSmallIconId() {
        try {
            ServiceInfo info = getPackageManager().getServiceInfo(
                    new ComponentName(this, getClass()), PackageManager.GET_META_DATA);

            if (info.metaData == null) return SMALL_ICON_NOT_SET;

            return info.metaData.getInt(META_DATA_NAME_SMALL_ICON, SMALL_ICON_NOT_SET);
        } catch (PackageManager.NameNotFoundException e) {
            // Will only happen if the package provided (the one we are running in) is not
            // installed - so should never happen.
            return SMALL_ICON_NOT_SET;
        }
    }

    @Override
    @Nullable
    @MainThread
    public final IBinder onBind(@Nullable Intent intent) {
        return mBinder;
    }

    @Override
    @MainThread
    public final boolean onUnbind(@Nullable Intent intent) {
        mVerifiedUid = -1;

        return super.onUnbind(intent);
    }

    /**
     * Returns a {@link TokenStore} that is used to determine whether the connecting package is
     * allowed to connect to this service.
     * @return An {@link TokenStore} containing the verified provider.
     */
    @BinderThread
    @NonNull
    public abstract TokenStore getTokenStore();

    /**
     * Contains a free form command from the browser. The client and browser will need to agree on
     * an additional API to use in advanced. This call can be used for testing or experimental
     * purposes.
     *
     * A return value of {@code null} will be used to signify that the client does not know how to
     * handle the request.
     *
     * As optional best practices, {@link #KEY_SUCCESS} could be use to identify
     * that command was *successfully* handled. For example, when returning a message with result:
     * <pre><code>
     *     Bundle result = new Bundle();
     *     result.putString("message", message);
     *     if (success)
     *         result.putBoolean(KEY_SUCCESS, true);
     *     return result;
     * </code></pre>
     * On the caller side:
     * <pre><code>
     *     Bundle result = service.extraCommand(commandName, args);
     *     if (result.getBoolean(service.KEY_SUCCESS)) {
     *         // Command was successfully handled
     *     }
     * </code></pre>
     *
     * @param commandName    Name of the command to execute.
     * @param args           Arguments to the command.
     * @param callbackRemote Contains the callback that passed with the command.
     * @return The result {@link Bundle} or {@code null}.
     */
    @BinderThread
    @Nullable
    public Bundle onExtraCommand(@NonNull String commandName, @NonNull Bundle args,
            @Nullable TrustedWebActivityCallbackRemote callbackRemote) {
        return null;
    }

    private static String channelNameToId(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_channel_id";
    }

    private void ensureOnCreateCalled() {
        if (mNotificationManager != null) return;
        throw new IllegalStateException("TrustedWebActivityService has not been properly "
                + "initialized. Did onCreate() call super.onCreate()?");
    }
}
