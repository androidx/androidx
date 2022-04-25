/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils.futures;

import static androidx.core.util.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  A {@link ListenableFuture} that supports chains of operations. For example:
 *
 *  <pre>{@code
 *  ListenableFuture<Boolean> adminIsLoggedIn =
 *      FutureChain.from(usersDatabase.getAdminUser())
 *          .transform(User::getId, directExecutor())
 *          .transform(ActivityService::isLoggedIn, threadPool);
 *  }</pre>
 *  @param <V>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FutureChain<V> implements ListenableFuture<V> {
    @NonNull
    private final ListenableFuture<V> mDelegate;
    @Nullable
    CallbackToFutureAdapter.Completer<V> mCompleter;

    /**
     * Converts the given {@code ListenableFuture} to an equivalent {@code FutureChain}.
     *
     * <p>If the given {@code ListenableFuture} is already a {@code FutureChain}, it is returned
     * directly. If not, it is wrapped in a {@code FutureChain} that delegates all calls to the
     * original {@code ListenableFuture}.
     *
     * @return directly if input a FutureChain or a ListenableFuture wrapped by FutureChain.
     */
    @NonNull
    public static <V> FutureChain<V> from(@NonNull ListenableFuture<V> future) {
        return future instanceof FutureChain
                ? (FutureChain<V>) future : new FutureChain<V>(future);
    }

    /**
     * Returns a new {@code Future} whose result is asynchronously derived from the result of
     * this {@code Future}. If the input {@code Future} fails, the returned {@code Future} fails
     * with the same exception (and the function is not invoked).
     *
     * @param function A function to transform the result of this future to the result of the
     *                 output future
     * @param executor Executor to run the function in.
     * @return A future that holds result of the function (if the input succeeded) or the
     * original input's failure (if not)
     */
    @NonNull
    public final <T> FutureChain<T> transformAsync(
            @NonNull AsyncFunction<? super V, T> function, @NonNull Executor executor) {
        return (FutureChain<T>) Futures.transformAsync(this, function, executor);
    }

    /**
     * Returns a new {@code Future} whose result is derived from the result of this {@code
     * Future}.
     * If this input {@code Future} fails, the returned {@code Future} fails with the same
     * exception (and the function is not invoked).
     *
     * @param function A Function to transform the results of this future to the results of the
     *                 returned future.
     * @param executor Executor to run the function in.
     * @return A future that holds result of the transformation.
     */
    @NonNull
    public final <T> FutureChain<T> transform(@NonNull Function<? super V, T> function,
            @NonNull Executor executor) {
        return (FutureChain<T>) Futures.transform(this, function, executor);
    }

    /**
     * Registers separate success and failure callbacks to be run when this {@code Future}'s
     * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
     * computation is already complete, immediately.
     *
     * @param callback The callback to invoke when this {@code Future} is completed.
     * @param executor The executor to run {@code callback} when the future completes.
     */
    public final void addCallback(@NonNull FutureCallback<? super V> callback,
            @NonNull Executor executor) {
        Futures.addCallback(this, callback, executor);
    }

    FutureChain(@NonNull ListenableFuture<V> delegate) {
        mDelegate = checkNotNull(delegate);
    }

    FutureChain() {
        mDelegate = CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<V>() {
                    @Override
                    public Object attachCompleter(
                            @NonNull CallbackToFutureAdapter.Completer<V> completer) {
                        Preconditions.checkState(mCompleter == null,
                                "The result can only set once!");
                        mCompleter = completer;
                        return "FutureChain[" + FutureChain.this + "]";
                    }
                });
    }

    @Override
    public void addListener(@NonNull Runnable listener, @NonNull Executor executor) {
        mDelegate.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mDelegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return mDelegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return mDelegate.isDone();
    }


    @Nullable
    @Override
    public V get() throws InterruptedException, ExecutionException {
        return mDelegate.get();
    }

    @Nullable
    @Override
    public V get(long timeout, @NonNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return mDelegate.get(timeout, unit);
    }

    boolean set(@Nullable V value) {
        if (mCompleter != null) {
            return mCompleter.set(value);
        }

        return false;
    }

    boolean setException(@NonNull Throwable throwable) {
        if (mCompleter != null) {
            return mCompleter.setException(throwable);
        }

        return false;
    }

}

