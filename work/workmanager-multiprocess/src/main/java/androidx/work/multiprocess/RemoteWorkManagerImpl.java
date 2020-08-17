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
import androidx.work.Logger;
import androidx.work.Operation;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * The implementation of a subset of WorkManager APIs that are safe to be supported across
 * processes.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteWorkManagerImpl extends IWorkManagerImpl.Stub {
    private final Context mContext;
    static final String TAG = Logger.tagWithPrefix("MultiProcessWorkManager");

    RemoteWorkManagerImpl(@NonNull Context context) {
        mContext = context.getApplicationContext();
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
            WorkManagerImpl workManager = WorkManagerImpl.getInstance(mContext);
            final Operation operation = workManager.enqueue(workRequests);
            final Executor executor = workManager.getWorkTaskExecutor().getBackgroundExecutor();
            final OperationCallback operationCallback =
                    new OperationCallback(operation, executor, callback);
            operationCallback.dispatchCallbacksSafely();
        } catch (Throwable throwable) {
            failureCallback(callback, throwable);
        }
    }
}
