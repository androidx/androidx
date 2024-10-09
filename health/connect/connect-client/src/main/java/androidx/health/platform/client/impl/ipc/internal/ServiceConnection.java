/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.platform.client.impl.ipc.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.health.platform.client.impl.logger.Logger;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A class that maintains a connection to IPC backend service. If connection is not available it
 * uses a queue to store service requests until connection is renewed. One {@link ServiceConnection}
 * is associated with one AIDL file .
 *
 * <p>Note: this class is not thread safe and should be called always from the same thread.
 *
 */
@NotThreadSafe
@RestrictTo(Scope.LIBRARY)
public class ServiceConnection implements android.content.ServiceConnection {
    private static final String TAG = "ServiceConnection";

    /** Callback for reporting back to the manager. */
    public interface Callback {

        /** Called when the connection to the server was successfully established. */
        void onConnected(ServiceConnection connection);

        /**
         * Called when the connection to the server was lost.
         *
         * @param connection       Represents this connection to a service.
         * @param reconnectDelayMs Delay before the caller should try to reconnect this connection.
         */
        void onDisconnected(ServiceConnection connection, long reconnectDelayMs);

        /**
         * Return true if the {@link ServiceConnection} should bind to the service in the same
         * application for testing reason.
         */
        boolean isBindToSelfEnabled();
    }

    private static final int MAX_RETRIES = 10;

    private final Context mContext;
    private final Queue<QueueOperation> mOperationQueue = new ConcurrentLinkedQueue<>();
    private final ConnectionConfiguration mConnectionConfiguration;
    private final ExecutionTracker mExecutionTracker;
    private final Map<ListenerKey, QueueOperation> mRegisteredListeners = new HashMap<>();
    private final Callback mCallback;

    @VisibleForTesting
    @Nullable IBinder mBinder;

    @VisibleForTesting
    volatile boolean mIsServiceBound;
    /** Denotes how many times connection to the service failed and we retried. */
    private int mServiceConnectionRetry;
    private final IBinder.DeathRecipient mDeathRecipient;

    ServiceConnection(
            Context context,
            ConnectionConfiguration connectionConfiguration,
            ExecutionTracker executionTracker,
            Callback callback) {
        this.mContext = checkNotNull(context);
        this.mConnectionConfiguration = checkNotNull(connectionConfiguration);
        this.mExecutionTracker = checkNotNull(executionTracker);
        this.mCallback = checkNotNull(callback);
        this.mDeathRecipient = () -> {
            Logger.warning(
                    TAG,
                    "Binder died for client:"
                            + mConnectionConfiguration.getClientName());
            handleRetriableDisconnection(new RemoteException("Binder died"));
        };
    }

    private String getBindPackageName() {
        if (mCallback.isBindToSelfEnabled()) {
            return mContext.getPackageName();
        } else {
            return mConnectionConfiguration.getPackageName();
        }
    }

    /** Connects to the service. */
    public void connect() {
        if (mIsServiceBound) {
            return;
        }
        try {
            mIsServiceBound =
                    mContext.bindService(
                            new Intent()
                                    .setPackage(getBindPackageName())
                                    .setAction(mConnectionConfiguration.getBindAction()),
                            this,
                            Context.BIND_AUTO_CREATE | Context.BIND_ADJUST_WITH_ACTIVITY);
        } catch (SecurityException exception) {
            Logger.warning(
                    TAG,
                    "Failed to bind connection '"
                            + mConnectionConfiguration.getKey()
                            + "', no permission or service not found.",
                    exception);
            mIsServiceBound = false;
            mBinder = null;
            throw exception;
        }

        if (!mIsServiceBound) {
            // Service not found or we don't have permission to call it.
            Logger.error(
                    TAG,
                    "Connection to service is not available for package '"
                            + mConnectionConfiguration.getPackageName()
                            + "' and action '"
                            + mConnectionConfiguration.getBindAction()
                            + "'.");
            handleNonRetriableDisconnection(new RemoteException("Binding to service failed"));
        }
    }

    private void handleNonRetriableDisconnection(Throwable throwable) {
        // Set retry count to maximum to prevent retries
        mServiceConnectionRetry = MAX_RETRIES;
        handleRetriableDisconnection(throwable);
    }

    private synchronized void handleRetriableDisconnection(Throwable throwable) {
        if (isConnected()) {
            // Connection is already re-established. So just return.
            Logger.warning(TAG, "Connection is already re-established. No need to reconnect again");
            return;
        }

        clearConnection(throwable);

        if (mServiceConnectionRetry < MAX_RETRIES) {
            Logger.warning(
                    TAG,
                    "WCS SDK Client '"
                            + mConnectionConfiguration.getClientName()
                            + "' disconnected, retrying connection. Retry attempt: "
                            + mServiceConnectionRetry,
                    throwable);
            mCallback.onDisconnected(this, getRetryDelayMs(mServiceConnectionRetry));
        } else {
            Logger.error(TAG, "Connection disconnected and maximum number of retries reached.",
                    throwable);
        }
    }

    private static int getRetryDelayMs(int retryNumber) {
        // Exponential retry delay starting on 200ms.
        return (200 << retryNumber);
    }

    /**
     * Unbinds the service if there is no pending operation queued.
     *
     * @return true if the service is idle and unbind service is attempted.
     */
    boolean clearConnectionIfIdle() {
        if (mOperationQueue.isEmpty() && mRegisteredListeners.isEmpty()) {
            tryClearConnection();
            return true;
        }
        return false;
    }

    @VisibleForTesting
    void clearConnection(Throwable throwable) {
        tryClearConnection();
        mExecutionTracker.cancelPendingFutures(throwable);
        cancelAllOperationsInQueue(throwable);
    }

    private void tryClearConnection() {
        // See Android Service unbind code sample.
        // https://developer.android.com/reference/android/app/Service#local-service-sample
        if (mIsServiceBound) {
            try {
                mContext.unbindService(this);
            } catch (IllegalArgumentException e) {
                // In the unlikely scenario that we couldn't unbind the service, we will continue
                // assuming the service is invalid. Future operations will try connect to service
                // again.
                Logger.error(TAG, "Failed to unbind the service. Ignoring and continuing", e);
            }
            mIsServiceBound = false;
        }
        if (mBinder != null) {
            try {
                mBinder.unlinkToDeath(mDeathRecipient, 0);
            } catch (NoSuchElementException e) {
                Logger.error(TAG, "mDeathRecipient not linked", e);
            }
            mBinder = null;
        }
        Logger.debug(TAG, "unbindService called");
    }

    void enqueue(QueueOperation operation) {
        if (isConnected()) {
            execute(operation);
        } else {
            mOperationQueue.add(operation);
            connect();
        }
    }

    void registerListener(ListenerKey listenerKey, QueueOperation registerListenerOperation) {
        mRegisteredListeners.put(listenerKey, registerListenerOperation);
        if (isConnected()) {
            enqueue(registerListenerOperation);
        } else {
            connect();
        }
    }

    void unregisterListener(ListenerKey listenerKey, QueueOperation unregisterListenerOperation) {
        mRegisteredListeners.remove(listenerKey);
        enqueue(unregisterListenerOperation);
    }

    void maybeReconnect() {
        if (mRegisteredListeners.isEmpty()) {
            Logger.debug(
                    TAG,
                    "No listeners registered, service "
                            + mConnectionConfiguration.getClientName()
                            + " is not automatically reconnected.");
        } else {
            mServiceConnectionRetry++;
            Logger.debug(
                    TAG,
                    "Listeners for service "
                            + mConnectionConfiguration.getClientName()
                            + " are registered, reconnecting.");
            connect();
        }
    }

    @VisibleForTesting
    void execute(QueueOperation operation) {
        try {
            operation.trackExecution(mExecutionTracker);
            operation.execute(checkNotNull(mBinder));
        } catch (DeadObjectException exception) {
            handleRetriableDisconnection(exception);
            // TODO(b/152024821): Consider possible TransactionTooLargeException failure.
        } catch (RemoteException | RuntimeException exception) {
            operation.setException(exception);
        }
    }

    void reRegisterAllListeners() {
        for (Map.Entry<ListenerKey, QueueOperation> entry : mRegisteredListeners.entrySet()) {
            Logger.debug(TAG, "Re-registering listener: " + entry.getKey());
            execute(entry.getValue());
        }
    }

    void refreshServiceVersion() {
        mOperationQueue.add(mConnectionConfiguration.getRefreshVersionOperation());
    }

    void flushQueue() {
        for (QueueOperation operation : new ArrayList<>(mOperationQueue)) {
            boolean removed = mOperationQueue.remove(operation);
            if (removed) {
                execute(operation);
            }
        }
    }

    private void cancelAllOperationsInQueue(Throwable throwable) {
        for (QueueOperation operation : new ArrayList<>(mOperationQueue)) {
            boolean removed = mOperationQueue.remove(operation);
            if (removed) {
                operation.setException(throwable);
            }
        }
    }

    private boolean isConnected() {
        return mBinder != null && mBinder.isBinderAlive();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        Logger.debug(TAG, "onServiceConnected(), componentName = " + componentName);
        if (binder == null) {
            Logger.error(TAG, "Service connected but binder is null.");
            return;
        }
        mServiceConnectionRetry = 0;
        cleanOnDeath(binder);
        this.mBinder = binder;
        mCallback.onConnected(this);
    }

    private void cleanOnDeath(IBinder binder) {
        try {
            binder.linkToDeath(mDeathRecipient, /* flags= */ 0);
        } catch (RemoteException exception) {
            Logger.warning(
                    TAG,
                    "Cannot link to death, binder already died. Cleaning operations.",
                    exception);
            handleRetriableDisconnection(exception);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Logger.debug(TAG, "onServiceDisconnected(), componentName = " + componentName);
        // Service disconnected but binding still exists so it should reconnect automatically.
    }

    @Override
    public void onBindingDied(ComponentName name) {
        Logger.error(TAG,
                "Binding died for client '" + mConnectionConfiguration.getClientName() + "'.");
        handleRetriableDisconnection(new RemoteException("Binding died"));
    }

    @Override
    public void onNullBinding(ComponentName name) {
        Logger.error(
                TAG,
                "Cannot bind client '"
                        + mConnectionConfiguration.getClientName()
                        + "', binder is null");
        // This connection will never be usable, don't bother with retries.
        handleRetriableDisconnection(new RemoteException("Null binding"));
    }
}
