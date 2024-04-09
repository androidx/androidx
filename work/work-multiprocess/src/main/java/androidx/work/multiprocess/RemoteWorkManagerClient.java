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

import static androidx.work.multiprocess.RemoteClientUtilsKt.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.work.Data;
import androidx.work.DirectExecutor;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.RunnableScheduler;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableForegroundRequestInfo;
import androidx.work.multiprocess.parcelable.ParcelableUpdateRequest;
import androidx.work.multiprocess.parcelable.ParcelableWorkContinuationImpl;
import androidx.work.multiprocess.parcelable.ParcelableWorkInfos;
import androidx.work.multiprocess.parcelable.ParcelableWorkQuery;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequest;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * The implementation of the {@link RemoteWorkManager} which sets up the
 * {@link android.content.ServiceConnection} and dispatches the request.
 */
@SuppressLint("BanKeepAnnotation")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteWorkManagerClient extends RemoteWorkManager {

    /* The session timeout. */
    private static final long SESSION_TIMEOUT_MILLIS = 6000 * 1000;

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("RemoteWorkManagerClient");

    /**
     * A mapper that essentially drops the byte[].
     */
    public static final Function<byte[], Void> sVoidMapper = input -> null;

    // Synthetic access
    Session<IWorkManagerImpl> mSession;

    final Context mContext;
    final WorkManagerImpl mWorkManager;
    final Executor mExecutor;
    final Object mLock;

    private volatile long mSessionIndex;
    private final long mSessionTimeout;
    private final RunnableScheduler mRunnableScheduler;
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
        mExecutor = mWorkManager.getWorkTaskExecutor().getSerialTaskExecutor();
        mLock = new Object();
        mSession = null;
        mSessionTracker = new SessionTracker(this);
        mSessionTimeout = sessionTimeout;
        mRunnableScheduler = mWorkManager.getConfiguration().getRunnableScheduler();
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
        if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.UPDATE) {
            ListenableFuture<byte[]> result = execute((iWorkManagerImpl, callback) -> {
                byte[] request = ParcelConverters.marshall(new ParcelableWorkRequest(periodicWork));
                iWorkManagerImpl.updateUniquePeriodicWorkRequest(uniqueWorkName, request, callback);
            });
            return map(result, sVoidMapper, mExecutor);
        }
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

    @NonNull
    @Override
    public ListenableFuture<Void> setForegroundAsync(
            @NonNull String id,
            @NonNull ForegroundInfo foregroundInfo) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                byte[] request = ParcelConverters.marshall(
                        new ParcelableForegroundRequestInfo(id, foregroundInfo));
                iWorkManagerImpl.setForegroundAsync(request, callback);
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
        return execute(getSession(), dispatcher);
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
    Session<IWorkManagerImpl> getCurrentSession() {
        return mSession;
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
            @NonNull final RemoteDispatcher<IWorkManagerImpl> dispatcher) {
        ListenableFuture<byte[]> future = RemoteExecuteKt.execute(mExecutor, session,
                dispatcher);
        future.addListener(() -> {
            SessionTracker tracker = getSessionTracker();
            // Start tracking for session timeout.
            // These callbacks are removed when the session timeout has expired or when getSession()
            // is called.
            mRunnableScheduler.scheduleWithDelay(getSessionTimeout(), tracker);
        }, mExecutor);
        return future;
    }

    @NonNull
    @VisibleForTesting
    ListenableFuture<IWorkManagerImpl> getSession(@NonNull Intent intent) {
        synchronized (mLock) {
            mSessionIndex += 1;
            ListenableFuture<IWorkManagerImpl> resultFuture;
            if (mSession == null) {
                mSession = ServiceBindingKt.bindToService(mContext, intent,
                        IWorkManagerImpl.Stub::asInterface, TAG);
                // reading future right away, because `this::cleanUp` will synchronously
                // set mSession to null.
                resultFuture = mSession.getConnectedFuture();
                mSession.getDisconnectedFuture()
                        .addListener(this::cleanUp, DirectExecutor.INSTANCE);
            } else {
                resultFuture = mSession.getConnectedFuture();
            }
            // Reset session tracker.
            mRunnableScheduler.cancel(mSessionTracker);
            return resultFuture;
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

    /**
     * @return the intent that is used to bind to the instance of {@link IWorkManagerImpl}.
     */
    private static Intent newIntent(@NonNull Context context) {
        return new Intent(context, RemoteWorkManagerService.class);
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
                final Session<IWorkManagerImpl> currentSession = mClient.getCurrentSession();
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
