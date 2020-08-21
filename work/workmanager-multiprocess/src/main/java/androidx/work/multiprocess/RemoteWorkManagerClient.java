/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.multiprocess;

import static android.content.Context.BIND_AUTO_CREATE;

import static androidx.work.multiprocess.OperationCallback.OperationCallbackRunnable.failureCallback;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;
import androidx.work.WorkRequest;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * The implementation of the {@link RemoteWorkManager} which sets up the
 * {@link android.content.ServiceConnection} and dispatches the request.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteWorkManagerClient extends RemoteWorkManager {

    static final String TAG = Logger.tagWithPrefix("RemoteWorkManagerClient");

    final Context mContext;
    final Executor mExecutor;
    final Object mLock;

    private Session mSession;

    public RemoteWorkManagerClient(@NonNull Context context, @NonNull Executor executor) {
        mContext = context.getApplicationContext();
        mExecutor = executor;
        mLock = new Object();
        mSession = null;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueue(@NonNull WorkRequest request) {
        return enqueue(Collections.singletonList(request));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueue(@NonNull final List<WorkRequest> requests) {
        return execute(new RemoteDispatcher() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws RemoteException {
                byte[] request = ParcelConverters.marshall(new ParcelableWorkRequests(requests));
                iWorkManagerImpl.enqueueWorkRequests(request, callback);
            }
        });
    }

    /**
     * Executes a {@link RemoteDispatcher} after having negotiated a service connection.
     *
     * @param dispatcher The {@link RemoteDispatcher} instance.
     * @return The {@link ListenableFuture} instance.
     */
    @NonNull
    public ListenableFuture<Void> execute(@NonNull final RemoteDispatcher dispatcher) {
        return execute(getSession(), dispatcher, new RemoteCallback());
    }

    /**
     * Gets a handle to an instance of {@link IWorkManagerImpl} by binding to the
     * {@link RemoteWorkManagerService} if necessary.
     */
    @NonNull
    public ListenableFuture<IWorkManagerImpl> getSession() {
        return getSession(newIntent(mContext));
    }

    @NonNull
    @VisibleForTesting
    ListenableFuture<Void> execute(
            @NonNull ListenableFuture<IWorkManagerImpl> session,
            @NonNull final RemoteDispatcher dispatcher,
            @NonNull final RemoteCallback callback) {
        try {
            final IWorkManagerImpl iWorkManager = session.get();
            // Set the binder to scope the request
            callback.setBinder(iWorkManager.asBinder());
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        dispatcher.execute(iWorkManager, callback);
                    } catch (Throwable innerThrowable) {
                        Logger.get().error(TAG, "Unable to execute", innerThrowable);
                        failureCallback(callback, innerThrowable);
                    }
                }
            });
        } catch (ExecutionException | InterruptedException exception) {
            Logger.get().error(TAG, "Unable to bind to service");
            failureCallback(callback, new RuntimeException("Unable to bind to service"));
            cleanUp();
        }
        return callback.getFuture();
    }

    @NonNull
    @VisibleForTesting
    ListenableFuture<IWorkManagerImpl> getSession(@NonNull Intent intent) {
        synchronized (mLock) {
            if (mSession == null) {
                Logger.get().debug(TAG, "Creating a new session");
                mSession = new Session(this);
                try {
                    boolean bound = mContext.bindService(intent, mSession, BIND_AUTO_CREATE);
                    if (!bound) {
                        unableToBind(mSession, new RuntimeException("Unable to bind to service"));
                    }
                } catch (Throwable throwable) {
                    unableToBind(mSession, throwable);
                }
            }
            return mSession.mFuture;
        }
    }

    /**
     * Cleans up a session. This could happen when we are unable to bind to the service or
     * we get disconnected.
     */
    public void cleanUp() {
        synchronized (mLock) {
            Logger.get().debug(TAG, "Cleaning up.");
            mSession = null;
        }
    }

    private void unableToBind(@NonNull Session session, @NonNull Throwable throwable) {
        Logger.get().error(TAG, "Unable to bind to service", throwable);
        session.mFuture.setException(throwable);
    }

    /**
     * @return the intent that is used to bind to the instance of {@link IWorkManagerImpl}.
     */
    private static Intent newIntent(@NonNull Context context) {
        return new Intent(context, RemoteWorkManagerService.class);
    }

    /**
     * @hide
     */
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface RemoteDispatcher {
        /**
         * Perform the actual work given an instance of {@link IWorkManagerImpl} and the
         * {@link IWorkManagerImplCallback} callback.
         */
        void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                @NonNull IWorkManagerImplCallback callback) throws Throwable;
    }

    /**
     * The implementation of {@link ServiceConnection} that handles changes in the connection.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Session implements ServiceConnection {
        private static final String TAG = Logger.tagWithPrefix("RemoteWMgr.Connection");

        final SettableFuture<IWorkManagerImpl> mFuture;
        final RemoteWorkManagerClient mClient;

        public Session(@NonNull RemoteWorkManagerClient client) {
            mClient = client;
            mFuture = SettableFuture.create();
        }

        @Override
        public void onServiceConnected(
                @NonNull ComponentName componentName,
                @NonNull IBinder iBinder) {
            Logger.get().debug(TAG, "Service connected");
            IWorkManagerImpl iWorkManagerImpl = IWorkManagerImpl.Stub.asInterface(iBinder);
            mFuture.set(iWorkManagerImpl);
        }

        @Override
        public void onServiceDisconnected(@NonNull ComponentName componentName) {
            Logger.get().debug(TAG, "Service disconnected");
            mFuture.setException(new RuntimeException("Service disconnected"));
            mClient.cleanUp();
        }

        @Override
        public void onBindingDied(@NonNull ComponentName name) {
            Logger.get().debug(TAG, "Binding died");
            mFuture.setException(new RuntimeException("Binding died"));
            mClient.cleanUp();
        }

        @Override
        public void onNullBinding(@NonNull ComponentName name) {
            Logger.get().error(TAG, "Unable to bind to service");
            mFuture.setException(
                    new RuntimeException(String.format("Cannot bind to service %s", name)));
        }
    }
}
