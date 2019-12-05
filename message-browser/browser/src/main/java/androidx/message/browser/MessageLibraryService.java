/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.message.browser;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Base class for message library services.
 * <p>
 * Message library services allows applications to share messages with the applications that
 * uses {@link MessageBrowser}.
 * <p>
 * When extending this class, add the following to your {@code AndroidManifest.xml}.
 * <pre>
 * &lt;service android:name="component_name_of_your_implementation" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="androidx.message.MessageLibraryService" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/service&gt;</pre>
 * <p>
 *
 * @see MessageBrowser
 * @hide
 */
@RestrictTo(LIBRARY)
public class MessageLibraryService extends Service {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE = "androidx.message.MessageLibraryService";

    static final String TAG = "MsgLibService";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    BrowserInfo mConnectedBrowser;

    private ServiceStub mServiceStub;

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceStub = new ServiceStub();
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return mServiceStub;
        }
        return null;
    }

    // TODO(sungsoo): Add onPostConnect and sendCustomCummand.
    /**
     * Called when a browser is created for this service. Return allowed commands for
     * the browser, or it allows all connection requests and commands by default.
     * <p>
     * You can reject the connection by returning {@code null}. In that case, the browser
     * receives {@link MessageBrowser.BrowserCallback#onDisconnected(MessageBrowser)} and
     * cannot be used anymore.
     * <p>
     * The browser hasn't connected yet in this method, so calls to the browser
     * (e.g. {@link #sendCustomCommand}) would be ignored. Override
     * {@link #onPostConnect} for the custom initialization for the browser after connection.
     *
     * @param browserInfo browser information.
     * @return allowed commands. Can be {@code null} to reject connection.
     * @see #onPostConnect(BrowserInfo)
     */
    @Nullable
    public MessageCommandGroup onConnect(@NonNull BrowserInfo browserInfo) {
        MessageCommandGroup commands = new MessageCommandGroup.Builder()
                .addAllPredefinedCommands(MessageCommand.COMMAND_VERSION_CURRENT)
                .build();
        return commands;
    }

    /**
     * Information of the connected browser.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final class BrowserInfo {
        private final int mBrowserVersion;
        private final String mPackageName;
        private final int mPid;
        private final int mUid;
        private final Bundle mConnectionHints;

        BrowserInfo(ConnectionRequest request, int pid, int uid) {
            mBrowserVersion = request.version;
            mPackageName = request.packageName;
            mPid = pid;
            mUid = uid;
            mConnectionHints = request.connectionHints;
        }

        /**
         * @return package name of the browser.
         */
        @NonNull
        public String getPackageName() {
            return mPackageName;
        }

        /**
         * @return pid of the controller.
         */
        public int getPid() {
            return mPid;
        }

        /**
         * @return uid of the controller. Can be a negative value if the uid cannot be obtained.
         */
        public int getUid() {
            return mUid;
        }

        /**
         * @return connection hints sent from the browser, or {@link Bundle#EMPTY} if none.
         */
        @NonNull
        public Bundle getConnectionHints() {
            return mConnectionHints == null ? Bundle.EMPTY : new Bundle(mConnectionHints);
        }
    }

    private class ServiceStub extends IMessageLibraryService.Stub {
        ServiceStub() {}

        @Override
        public void connect(IMessageBrowser browser, int seq, Bundle connectionRequest) {
            if (browser == null || connectionRequest == null) {
                return;
            }
            final ConnectionRequest request = ConnectionRequest.fromBundle(connectionRequest);
            // TODO(sungsoo): Allow multiple connections.
            if (mConnectedBrowser != null || request == null) {
                try {
                    browser.notifyDisconnected();
                } catch (RemoteException ex) {
                    Log.w(TAG, "Calling notifyDisconnected() failed");
                }
                return;
            }

            final int uid = Binder.getCallingUid();
            final int callingPid = Binder.getCallingPid();
            final long token = Binder.clearCallingIdentity();
            // Binder.getCallingPid() can be 0 for an oneway call from the remote process.
            // If it's the case, use PID from the ConnectionRequest.
            final int pid = (callingPid != 0) ? callingPid : request.pid;
            try {
                BrowserInfo browserInfo = new BrowserInfo(request, pid, uid);
                MessageCommandGroup allowedCommands = onConnect(browserInfo);
                if (allowedCommands != null) {
                    mConnectedBrowser = browserInfo;
                    try {
                        browser.notifyConnected(allowedCommands.toBundle());
                    } catch (RemoteException ex) {
                        Log.w(TAG, "Calling notifyConnected() failed");
                    }
                } else {
                    try {
                        browser.notifyDisconnected();
                    } catch (RemoteException ex) {
                        Log.w(TAG, "Calling notifyDisconnected() failed");
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void disconnect(IMessageBrowser browser, int seq) {
            // TODO(sungsoo): implement this
        }
    }
}
