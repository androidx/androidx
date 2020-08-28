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

import static androidx.work.multiprocess.OperationCallback.OperationCallbackRunnable.failureCallback;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Operation;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableWorkContinuationImpl;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests;

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
            final OperationCallback operationCallback =
                    new OperationCallback(operation, executor, callback);
            operationCallback.dispatchCallbacksSafely();
        } catch (Throwable throwable) {
            failureCallback(callback, throwable);
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
            final OperationCallback operationCallback =
                    new OperationCallback(operation, executor, callback);
            operationCallback.dispatchCallbacksSafely();
        } catch (Throwable throwable) {
            failureCallback(callback, throwable);
        }
    }

    @Override
    public void cancelWorkById(@NonNull String id, @NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelWorkById(UUID.fromString(id));
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final OperationCallback operationCallback =
                    new OperationCallback(operation, executor, callback);
            operationCallback.dispatchCallbacksSafely();
        } catch (Throwable throwable) {
            failureCallback(callback, throwable);
        }
    }

    @Override
    public void cancelAllWorkByTag(
            @NonNull String tag,
            @NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelAllWorkByTag(tag);
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final OperationCallback operationCallback =
                    new OperationCallback(operation, executor, callback);
            operationCallback.dispatchCallbacksSafely();
        } catch (Throwable throwable) {
            failureCallback(callback, throwable);
        }
    }

    @Override
    public void cancelUniqueWork(
            @NonNull String name,
            @NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelUniqueWork(name);
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final OperationCallback operationCallback =
                    new OperationCallback(operation, executor, callback);
            operationCallback.dispatchCallbacksSafely();
        } catch (Throwable throwable) {
            failureCallback(callback, throwable);
        }
    }

    @Override
    public void cancelAllWork(@NonNull IWorkManagerImplCallback callback) {
        try {
            final Operation operation = mWorkManager.cancelAllWork();
            final Executor executor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
            final OperationCallback operationCallback =
                    new OperationCallback(operation, executor, callback);
            operationCallback.dispatchCallbacksSafely();
        } catch (Throwable throwable) {
            failureCallback(callback, throwable);
        }
    }
}
