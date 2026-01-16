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
import static androidx.work.multiprocess.RemoteWorkerWrapperKt.executeRemoteWorker;

import android.content.Context;

import androidx.annotation.RestrictTo;
import androidx.work.Configuration;
import androidx.work.ForegroundInfo;
import androidx.work.ForegroundUpdater;
import androidx.work.ListenableWorker;
import androidx.work.Logger;
import androidx.work.ProgressUpdater;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableForegroundInfo;
import androidx.work.multiprocess.parcelable.ParcelableInterruptRequest;
import androidx.work.multiprocess.parcelable.ParcelableRemoteWorkRequest;
import androidx.work.multiprocess.parcelable.ParcelableResult;
import androidx.work.multiprocess.parcelable.ParcelableWorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * An implementation of ListenableWorker that can be executed in a remote process.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListenableWorkerImpl extends IListenableWorkerImpl.Stub {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("ListenableWorkerImpl");
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
    final Map<String, ListenableWorker> mListenableWorkerMap;
    // Synthetic access
    final Map<String, Throwable> mThrowableMap;

    ListenableWorkerImpl(@NonNull Context context) {
        mContext = context.getApplicationContext();
        RemoteWorkManagerInfo remoteInfo = RemoteWorkManagerInfo.getInstance(context);
        mConfiguration = remoteInfo.getConfiguration();
        mTaskExecutor = remoteInfo.getTaskExecutor();
        mProgressUpdater = remoteInfo.getProgressUpdater();
        mForegroundUpdater = remoteInfo.getForegroundUpdater();
        // We need to track the actual workers and exceptions when creating instances of workers
        // using the WorkerFactory. The service is longer lived than the workers, and therefore
        // needs to be cognizant of attributing state to the right workerClassName. The keys
        // to both the maps are the unique work request ids.
        mListenableWorkerMap = new HashMap<>();
        mThrowableMap = new HashMap<>();
    }

    @Override
    public void startWork(
            final byte @NonNull [] request,
            final @NonNull IWorkManagerImplCallback callback) {
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
                    executeWorkRequest(workerClassName, workerParameters);

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
                            mListenableWorkerMap.remove(id);
                            mThrowableMap.remove(id);
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
            final byte @NonNull [] request,
            final @NonNull IWorkManagerImplCallback callback) {
        try {
            ParcelableInterruptRequest interruptRequest =
                    ParcelConverters.unmarshall(request, ParcelableInterruptRequest.CREATOR);
            final String id = interruptRequest.getId();
            final int stopReason = interruptRequest.getStopReason();
            Logger.get().debug(TAG, "Interrupting work with id (" + id + ")");
            // No need to remove the ListenableWorker from the map here, given after interruption
            // the future gets notified and the cleanup happens automatically.
            final ListenableWorker worker = mListenableWorkerMap.get(id);
            if (worker != null) {
                mTaskExecutor.getSerialTaskExecutor()
                        .execute(() -> {
                            worker.stop(stopReason);
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

    @Override
    public void getForegroundInfoAsync(
            final byte @NonNull [] request,
            final @NonNull IWorkManagerImplCallback callback) {
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

            // Only instantiate the Worker if necessary.
            createWorker(id, workerClassName, workerParameters);
            ListenableWorker worker = mListenableWorkerMap.get(id);
            Throwable throwable = mThrowableMap.get(id);

            if (throwable != null) {
                reportFailure(callback, throwable);
            } else if (worker != null) {
                mTaskExecutor.getSerialTaskExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        final ListenableFuture<ForegroundInfo> futureResult =
                                worker.getForegroundInfoAsync();
                        futureResult.addListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ForegroundInfo foregroundInfo = futureResult.get();
                                    ParcelableForegroundInfo parcelableForegroundInfo =
                                            new ParcelableForegroundInfo(foregroundInfo);
                                    byte[] response = ParcelConverters.marshall(
                                            parcelableForegroundInfo
                                    );
                                    reportSuccess(callback, response);
                                } catch (Throwable throwable) {
                                    reportFailure(callback, throwable);
                                }
                            }
                        }, mTaskExecutor.getSerialTaskExecutor());
                    }
                });
            } else {
                reportFailure(callback, new IllegalStateException("Should never happen."));
            }
        } catch (Throwable throwable) {
            reportFailure(callback, throwable);
        }
    }

    private @NonNull ListenableFuture<ListenableWorker.Result> executeWorkRequest(
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {

        String id = workerParameters.getId().toString();
        // Only instantiate the Worker if necessary.
        createWorker(id, workerClassName, workerParameters);

        ListenableWorker worker = mListenableWorkerMap.get(id);
        Throwable throwable = mThrowableMap.get(id);

        return executeRemoteWorker(
                mConfiguration, workerClassName, workerParameters, worker, throwable,
                mTaskExecutor);
    }

    private void createWorker(
            @NonNull String id,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {

        // Use the id to keep track of the underlying instances. This is because the same worker
        // could be concurrently being executed with a different set of inputs.
        ListenableWorker worker = mListenableWorkerMap.get(id);
        Throwable throwable = mThrowableMap.get(id);
        // Check before we acquire a lock here, to make things as cheap as possible.
        if (worker == null && throwable == null) {
            synchronized (sLock) {
                worker = mListenableWorkerMap.get(id);
                throwable = mThrowableMap.get(id);
                if (worker == null && throwable == null) {
                    try {
                        worker = mConfiguration.getWorkerFactory()
                                .createWorkerWithDefaultFallback(
                                        mContext, workerClassName, workerParameters
                                );
                        mListenableWorkerMap.put(id, worker);
                    } catch (Throwable workerThrowable) {
                        mThrowableMap.put(id, workerThrowable);
                    }
                }
            }
        }
    }
}
