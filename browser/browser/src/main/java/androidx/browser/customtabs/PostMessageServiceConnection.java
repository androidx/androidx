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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.IPostMessageService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A {@link ServiceConnection} for Custom Tabs providers to use while connecting to a
 * {@link PostMessageService} on the client side.
 *
 * TODO(peconn): Make this not abstract with API change.
 */
@SuppressWarnings("HiddenSuperclass")
public abstract class PostMessageServiceConnection
        implements PostMessageBackend, ServiceConnection {
    private static final String TAG = "PostMessageServConn";

    private final Object mLock = new Object();
    private final ICustomTabsCallback mSessionBinder;
    @Nullable private IPostMessageService mService;
    @Nullable private String mPackageName;
    // Indicates that a message channel has been opened. We're ready to post messages once this is
    // true and we've connected to the {@link PostMessageService}.
    private boolean mMessageChannelCreated;

    public PostMessageServiceConnection(@NonNull CustomTabsSessionToken session) {
        IBinder binder = session.getCallbackBinder();
        if (binder == null) {
            throw new IllegalArgumentException("Provided session must have binder.");
        }
        mSessionBinder = ICustomTabsCallback.Stub.asInterface(binder);
    }

    /**
     * Sets the package name unique to the session.
     * @param packageName The package name for the client app for the owning session.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setPackageName(@NonNull String packageName) {
        mPackageName = packageName;
    }

    /**
     * Binds the browser side to the client app through the given {@link PostMessageService} name.
     * After this, this {@link PostMessageServiceConnection} can be used for sending postMessage
     * related communication back to the client.
     * @param context A context to bind to the service.
     * @param packageName The name of the package to be bound to.
     * @return Whether the binding was successful.
     */
    public boolean bindSessionToPostMessageService(@NonNull Context context,
            @NonNull String packageName) {
        Intent intent = new Intent();
        intent.setClassName(packageName, PostMessageService.class.getName());
        boolean success = context.bindService(intent, this, Context.BIND_AUTO_CREATE);
        if (!success) {
            Log.w(TAG, "Could not bind to PostMessageService in client.");
        }
        return success;
    }

    /**
     * See
     * {@link PostMessageServiceConnection#bindSessionToPostMessageService(Context, String)}.
     * Attempts to bind with the package name set during initialization.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean bindSessionToPostMessageService(@NonNull Context appContext) {
        if (mPackageName == null) {
            throw new IllegalStateException("setPackageName must be called before "
                    + "bindSessionToPostMessageService.");
        }
        return bindSessionToPostMessageService(appContext, mPackageName);
    }

    private boolean isBoundToService() {
        return mService != null;
    }

    /**
     * Unbinds this service connection from the given context.
     * @param context The context to be unbound from.
     */
    public void unbindFromContext(@NonNull Context context) {
        if (isBoundToService()) {
            context.unbindService(this);
            mService = null;
        }
    }

    @Override
    public final void onServiceConnected(@NonNull ComponentName name, @NonNull IBinder service) {
        mService = IPostMessageService.Stub.asInterface(service);
        onPostMessageServiceConnected();
    }

    @Override
    public final void onServiceDisconnected(@NonNull ComponentName name) {
        mService = null;
        onPostMessageServiceDisconnected();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public final boolean onNotifyMessageChannelReady(@Nullable Bundle extras) {
        return notifyMessageChannelReady(extras);
    }

    /**
     * Records that the message channel has been created and notifies the client. This method
     * should be called when the browser binds to the client side {@link PostMessageService} and
     * also readies a connection to the web frame.
     * @param extras Unused.
     * @return Whether the notification was sent successfully.
     */
    public final boolean notifyMessageChannelReady(@Nullable Bundle extras) {
        mMessageChannelCreated = true;
        return notifyMessageChannelReadyInternal(extras);
    }

    /**
     * Notifies the client that the postMessage channel requested with
     * {@link CustomTabsService#requestPostMessageChannel(
     * CustomTabsSessionToken, android.net.Uri)} is ready. This method should be
     * called when the browser binds to the client side {@link PostMessageService} and also readies
     * a connection to the web frame.
     *
     * @param extras Reserved for future use.
     * @return Whether the notification was sent to the remote successfully.
     */
    @SuppressWarnings("NullAway")  // onMessageChannelReady accepts null extras.
    private boolean notifyMessageChannelReadyInternal(@Nullable Bundle extras) {
        if (mService == null) return false;
        synchronized (mLock) {
            try {
                mService.onMessageChannelReady(mSessionBinder, extras);
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public final boolean onPostMessage(@NonNull String message, @Nullable Bundle extras) {
        return postMessage(message, extras);
    }

    /**
     * Posts a message to the client. This should be called when a tab controlled by related
     * {@link CustomTabsSession} has sent a postMessage. If postMessage() is called from a single
     * thread, then the messages will be posted in the same order.
     *
     * @param message The message sent.
     * @param extras Reserved for future use.
     * @return Whether the postMessage was sent to the remote successfully.
     */
    @SuppressWarnings("NullAway")  // onPostMessage accepts null extras.
    public final boolean postMessage(@NonNull String message, @Nullable Bundle extras) {
        if (mService == null) return false;
        synchronized (mLock) {
            try {
                mService.onPostMessage(mSessionBinder, message, extras);
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onDisconnectChannel(@NonNull Context appContext) {
        unbindFromContext(appContext);
    }

    /**
     * Called when the {@link PostMessageService} connection is established.
     */
    public void onPostMessageServiceConnected() {
        if (mMessageChannelCreated) notifyMessageChannelReadyInternal(null);
    }

    /**
     * Called when the connection is lost with the {@link PostMessageService}.
     */
    public void onPostMessageServiceDisconnected() {}

    /**
     * Cleans up any dependencies that this handler might have.
     * @param context Context to use for unbinding if necessary.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void cleanup(@NonNull Context context) {
        if (isBoundToService()) unbindFromContext(context);
    }
}
