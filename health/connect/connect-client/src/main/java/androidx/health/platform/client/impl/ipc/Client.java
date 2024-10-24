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

package androidx.health.platform.client.impl.ipc;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.health.platform.client.impl.ipc.internal.BaseQueueOperation;
import androidx.health.platform.client.impl.ipc.internal.ConnectionConfiguration;
import androidx.health.platform.client.impl.ipc.internal.ConnectionManager;
import androidx.health.platform.client.impl.ipc.internal.ExecutionTracker;
import androidx.health.platform.client.impl.ipc.internal.ListenerKey;
import androidx.health.platform.client.impl.ipc.internal.QueueOperation;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import org.jspecify.annotations.NonNull;

/**
 * Client for establishing connection to a cross process service.
 *
 * <p>Extend this class to create a new client. Each client should represent one connection to AIDL
 * interface. For user instruction see: go/wear-dd-wcs-sdk
 *
 * @param <S> type of the service interface
 */
@RestrictTo(Scope.LIBRARY)
public abstract class Client<S extends IInterface> {

    private static final int UNKNOWN_VERSION = -1;

    protected final ConnectionConfiguration mConnectionConfiguration;
    protected final ConnectionManager mConnectionManager;
    private final ServiceGetter<S> mServiceGetter;
    private final RemoteOperation<S, Integer> mRemoteVersionGetter;

    protected volatile int mCurrentVersion = UNKNOWN_VERSION;

    public Client(
            @NonNull ClientConfiguration clientConfiguration,
            @NonNull ConnectionManager connectionManager,
            @NonNull ServiceGetter<S> serviceGetter,
            @NonNull RemoteOperation<S, Integer> remoteVersionGetter) {
        QueueOperation versionOperation =
                new QueueOperation() {
                    @Override
                    public void execute(IBinder binder) throws RemoteException {
                        mCurrentVersion =
                                remoteVersionGetter.execute(serviceGetter.getService(binder));
                    }

                    @Override
                    public void setException(Throwable exception) {}

                    @Override
                    public QueueOperation trackExecution(ExecutionTracker tracker) {
                        return this;
                    }

                    @Override
                    public ConnectionConfiguration getConnectionConfiguration() {
                        return mConnectionConfiguration;
                    }
                };
        this.mConnectionConfiguration =
                new ConnectionConfiguration(
                        clientConfiguration.getServicePackageName(),
                        clientConfiguration.getApiClientName(),
                        clientConfiguration.getBindAction(),
                        versionOperation);
        this.mConnectionManager = connectionManager;
        this.mServiceGetter = serviceGetter;
        this.mRemoteVersionGetter = remoteVersionGetter;
    }

    /**
     * Executes given {@code operation} against the service.
     *
     * @see #execute(RemoteFutureOperation)
     */
    protected <R> @NonNull ListenableFuture<R> execute(@NonNull RemoteOperation<S, R> operation) {
        return execute((service, resultFuture) -> resultFuture.set(operation.execute(service)));
    }

    /**
     * Executes given {@code operation} against the service and the result future.
     *
     * @param operation operation that will be executed against the service and the result future
     * @param <R> type of the result value returned in the future
     * @return {@link ListenableFuture} with the result of the operation or an exception if the
     *     execution fails.
     */
    protected <R> @NonNull ListenableFuture<R> execute(
            @NonNull RemoteFutureOperation<S, R> operation) {
        SettableFuture<R> settableFuture = SettableFuture.create();
        mConnectionManager.scheduleForExecution(createQueueOperation(operation, settableFuture));
        return settableFuture;
    }

    /**
     * Executes given {@code operation} against the service and the result future with version
     * check.
     *
     * @param operation operation that will be executed against the service and the result future
     * @param <R> type of the result value returned in the future
     * @return {@link ListenableFuture} with the result of the operation or an exception if the
     *     execution fails or if the remote service version is lower than {@code minApiVersion}
     */
    protected <R> @NonNull ListenableFuture<R> executeWithVersionCheck(
            int minApiVersion, @NonNull RemoteFutureOperation<S, R> operation) {
        SettableFuture<R> settableFuture = SettableFuture.create();
        ListenableFuture<Integer> versionFuture =
                getCurrentRemoteVersion(/* forceRefresh= */ false);
        Futures.addCallback(
                versionFuture,
                new FutureCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer remoteVersion) {
                        if (remoteVersion < minApiVersion) {
                            // This empty operation is executed just to connect to the service.
                            // If we didn't connect it could happen that we won't detect
                            // change in the API version.
                            mConnectionManager.scheduleForExecution(
                                    new BaseQueueOperation(mConnectionConfiguration));

                            settableFuture.setException(
                                    getApiVersionCheckFailureException(
                                            remoteVersion, minApiVersion));
                        } else {
                            mConnectionManager.scheduleForExecution(
                                    createQueueOperation(operation, settableFuture));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                        settableFuture.setException(throwable);
                    }
                },
                MoreExecutors.directExecutor());
        return settableFuture;
    }

    /**
     * Return current version of the backing service implementation.
     *
     * <p>If current version is available from earlier calls, it would return the value from cache.
     */
    protected @NonNull ListenableFuture<Integer> getCurrentRemoteVersion(boolean forceRefresh) {
        if (mCurrentVersion == UNKNOWN_VERSION || forceRefresh) {
            return Futures.transform(
                    execute(mRemoteVersionGetter),
                    version -> {
                        mCurrentVersion = version;
                        return mCurrentVersion;
                    },
                    MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(mCurrentVersion);
        }
    }

    /**
     * Registers a listener by executing the provided {@link RemoteOperation
     * registerListenerOperation}.
     *
     * <p>The provided {@code registerListenerOperation} will be stored for every unique {@code
     * listenerKey} and re-executed when connection is lost.
     *
     * @param listenerKey key based on which listeners will be distinguished
     * @param registerListenerOperation {@link RemoteOperation} to register the listener
     * @param <R> return type of {@code registerListenerOperation}
     * @return {@link ListenableFuture} with the result of the operation or an exception if the
     *     execution fails
     */
    protected <R> @NonNull ListenableFuture<R> registerListener(
            @NonNull ListenerKey listenerKey,
            @NonNull RemoteOperation<S, R> registerListenerOperation
    ) {
        return registerListener(
                listenerKey,
                (service, resultFuture) ->
                        resultFuture.set(registerListenerOperation.execute(service)));
    }

    /**
     * Registers a listener by executing the provided {@link RemoteFutureOperation
     * registerListenerOperation}.
     *
     * <p>The provided {@code registerListenerOperation} will be stored for every unique {@code
     * listenerKey} and re-executed when connection is lost.
     *
     * @param listenerKey key based on which listeners will be distinguished
     * @param registerListenerOperation {@link RemoteFutureOperation} to register the listener
     * @param <R> return type of {@code registerListenerOperation}
     * @return {@link ListenableFuture} with the result of the operation or an exception if the
     *     execution fails
     */
    protected <R> @NonNull ListenableFuture<R> registerListener(
            @NonNull ListenerKey listenerKey,
            @NonNull RemoteFutureOperation<S, R> registerListenerOperation
    ) {
        SettableFuture<R> settableFuture = SettableFuture.create();
        mConnectionManager.registerListener(
                listenerKey, createQueueOperation(registerListenerOperation, settableFuture));
        return settableFuture;
    }

    /**
     * Unregisters a listener by executing the provided {@link RemoteOperation
     * unregisterListenerOperation}.
     *
     * @param listenerKey key based on which listeners will be distinguished
     * @param unregisterListenerOperation {@link RemoteOperation} to unregister the listener
     * @param <R> return type of {@code unregisterListenerOperation}
     * @return {@link ListenableFuture} with the result of the operation or an exception if the
     *     execution fails
     */
    protected <R> @NonNull ListenableFuture<R> unregisterListener(
            @NonNull ListenerKey listenerKey,
            @NonNull RemoteOperation<S, R> unregisterListenerOperation
    ) {
        return unregisterListener(
                listenerKey,
                (service, resultFuture) ->
                        resultFuture.set(unregisterListenerOperation.execute(service)));
    }

    /**
     * Unregisters a listener by executing the provided {@link RemoteFutureOperation
     * unregisterListenerOperation}.
     *
     * @param listenerKey key based on which listeners will be distinguished
     * @param unregisterListenerOperation {@link RemoteFutureOperation} to unregister the listener
     * @param <R> return type of {@code unregisterListenerOperation}
     * @return {@link ListenableFuture} with the result of the operation or an exception if the
     *     execution fails
     */
    protected <R> @NonNull ListenableFuture<R> unregisterListener(
            @NonNull ListenerKey listenerKey,
            @NonNull RemoteFutureOperation<S, R> unregisterListenerOperation
    ) {
        SettableFuture<R> settableFuture = SettableFuture.create();
        mConnectionManager.unregisterListener(
                listenerKey, createQueueOperation(unregisterListenerOperation, settableFuture));
        return settableFuture;
    }

    protected @NonNull Exception getApiVersionCheckFailureException(
            int currentVersion, int minApiVersion) {
        return new ApiVersionException(currentVersion, minApiVersion);
    }

    ConnectionConfiguration getConnectionConfiguration() {
        return mConnectionConfiguration;
    }

    ConnectionManager getConnectionManager() {
        return mConnectionManager;
    }

    <R> QueueOperation createQueueOperation(
            RemoteFutureOperation<S, R> operation, SettableFuture<R> settableFuture) {
        return new BaseQueueOperation(mConnectionConfiguration) {
            @Override
            public void execute(@NonNull IBinder binder) throws RemoteException {
                operation.execute(getService(binder), settableFuture);
            }

            @Override
            public void setException(@NonNull Throwable exception) {
                settableFuture.setException(exception);
            }

            @Override
            public @NonNull QueueOperation trackExecution(@NonNull ExecutionTracker tracker) {
                tracker.track(settableFuture);
                return this;
            }
        };
    }

    S getService(@NonNull IBinder binder) {
        return mServiceGetter.getService(binder);
    }

    /** Interface for obtaining the service instance from the binder. */
    protected interface ServiceGetter<S> {
        S getService(IBinder binder);
    }
}
