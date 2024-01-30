/*
 * Copyright 2021 The Android Open Source Project
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
import static androidx.work.multiprocess.ListenableCallback.ListenableCallbackRunnable.reportSuccess;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Configuration;
import androidx.work.ForegroundUpdater;
import androidx.work.ListenableWorker;
import androidx.work.Logger;
import androidx.work.ProgressUpdater;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableInterruptRequest;
import androidx.work.multiprocess.parcelable.ParcelableRemoteWorkRequest;
import androidx.work.multiprocess.parcelable.ParcelableResult;
import androidx.work.multiprocess.parcelable.ParcelableWorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * An implementation of ListenableWorker that can be executed in a remote process.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListenableWorkerImpl extends IListenableWorkerImpl.Stub {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("WM-RemoteWorker ListenableWorkerImpl");
    // Synthetic access
    static byte[] sEMPTY = new byte[0];
    // Synthetic access
    static final Object sLock = new Object();

    // Synthetic access
    final Context mContext;
    // Synthetic access
    final Configuration mConfiguration;
    // Synthetic access
    final TaskExecutor mTaskExecutor;
    // Synthetic access
    final ProgressUpdater mProgressUpdater;
    // Synthetic access
    final ForegroundUpdater mForegroundUpdater;
    // Synthetic access
    final Map<String, RemoteWorkerWrapper> mRemoteWorkerWrapperMap;

    ListenableWorkerImpl(@NonNull Context context) {
        mContext = context.getApplicationContext();
        RemoteWorkManagerInfo remoteInfo = RemoteWorkManagerInfo.getInstance(context);
        mConfiguration = remoteInfo.getConfiguration();
        mTaskExecutor = remoteInfo.getTaskExecutor();
        mProgressUpdater = remoteInfo.getProgressUpdater();
        mForegroundUpdater = remoteInfo.getForegroundUpdater();
        mRemoteWorkerWrapperMap = new HashMap<>();
    }

    @Override
    public void startWork(
            @NonNull final byte[] request,
            @NonNull final IWorkManagerImplCallback callback) {
        try {
            ParcelableRemoteWorkRequest parcelableRemoteWorkRequest =
                    ParcelConverters.unmarshall(request, ParcelableRemoteWorkRequest.CREATOR);

            ParcelableWorkerParameters parcelableWorkerParameters =
                    parcelableRemoteWorkRequest.getParcelableWorkerParameters();

            WorkerParameters workerParameters =
                    parcelableWorkerParameters.toWorkerParameters(
                            mConfiguration,
                            mTaskExecutor,
                            mProgressUpdater,
                            mForegroundUpdater
                    );

            final String id = workerParameters.getId().toString();
            final String workerClassName = parcelableRemoteWorkRequest.getWorkerClassName();

            Logger.get().debug(TAG,
                    "Executing work request (" + id + ", " + workerClassName + ")");

            final ListenableFuture<ListenableWorker.Result> futureResult =
                    executeWorkRequest(id, workerClassName, workerParameters);

            futureResult.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        ListenableWorker.Result result = futureResult.get();
                        ParcelableResult parcelableResult = new ParcelableResult(result);
                        byte[] response = ParcelConverters.marshall(parcelableResult);
                        reportSuccess(callback, response);
                    } catch (ExecutionException | InterruptedException exception) {
                        reportFailure(callback, exception);
                    } catch (CancellationException cancellationException) {
                        Logger.get().debug(TAG, "Worker (" + id + ") was cancelled");
                        reportFailure(callback, cancellationException);
                    } finally {
                        synchronized (sLock) {
                            mRemoteWorkerWrapperMap.remove(id);
                        }
                    }
                }
            }, mTaskExecutor.getSerialTaskExecutor());
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @Override
    public void interrupt(
            @NonNull final byte[] request,
            @NonNull final IWorkManagerImplCallback callback) {
        try {
            ParcelableInterruptRequest interruptRequest =
                    ParcelConverters.unmarshall(request, ParcelableInterruptRequest.CREATOR);
            final String id = interruptRequest.getId();
            final int stopReason = interruptRequest.getStopReason();
            Logger.get().debug(TAG, "Interrupting work with id (" + id + ")");

            final RemoteWorkerWrapper remoteWorker;
            synchronized (sLock) {
                remoteWorker = mRemoteWorkerWrapperMap.remove(id);
            }
            if (remoteWorker != null) {
                mTaskExecutor.getSerialTaskExecutor()
                        .execute(() -> {
                            remoteWorker.interrupt(stopReason);
                            reportSuccess(callback, sEMPTY);
                        });
            } else {
                // Nothing to do.
                reportSuccess(callback, sEMPTY);
            }
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    @NonNull
    private ListenableFuture<ListenableWorker.Result> executeWorkRequest(
            @NonNull String id,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {

        RemoteWorkerWrapper remoteWorker = RemoteWorkerWrapperKt.create(
                mContext, mConfiguration, workerClassName, workerParameters, mTaskExecutor
        );

        synchronized (sLock) {
            mRemoteWorkerWrapperMap.put(id, remoteWorker);
        }

        return remoteWorker.getFuture();
    }
}
