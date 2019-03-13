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

package androidx.paging.futures;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Futures {
    private Futures() {}
  /**
   * Registers separate success and failure callbacks to be run when the {@code Future}'s
   * computation is complete or, if the computation is already complete, immediately.
   *
   * <p>The callback is run on {@code executor}. There is no guaranteed ordering of execution of
   * callbacks, but any callback added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * <p>Example:
   *
   * <pre>{@code
   * ListenableFuture<QueryResult> future = ...;
   * Executor e = ...
   * addCallback(future,
   *     new FutureCallback<QueryResult>() {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     }, e);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. All its warnings about heavyweight listeners are also applicable to heavyweight
   * callbacks passed to this method.
   *
   * <p>For a more general interface to attach a completion listener to a {@code Future}, see {@link
   * ListenableFuture#addListener addListener}.
   *
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @param executor The executor to run {@code callback} when the future completes.
   */
    public static <V> void addCallback(@NonNull final ListenableFuture<V> future,
            @NonNull final FutureCallback<? super V> callback, @NonNull Executor executor) {
        future.addListener(new Runnable() {
            @Override
            public void run() {
                final V value;
                try {
                    value = future.get();
                } catch (ExecutionException e) {
                    callback.onError(e.getCause());
                    return;
                } catch (Throwable e) {
                    callback.onError(e);
                    return;
                }
                callback.onSuccess(value);
            }
        }, executor);
    }

    /**
     * Returns a new {@code Future} whose result is derived from the result of the given {@code
     * Future}. If {@code input} fails, the returned {@code Future} fails with the same exception
     * (and the function is not invoked). Example usage:
     *
     * <pre>{@code
     * ListenableFuture<QueryResult> queryFuture = ...;
     * ListenableFuture<List<Row>> rowsFuture =
     *     transform(queryFuture, QueryResult::getRows, executor);
     * }</pre>
     *
     * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases.
     * See the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
     * documentation. All its warnings about heavyweight listeners are also applicable to
     * heavyweight functions passed to this method.
     *
     * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of
     * the input future. That is, if the returned {@code Future} is cancelled, it will attempt to
     * cancel the input, and if the input is cancelled, the returned {@code Future} will receive a
     * callback in which it will attempt to cancel itself.
     *
     * <p>An example use of this method is to convert a serializable object returned from an RPC
     * into a POJO.
     *
     * @param input The future to transform
     * @param function A Function to transform the results of the provided future to the results of
     *     the returned future.
     * @param executor Executor to run the function in.
     * @return A future that holds result of the transformation.
     */
    @NonNull
    public static <I, O> ListenableFuture<O> transform(
            @NonNull final ListenableFuture<I> input,
            @NonNull final Function<? super I, ? extends O> function,
            @NonNull final Executor executor) {
        final ResolvableFuture<O> out = ResolvableFuture.create();

        // add success/error callback
        addCallback(input, new FutureCallback<I>() {
            @Override
            public void onSuccess(I value) {
                out.set(function.apply(value));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                out.setException(throwable);
            }
        }, executor);

        // propagate output future's cancellation to input future
        addCallback(out, new FutureCallback<O>() {
            @Override
            public void onSuccess(O value) {}

            @Override
            public void onError(@NonNull Throwable throwable) {
                if (throwable instanceof CancellationException) {
                    input.cancel(false);
                }
            }
        }, executor);
        return out;
    }
}
