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

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Logger;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;


/**
 * Is an implementation of a {@link ListenableWorker} that can bind to a remote process.
 * <p>
 * To be able to bind to a remote process, A {@link RemoteListenableWorker} needs additional
 * arguments as part of its input {@link Data}.
 * <p>
 * The arguments ({@link #ARGUMENT_PACKAGE_NAME}, {@link #ARGUMENT_CLASS_NAME}) are used to
 * determine the {@link android.app.Service} that the {@link RemoteListenableWorker} can bind to.
 * {@link #startRemoteWork()} is then subsequently called in the process that the
 * {@link android.app.Service} is running in.
 */
public abstract class RemoteListenableWorker extends ListenableWorker {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("RemoteListenableWorker");

    /**
     * The {@code #ARGUMENT_PACKAGE_NAME}, {@link #ARGUMENT_CLASS_NAME} together determine the
     * {@link ComponentName} that the {@link RemoteListenableWorker} binds to before calling
     * {@link #startRemoteWork()}.
     */
    public static final String ARGUMENT_PACKAGE_NAME =
            "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME";

    /**
     * The {@link #ARGUMENT_PACKAGE_NAME}, {@code className} together determine the
     * {@link ComponentName} that the {@link RemoteListenableWorker} binds to before calling
     * {@link #startRemoteWork()}.
     */
    public static final String ARGUMENT_CLASS_NAME =
            "androidx.work.impl.workers.RemoteListenableWorker.ARGUMENT_CLASS_NAME";

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams {@link WorkerParameters} to setup the internal state of this worker
     */
    public RemoteListenableWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @Override
    @NonNull
    public final ListenableFuture<Result> startWork() {
        String message = "startWork() shouldn't never be called on RemoteListenableWorker";
        return getFailedFuture(message);
    }

    /**
     * Override this method to define the work that needs to run in the remote process. This method
     * is called on the main thread.
     * <p>
     * A ListenableWorker has a well defined
     * <a href="https://d.android.com/reference/android/app/job/JobScheduler">execution window</a>
     * to to finish its execution and return a {@link androidx.work.ListenableWorker.Result}.
     * After this time has expired, the worker will be signalled to stop and its
     * {@link ListenableFuture} will be cancelled. Note that the execution window also includes
     * the cost of binding to the remote process.
     * <p>
     * The {@link RemoteListenableWorker} will also be signalled to stop when its constraints are
     * no longer met.
     *
     * @return A {@link ListenableFuture} with the {@code Result} of the computation.  If you
     * cancel this Future, WorkManager will treat this unit of work as a {@code Result#failure()}.
     */
    @NonNull
    public abstract ListenableFuture<Result> startRemoteWork();

    private static ListenableFuture<Result> getFailedFuture(@NonNull String message) {
        return CallbackToFutureAdapter.getFuture((completer) -> {
            Logger.get().error(TAG, message);
            completer.setException(new IllegalArgumentException(message));
            return "RemoteListenableWorker Failed Future";
        });
    }
}
