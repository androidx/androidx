/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.impl.concurrent;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Function;

/** Future/ListenableFuture related utility methods. */
public final class Futures {
    private Futures() {
    }

    /** Attach a FutureCallback to a ListenableFuture instance. */
    public static <V> void addCallback(
            @NonNull final ListenableFuture<V> future,
            @NonNull final FutureCallback<? super V> callback,
            @NonNull Executor executor) {
        Utils.checkNotNull(callback);
        future.addListener(new CallbackListener<>(future, callback), executor);
    }

    /**
     * Transforms an input ListenableFuture into a second ListenableFuture by applying a
     * transforming
     * function to the result of the input ListenableFuture.
     */
    @NonNull
    @SuppressLint("LambdaLast")
    public static <I, O> ListenableFuture<O> transform(
            @NonNull ListenableFuture<I> input,
            @NonNull Function<I, O> function,
            @NonNull Executor executor,
            @Nullable String tag) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    addCallback(input, transformFutureCallback(completer, function), executor);
                    return tag;
                });
    }

    /**
     * Transforms an input ListenableFuture into a second ListenableFuture by applying an
     * asynchronous
     * transforming function to the result of the input ListenableFuture.
     */
    @NonNull
    @SuppressLint("LambdaLast")
    public static <I, O> ListenableFuture<O> transformAsync(
            @NonNull ListenableFuture<I> input,
            @NonNull Function<I, ListenableFuture<O>> asyncFunction,
            @NonNull Executor executor,
            @NonNull String tag) {

        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    addCallback(input, asyncTransformFutureCallback(completer, asyncFunction),
                            executor);
                    return tag;
                });
    }

    /** Returns a Future that is immediately complete with the given value. */
    @NonNull
    public static <V> ListenableFuture<V> immediateFuture(V value) {
        return CallbackToFutureAdapter.getFuture(completer -> completer.set(value));
    }

    /** Returns a Future that is immediately complete with null value. */
    @NonNull
    public static ListenableFuture<Void> immediateVoidFuture() {
        return CallbackToFutureAdapter.getFuture(completer -> completer.set(null));
    }

    /** Returns a Future that is immediately complete with an exception. */
    @NonNull
    public static <V> ListenableFuture<V> immediateFailedFuture(@NonNull Throwable throwable) {
        return CallbackToFutureAdapter.getFuture(completer -> completer.setException(throwable));
    }

    /**
     * Returns a FutureCallback that transform the result in onSuccess, and then set the result in
     * completer.
     */
    static <I, O> FutureCallback<I> transformFutureCallback(
            Completer<O> completer, Function<I, O> function) {
        return new FutureCallback<I>() {
            @Override
            public void onSuccess(I result) {
                try {
                    completer.set(function.apply(result));
                } catch (Throwable t) {
                    if (t instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    completer.setException(t);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable failure) {
                completer.setException(failure);
            }
        };
    }

    /** Returns a FutureCallback that asynchronously transform the result. */
    private static <I, O> FutureCallback<I> asyncTransformFutureCallback(
            Completer<O> completer, Function<I, ListenableFuture<O>> asyncFunction) {
        return new FutureCallback<I>() {
            @Override
            public void onSuccess(I inputResult) {
                try {
                    addCallback(
                            asyncFunction.apply(inputResult),
                            transformFutureCallback(completer, Function.identity()),
                            Runnable::run);
                } catch (Throwable t) {
                    if (t instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    completer.setException(t);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable failure) {
                completer.setException(failure);
            }
        };
    }

    static <V> V getDone(Future<V> future) throws ExecutionException {
        Utils.checkState(future.isDone(), "future is expected to be done already.");
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class CallbackListener<V> implements Runnable {
        final Future<V> mFuture;
        final FutureCallback<? super V> mCallback;

        CallbackListener(Future<V> future, FutureCallback<? super V> callback) {
            this.mFuture = future;
            this.mCallback = callback;
        }

        @Override
        public void run() {
            final V value;
            try {
                value = getDone(mFuture);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                mCallback.onFailure(cause != null ? cause : e);
                return;
            } catch (RuntimeException | Error e) {
                mCallback.onFailure(e);
                return;
            }
            mCallback.onSuccess(value);
        }
    }
}
