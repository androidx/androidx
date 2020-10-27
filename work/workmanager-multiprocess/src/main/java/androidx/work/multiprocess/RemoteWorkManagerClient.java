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

import static androidx.work.multiprocess.ListenableCallback.ListenableCallbackRunnable.failureCallback;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableWorkContinuationImpl;
import androidx.work.multiprocess.parcelable.ParcelableWorkInfos;
import androidx.work.multiprocess.parcelable.ParcelableWorkQuery;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
    final WorkManagerImpl mWorkManager;
    final Executor mExecutor;
    final Object mLock;

    private Session mSession;

    @Keep
    public RemoteWorkManagerClient(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        mContext = context.getApplicationContext();
        mWorkManager = workManager;
        mExecutor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
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
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws RemoteException {
                byte[] request = ParcelConverters.marshall(new ParcelableWorkRequests(requests));
                iWorkManagerImpl.enqueueWorkRequests(request, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueueUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        return beginUniqueWork(uniqueWorkName, existingWorkPolicy, work).enqueue();
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork) {

        WorkContinuation continuation = mWorkManager.createWorkContinuationForUniquePeriodicWork(
                uniqueWorkName,
                existingPeriodicWorkPolicy,
                periodicWork
        );
        return enqueue(continuation);
    }

    @NonNull
    @Override
    public RemoteWorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work) {
        return new RemoteWorkContinuationImpl(this, mWorkManager.beginWith(work));
    }

    @NonNull
    @Override
    public RemoteWorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        return new RemoteWorkContinuationImpl(this,
                mWorkManager.beginUniqueWork(uniqueWorkName, existingWorkPolicy, work));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueue(@NonNull final WorkContinuation continuation) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                WorkContinuationImpl workContinuation = (WorkContinuationImpl) continuation;
                byte[] request = ParcelConverters.marshall(
                        new ParcelableWorkContinuationImpl(workContinuation));
                iWorkManagerImpl.enqueueContinuation(request, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelWorkById(@NonNull final UUID id) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelWorkById(id.toString(), callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelAllWorkByTag(@NonNull final String tag) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelAllWorkByTag(tag, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelUniqueWork(@NonNull final String uniqueWorkName) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelUniqueWork(uniqueWorkName, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelAllWork() {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelAllWork(callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @NonNull
    @Override
    public ListenableFuture<List<WorkInfo>> getWorkInfos(@NonNull final WorkQuery workQuery) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                byte[] request = ParcelConverters.marshall(new ParcelableWorkQuery(workQuery));
                iWorkManagerImpl.queryWorkInfo(request, callback);
            }
        });
        return map(result, new Function<byte[], List<WorkInfo>>() {
            @Override
            public List<WorkInfo> apply(byte[] input) {
                ParcelableWorkInfos infos =
                        ParcelConverters.unmarshall(input, ParcelableWorkInfos.CREATOR);
                return infos.getWorkInfos();
            }
        }, mExecutor);
    }

    /**
     * Executes a {@link RemoteDispatcher} after having negotiated a service connection.
     *
     * @param dispatcher The {@link RemoteDispatcher} instance.
     * @return The {@link ListenableFuture} instance.
     */
    @NonNull
    public ListenableFuture<byte[]> execute(@NonNull final RemoteDispatcher dispatcher) {
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
    ListenableFuture<byte[]> execute(
            @NonNull final ListenableFuture<IWorkManagerImpl> session,
            @NonNull final RemoteDispatcher dispatcher,
            @NonNull final RemoteCallback callback) {
        session.addListener(new Runnable() {
            @Override
            public void run() {
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
            }
        }, mExecutor);
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
     * A mapper that essentially drops the byte[].
     */
    private static final Function<byte[], Void> sVoidMapper = new Function<byte[], Void>() {
        @Override
        public Void apply(byte[] input) {
            return null;
        }
    };

    private static <I, O> ListenableFuture<O> map(
            @NonNull final ListenableFuture<I> input,
            @NonNull final Function<I, O> transformation,
            @NonNull Executor executor) {

        final SettableFuture<O> output = SettableFuture.create();
        input.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    I in = input.get();
                    O out = transformation.apply(in);
                    output.set(out);
                } catch (Throwable throwable) {
                    Throwable cause = throwable.getCause();
                    cause = cause == null ? throwable : cause;
                    output.setException(cause);
                }
            }
        }, executor);
        return output;
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
