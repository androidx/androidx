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

import static androidx.work.multiprocess.ListenableCallback.ListenableCallbackRunnable.reportFailure;
import static androidx.work.multiprocess.RemoteClientUtils.map;
import static androidx.work.multiprocess.RemoteClientUtils.sVoidMapper;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.core.os.HandlerCompat;
import androidx.work.Data;
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
import androidx.work.multiprocess.parcelable.ParcelableUpdateRequest;
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
@SuppressLint("BanKeepAnnotation")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteWorkManagerClient extends RemoteWorkManager {

    /* The session timeout. */
    private static final long SESSION_TIMEOUT_MILLIS = 60 * 1000;

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("RemoteWorkManagerClient");

    // Synthetic access
    Session mSession;

    final Context mContext;
    final WorkManagerImpl mWorkManager;
    final Executor mExecutor;
    final Object mLock;

    private volatile long mSessionIndex;
    private final long mSessionTimeout;
    private final Handler mHandler;
    private final SessionTracker mSessionTracker;

    public RemoteWorkManagerClient(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        this(context, workManager, SESSION_TIMEOUT_MILLIS);
    }

    public RemoteWorkManagerClient(
            @NonNull Context context,
            @NonNull WorkManagerImpl workManager,
            long sessionTimeout) {
        mContext = context.getApplicationContext();
        mWorkManager = workManager;
        mExecutor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
        mLock = new Object();
        mSession = null;
        mSessionTracker = new SessionTracker(this);
        mSessionTimeout = sessionTimeout;
        mHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueue(@NonNull WorkRequest request) {
        return enqueue(Collections.singletonList(request));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enqueue(@NonNull final List<WorkRequest> requests) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
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
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
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
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
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
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
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
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
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
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
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
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
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

    @NonNull
    @Override
    public ListenableFuture<Void> setProgress(@NonNull final UUID id, @NonNull final Data data) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                byte[] request = ParcelConverters.marshall(new ParcelableUpdateRequest(id, data));
                iWorkManagerImpl.setProgress(request, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    /**
     * Executes a {@link RemoteDispatcher} after having negotiated a service connection.
     *
     * @param dispatcher The {@link RemoteDispatcher} instance.
     * @return The {@link ListenableFuture} instance.
     */
    @NonNull
    public ListenableFuture<byte[]> execute(
            @NonNull final RemoteDispatcher<IWorkManagerImpl> dispatcher) {
        return execute(getSession(), dispatcher, new SessionRemoteCallback(this));
    }

    /**
     * Gets a handle to an instance of {@link IWorkManagerImpl} by binding to the
     * {@link RemoteWorkManagerService} if necessary.
     */
    @NonNull
    public ListenableFuture<IWorkManagerImpl> getSession() {
        return getSession(newIntent(mContext));
    }

    /**
     * @return The application {@link Context}.
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * @return The session timeout in milliseconds.
     */
    public long getSessionTimeout() {
        return mSessionTimeout;
    }

    /**
     * @return The current {@link Session} in use by {@link RemoteWorkManagerClient}.
     */
    @Nullable
    public Session getCurrentSession() {
        return mSession;
    }

    /**
     * @return The {@link Handler} managing session timeouts.
     */
    @NonNull
    public Handler getSessionHandler() {
        return mHandler;
    }

    /**
     * @return the {@link SessionTracker} instance.
     */
    @NonNull
    public SessionTracker getSessionTracker() {
        return mSessionTracker;
    }

    /**
     * @return The {@link Object} session lock.
     */
    @NonNull
    public Object getSessionLock() {
        return mLock;
    }

    /**
     * @return The background {@link Executor} used by {@link RemoteWorkManagerClient}.
     */
    @NonNull
    public Executor getExecutor() {
        return mExecutor;
    }

    /**
     * @return The session index.
     */
    public long getSessionIndex() {
        return mSessionIndex;
    }

    @NonNull
    @VisibleForTesting
    ListenableFuture<byte[]> execute(
            @NonNull final ListenableFuture<IWorkManagerImpl> session,
            @NonNull final RemoteDispatcher<IWorkManagerImpl> dispatcher,
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
                                reportFailure(callback, innerThrowable);
                            }
                        }
                    });
                } catch (ExecutionException | InterruptedException exception) {
                    Logger.get().error(TAG, "Unable to bind to service");
                    reportFailure(callback, new RuntimeException("Unable to bind to service"));
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
            mSessionIndex += 1;
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
            // Reset session tracker.
            mHandler.removeCallbacks(mSessionTracker);
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
            onBindingDied();
        }

        /**
         * Clean-up client when a binding dies.
         */
        public void onBindingDied() {
            Logger.get().debug(TAG, "Binding died");
            mFuture.setException(new RuntimeException("Binding died"));
            mClient.cleanUp();
        }

        @Override
        public void onNullBinding(@NonNull ComponentName name) {
            Logger.get().error(TAG, "Unable to bind to service");
            mFuture.setException(
                    new RuntimeException("Cannot bind to service " + name));
        }
    }

    /**
     * An extension of {@link RemoteCallback} that kills a {@link Session} after a timeout has
     * elapsed.
     */
    public static class SessionRemoteCallback extends RemoteCallback {
        private final RemoteWorkManagerClient mClient;

        public SessionRemoteCallback(@NonNull RemoteWorkManagerClient client) {
            mClient = client;
        }

        @Override
        protected void onRequestCompleted() {
            super.onRequestCompleted();
            Handler handler = mClient.getSessionHandler();
            SessionTracker tracker = mClient.getSessionTracker();
            // Start tracking for session timeout.
            // These callbacks are removed when the session timeout has expired or when getSession()
            // is called.
            handler.postDelayed(tracker, mClient.getSessionTimeout());
        }
    }

    /**
     * A {@link Runnable} that enforces a TTL for a {@link RemoteWorkManagerClient} session.
     */
    public static class SessionTracker implements Runnable {
        private static final String TAG = Logger.tagWithPrefix("SessionHandler");
        private final RemoteWorkManagerClient mClient;

        public SessionTracker(@NonNull RemoteWorkManagerClient client) {
            mClient = client;
        }

        @Override
        public void run() {
            final long preLockIndex = mClient.getSessionIndex();
            synchronized (mClient.getSessionLock()) {
                final long sessionIndex = mClient.getSessionIndex();
                final Session currentSession = mClient.getCurrentSession();
                // We check for a session index here. This is because if the index changes
                // while we acquire a lock, that would mean that a new session request came through.
                if (currentSession != null) {
                    if (preLockIndex == sessionIndex) {
                        Logger.get().debug(TAG, "Unbinding service");
                        mClient.getContext().unbindService(currentSession);
                        // Cleanup as well.
                        currentSession.onBindingDied();
                    } else {
                        Logger.get().debug(TAG, "Ignoring request to unbind.");
                    }
                }
            }
        }
    }
}
