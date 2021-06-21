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

package androidx.health.services.client.impl.ipc;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.health.services.client.impl.ipc.internal.BaseQueueOperation;
import androidx.health.services.client.impl.ipc.internal.ConnectionConfiguration;
import androidx.health.services.client.impl.ipc.internal.ConnectionManager;
import androidx.health.services.client.impl.ipc.internal.ExecutionTracker;
import androidx.health.services.client.impl.ipc.internal.ListenerKey;
import androidx.health.services.client.impl.ipc.internal.QueueOperation;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

/**
 * SDK client for establishing connection to a cross process service.
 *
 * <p>Extend this class to create a new client. Each client should represent one connection to AIDL
 * interface. For user instruction see: go/wear-dd-wcs-sdk
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public abstract class Client {

    /** Interface abstracting extraction of the API version from the binder. */
    public interface VersionGetter {
        /** Returns the API version. */
        Integer readVersion(IBinder binder) throws RemoteException;
    }

    private static final int UNKNOWN_VERSION = -1;

    private final ConnectionConfiguration mConnectionConfiguration;
    private final ConnectionManager mConnectionManager;
    private final ServiceOperation<Integer> mApiVersionOperation;

    @VisibleForTesting volatile int mCurrentVersion = UNKNOWN_VERSION;

    public Client(
            ClientConfiguration clientConfiguration,
            ConnectionManager connectionManager,
            VersionGetter versionGetter) {
        QueueOperation versionOperation =
                new QueueOperation() {
                    @Override
                    public void execute(IBinder binder) throws RemoteException {
                        mCurrentVersion = versionGetter.readVersion(binder);
                    }

                    @Override
                    public void setException(Throwable exception) {}

                    @Override
                    public QueueOperation trackExecution(ExecutionTracker tracker) {
                        return this;
                    }

                    @Override
                    public ConnectionConfiguration getConnectionConfiguration() {
                        return Client.this.getConnectionConfiguration();
                    }
                };
        this.mConnectionConfiguration =
                new ConnectionConfiguration(
                        clientConfiguration.getServicePackageName(),
                        clientConfiguration.getApiClientName(),
                        clientConfiguration.getBindAction(),
                        versionOperation);
        this.mConnectionManager = connectionManager;
        this.mApiVersionOperation =
                (binder, resultFuture) -> resultFuture.set(versionGetter.readVersion(binder));
    }

    /**
     * Executes given operation against a IPC service defined by {@code clientConfiguration}.
     *
     * @param operation Operation that will be executed against the service
     * @param <R> Type of returned variable
     * @return {@link ListenableFuture<R>} with the result of the operation or an exception if the
     *     execution fails.
     */
    protected <R> ListenableFuture<R> execute(ServiceOperation<R> operation) {
        SettableFuture<R> settableFuture = SettableFuture.create();
        mConnectionManager.scheduleForExecution(
                createQueueOperation(operation, mConnectionConfiguration, settableFuture));
        return settableFuture;
    }

    protected <R> ListenableFuture<R> executeWithVersionCheck(
            ServiceOperation<R> operation, int minApiVersion) {
        if (mCurrentVersion == UNKNOWN_VERSION) {
            SettableFuture<R> settableFuture = SettableFuture.create();
            ListenableFuture<Integer> versionFuture = execute(mApiVersionOperation);
            Futures.addCallback(
                    versionFuture,
                    new FutureCallback<Integer>() {
                        @Override
                        public void onSuccess(@Nullable Integer remoteVersion) {
                            mCurrentVersion =
                                    remoteVersion == null ? UNKNOWN_VERSION : remoteVersion;
                            if (mCurrentVersion < minApiVersion) {
                                settableFuture.setException(
                                        getApiVersionCheckFailureException(
                                                mCurrentVersion, minApiVersion));
                            } else {
                                getConnectionManager()
                                        .scheduleForExecution(
                                                createQueueOperation(
                                                        operation,
                                                        getConnectionConfiguration(),
                                                        settableFuture));
                            }
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            settableFuture.setException(throwable);
                        }
                    },
                    MoreExecutors.directExecutor());
            return settableFuture;
        } else if (mCurrentVersion >= minApiVersion) {
            return execute(operation);
        } else {
            // This empty operation is executed just to connect to the service. If we didn't connect
            // it
            // could happen that we won't detect change in the API version.
            mConnectionManager.scheduleForExecution(
                    new BaseQueueOperation(mConnectionConfiguration));
            return Futures.immediateFailedFuture(
                    getApiVersionCheckFailureException(mCurrentVersion, minApiVersion));
        }
    }

    /**
     * Registers a listener by executing the provided {@link ServiceOperation}.
     *
     * <p>The provided {@code registerListenerOperation} will be stored for every unique {@code
     * listenerKey} and re-executed when connection is lost.
     *
     * @param listenerKey Key based on which listeners will be distinguished.
     * @param registerListenerOperation Method that registers the listener, can by any {@link
     *     ServiceOperation}.
     * @param <R> Type of return value returned in the future.
     * @return {@link ListenableFuture<R>} with the result of the operation or an exception if the
     *     execution fails.
     */
    protected <R> ListenableFuture<R> registerListener(
            ListenerKey listenerKey, ServiceOperation<R> registerListenerOperation) {
        SettableFuture<R> settableFuture = SettableFuture.create();
        mConnectionManager.registerListener(
                listenerKey,
                createQueueOperation(
                        registerListenerOperation, mConnectionConfiguration, settableFuture));
        return settableFuture;
    }

    /**
     * Unregisters a listener by executing the provided {@link ServiceOperation}.
     *
     * @param listenerKey Key based on which listeners will be distinguished.
     * @param unregisterListenerOperation Method that unregisters the listener, can by any {@link
     *     ServiceOperation}.
     * @param <R> Type of return value returned in the future.
     * @return {@link ListenableFuture<R>} with the result of the operation or an exception if the
     *     execution fails.
     */
    protected <R> ListenableFuture<R> unregisterListener(
            ListenerKey listenerKey, ServiceOperation<R> unregisterListenerOperation) {
        SettableFuture<R> settableFuture = SettableFuture.create();
        mConnectionManager.unregisterListener(
                listenerKey,
                createQueueOperation(
                        unregisterListenerOperation, getConnectionConfiguration(), settableFuture));
        return settableFuture;
    }

    protected Exception getApiVersionCheckFailureException(int currentVersion, int minApiVersion) {
        return new ApiVersionException(currentVersion, minApiVersion);
    }

    ConnectionConfiguration getConnectionConfiguration() {
        return mConnectionConfiguration;
    }

    ConnectionManager getConnectionManager() {
        return mConnectionManager;
    }

    private static <R> QueueOperation createQueueOperation(
            ServiceOperation<R> operation,
            ConnectionConfiguration connectionConfiguration,
            SettableFuture<R> settableFuture) {
        return new BaseQueueOperation(connectionConfiguration) {
            @Override
            public void execute(IBinder binder) throws RemoteException {
                operation.execute(binder, settableFuture);
            }

            @Override
            public void setException(Throwable exception) {
                settableFuture.setException(exception);
            }

            @Override
            public QueueOperation trackExecution(ExecutionTracker tracker) {
                tracker.track(settableFuture);
                return this;
            }
        };
    }
}
