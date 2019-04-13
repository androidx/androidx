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


import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Primary logic is copy from concurrent-futures package in Guava to AndroidX namespace since we
 * would need ListenableFuture related implementation but not want to include whole Guava library.
 *
 * TODO: b/130187641 Update ImageCapture and remove FluentFuture.
 *
 *  A {@link ListenableFuture} that supports fluent chains of operations. For example:
 *
 *  <pre>{@code
 *  ListenableFuture<Boolean> adminIsLoggedIn =
 *      FluentFuture.from(usersDatabase.getAdminUser())
 *          .transform(User::getId, directExecutor())
 *          .transform(ActivityService::isLoggedIn, threadPool)
 *          .catching(RpcException.class, e -> false, directExecutor());
 *  }</pre>
 *  @hide
 *  @param <V>
 *  @deprecated Temporarily to use this class to keep code in the same behavior, will remove after
 *  ImageCapture update.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated
public abstract class FluentFuture<V> extends AbstractFuture<V> {

    /**
     * Converts the given {@code ListenableFuture} to an equivalent {@code FluentFuture}.
     *
     * <p>If the given {@code ListenableFuture} is already a {@code FluentFuture}, it is returned
     * directly. If not, it is wrapped in a {@code FluentFuture} that delegates all calls to the
     * original {@code ListenableFuture}.
     *
     * @param future
     * @param <V>
     * @return directly if input a FluentFuture or wrapped by ForwardingFluentFuture.
     */
    public static <V> FluentFuture<V> from(ListenableFuture<V> future) {
        return future instanceof FluentFuture
                ? (FluentFuture<V>) future
                : new ForwardingFluentFuture<V>(future);
    }

    /**
     * Returns a new {@code Future} whose result is asynchronously derived from the result of this
     * {@code Future}. If the input {@code Future} fails, the returned {@code Future} fails with the
     * same exception (and the function is not invoked).
     *
     * @param function A function to transform the result of this mFuture to the result of the
     *                 output mFuture
     * @param executor Executor to run the function in.
     * @return A mFuture that holds result of the function (if the input succeeded) or the original
     *     input's failure (if not)
     */
    public final <T> FluentFuture<T> transformAsync(
            AsyncFunction<? super V, T> function, Executor executor) {
        return (FluentFuture<T>) Futures.transformAsync(this, function, executor);
    }

    /**
     * Returns a new {@code Future} whose result is derived from the result of this {@code Future}.
     * If this input {@code Future} fails, the returned {@code Future} fails with the same exception
     * (and the function is not invoked).
     *
     * @param function A Function to transform the results of this mFuture to the results of the
     *     returned mFuture.
     * @param executor Executor to run the function in.
     * @return A mFuture that holds result of the transformation.
     */
    public final <T> FluentFuture<T> transform(Function<? super V, T> function, Executor executor) {
        return (FluentFuture<T>) Futures.transform(this, function, executor);
    }

    /**
     * Registers separate success and failure callbacks to be run when this {@code Future}'s
     * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
     * computation is already complete, immediately.
     *
     * @param callback The callback to invoke when this {@code Future} is completed.
     * @param executor The mExecutor to run {@code callback} when the mFuture completes.
     */
    public final void addCallback(FutureCallback<? super V> callback, Executor executor) {
        Futures.addCallback(this, callback, executor);
    }

    /**
     * A less abstract subclass of AbstractFuture. This can be used to optimize setFuture by
     * ensuring that {@link #get} calls exactly the implementation of {@link AbstractFuture#get}.
     */
    abstract static class TrustedFuture<V> extends FluentFuture<V>
            implements AbstractFuture.Trusted<V> {
        @Override
        public final V get() throws InterruptedException, ExecutionException {
            return super.get();
        }

        @Override
        public final V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return super.get(timeout, unit);
        }

        @Override
        public final boolean isDone() {
            return super.isDone();
        }

        @Override
        public final boolean isCancelled() {
            return super.isCancelled();
        }

        @Override
        public final void addListener(Runnable listener, Executor executor) {
            super.addListener(listener, executor);
        }

        @Override
        public final boolean cancel(boolean mayInterruptIfRunning) {
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
