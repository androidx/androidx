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

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Takes an {@link com.google.common.util.concurrent.ListenableFuture} and dispatches
 * {@link androidx.work.multiprocess.IWorkManagerImplCallback}s.
 *
 * @param <I> represents the result returned by the {@link ListenableFuture}
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ListenableCallback<I> {
    // Synthetic access
    final Executor mExecutor;
    // Synthetic access
    final IWorkManagerImplCallback mCallback;
    // Synthetic access
    final ListenableFuture<I> mFuture;

    public ListenableCallback(
            @NonNull Executor executor,
            @NonNull IWorkManagerImplCallback callback,
            @NonNull ListenableFuture<I> future) {
        mExecutor = executor;
        mCallback = callback;
        mFuture = future;
    }

    /**
     * Transforms the result of the {@link ListenableFuture} to a byte array.
     */
    @NonNull
    public abstract byte[] toByteArray(@NonNull I result);

    /**
     * Dispatches callbacks safely while handling {@link android.os.RemoteException}s.
     */
    public void dispatchCallbackSafely() {
        mFuture.addListener(new ListenableCallbackRunnable<I>(this), mExecutor);
    }

    /**
     * @param <I> represents the result returned by the {@link ListenableFuture}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class ListenableCallbackRunnable<I> implements Runnable {
        private static final String TAG = Logger.tagWithPrefix("ListenableCallbackRbl");
        private final ListenableCallback<I> mCallback;

        public ListenableCallbackRunnable(@NonNull ListenableCallback<I> callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            try {
                I result = mCallback.mFuture.get();
                reportSuccess(mCallback.mCallback, mCallback.toByteArray(result));
            } catch (Throwable throwable) {
                reportFailure(mCallback.mCallback, throwable);
            }
        }

        /**
         * Dispatches successful callbacks safely.
         */
        public static void reportSuccess(
                @NonNull IWorkManagerImplCallback callback,
                @NonNull byte[] response) {
            try {
                callback.onSuccess(response);
            } catch (RemoteException exception) {
                Logger.get().error(TAG, "Unable to notify successful operation", exception);
            }
        }

        /**
         * Dispatches failures callbacks safely.
         **/
        public static void reportFailure(
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
