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

import static android.content.Context.BIND_AUTO_CREATE;

import static androidx.work.multiprocess.ListenableCallback.ListenableCallbackRunnable.reportFailure;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/***
 * A client for {@link IListenableWorkerImpl}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListenableWorkerImplClient {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("ListenableWorkerImplClient");

    // Synthetic access
    final Context mContext;

    // Synthetic access
    final Executor mExecutor;

    public ListenableWorkerImplClient(
            @NonNull Context context,
            @NonNull Executor executor) {
        mContext = context;
        mExecutor = executor;
    }

    /**
     * @return a {@link ListenableFuture} of {@link IListenableWorkerImpl} after a
     * {@link ServiceConnection} is established.
     */
    @NonNull
    public ListenableFuture<IListenableWorkerImpl> getListenableWorkerImpl(
            @NonNull ComponentName component) {

        Logger.get().debug(TAG,
                String.format("Binding to %s, %s", component.getPackageName(),
                        component.getClassName()));

        Connection session = new Connection();
        try {
            Intent intent = new Intent();
            intent.setComponent(component);
            boolean bound = mContext.bindService(intent, session, BIND_AUTO_CREATE);
            if (!bound) {
                unableToBind(session, new RuntimeException("Unable to bind to service"));
            }
        } catch (Throwable throwable) {
            unableToBind(session, throwable);
        }

        return session.mFuture;
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
        return execute(session, dispatcher, new RemoteCallback());
    }

    /**
     * Executes a method on an instance of {@link IListenableWorkerImpl} using the instance of
     * {@link RemoteDispatcher} and the {@link RemoteCallback}.
     */
    @NonNull
    @SuppressLint("LambdaLast")
    public ListenableFuture<byte[]> execute(
            @NonNull ListenableFuture<IListenableWorkerImpl> session,
            @NonNull final RemoteDispatcher<IListenableWorkerImpl> dispatcher,
            @NonNull final RemoteCallback callback) {

        session.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final IListenableWorkerImpl iListenableWorker = session.get();
                    callback.setBinder(iListenableWorker.asBinder());
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                dispatcher.execute(iListenableWorker, callback);
                            } catch (Throwable innerThrowable) {
                                Logger.get().error(TAG, "Unable to execute", innerThrowable);
                                reportFailure(callback, innerThrowable);
                            }
                        }
                    });
                } catch (ExecutionException | InterruptedException exception) {
                    String message = "Unable to bind to service";
                    Logger.get().error(TAG, message, exception);
                    reportFailure(callback, exception);
                }
            }
        }, mExecutor);
        return callback.getFuture();
    }

    /**
     * The implementation of {@link ServiceConnection} that handles changes in the connection.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Connection implements ServiceConnection {
        private static final String TAG = Logger.tagWithPrefix("ListenableWorkerImplSession");

        final SettableFuture<IListenableWorkerImpl> mFuture;

        public Connection() {
            mFuture = SettableFuture.create();
        }

        @Override
        public void onServiceConnected(
                @NonNull ComponentName componentName,
                @NonNull IBinder iBinder) {
            Logger.get().debug(TAG, "Service connected");
            IListenableWorkerImpl iListenableWorkerImpl =
                    IListenableWorkerImpl.Stub.asInterface(iBinder);
            mFuture.set(iListenableWorkerImpl);
        }

        @Override
        public void onServiceDisconnected(@NonNull ComponentName componentName) {
            Logger.get().warning(TAG, "Service disconnected");
            mFuture.setException(new RuntimeException("Service disconnected"));
        }

        @Override
        public void onBindingDied(@NonNull ComponentName name) {
            Logger.get().warning(TAG, "Binding died");
            mFuture.setException(new RuntimeException("Binding died"));
        }

        @Override
        public void onNullBinding(@NonNull ComponentName name) {
            Logger.get().error(TAG, "Unable to bind to service");
            mFuture.setException(
                    new RuntimeException(String.format("Cannot bind to service %s", name)));
        }
    }

    private static void unableToBind(@NonNull Connection session, @NonNull Throwable throwable) {
        Logger.get().error(TAG, "Unable to bind to service", throwable);
        session.mFuture.setException(throwable);
    }
}
