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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage.util;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utilities for working with {@link com.google.common.util.concurrent.ListenableFuture}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class FutureUtil {
    private FutureUtil() {}

    /** Executes the given lambda on the given executor and returns a {@link ListenableFuture}. */
    @NonNull
    public static <T> ListenableFuture<T> execute(
            @NonNull ExecutorService executor,
            @NonNull Callable<T> callable) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(callable);

        ResolvableFuture<T> future = ResolvableFuture.create();
        executor.execute(() -> {
            if (!future.isCancelled()) {
                try {
                    future.set(callable.call());
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        });
        return future;
    }

    /**
     * Returns a new {@link ListenableFuture} by applying the given lambda to the result of the old
     * future.
     *
     * <p>The lambda is applied as part of the get() call and its result is not cached.
     */
    @NonNull
    public static <I, O> ListenableFuture<O> map(
            @NonNull ListenableFuture<I> inputFuture, @NonNull Function<I, O> lambda) {
        Preconditions.checkNotNull(inputFuture);
        Preconditions.checkNotNull(lambda);
        return new ListenableFuture<O>() {
            @Override
            public void addListener(Runnable listener, Executor executor) {
                inputFuture.addListener(listener, executor);
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return inputFuture.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return inputFuture.isCancelled();
            }

            @Override
            public boolean isDone() {
                return inputFuture.isDone();
            }

            @Override
            public O get() throws ExecutionException, InterruptedException {
                I input = inputFuture.get();
                return lambda.apply(input);
            }

            @Override
            public O get(long timeout, TimeUnit unit)
                    throws ExecutionException, InterruptedException, TimeoutException {
                I input = inputFuture.get(timeout, unit);
                return lambda.apply(input);
            }
        };
    }
}
