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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

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
    final Object mLock = new Object();
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayMap<IBinder, BrowserRecord> mBrowserRecords = new ArrayMap<>();

    private ServiceStub mServiceStub;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceStub = new ServiceStub();
        mHandler = new Handler(Looper.getMainLooper());
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
     * Called when a browser sent a command which will be sent directly.
     * <p>
     * Return {@code true} to accept the command, {@code false} to decline the command.
     *
     * @param browserInfo the browser information
     * @param command a command. This method will be called for every single command.
     * @return {@code RESULT_SUCCESS} if you want to proceed with incoming command.
     *         Another code for ignore.
     */
    public boolean onCommandRequest(@NonNull BrowserInfo browserInfo,
            @NonNull MessageCommand command) {
        return true;
    }

    /**
     * Called when a browser sent a custom command through
     * {@link MessageBrowser#sendCustomCommand(MessageCommand, Bundle)}.
     * <p>
     * @param browserInfo the browser information
     * @param customCommand custom command.
     * @param args optional arguments
     * @return result of handling custom command. A runtime exception will be thrown if
     *         {@code null} is returned.
     * @see MessageCommand#COMMAND_CODE_CUSTOM
     */
    @NonNull
    public Bundle onCustomCommand(@NonNull BrowserInfo browserInfo,
            @NonNull MessageCommand customCommand, @Nullable Bundle args) {
        return Bundle.EMPTY;
    }

    void releaseBrowserRecord(BrowserRecord record) {
        IBinder browserBinder = record.browser.asBinder();
        browserBinder.unlinkToDeath(record, 0);
        synchronized (mLock) {
            mBrowserRecords.remove(browserBinder);
        }
    }

    /**
     * Information of the connected browser.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final class BrowserInfo {
        private final String mPackageName;
        private final int mPid;
        private final int mUid;
        private final Bundle mConnectionHints;

        BrowserInfo(ConnectionRequest request, int pid, int uid) {
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
            if (request == null) {
                try {
                    browser.notifyDisconnected(seq);
                } catch (RemoteException ex) {
                    Log.w(TAG, "Calling notifyDisconnected() failed");
                }
                return;
            }
            IBinder browserBinder = browser.asBinder();
            synchronized (mLock) {
                mBrowserRecords.remove(browserBinder);
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
                    BrowserRecord record = new BrowserRecord(browser, browserInfo, allowedCommands);
                    synchronized (mLock) {
                        mBrowserRecords.put(browserBinder, record);
                    }
                    try {
                        browserBinder.linkToDeath(record, 0);
                        browser.notifyConnected(seq, allowedCommands.toBundle());
                    } catch (RemoteException ex) {
                        Log.w(TAG, "Calling notifyConnected() failed");
                        synchronized (mLock) {
                            mBrowserRecords.remove(browserBinder);
                        }
                    }
                } else {
                    try {
                        browser.notifyDisconnected(seq);
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
            synchronized (mLock) {
                IBinder browserBinder = browser.asBinder();
                BrowserRecord record = mBrowserRecords.remove(browserBinder);
                try {
                    browserBinder.unlinkToDeath(record, 0);
                    browser.notifyDisconnected(seq);
                } catch (RemoteException e) {
                    Log.w(TAG, "Calling notifyDisconnected() failed");
                }
            }
        }

        @Override
        public void sendCustomCommand(IMessageBrowser browser, int seq, Bundle command,
                Bundle args) {
            synchronized (mLock) {
                BrowserRecord record = mBrowserRecords.get(browser.asBinder());
                if (record == null) {
                    return;
                }
                MessageCommand customCommand = MessageCommand.fromBundle(command);
                mHandler.post(() -> {
                    try {
                        Bundle result = onCommandRequest(record.browserInfo, customCommand)
                                ? onCustomCommand(record.browserInfo, customCommand, args)
                                : Bundle.EMPTY;
                        record.browser.notifyCommandResult(seq, result);
                    } catch (RemoteException e) {
                        releaseBrowserRecord(record);
                    }
                });
            }
        }
    }

    private class BrowserRecord implements IBinder.DeathRecipient {
        public IMessageBrowser browser;
        public BrowserInfo browserInfo;
        public MessageCommandGroup allowedCommands;

        BrowserRecord(IMessageBrowser browser, BrowserInfo browserInfo,
                MessageCommandGroup allowedCommands) {
            this.browser = browser;
            this.browserInfo = browserInfo;
            this.allowedCommands = allowedCommands;
        }

        @Override
        public void binderDied() {
            synchronized (mLock) {
                mBrowserRecords.remove(browser.asBinder());
            }
        }
    }
}
