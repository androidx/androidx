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

import static androidx.camera.core.impl.utils.futures.Futures.getUninterruptibly;
import static androidx.core.util.Preconditions.checkNotNull;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  The Class is based on the ChainingListenableFuture in Guava, the constructor of FutureChain
 *  will use the CallbackToFutureAdapter instead of the AbstractFuture.
 *
 * An implementation of {@code ListenableFuture} that also implements
 * {@code Runnable} so that it can be used to nest ListenableFutures.
 * Once the passed-in {@code ListenableFuture} is complete, it calls the
 * passed-in {@code Function} to generate the result.
 *
 * <p>If the Function throws any checked exceptions, they should be wrapped
 * in a {@code UndeclaredThrowableException} so that this class can get access to the cause.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ChainingListenableFuture<I, O> extends FutureChain<O> implements Runnable {
    @Nullable
    private AsyncFunction<? super I, ? extends O> mFunction;
    private final BlockingQueue<Boolean> mMayInterruptIfRunningChannel =
            new LinkedBlockingQueue<>(1);
    private final CountDownLatch mOutputCreated = new CountDownLatch(1);
    @Nullable
    private ListenableFuture<? extends I> mInputFuture;
    @Nullable
    volatile ListenableFuture<? extends O> mOutputFuture;

    ChainingListenableFuture(
            @NonNull AsyncFunction<? super I, ? extends O> function,
            @NonNull ListenableFuture<? extends I> inputFuture) {
        super();
        mFunction = checkNotNull(function);
        mInputFuture = checkNotNull(inputFuture);
    }

    /**
     * Delegate the get() to the input and output mFutures, in case
     * their implementations defer starting computation until their
     * own get() is invoked.
     */
    @Override
    @Nullable
    public O get() throws InterruptedException, ExecutionException {
        if (!isDone()) {
            // Invoking get on the mInputFuture will ensure our own run()
            // method below is invoked as a listener when mInputFuture sets
            // its value.  Therefore when get() returns we should then see
            // the mOutputFuture be created.
            ListenableFuture<? extends I> inputFuture = mInputFuture;
            if (inputFuture != null) {
                inputFuture.get();
            }

            // If our listener was scheduled to run on an executor we may
            // need to wait for our listener to finish running before the
            // mOutputFuture has been constructed by the mFunction.
            mOutputCreated.await();

            // Like above with the mInputFuture, we have a listener on
            // the mOutputFuture that will set our own value when its
            // value is set.  Invoking get will ensure the output can
            // complete and invoke our listener, so that we can later
            // get the mResult.
            ListenableFuture<? extends O> outputFuture = mOutputFuture;
            if (outputFuture != null) {
                outputFuture.get();
            }
        }
        return super.get();
    }

    /**
     * Delegate the get() to the input and output mFutures, in case
     * their implementations defer starting computation until their
     * own get() is invoked.
     */
    @Override
    @Nullable
    public O get(long timeout, @NonNull TimeUnit unit) throws TimeoutException,
            ExecutionException, InterruptedException {
        if (!isDone()) {
            // Use a single time unit so we can decrease mRemaining timeout
            // as we wait for various phases to complete.
            if (unit != NANOSECONDS) {
                timeout = NANOSECONDS.convert(timeout, unit);
                unit = NANOSECONDS;
            }

            // Invoking get on the mInputFuture will ensure our own run()
            // method below is invoked as a listener when mInputFuture sets
            // its value.  Therefore when get() returns we should then see
            // the mOutputFuture be created.
            ListenableFuture<? extends I> inputFuture = mInputFuture;
            if (inputFuture != null) {
                long start = System.nanoTime();
                inputFuture.get(timeout, unit);
                timeout -= Math.max(0, System.nanoTime() - start);
            }

            // If our listener was scheduled to run on an executor we may
            // need to wait for our listener to finish running before the
            // mOutputFuture has been constructed by the mFunction.
            long start = System.nanoTime();
            if (!mOutputCreated.await(timeout, unit)) {
                throw new TimeoutException();
            }
            timeout -= Math.max(0, System.nanoTime() - start);

            // Like above with the mInputFuture, we have a listener on
            // the mOutputFuture that will set our own value when its
            // value is set.  Invoking get will ensure the output can
            // complete and invoke our listener, so that we can later
            // get the mResult.
            ListenableFuture<? extends O> outputFuture = mOutputFuture;
            if (outputFuture != null) {
                outputFuture.get(timeout, unit);
            }
        }
        return super.get(timeout, unit);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        /*
         * Our additional cancellation work needs to occur even if
         * !mayInterruptIfRunning, so we can't move it into interruptTask().
         */
        if (super.cancel(mayInterruptIfRunning)) {
            // This should never block since only one thread is allowed to cancel
            // this Future.
            putUninterruptibly(mMayInterruptIfRunningChannel, mayInterruptIfRunning);
            cancel(mInputFuture, mayInterruptIfRunning);
            cancel(mOutputFuture, mayInterruptIfRunning);
            return true;
        }
        return false;
    }

    private void cancel(@Nullable Future<?> future,
            boolean mayInterruptIfRunning) {
        if (future != null) {
            future.cancel(mayInterruptIfRunning);
        }
    }

    @Override
    public void run() {
        try {
            I sourceResult;
            try {
                sourceResult = getUninterruptibly(mInputFuture);
            } catch (CancellationException e) {
                // Cancel this future and return.
                // At this point, mInputFuture is cancelled and mOutputFuture doesn't
                // exist, so the value of mayInterruptIfRunning is irrelevant.
                cancel(false);
                return;
            } catch (ExecutionException e) {
                // Set the cause of the exception as this future's exception
                setException(e.getCause());
                return;
            }

            final ListenableFuture<? extends O> outputFuture = mOutputFuture =
                    mFunction.apply(sourceResult);
            if (isCancelled()) {
                // Handles the case where cancel was called while the mFunction was
                // being applied.
                // There is a gap in cancel(boolean) between calling sync.cancel()
                // and storing the value of mayInterruptIfRunning, so this thread
                // needs to block, waiting for that value.
                outputFuture.cancel(takeUninterruptibly(mMayInterruptIfRunningChannel));
                mOutputFuture = null;
                return;
            }
            outputFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Here it would have been nice to have had an
                        // UninterruptibleListenableFuture, but we don't want to start a
                        // combinatorial explosion of interfaces, so we have to make do.
                        set(getUninterruptibly(outputFuture));
                    } catch (CancellationException e) {
                        // Cancel this future and return.
                        // At this point, mInputFuture and mOutputFuture are done, so the
                        // value of mayInterruptIfRunning is irrelevant.
                        cancel(false);
                        return;
                    } catch (ExecutionException e) {
                        // Set the cause of the exception as this future's exception
                        setException(e.getCause());
                    } finally {
                        // Don't pin inputs beyond completion
                        ChainingListenableFuture.this.mOutputFuture = null;
                    }
                }
            }, CameraXExecutors.directExecutor());
        } catch (UndeclaredThrowableException e) {
            // Set the cause of the exception as this future's exception
            setException(e.getCause());
        } catch (Exception e) {
            // This exception is irrelevant in this thread, but useful for the
            // client
            setException(e);
        } catch (Error e) {
            // Propagate errors up ASAP - our superclass will rethrow the error
            setException(e);
        } finally {
            // Don't pin inputs beyond completion
            mFunction = null;
            mInputFuture = null;
            // Allow our get routines to examine mOutputFuture now.
            mOutputCreated.countDown();
        }
    }

    /**
     * Invokes {@code queue.}{@link BlockingQueue#take() take()} uninterruptibly.
     */
    private <E> E takeUninterruptibly(@NonNull BlockingQueue<E> queue) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return queue.take();
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

    /**
     * Invokes {@code queue.}{@link BlockingQueue#put(Object) put(element)}
     * uninterruptibly.
     */
    private <E> void putUninterruptibly(@NonNull BlockingQueue<E> queue, @NonNull E element) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    queue.put(element);
                    return;
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
}
