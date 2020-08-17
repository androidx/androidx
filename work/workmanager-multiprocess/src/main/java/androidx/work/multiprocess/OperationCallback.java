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

import android.annotation.SuppressLint;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;
import androidx.work.Operation;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Takes an {@link androidx.work.Operation} and dispatches
 * {@link androidx.work.multiprocess.IWorkManagerImplCallback}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OperationCallback {

    private final Operation mOperation;
    private final Executor mExecutor;
    private final IWorkManagerImplCallback mCallback;

    public OperationCallback(
            @NonNull Operation operation,
            @NonNull Executor executor,
            @NonNull IWorkManagerImplCallback callback) {
        mOperation = operation;
        mExecutor = executor;
        mCallback = callback;
    }

    /**
     * Dispatches callbacks safely while handling {@link android.os.RemoteException}s.
     */
    public void dispatchCallbacksSafely() {
        ListenableFuture<Operation.State.SUCCESS> future = mOperation.getResult();
        future.addListener(new OperationCallbackRunnable(future, mCallback), mExecutor);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class OperationCallbackRunnable implements Runnable {
        private static final String TAG = Logger.tagWithPrefix("OperationCallbackRbl");
        private final ListenableFuture<Operation.State.SUCCESS> mFuture;
        private final IWorkManagerImplCallback mCallback;

        @SuppressLint("LambdaLast")
        public OperationCallbackRunnable(
                @NonNull ListenableFuture<Operation.State.SUCCESS> future,
                @NonNull IWorkManagerImplCallback callback) {
            mFuture = future;
            mCallback = callback;
        }

        @Override
        public void run() {
            try {
                mFuture.get();
                successCallback(mCallback);
            } catch (Throwable exception) {
                failureCallback(mCallback, exception);
            }
        }

        /**
         * Dispatches successful callbacks safely.
         */
        public static void successCallback(@NonNull IWorkManagerImplCallback callback) {
            try {
                callback.onSuccess();
            } catch (RemoteException exception) {
                Logger.get().error(TAG, "Unable to notify successful operation", exception);
            }
        }

        /**
         * Dispatches failures callbacks safely.
         **/
        public static void failureCallback(
                @NonNull IWorkManagerImplCallback callback,
                @NonNull Throwable throwable) {
            try {
                callback.onFailure(throwable.getMessage());
            } catch (RemoteException exception) {
                Logger.get().error(TAG, "Unable to notify failures in operation", exception);
            }
        }
    }
}
