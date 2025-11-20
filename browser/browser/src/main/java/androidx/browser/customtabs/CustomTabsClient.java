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

package androidx.browser.customtabs;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.customtabs.IAuthTabCallback;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;
import android.text.TextUtils;
import android.util.Log;

import androidx.browser.auth.AuthTabCallback;
import androidx.browser.auth.AuthTabIntent;
import androidx.browser.auth.AuthTabSession;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Class to communicate with a {@link CustomTabsService} and create
 * {@link CustomTabsSession} from it.
 */
public class CustomTabsClient {
    private static final String TAG = "CustomTabsClient";

    private final ICustomTabsService mService;
    private final ComponentName mServiceComponentName;
    private final Context mApplicationContext;

    CustomTabsClient(ICustomTabsService service, ComponentName componentName,
            Context applicationContext) {
        mService = service;
        mServiceComponentName = componentName;
        mApplicationContext = applicationContext;
    }

    /**
     * Bind to a {@link CustomTabsService} using the given package name and
     * {@link ServiceConnection}.
     * @param context     {@link Context} to use while calling
     *                    {@link Context#bindService(Intent, ServiceConnection, int)}
     * @param packageName Package name to set on the {@link Intent} for binding.
     * @param connection  {@link CustomTabsServiceConnection} to use when binding. This will
     *                    return a {@link CustomTabsClient} on
     *                    {@link CustomTabsServiceConnection
     *                    #onCustomTabsServiceConnected(ComponentName, CustomTabsClient)}
     * @return Whether the binding was successful.
     */
    public static boolean bindCustomTabsService(@NonNull Context context,
            @NonNull String packageName, @NonNull CustomTabsServiceConnection connection) {
        connection.setApplicationContext(context.getApplicationContext());
        Intent intent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("Service Intents must be explicit");
        }
        intent.setPackage(packageName);
        return context.bindService(intent, connection,
                Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
    }

    /**
     * Bind to a {@link CustomTabsService} using the given package name and
     * {@link ServiceConnection}. This is similar to {@link #bindCustomTabsService} but does
     * not use {@link Context#BIND_WAIVE_PRIORITY}, making it suitable for use cases where
     * the browser is immediately going to be launched and breaking the connection would be
     * unrecoverable.
     * @param context     {@link Context} to use while calling
     *                    {@link Context#bindService(Intent, ServiceConnection, int)}
     * @param packageName Package name to set on the {@link Intent} for binding.
     * @param connection  {@link CustomTabsServiceConnection} to use when binding. This will
     *                    return a {@link CustomTabsClient} on
     *                    {@link CustomTabsServiceConnection
     *                    #onCustomTabsServiceConnected(ComponentName, CustomTabsClient)}
     * @return Whether the binding was successful.
     */
    public static boolean bindCustomTabsServicePreservePriority(@NonNull Context context,
            @NonNull String packageName, @NonNull CustomTabsServiceConnection connection) {
        connection.setApplicationContext(context.getApplicationContext());
        Intent intent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("Service Intents must be explicit");
        }
        intent.setPackage(packageName);
        return context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Returns the preferred package to use for Custom Tabs, preferring the default VIEW handler.
     */
    public static @Nullable String getPackageName(@NonNull Context context,
            @Nullable List<String> packages) {
        return getPackageName(context, packages, false);
    }

    /**
     * Returns the preferred package to use for Custom Tabs.
     *
     * The preferred package name is the default VIEW intent handler as long as it supports Custom
     * Tabs. To modify this preferred behavior, set <code>ignoreDefault</code> to true and give a
     * non empty list of package names in <code>packages</code>.
     *
     * This method queries the {@link PackageManager} to determine which packages support the
     * Custom Tabs API. On apps that target Android 11 and above, this requires adding the
     * following package visibility elements to your manifest.
     *
     * <pre>
     * {@code
     * <!-- Place inside the <queries> element. -->
     * <intent>
     *   <action android:name="android.support.customtabs.action.CustomTabsService" />
     * </intent>
     * }
     * </pre>
     *
     * @param context       {@link Context} to use for querying the packages.
     * @param packages      Ordered list of packages to test for Custom Tabs support, in
     *                      decreasing order of priority.
     * @param ignoreDefault If set, the default VIEW handler won't get priority over other browsers.
     * @return The preferred package name for handling Custom Tabs, or <code>null</code>.
     */
    @SuppressWarnings("deprecation")
    public static @Nullable String getPackageName(
            @NonNull Context context, @Nullable List<String> packages, boolean ignoreDefault) {
        PackageManager pm = context.getPackageManager();

        List<String> packageNames = packages == null ? new ArrayList<String>() : packages;
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));

        if (!ignoreDefault) {
            ResolveInfo defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0);
            if (defaultViewHandlerInfo != null) {
                String packageName = defaultViewHandlerInfo.activityInfo.packageName;
                packageNames = new ArrayList<String>(packageNames.size() + 1);
                packageNames.add(packageName);
                if (packages != null) packageNames.addAll(packages);
            }
        }

        Intent serviceIntent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        for (String packageName : packageNames) {
            serviceIntent.setPackage(packageName);
            if (pm.resolveService(serviceIntent, 0) != null) return packageName;
        }

        if (Build.VERSION.SDK_INT >= 30) {
            Log.w(TAG, "Unable to find any Custom Tabs packages, you may need to add a "
                    + "<queries> element to your manifest. See the docs for "
                    + "CustomTabsClient#getPackageName.");
        }
        return null;
    }

    /**
     * Connects to the Custom Tabs warmup service, and initializes the browser.
     *
     * This convenience method connects to the service, and immediately warms up the Custom Tabs
     * implementation. Since service connection is asynchronous, the return code is not the return
     * code of warmup.
     * This call is optional, and clients are encouraged to connect to the service, call
     * <code>warmup()</code> and create a session. In this case, calling this method is not
     * necessary.
     *
     * @param context     {@link Context} to use to connect to the remote service.
     * @param packageName Package name of the target implementation.
     * @return Whether the binding was successful.
     */
    public static boolean connectAndInitialize(@NonNull Context context,
            @NonNull String packageName) {
        if (packageName == null) return false;
        final Context applicationContext = context.getApplicationContext();
        CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
            @Override
            public final void onCustomTabsServiceConnected(
                    @NonNull ComponentName name, @NonNull CustomTabsClient client) {
                client.warmup(0);
                // Unbinding immediately makes the target process "Empty", provided that it is
                // not used by anyone else, and doesn't contain any Activity. This makes it
                // likely to get killed, but is preferable to keeping the connection around.
                applicationContext.unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) { }
        };
        try {
            return bindCustomTabsService(applicationContext, packageName, connection);
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Warm up the browser process.
     *
     * Allows the browser application to pre-initialize itself in the background. Significantly
     * speeds up URL opening in the browser. This is asynchronous and can be called several times.
     *
     * @param flags Reserved for future use.
     * @return      Whether the warmup was successful.
     */
    public boolean warmup(long flags) {
        try {
            return mService.warmup(flags);
        } catch (RemoteException e) {
            return false;
        }
    }

    private static PendingIntent createSessionId(Context context, int sessionId) {
        // Create a {@link PendingIntent} with empty Action to prevent using it other than
        // a session identifier.
        return PendingIntent.getActivity(
                context, sessionId, new Intent(), PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Creates a new session through an ICustomTabsService with the optional callback. This session
     * can be used to associate any related communication through the service with an intent and
     * then later with a Custom Tab. The client can then send later service calls or intents to
     * through same session-intent-Custom Tab association.
     * @param callback The callback through which the client will receive updates about the created
     *                 session. Can be null. All the callbacks will be received on the UI thread.
     * @return The session object that was created as a result of the transaction. The client can
     *         use this to relay session specific calls.
     *         Null if the service failed to respond (threw a RemoteException).
     */
    public @Nullable CustomTabsSession newSession(final @Nullable CustomTabsCallback callback) {
        return newSessionInternal(callback, null);
    }

    /**
     * Creates a new session or updates a callback for the existing session
     * through an ICustomTabsService. This session can be used to associate any related
     * communication through the service with an intent and then later with a Custom Tab.
     * The client can then send later service calls or intents to through same
     * session-intent-Custom Tab association.
     * @param callback The callback through which the client will receive updates about the created
     *                 session. Can be null. All the callbacks will be received on the UI thread.
     * @param id The session id. If the session with the specified id already exists for the given
     *           client application, the new callback is supplied to that session and further
     *           attempts to launch URLs using that session will update the existing Custom Tab
     *           instead of launching a new one.
     * @return The session object that was created as a result of the transaction. The client can
     *         use this to relay session specific calls.
     *         Null if the service failed to respond (threw a RemoteException).
     *         If {@code null} is returned, attempt using {@link #newSession(CustomTabsCallback)}
     *         which is supported with older browsers.
     */
    public @Nullable CustomTabsSession newSession(final @Nullable CustomTabsCallback callback,
            int id) {
        return newSessionInternal(callback, createSessionId(mApplicationContext, id));
    }

    /**
     * Creates a new pending session with an optional callback. This session can be converted to
     * a standard session using {@link #attachSession} after connection.
     *
     * {@see PendingSession}
     */
    public static CustomTabsSession.@NonNull PendingSession newPendingSession(
            @NonNull Context context, final @Nullable CustomTabsCallback callback, int id) {
        PendingIntent sessionId = createSessionId(context, id);

        return new CustomTabsSession.PendingSession(callback, sessionId);
    }

    /**
     * Creates a new pending session with a callback. This session can be converted to a standard
     * session using {@link #attachSession} after connection.
     *
     * @param context The {@link Context} to use.
     * @param id The session id.
     * @param executor The {@link Executor} to be used to execute the callbacks.
     * @param callback The callback through which the client will receive updates about the created
     *                 session.
     * @return The newly created {@link AuthTabSession.PendingSession}.
     */
    @ExperimentalPendingSession
    public static AuthTabSession.@NonNull PendingSession createPendingAuthTabSession(
            @NonNull Context context, int id, @NonNull Executor executor,
            @NonNull AuthTabCallback callback) {
        PendingIntent sessionId = createSessionId(context, id);

        return new AuthTabSession.PendingSession(sessionId, executor, callback);
    }

    /**
     * Creates a new pending session without a callback. This session can be converted to a standard
     * session using {@link #attachSession} after connection.
     *
     * @param context The {@link Context} to use.
     * @param id      The session id.
     * @return The newly created {@link AuthTabSession.PendingSession}.
     */
    @ExperimentalPendingSession
    public static AuthTabSession.@NonNull PendingSession createPendingAuthTabSession(
            @NonNull Context context, int id) {
        PendingIntent sessionId = createSessionId(context, id);

        return new AuthTabSession.PendingSession(sessionId, null, null);
    }

    /**
     * Creates a new session through an ICustomTabsService with the optional callback. This session
     * can be used to associate any related communication through the service with an intent and
     * then later with a Custom Tab. The client can then send later service calls or intents to
     * through same session-intent-Custom Tab association.
     *
     * @param callback The callback through which the client will receive updates about the created
     *                 session. Can be null.
     * @param executor The {@link Executor} to be used to execute the callbacks. If null, the
     *                 callbacks will be received on the UI thread.
     * @return The session object that was created as a result of the transaction. The client can
     * use this to relay session specific calls. Null if the service failed to respond
     * (threw a RemoteException).
     */
    @Nullable
    public AuthTabSession newAuthTabSession(@Nullable AuthTabCallback callback,
            @Nullable Executor executor) {
        return newAuthTabSessionInternal(callback, executor, null);
    }

    /**
     * Creates a new session through an ICustomTabsService with the optional callback. This session
     * can be used to associate any related communication through the service with an intent and
     * then later with a Custom Tab. The client can then send later service calls or intents to
     * through same session-intent-Custom Tab association.
     *
     * @param callback The callback through which the client will receive updates about the created
     *                 session. Can be null.
     * @param executor The {@link Executor} to be used to execute the callbacks. If null, the
     *                 callbacks will be received on the UI thread.
     * @param id       The session id. If the session with the specified id already exists for
     *                 the given client application, the new callback is supplied to that session
     *                 and
     *                 further attempts to launch URLs using that session will update the
     *                 existing Auth
     *                 Tab instead of launching a new one.
     * @return The session object that was created as a result of the transaction. The client can
     * use this to relay session specific calls. Null if the service failed to respond
     * (threw a RemoteException).
     */
    @Nullable
    public AuthTabSession newAuthTabSession(@Nullable AuthTabCallback callback,
            @Nullable Executor executor, int id) {
        return newAuthTabSessionInternal(callback, executor,
                createSessionId(mApplicationContext, id));
    }

    @Nullable
    private AuthTabSession newAuthTabSessionInternal(@Nullable AuthTabCallback callback,
            @Nullable Executor executor, @Nullable PendingIntent sessionId) {
        IAuthTabCallback.Stub wrapper = createAuthTabCallbackWrapper(callback, executor);

        try {
            boolean success;

            Bundle extras = new Bundle();
            extras.putParcelable(CustomTabsIntent.EXTRA_SESSION_ID, sessionId);
            success = mService.newAuthTabSession(wrapper, extras);

            if (!success) return null;
        } catch (RemoteException e) {
            return null;
        }
        return new AuthTabSession(wrapper, mServiceComponentName, sessionId);
    }

    private IAuthTabCallback.Stub createAuthTabCallbackWrapper(@Nullable AuthTabCallback callback,
            @Nullable Executor executor) {
        return new IAuthTabCallback.Stub() {
            private final Executor mExecutor = executor != null ? executor : new Handler(
                    Looper.getMainLooper())::post;

            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras)
                    throws RemoteException {
                if (callback == null) return;
                long identity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> callback.onNavigationEvent(navigationEvent, extras));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onExtraCallback(String callbackName, Bundle args) throws RemoteException {
                if (callback == null) return;
                long identity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> callback.onExtraCallback(callbackName, args));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public Bundle onExtraCallbackWithResult(String callbackName, Bundle args)
                    throws RemoteException {
                if (callback == null) return Bundle.EMPTY;
                long identity = Binder.clearCallingIdentity();
                try {
                    return callback.onExtraCallbackWithResult(callbackName, args);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onWarmupCompleted(Bundle extras) throws RemoteException {
                if (callback == null) return;
                long identity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> callback.onWarmupCompleted(extras));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        };
    }

    /**
     * Associate {@link AuthTabSession.PendingSession} with the service and turn it into an
     * {@link AuthTabSession}.
     *
     * @param session The {@link AuthTabSession.PendingSession} to attach.
     * @return The {@link AuthTabSession} that was created, or null if the browser doesn't support
     * this feature.
     */
    @ExperimentalPendingSession
    @Nullable
    public AuthTabSession attachAuthTabSession(AuthTabSession.@NonNull PendingSession session) {
        return newAuthTabSessionInternal(session.getCallback(), session.getExecutor(),
                session.getId());
    }

    private @Nullable CustomTabsSession newSessionInternal(
            final @Nullable CustomTabsCallback callback, @Nullable PendingIntent sessionId) {
        ICustomTabsCallback.Stub wrapper = createCallbackWrapper(callback);

        try {
            boolean success;

            if (sessionId != null) {
                Bundle extras = new Bundle();
                extras.putParcelable(CustomTabsIntent.EXTRA_SESSION_ID, sessionId);
                success = mService.newSessionWithExtras(wrapper, extras);
            } else {
                success = mService.newSession(wrapper);
            }

            if (!success) return null;
        } catch (RemoteException e) {
            return null;
        }
        return new CustomTabsSession(mService, wrapper, mServiceComponentName, sessionId);
    }

    /**
     * Can be used as a channel between the Custom Tabs client and the provider to do something that
     * is not part of the API yet.
     */
    @SuppressWarnings("NullAway") // TODO: b/141869399
    public @Nullable Bundle extraCommand(@NonNull String commandName, @Nullable Bundle args) {
        try {
            return mService.extraCommand(commandName, args);
        } catch (RemoteException e) {
            return null;
        }
    }

    private ICustomTabsCallback.Stub createCallbackWrapper(
            final @Nullable CustomTabsCallback callback) {
        return new ICustomTabsCallback.Stub() {
            private Handler mHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onNavigationEvent(final int navigationEvent, final Bundle extras) {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onNavigationEvent(navigationEvent, extras);
                    }
                });
            }

            @Override
            public void extraCallback(final String callbackName, final Bundle args)
                    throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.extraCallback(callbackName, args);
                    }
                });
            }

            @Override
            @SuppressWarnings("NullAway") // TODO: b/141869399
            public Bundle extraCallbackWithResult(@NonNull String callbackName,
                    @Nullable Bundle args)
                    throws RemoteException {
                if (callback == null) return null;
                return callback.extraCallbackWithResult(callbackName, args);
            }

            @Override
            public void onMessageChannelReady(final Bundle extras)
                    throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onMessageChannelReady(extras);
                    }
                });
            }

            @Override
            public void onPostMessage(final String message, final Bundle extras)
                    throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onPostMessage(message, extras);
                    }
                });
            }

            @Override
            public void onRelationshipValidationResult(
                    final @CustomTabsService.Relation int relation, final Uri requestedOrigin,
                    final boolean result, final @Nullable Bundle extras) throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onRelationshipValidationResult(
                                relation, requestedOrigin, result, extras);
                    }
                });
            }

            @Override
            public void onActivityResized(final int height, final int width,
                    final @Nullable Bundle extras)
                    throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @SuppressWarnings("NullAway") // b/316641009
                    @Override
                    public void run() {
                        callback.onActivityResized(height, width, extras);
                    }
                });
            }

            @Override
            public void onWarmupCompleted(final @NonNull Bundle extras) throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onWarmupCompleted(extras);
                    }
                });
            }

            @Override
            public void onActivityLayout(final int left, final int top, final int right,
                    final int bottom, @CustomTabsCallback.ActivityLayoutState int state,
                    @NonNull Bundle extras)
                    throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onActivityLayout(left, top, right, bottom, state, extras);
                    }
                });
            }

            @Override
            public void onMinimized(@NonNull Bundle extras) throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onMinimized(extras);
                    }
                });
            }

            @Override
            public void onUnminimized(@NonNull Bundle extras)
                    throws RemoteException {
                if (callback == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onUnminimized(extras);
                    }
                });
            }
        };
    }

    /**
     * Associate {@link CustomTabsSession.PendingSession} with the service
     * and turn it into a {@link CustomTabsSession}.
     *
     */
    @SuppressWarnings("NullAway") // TODO: b/141869399
    public @Nullable CustomTabsSession attachSession(
            CustomTabsSession.@NonNull PendingSession session) {
        return newSessionInternal(session.getCallback(), session.getId());
    }

    /**
     * Check whether the Custom Tabs provider supports multi-network feature {@link
     * CustomTabsIntent.Builder#setNetwork}, i.e. be able to bind a custom tab to a
     * particular network.
     *
     * @param context Application context.
     * @param provider the package name of Custom Tabs provider.
     * @return whether a Custom Tabs provider supports multi-network feature.
     * @see CustomTabsIntent.Builder#setNetwork and CustomTabsService#CATEGORY_SET_NETWORK.
     */
    public static boolean isSetNetworkSupported(@NonNull Context context,
            @NonNull String provider) {
        return packageHasCategory(context, provider, CustomTabsService.CATEGORY_SET_NETWORK);
    }

    /**
     * Checks whether the Custom Tabs provider supports Auth Tab. See {@link AuthTabIntent} for more
     * information on how to launch an Auth Tab.
     *
     * @param context The application {@link Context}.
     * @param provider The package name of the Custom Tabs provider.
     * @return Whether the Custom Tabs provider supports Auth Tab.
     * @see CustomTabsService#CATEGORY_AUTH_TAB
     */
    public static boolean isAuthTabSupported(@NonNull Context context,
            @NonNull String provider) {
        return packageHasCategory(context, provider, CustomTabsService.CATEGORY_AUTH_TAB);
    }

    /**
     * Checks whether the Custom Tabs provider supports Ephemeral Browsing.
     *
     * @param context The application {@link Context}.
     * @param provider The package name of the Custom Tabs provider.
     * @return Whether the Custom Tabs provider supports Ephemeral Browsing.
     * @see CustomTabsService#CATEGORY_EPHEMERAL_BROWSING
     */
    public static boolean isEphemeralBrowsingSupported(@NonNull Context context,
            @NonNull String provider) {
        return packageHasCategory(context, provider, CustomTabsService.CATEGORY_EPHEMERAL_BROWSING);
    }

    private static boolean packageHasCategory(@NonNull Context context, @NonNull String provider,
            @NonNull String category) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION),
                PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo service : services) {
            ServiceInfo serviceInfo = service.serviceInfo;
            if (serviceInfo != null && provider.equals(serviceInfo.packageName)) {
                IntentFilter filter = service.filter;
                if (filter != null && filter.hasCategory(category)) {
                    return true;
                }
            }
        }
        return false;
    }
}