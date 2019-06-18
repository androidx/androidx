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
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to communicate with a {@link CustomTabsService} and create
 * {@link CustomTabsSession} from it.
 */
public class CustomTabsClient {
    private final ICustomTabsService mService;
    private final ComponentName mServiceComponentName;
    private final Context mApplicationContext;

    /**@hide*/
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
            @Nullable String packageName, @NonNull CustomTabsServiceConnection connection) {
        connection.setApplicationContext(context.getApplicationContext());
        Intent intent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        if (!TextUtils.isEmpty(packageName)) intent.setPackage(packageName);
        return context.bindService(intent, connection,
                Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
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
     * @param context       {@link Context} to use for querying the packages.
     * @param packages      Ordered list of packages to test for Custom Tabs support, in
     *                      decreasing order of priority.
     * @param ignoreDefault If set, the default VIEW handler won't get priority over other browsers.
     * @return The preferred package name for handling Custom Tabs, or <code>null</code>.
     */
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
                    ComponentName name, CustomTabsClient client) {
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
        return PendingIntent.getActivity(context, sessionId, new Intent(), 0);
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
    public @Nullable CustomTabsSession newSession(@Nullable final CustomTabsCallback callback) {
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
     */
    public @Nullable CustomTabsSession newSession(@Nullable final CustomTabsCallback callback,
            int id) {
        return newSessionInternal(callback, createSessionId(mApplicationContext, id));
    }

    /**
     * Creates a new pending session with an optional callback. This session can be converted to
     * a standard session using {@link #attachSession} after connection.
     *
     * {@see PendingSession}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static CustomTabsSession.PendingSession newPendingSession(
            Context context, final CustomTabsCallback callback, int id) {
        PendingIntent sessionId = createSessionId(context, id);

        return new CustomTabsSession.PendingSession(callback, sessionId);
    }

    private @Nullable CustomTabsSession newSessionInternal(final CustomTabsCallback callback,
                @Nullable PendingIntent sessionId) {
        ICustomTabsCallback.Stub wrapper = createCallbackWrapper(callback);
        Bundle extras = new Bundle();
        if (sessionId != null) extras.putParcelable(CustomTabsIntent.EXTRA_SESSION_ID, sessionId);
        try {
            if (!mService.newSessionWithExtras(wrapper, extras)) return null;
        } catch (RemoteException e) {
            return null;
        }
        return new CustomTabsSession(mService, wrapper, mServiceComponentName, sessionId);
    }

    /**
     * Can be used as a channel between the Custom Tabs client and the provider to do something that
     * is not part of the API yet.
     */
    public @Nullable Bundle extraCommand(@NonNull String commandName, @Nullable Bundle args) {
        try {
            return mService.extraCommand(commandName, args);
        } catch (RemoteException e) {
            return null;
        }
    }

    private ICustomTabsCallback.Stub createCallbackWrapper(final CustomTabsCallback callback) {
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
        };
    }

    /**
     * Associate {@link CustomTabsSession.PendingSession} with the service
     * and turn it into a {@link CustomTabsSession}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public CustomTabsSession attachSession(CustomTabsSession.PendingSession session) {
        return newSessionInternal(session.getCallback(), session.getId());
    }
}
