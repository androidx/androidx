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
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Logger;
import androidx.work.WorkerParameters;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableRemoteWorkRequest;
import androidx.work.multiprocess.parcelable.ParcelableResult;
import androidx.work.multiprocess.parcelable.ParcelableWorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;


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

    // Synthetic access
    final WorkerParameters mWorkerParameters;

    // Synthetic access
    final WorkManagerImpl mWorkManager;

    // Synthetic access
    final Executor mExecutor;

    // Synthetic access
    final ListenableWorkerImplClient mClient;

    // Synthetic access
    @Nullable
    String mWorkerClassName;

    @Nullable
    private ComponentName mComponentName;

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams {@link WorkerParameters} to setup the internal state of this worker
     */
    public RemoteListenableWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
        mWorkerParameters = workerParams;
        mWorkManager = WorkManagerImpl.getInstance(appContext);
        mExecutor = mWorkManager.getWorkTaskExecutor().getBackgroundExecutor();
        mClient = new ListenableWorkerImplClient(getApplicationContext(), mExecutor);
    }

    @Override
    @NonNull
    public final ListenableFuture<Result> startWork() {
        SettableFuture<Result> future = SettableFuture.create();
        Data data = getInputData();
        final String id = mWorkerParameters.getId().toString();
        String packageName = data.getString(ARGUMENT_PACKAGE_NAME);
        String serviceClassName = data.getString(ARGUMENT_CLASS_NAME);

        if (TextUtils.isEmpty(packageName)) {
            String message = "Need to specify a package name for the Remote Service.";
            Logger.get().error(TAG, message);
            future.setException(new IllegalArgumentException(message));
            return future;
        }

        if (TextUtils.isEmpty(serviceClassName)) {
            String message = "Need to specify a class name for the Remote Service.";
            Logger.get().error(TAG, message);
            future.setException(new IllegalArgumentException(message));
            return future;
        }

        mComponentName = new ComponentName(packageName, serviceClassName);

        ListenableFuture<byte[]> result = mClient.execute(
                mComponentName,
                new RemoteDispatcher<IListenableWorkerImpl>() {
                    @Override
                    public void execute(
                            @NonNull IListenableWorkerImpl listenableWorkerImpl,
                            @NonNull IWorkManagerImplCallback callback) throws RemoteException {

                        WorkSpec workSpec = mWorkManager.getWorkDatabase()
                                .workSpecDao()
                                .getWorkSpec(id);

                        mWorkerClassName = workSpec.workerClassName;
                        ParcelableRemoteWorkRequest remoteWorkRequest =
                                new ParcelableRemoteWorkRequest(
                                        workSpec.workerClassName, mWorkerParameters
                                );
                        byte[] request = ParcelConverters.marshall(remoteWorkRequest);
                        listenableWorkerImpl.startWork(request, callback);
                    }
                });

        return RemoteClientUtils.map(result, new Function<byte[], Result>() {
            @Override
            public Result apply(byte[] input) {
                ParcelableResult parcelableResult = ParcelConverters.unmarshall(input,
                        ParcelableResult.CREATOR);
                return parcelableResult.getResult();
            }
        }, mExecutor);
    }

    /**
     * Override this method to define the work that needs to run in the remote process. This method
     * is called on the main thread.
     * <p>
     * A ListenableWorker is given a maximum of ten minutes to finish its execution and return a
     * {@code Result}.  After this time has expired, the worker will be signalled to stop and its
     * {@link ListenableFuture} will be cancelled. Note that the ten minute execution window also
     * includes the cost of binding to the remote process.
     * <p>
     * The {@link RemoteListenableWorker} will also be signalled to stop when its constraints are
     * no longer met.
     *
     * @return A {@link ListenableFuture} with the {@code Result} of the computation.  If you
     * cancel this Future, WorkManager will treat this unit of work as a {@code Result#failure()}.
     */
    @NonNull
    public abstract ListenableFuture<Result> startRemoteWork();

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void onStopped() {
        super.onStopped();
        // Delegate interruptions to the remote process.
        if (mComponentName != null) {
            mClient.execute(mComponentName,
                    new RemoteDispatcher<IListenableWorkerImpl>() {
                        @Override
                        public void execute(
                                @NonNull IListenableWorkerImpl listenableWorkerImpl,
                                @NonNull IWorkManagerImplCallback callback)
                                throws RemoteException {
                            ParcelableWorkerParameters parcelableWorkerParameters =
                                    new ParcelableWorkerParameters(mWorkerParameters);
                            byte[] request = ParcelConverters.marshall(parcelableWorkerParameters);
                            listenableWorkerImpl.interrupt(request, callback);
                        }
                    });
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ListenableFuture<Void> setProgressAsync(@NonNull Data data) {
        // Delegate progress updates to the designated process.
        RemoteWorkManager remoteWorkManager =
                RemoteWorkManager.getInstance(getApplicationContext());
        return remoteWorkManager.setProgress(getId(), data);
    }
}
