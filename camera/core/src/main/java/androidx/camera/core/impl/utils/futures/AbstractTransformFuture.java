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

import static androidx.camera.core.impl.utils.futures.Futures.getDone;
import static androidx.core.util.Preconditions.checkNotNull;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Cloned from concurrent-futures package in Guava to AndroidX namespace since we would need
 * ListenableFuture related implementation but not want to include whole Guava library.
 *
 * Implementations of {@code Futures.transform*}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class AbstractTransformFuture<I, O, F, T> extends FluentFuture.TrustedFuture<O> implements
        Runnable {

    static <I, O> ListenableFuture<O> create(
            ListenableFuture<I> input,
            AsyncFunction<? super I, ? extends O> function,
            Executor executor) {
        checkNotNull(executor);
        AsyncTransformFuture<I, O> output = new AsyncTransformFuture<>(input, function);
        input.addListener(output, rejectionPropagatingExecutor(executor, output));
        return output;
    }

    static <I, O> ListenableFuture<O> create(
            ListenableFuture<I> input, Function<? super I, ? extends O> function,
            Executor executor) {
        checkNotNull(function);
        TransformFuture<I, O> output = new TransformFuture<>(input, function);
        input.addListener(output, rejectionPropagatingExecutor(executor, output));
        return output;
    }

    /*
     * In certain circumstances, this field might theoretically not be visible to an afterDone()
     * call triggered by cancel(). For details, see the comments on the fields of TimeoutFuture.
     */
    @Nullable ListenableFuture<? extends I> mInputFuture;
    @Nullable F mFunction;

    AbstractTransformFuture(ListenableFuture<? extends I> inputFuture, F function) {
        mInputFuture = checkNotNull(inputFuture);
        mFunction = checkNotNull(function);
    }

    @Override
    public final void run() {
        ListenableFuture<? extends I> localInputFuture = mInputFuture;
        F localFunction = mFunction;
        if (isCancelled() | localInputFuture == null | localFunction == null) {
            return;
        }
        mInputFuture = null;

        if (localInputFuture.isCancelled()) {
            @SuppressWarnings("unchecked")
            boolean unused =
                    setFuture(
                            (ListenableFuture<O>) localInputFuture); // Respects cancellation
            // cause setting
            return;
        }

        /*
         * Any of the setException() calls below can fail if the output Future is cancelled
         * between now
         * and then. This means that we're silently swallowing an exception -- maybe even an
         * Error. But
         * this is no worse than what FutureTask does in that situation. Additionally, because the
         * Future was cancelled, its listeners have been run, so its consumers will not hang.
         *
         * Contrast this to the situation we have if setResult() throws, a situation described
         * below.
         */
        I sourceResult;
        try {
            sourceResult = getDone(localInputFuture);
        } catch (CancellationException e) {
            // TODO(clm): verify future behavior - unify logic with getFutureValue in
            //  AbstractFuture. This
            // code should be unreachable with correctly implemented Futures.
            // Cancel this future and return.
            // At this point, mInputFuture is cancelled and outputFuture doesn't exist, so the
            // value of mayInterruptIfRunning is irrelevant.
            cancel(false);
            return;
        } catch (ExecutionException e) {
            // Set the cause of the exception as this future's exception.
            setException(e.getCause());
            return;
        } catch (RuntimeException e) {
            // Bug in mInputFuture.get(). Propagate to the output Future so that its consumers
            // don't hang.
            setException(e);
            return;
        } catch (Error e) {
            /*
             * StackOverflowError, OutOfMemoryError (e.g., from allocating ExecutionException), or
             * something. Try to treat it like a RuntimeException. If we overflow the stack
             * again, the resulting Error will propagate upward up to the root call to set().
             */
            setException(e);
            return;
        }

        T transformResult;
        try {
            transformResult = doTransform(localFunction, sourceResult);
        } catch (Throwable t) {
            // This exception is irrelevant in this thread, but useful for the client.
            setException(t);
            return;
        } finally {
            mFunction = null;
        }

        /*
         * If set()/setValue() throws an Error, we let it propagate. Why? The most likely Error is a
         * StackOverflowError (from deep transform(..., directExecutor()) nesting), and calling
         * setException(stackOverflowError) would fail:
         *
         * - If the stack overflowed before set()/setValue() could even store the result in the
         * output Future, then a call setException() would likely also overflow.
         *
         * - If the stack overflowed after set()/setValue() stored its result, then a call to
         * setException() will be a no-op because the Future is already done.
         *
         * Both scenarios are bad: The output Future might never complete, or, if it does
         * complete, it
         * might not run some of its listeners. The likely result is that the app will hang. (And of
         * course stack overflows are bad news in general. For example, we may have overflowed in
          * the
         * middle of defining a class. If so, that class will never be loadable in this process.)
          * The
         * best we can do (since logging may overflow the stack) is to let the error propagate.
         * Because
         * it is an Error, it won't be caught and logged by AbstractFuture.executeListener.
         * Instead, it
         * can propagate through many layers of AbstractTransformFuture up to the root call to
         * set().
         *
         * https://github.com/google/guava/issues/2254
         *
         * Other kinds of Errors are possible:
         *
         * - OutOfMemoryError from allocations in setFuture(): The calculus here is similar to
         * StackOverflowError: We can't reliably call setException(error).
         *
         * - Any kind of Error from a listener. Even if we could distinguish that case (by
         * exposing some
         * extra state from AbstractFuture), our options are limited: A call to setException()
         * would be a no-op. We could log, but if that's what we really want, we should modify
         * AbstractFuture.executeListener to do so, since that method would have the ability to
         * continue
         * to execute other listeners.
         *
         * What about RuntimeException? If there is a bug in set()/setValue() that produces one,
         * it will propagate, too, but only as far as AbstractFuture.executeListener, which will
         * catch and log it.
         */
        setResult(transformResult);
    }

    /** Template method for subtypes to actually run the transform. */
    abstract @Nullable T doTransform(F function, @Nullable I result) throws Exception;

    /** Template method for subtypes to actually set the result. */
    abstract void setResult(@Nullable T result);

    @Override
    protected final void afterDone() {
        maybePropagateCancellationTo(mInputFuture);
        this.mInputFuture = null;
        this.mFunction = null;
    }

    @Override
    protected String pendingToString() {
        ListenableFuture<? extends I> localInputFuture = mInputFuture;
        F localFunction = mFunction;
        String superString = super.pendingToString();
        String resultString = "";
        if (localInputFuture != null) {
            resultString = "mInputFuture=[" + localInputFuture + "], ";
        }
        if (localFunction != null) {
            return resultString + "mFunction=[" + localFunction + "]";
        } else if (superString != null) {
            return resultString + superString;
        }
        return null;
    }

    /**
     * An {@link AbstractTransformFuture} that delegates to an {@link AsyncFunction} and {@link
     * #setFuture(ListenableFuture)}.
     */
    private static final class AsyncTransformFuture<I, O>
            extends AbstractTransformFuture<
            I, O, AsyncFunction<? super I, ? extends O>, ListenableFuture<? extends O>> {
        AsyncTransformFuture(
                ListenableFuture<? extends I> inputFuture,
                AsyncFunction<? super I, ? extends O> function) {
            super(inputFuture, function);
        }

        @Override
        ListenableFuture<? extends O> doTransform(
                AsyncFunction<? super I, ? extends O> function, @Nullable I input)
                throws Exception {
            ListenableFuture<? extends O> outputFuture = function.apply(input);
            checkNotNull(
                    outputFuture,
                    "AsyncFunction.apply returned null instead of a Future. "
                            + "Did you mean to return immediateFuture(null)? " + function);
            return outputFuture;
        }

        @Override
        void setResult(ListenableFuture<? extends O> result) {
            setFuture(result);
        }
    }

    /**
     * An {@link AbstractTransformFuture} that delegates to a {@link Function} and {@link
     * #set(Object)}.
     */
    private static final class TransformFuture<I, O>
            extends AbstractTransformFuture<I, O, Function<? super I, ? extends O>, O> {
        TransformFuture(
                ListenableFuture<? extends I> inputFuture,
                Function<? super I, ? extends O> function) {
            super(inputFuture, function);
        }

        @Override
        @Nullable
        O doTransform(Function<? super I, ? extends O> function, @Nullable I input) {
            return function.apply(input);
        }

        @Override
        void setResult(@Nullable O result) {
            set(result);
        }
    }

    /**
     * Returns an Executor that will propagate {@link RejectedExecutionException} from the delegate
     * executor to the given {@code mFuture}.
     *
     * <p>Note, the returned executor can only be used once.
     */
    static Executor rejectionPropagatingExecutor(
            final Executor delegate, final AbstractFuture<?> future) {
        checkNotNull(delegate);
        checkNotNull(future);
        if (delegate == CameraXExecutors.directExecutor()) {
            // directExecutor() cannot throw RejectedExecutionException
            return delegate;
        }
        return new Executor() {
            boolean mThrownFromDelegate = true;

            @Override
            public void execute(final Runnable command) {
                try {
                    delegate.execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    mThrownFromDelegate = false;
                                    command.run();
                                }
                            });
                } catch (RejectedExecutionException e) {
                    if (mThrownFromDelegate) {
                        // wrap exception?
                        future.setException(e);
                    }
                    // otherwise it must have been thrown from a transitive call and the delegate
                    // runnable should have handled it.
                }
            }
        };
    }
}
