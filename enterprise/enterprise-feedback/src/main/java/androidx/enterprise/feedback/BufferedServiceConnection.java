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

package androidx.enterprise.feedback;

import static androidx.enterprise.feedback.KeyedAppStatesCallback.STATUS_EXCEEDED_BUFFER_ERROR;
import static androidx.enterprise.feedback.KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR;
import static androidx.enterprise.feedback.KeyedAppStatesCallback.STATUS_UNKNOWN_ERROR;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.canPackageReceiveAppStates;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * A wrapper around {@link ServiceConnection} and {@link Messenger} which will buffer messages sent
 * while disconnected.
 *
 * <p>Each instance is single-use. After being unbound either manually (using {@link #unbind()} or
 * due to an error it will become "dead" (see {@link #isDead()} and cannot be used further.
 *
 * <p>Instances are not thread safe, so avoid using on multiple different threads.
 */
class BufferedServiceConnection {

    @VisibleForTesting
    static final int MAX_BUFFER_SIZE = 100;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Messenger mMessenger = null;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;
    private final Intent mBindIntent;
    private final int mFlags;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mHasBeenDisconnected = false;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mIsDead = false;
    private boolean mHasBound = false;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Queue<SendableMessage> mBuffer = new ArrayDeque<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Executor mExecutor;

    /**
     * Create a {@link BufferedServiceConnection}.
     *
     * <p>The {@link Executor} must execute serially on the same thread as all calls to
     * this instance.
     */
    BufferedServiceConnection(
            Executor executor, Context context, Intent bindIntent, int flags) {
        if (executor == null) {
            throw new NullPointerException("executor must not be null");
        }
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        if (bindIntent == null) {
            throw new NullPointerException("bindIntent must not be null");
        }
        this.mExecutor = executor;
        this.mContext = context;
        this.mBindIntent = bindIntent;
        this.mFlags = flags;
    }

    /**
     * Calls {@link Context#bindService(Intent, ServiceConnection, int)} with the wrapped {@link
     * ServiceConnection}.
     *
     * <p>This can only be called once per instance.
     */
    void bindService() {
        if (mHasBound) {
            throw new IllegalStateException(
                    "Each BufferedServiceConnection can only be bound once.");
        }
        mHasBound = true;
        mContext.bindService(mBindIntent, mConnection, mFlags);
    }

    void unbind() {
        if (!mHasBound) {
            throw new IllegalStateException("bindService must be called before unbind");
        }
        mIsDead = true;
        mContext.unbindService(mConnection);
    }

    private final ServiceConnection mConnection =
            new ServiceConnection() {
                @Override
                public void onBindingDied(ComponentName name) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            // If this is now dead then the messages should not be sent, report
                            // success
                            reportSuccessOnBufferedMessages();
                            mIsDead = true;
                        }
                    });
                }

                @Override
                public void onServiceConnected(final ComponentName componentName,
                        final IBinder service) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mHasBeenDisconnected = false;
                            if (canPackageReceiveAppStates(
                                    mContext, componentName.getPackageName())) {
                                mMessenger = new Messenger(service);
                                sendBufferedMessages();
                            } else {
                                // If this is now dead then the messages should not be sent, report
                                // success
                                reportSuccessOnBufferedMessages();
                                mIsDead = true;
                            }
                        }
                    });
                }

                @SuppressWarnings("WeakerAccess") /* synthetic access */
                void sendBufferedMessages() {
                    while (!mBuffer.isEmpty()) {
                        trySendMessage(mBuffer.poll());
                    }
                }

                @SuppressWarnings("WeakerAccess") /* synthetic access */
                void reportSuccessOnBufferedMessages() {
                    while (!mBuffer.isEmpty()) {
                        mBuffer.poll().onSuccess();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mHasBeenDisconnected = true;
                            mMessenger = null;
                        }
                    });
                }
            };

    /**
     * Call {@link Messenger#send(Message)} immediately if wrapped {@link ServiceConnection} is
     * connected. Otherwise adds the message to a queue to be delivered when a connection is
     * established.
     *
     * <p>The queue is capped at 100 messages. If 100 messages are already queued when send is
     * called and a connection is not established, the earliest message in the queue will be lost.
     */
    void send(SendableMessage message) {
        if (mIsDead) {
            // Nothing will send on this connection, so we need to report success to allow it to
            // resolve.
            message.onSuccess();
            return;
        }

        if (mMessenger == null) {
            while (mBuffer.size() >= MAX_BUFFER_SIZE) {
                mBuffer.poll().dealWithError(STATUS_EXCEEDED_BUFFER_ERROR, /* throwable= */ null);
            }
            mBuffer.add(message);
            return;
        }

        trySendMessage(message);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void trySendMessage(SendableMessage message) {
        try {
            mMessenger.send(message.createStateMessage());
            message.onSuccess();
        } catch (TransactionTooLargeException e) {
            message.dealWithError(STATUS_TRANSACTION_TOO_LARGE_ERROR, e);
        } catch (RemoteException e) {
            message.dealWithError(STATUS_UNKNOWN_ERROR, e);
        }
    }

    boolean isDead() {
        return mIsDead;
    }

    /**
     * Returns true if the connection has been established and disconnected, and has not since been
     * re-established.
     *
     * <p>This can be used to kill service connections if running on a SDK < 26. For later versions,
     * {@link #isDead()} should be used.
     */
    boolean hasBeenDisconnected() {
        return mHasBeenDisconnected;
    }
}
