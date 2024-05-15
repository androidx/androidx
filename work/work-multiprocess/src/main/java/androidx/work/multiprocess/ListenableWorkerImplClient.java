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

import static androidx.work.multiprocess.ServiceBindingKt.bindToService;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.work.Logger;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/***
 * A client for {@link IListenableWorkerImpl}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListenableWorkerImplClient {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("ListenableWorkerImplClient");

    // Synthetic access
    final Context mContext;

    // Synthetic access
    final Executor mExecutor;

    private final Object mLock;
    private Session<IListenableWorkerImpl> mConnection;

    public ListenableWorkerImplClient(
            @NonNull Context context,
            @NonNull Executor executor) {
        mContext = context;
        mExecutor = executor;
        mLock = new Object();
    }

    /**
     * @return a {@link ListenableFuture} of {@link IListenableWorkerImpl} after a
     * {@link ServiceConnection} is established.
     */
    @NonNull
    public ListenableFuture<IListenableWorkerImpl> getListenableWorkerImpl(
            @NonNull ComponentName component) {

        synchronized (mLock) {
            if (mConnection == null) {
                Logger.get().debug(TAG,
                        "Binding to " + component.getPackageName() + ", "
                                + component.getClassName());
                Intent intent = new Intent();
                intent.setComponent(component);
                mConnection = bindToService(mContext, intent,
                        IListenableWorkerImpl.Stub::asInterface, TAG);
            }
            return mConnection.getConnectedFuture();
        }
    }

    /**
     * Executes a method on an instance of {@link IListenableWorkerImpl} using the instance of
     * {@link RemoteDispatcher}.
     */
    @NonNull
    public ListenableFuture<byte[]> execute(
            @NonNull ComponentName componentName,
            @NonNull RemoteDispatcher<IListenableWorkerImpl> dispatcher) {

        ListenableFuture<IListenableWorkerImpl> session = getListenableWorkerImpl(componentName);
        return execute(session, dispatcher);
    }

    /**
     * Executes a method on an instance of {@link IListenableWorkerImpl} using the instance of
     * {@link RemoteDispatcher}
     */
    @NonNull
    @SuppressLint("LambdaLast")
    public ListenableFuture<byte[]> execute(
            @NonNull ListenableFuture<IListenableWorkerImpl> session,
            @NonNull final RemoteDispatcher<IListenableWorkerImpl> dispatcher) {
        return RemoteExecuteKt.execute(mExecutor, session, dispatcher);
    }

    /**
     * Unbinds the {@link ServiceConnection}.
     */
    public void unbindService() {
        synchronized (mLock) {
            if (mConnection != null) {
                mContext.unbindService(mConnection);
                mConnection = null;
            }
        }
    }

    /**
     * @return the {@link ServiceConnection} instance.
     */
    @Nullable
    @VisibleForTesting
    Session<IListenableWorkerImpl> getConnection() {
        return mConnection;
    }
}
