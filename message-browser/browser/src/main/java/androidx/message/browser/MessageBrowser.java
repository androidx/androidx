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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Browses messages offered by a {@link MessageLibraryService}.
 * @hide
 */
@RestrictTo(LIBRARY)
public class MessageBrowser {
    static final String TAG = "MessageBrowser";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);


    // TODO(sungsoo): Revisit the state constants and check the state before changing mBrowserState.
    // Something wrong.
    private static final int STATE_ERROR = -1;
    // The initial state
    private static final int STATE_IDLE = 0;
    // The state after calling mServiceBinder.connect()
    private static final int STATE_CONNECTING = 1;
    // The state after BrowserStub.notifyConnected()
    private static final int STATE_CONNECTED = 2;
    // The state after mServiceBinder.disconnect() except by close()
    @SuppressWarnings("unused")
    private static final int STATE_DISCONNECTING = 3;
    // The state after BrowserStub.notifyDisconnected()
    private static final int STATE_DISCONNECTED = 4;
    // The state after calling close()
    private static final int STATE_CLOSING = 5;
    // The state after calling close() and mBrowserCallback.onDisconnected()
    private static final int STATE_CLOSED = 6;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ComponentName mServiceComponent;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final BrowserStub mBrowserStub;

    final BrowserCallback mBrowserCallback;
    final Executor mCallbackExecutor;
    final SequencedFutureManager mSequencedFutureManager;

    private final MessageLibraryServiceConnection mServiceConnection;

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mLock = new Object();
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ConnectionRequest mConnectionRequest;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<Runnable> mPendingTasks = new ArrayList<>();

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    IMessageLibraryService mService;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mBrowserState;

    MessageBrowser(@NonNull Context context, @NonNull ComponentName serviceComponent,
            @Nullable Bundle connectionHints, @Nullable Executor callbackExecutor,
            @Nullable BrowserCallback callback) {
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
        if (serviceComponent == null) {
            throw new NullPointerException("serviceComponent shouldn't be null");
        }
        mContext = context;
        mServiceComponent = serviceComponent;
        mCallbackExecutor = callbackExecutor;
        mBrowserCallback = callback;
        mSequencedFutureManager = new SequencedFutureManager();
        mConnectionRequest = new ConnectionRequest(mContext.getPackageName(), Process.myPid(),
                connectionHints);
        mBrowserStub = new BrowserStub();
        mServiceConnection = new MessageLibraryServiceConnection();

        mBrowserState = STATE_IDLE;
        if (!requestConnection()) {
            close();
        }
    }

    /**
     * Releases this object, and disconnects from the library service.
     */
    public void close() {
        synchronized (mLock) {
            if (mBrowserState == STATE_CLOSED || mBrowserState == STATE_CLOSING) return;
            try {
                if (mBrowserState == STATE_CONNECTING || mBrowserState == STATE_CONNECTED) {
                    mService.disconnect(mBrowserStub,
                            mSequencedFutureManager.obtainNextSequenceNumber());
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Service " + mServiceComponent + " has died prematurely");
            }
            mBrowserState = STATE_CLOSING;
            mSequencedFutureManager.close();
        }
    }

    /**
     * Sends a custom command to the library service
     * <p>
     * A command is not accepted if it is not a custom command.
     *
     * @param command custom command to be sent to the library service
     * @param args optional argument
     *
     * @return a listenable future that contains a {@link Bundle} for the result.
     */
    @NonNull
    public ListenableFuture<Bundle> sendCustomCommand(@NonNull MessageCommand command,
            @Nullable Bundle args) {
        if (command == null) {
            throw new NullPointerException("command shouldn't be null");
        }
        if (command.getCommandCode() != MessageCommand.COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("command should be a custom command");
        }
        final SequencedFutureManager.SequencedFuture<Bundle> result =
                mSequencedFutureManager.createSequencedFuture(Bundle.EMPTY);
        executeOrPendRunnable(() -> {
            synchronized (mLock) {
                try {
                    mService.sendCustomCommand(mBrowserStub, result.getSequenceNumber(),
                            command.toBundle(), args);
                } catch (RemoteException e) {
                    mSequencedFutureManager.setFutureResult(result.getSequenceNumber(),
                            Bundle.EMPTY);
                }
            }
        });
        return result;
    }

    void executeOrPendRunnable(Runnable runnable) {
        synchronized (mLock) {
            if (mBrowserState != STATE_CONNECTED) {
                mPendingTasks.add(runnable);
                return;
            }
        }
        runnable.run();
    }

    void notifyBrowserCallback(Runnable runnable) {
        if (mCallbackExecutor != null) {
            mCallbackExecutor.execute(() -> runnable.run());
        }

    }

    private boolean requestConnection() {
        final Intent intent = new Intent(MessageLibraryService.SERVICE_INTERFACE);
        intent.setClassName(mServiceComponent.getPackageName(), mServiceComponent.getClassName());
        boolean result = mContext.bindService(
                intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (!result) {
            Log.w(TAG, "bind to " + mServiceComponent + " failed");
        }
        return result;
    }

    /**
     * Builder for {@link MessageBrowser}.
     * <p>
     * Any incoming event from the {@link MessageLibraryService} will be handled on the callback
     * executor.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final class Builder {
        private Context mContext;
        private ComponentName mServiceComponent;
        private Executor mCallbackExecutor;
        private BrowserCallback mBrowserCallback;
        private Bundle mConnectionHints;

        /**
         * Builder for {@link MessageBrowser}.
         *
         * @param context The context.
         * @param serviceComponent The component name of the service.
         */
        public Builder(@NonNull Context context, @NonNull ComponentName serviceComponent) {
            if (context == null) {
                throw new NullPointerException("context shouldn't be null");
            }
            if (serviceComponent == null) {
                throw new NullPointerException("serviceComponent shouldn't be null");
            }
            mContext = context;
            mServiceComponent = serviceComponent;
        }

        /**
         * Sets the connection hints for the browser.
         * <p>
         * {@code connectionHints} is an argument to send to the message library service when
         * connecting. The contents of this bundle may affect the connection result.
         *
         * @param connectionHints a bundle which contains the connection hints
         * @return the Builder to allow chaining
         */
        @NonNull
        public Builder setConnectionHints(@NonNull Bundle connectionHints) {
            if (connectionHints == null) {
                throw new NullPointerException("connectionHints shouldn't be null");
            }
            mConnectionHints = new Bundle(connectionHints);
            return this;
        }

        /**
         * Sets the callback for the browser and its executor.
         *
         * @param executor callback executor
         * @param callback browser callback.
         * @return the Builder to allow chaining
         */
        @NonNull
        public Builder setBrowserCallback(@NonNull Executor executor,
                @NonNull BrowserCallback callback) {
            if (executor == null) {
                throw new NullPointerException("executor shouldn't be null");
            }
            if (callback == null) {
                throw new NullPointerException("callback shouldn't be null");
            }
            mCallbackExecutor = executor;
            mBrowserCallback = callback;
            return this;
        }

        /**
         * Builds a {@link MessageBrowser}.
         *
         * @return a new browser
         */
        @NonNull
        public MessageBrowser build() {
            return new MessageBrowser(mContext, mServiceComponent, mConnectionHints,
                    mCallbackExecutor, mBrowserCallback);
        }
    }

    /**
     * @hide
     */
    // TODO(sungsoo): consider to add onError().
    @RestrictTo(LIBRARY)
    public abstract static class BrowserCallback {
        /**
         * Called when the browser is successfully connected to the library service. The browser
         * becomes available afterwards.
         *
         * @param browser the browser for this event
         * @param allowedCommands commands that's allowed by the connected library service.
         */
        public void onConnected(@NonNull MessageBrowser browser,
                @NonNull MessageCommandGroup allowedCommands) {}

        /**
         * Called when the library service refuses the browser or the browser is disconnected
         * from the library service. The browser becomes unavailable afterwards and the callback
         * wouldn't be called.
         *
         * @param browser the browser for this event
         */
        public void onDisconnected(@NonNull MessageBrowser browser) {}
    }

    private class BrowserStub extends IMessageBrowser.Stub {
        BrowserStub() {}

        @Override
        public void notifyConnected(final int seq, final Bundle allowedCommands) {
            synchronized (mLock) {
                mBrowserState = STATE_CONNECTED;
            }
            notifyBrowserCallback(() -> mBrowserCallback.onConnected(MessageBrowser.this,
                    MessageCommandGroup.fromBundle(allowedCommands)));
            synchronized (mLock) {
                for (Runnable task : mPendingTasks) {
                    task.run();
                }
                mPendingTasks.clear();
            }
        }

        @Override
        public void notifyDisconnected(final int seq) {
            synchronized (mLock) {
                if (mBrowserState == STATE_CLOSING) {
                    mBrowserState = STATE_CLOSED;
                } else if (mBrowserState != STATE_ERROR) {
                    mBrowserState = STATE_DISCONNECTED;
                }
                mPendingTasks.clear();
            }
            mSequencedFutureManager.close();
            notifyBrowserCallback(() -> mBrowserCallback.onDisconnected(MessageBrowser.this));
        }

        @Override
        public void notifyCommandResult(final int seq, final Bundle result) {
            mSequencedFutureManager.setFutureResult(seq, result);
        }
    }

    // This will be called on the main thread.
    private class MessageLibraryServiceConnection implements ServiceConnection {
        MessageLibraryServiceConnection() {}

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Note that it's always main-thread.
            int browserState = STATE_IDLE;
            if (DEBUG) {
                Log.d(TAG, "onServiceConnected " + componentName + " " + this);
            }
            synchronized (mLock) {
                // Sanity check
                if (!mServiceComponent.equals(componentName)) {
                    Log.wtf(TAG, "Expected connection to " + mServiceComponent + " but is"
                            + " connected to " + componentName);
                    browserState = mBrowserState = STATE_ERROR;
                } else {
                    mService = IMessageLibraryService.Stub.asInterface(iBinder);
                    if (mService == null) {
                        Log.wtf(TAG, "Service interface is missing.");
                        browserState = mBrowserState = STATE_ERROR;
                    } else {
                        browserState = mBrowserState = STATE_CONNECTING;
                        try {
                            mService.connect(mBrowserStub,
                                    mSequencedFutureManager.obtainNextSequenceNumber(),
                                    mConnectionRequest.toBundle());
                        } catch (RemoteException e) {
                            Log.w(TAG, "Service " + mServiceComponent + " has died prematurely");
                            browserState = mBrowserState = STATE_ERROR;
                        }
                    }
                }
            }
            if (browserState == STATE_ERROR) {
                notifyBrowserCallback(
                        () -> mBrowserCallback.onDisconnected(MessageBrowser.this));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {
                mService = null;
            }
        }
    }
}
