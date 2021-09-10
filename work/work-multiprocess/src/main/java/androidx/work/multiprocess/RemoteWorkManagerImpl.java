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


import static androidx.work.multiprocess.ListenableCallback.ListenableCallbackRunnable.reportFailure;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Operation;
import androidx.work.WorkInfo;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.WorkProgressUpdater;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableUpdateRequest;
import androidx.work.multiprocess.parcelable.ParcelableWorkContinuationImpl;
import androidx.work.multiprocess.parcelable.ParcelableWorkInfos;
import androidx.work.multiprocess.parcelable.ParcelableWorkQuery;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * The implementation of a subset of WorkManager APIs that are safe to be supported across
 * processes.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteWorkManagerImpl extends IWorkManagerImpl.Stub {

    // Synthetic access
    static byte[] sEMPTY = new byte[0];

    private final WorkManagerImpl mWorkManager;

    RemoteWorkManagerImpl(@NonNull Context context) {
        mWorkManager = WorkManagerImpl.getInstance(context);
    }

    @Override
    @MainThread
    public void enqueueWorkRequests(
            final @NonNull byte[] request,
            final @NonNull IWorkManagerImplCallback callback) {
        try {
            ParcelableWorkRequests parcelledRequests =
                    ParcelConverters.unmarshall(request, ParcelableWorkRequests.CREATOR);
            List<WorkRequest> workRequests = parcelledRequests.getRequests();
            final Operation operation = mWorkManager.enqueue(workRequests);
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final ListenableCallback<Operation.State.SUCCESS> listenableCallback =
                    new ListenableCallback<Operation.State.SUCCESS>(executor, callback,
                            operation.getResult()) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull Operation.State.SUCCESS result) {
                            return sEMPTY;
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void enqueueContinuation(
            final @NonNull byte[] request,
            final @NonNull IWorkManagerImplCallback callback) {
        try {
            ParcelableWorkContinuationImpl parcelledRequest =
                    ParcelConverters.unmarshall(request, ParcelableWorkContinuationImpl.CREATOR);
            WorkContinuationImpl continuation =
                    parcelledRequest.toWorkContinuationImpl(mWorkManager);
            final Operation operation = continuation.enqueue();
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final ListenableCallback<Operation.State.SUCCESS> listenableCallback =
                    new ListenableCallback<Operation.State.SUCCESS>(executor, callback,
                            operation.getResult()) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull Operation.State.SUCCESS result) {
                            return sEMPTY;
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void cancelWorkById(@NonNull String id, @NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelWorkById(UUID.fromString(id));
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final ListenableCallback<Operation.State.SUCCESS> listenableCallback =
                    new ListenableCallback<Operation.State.SUCCESS>(executor, callback,
                            operation.getResult()) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull Operation.State.SUCCESS result) {
                            return sEMPTY;
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void cancelAllWorkByTag(
            @NonNull String tag,
            @NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelAllWorkByTag(tag);
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final ListenableCallback<Operation.State.SUCCESS> listenableCallback =
                    new ListenableCallback<Operation.State.SUCCESS>(executor, callback,
                            operation.getResult()) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull Operation.State.SUCCESS result) {
                            return sEMPTY;
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void cancelUniqueWork(
            @NonNull String name,
            @NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelUniqueWork(name);
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final ListenableCallback<Operation.State.SUCCESS> listenableCallback =
                    new ListenableCallback<Operation.State.SUCCESS>(executor, callback,
                            operation.getResult()) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull Operation.State.SUCCESS result) {
                            return sEMPTY;
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void cancelAllWork(@NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelAllWork();
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final ListenableCallback<Operation.State.SUCCESS> listenableCallback =
                    new ListenableCallback<Operation.State.SUCCESS>(executor, callback,
                            operation.getResult()) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull Operation.State.SUCCESS result) {
                            return sEMPTY;
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void queryWorkInfo(@NonNull byte[] request, @NonNull IWorkManagerImplCallback callback) {
        try {
            ParcelableWorkQuery parcelled =
                    ParcelConverters.unmarshall(request, ParcelableWorkQuery.CREATOR);
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final ListenableFuture<List<WorkInfo>> future =
                    mWorkManager.getWorkInfos(parcelled.getWorkQuery());
            final ListenableCallback<List<WorkInfo>> listenableCallback =
                    new ListenableCallback<List<WorkInfo>>(executor, callback, future) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull List<WorkInfo> result) {
                            ParcelableWorkInfos parcelables = new ParcelableWorkInfos(result);
                            return ParcelConverters.marshall(parcelables);
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void setProgress(@NonNull byte[] request, @NonNull IWorkManagerImplCallback callback) {
        try {
            ParcelableUpdateRequest parcelled =
                    ParcelConverters.unmarshall(request, ParcelableUpdateRequest.CREATOR);
            final Context context = mWorkManager.getApplicationContext();
            final TaskExecutor taskExecutor = mWorkManager.getWorkTaskExecutor();
            final Executor executor = taskExecutor.getBackgroundExecutor();
            final WorkDatabase database = mWorkManager.getWorkDatabase();
            final WorkProgressUpdater progressUpdater =
                    new WorkProgressUpdater(database, taskExecutor);
            final ListenableFuture<Void> future = progressUpdater.updateProgress(
                    context,
                    UUID.fromString(parcelled.getId()),
                    parcelled.getData()
            );
            final ListenableCallback<Void> listenableCallback =
                    new ListenableCallback<Void>(executor, callback, future) {
                        @NonNull
                        @Override
                        public byte[] toByteArray(@NonNull Void result) {
                            return sEMPTY;
                        }
                    };
            listenableCallback.dispatchCallbackSafely();
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }
}
