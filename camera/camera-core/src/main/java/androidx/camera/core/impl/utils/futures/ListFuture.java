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
import static androidx.core.util.Preconditions.checkState;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Class is based on the ListFuture in Guava and to use the CallbackToFutureAdapter instead
 * of the AbstractFuture.
 *
 * Class that implements {@link Futures#allAsList(Collection)} and
 * {@link Futures#successfulAsList(Collection)}.
 * The idea is to create a (null-filled) List and register a listener with
 * each component future to fill out the value in the List when that future
 * completes.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ListFuture<V> implements ListenableFuture<List<V>> {
    @Nullable
    List<? extends ListenableFuture<? extends V>> mFutures;
    @Nullable
    List<V> mValues;
    private final boolean mAllMustSucceed;
    @NonNull
    private final AtomicInteger mRemaining;
    @NonNull
    private final ListenableFuture<List<V>> mResult;
    CallbackToFutureAdapter.Completer<List<V>> mResultNotifier;

    /**
     * Constructor.
     *
     * @param futures          all the futures to build the list from
     * @param allMustSucceed   whether a single failure or cancellation should
     *                         propagate to this future
     * @param listenerExecutor used to run listeners on all the passed in futures.
     */
    ListFuture(
            @NonNull List<? extends ListenableFuture<? extends V>> futures,
            boolean allMustSucceed, @NonNull Executor listenerExecutor) {
        mFutures = checkNotNull(futures);
        mValues = new ArrayList<>(futures.size());
        mAllMustSucceed = allMustSucceed;
        mRemaining = new AtomicInteger(futures.size());
        mResult = CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<List<V>>() {
                    @Override
                    public Object attachCompleter(
                            @NonNull CallbackToFutureAdapter.Completer<List<V>> completer) {
                        Preconditions.checkState(mResultNotifier == null,
                                "The result can only set once!");
                        mResultNotifier = completer;
                        return "ListFuture[" + this + "]";
                    }
                });

        init(listenerExecutor);
    }

    private void init(@NonNull Executor listenerExecutor) {
        // First, schedule cleanup to execute when the Future is done.
        addListener(new Runnable() {
            @Override
            public void run() {
                // By now the mValues array has either been set as the Future's value,
                // or (in case of failure) is no longer useful.
                ListFuture.this.mValues = null;

                // Let go of the memory held by other mFutures
                ListFuture.this.mFutures = null;
            }
        }, CameraXExecutors.directExecutor());

        // Now begin the "real" initialization.

        // Corner case: List is empty.
        if (mFutures.isEmpty()) {
            mResultNotifier.set(new ArrayList<>(mValues));
            return;
        }

        // Populate the results list with null initially.
        for (int i = 0; i < mFutures.size(); ++i) {
            mValues.add(null);
        }

        // Register a listener on each Future in the list to update
        // the state of this future.
        // Note that if all the mFutures on the list are done prior to completing
        // this loop, the last call to addListener() will callback to
        // setOneValue(), transitively call our cleanup listener, and set
        // mFutures to null.
        // We store a reference to mFutures to avoid the NPE.
        List<? extends ListenableFuture<? extends V>> localFutures = mFutures;
        for (int i = 0; i < localFutures.size(); i++) {
            final ListenableFuture<? extends V> listenable = localFutures.get(i);
            final int index = i;
            listenable.addListener(new Runnable() {
                @Override
                public void run() {
                    setOneValue(index, listenable);
                }
            }, listenerExecutor);
        }
    }

    /**
     * Sets the value at the given index to that of the given future.
     */
    void setOneValue(int index, @NonNull Future<? extends V> future) {
        List<V> localValues = mValues;
        if (isDone() || localValues == null) {
            // Some other future failed or has been cancelled, causing this one to
            // also be cancelled or have an exception set. This should only happen
            // if mAllMustSucceed is true.
            checkState(mAllMustSucceed,
                    "Future was done before all dependencies completed");
            return;
        }

        try {
            checkState(future.isDone(),
                    "Tried to set value from future which is not done");
            localValues.set(index, getUninterruptibly(future));
        } catch (CancellationException e) {
            if (mAllMustSucceed) {
                // Set ourselves as cancelled. Let the input futures keep running
                // as some of them may be used elsewhere.
                // (Currently we don't override interruptTask, so
                // mayInterruptIfRunning==false isn't technically necessary.)
                cancel(false);
            }
        } catch (ExecutionException e) {
            if (mAllMustSucceed) {
                // As soon as the first one fails, throw the exception up.
                // The mResult of all other inputs is then ignored.
                mResultNotifier.setException(e.getCause());
            }
        } catch (RuntimeException e) {
            if (mAllMustSucceed) {
                mResultNotifier.setException(e);
            }
        } catch (Error e) {
            // Propagate errors up ASAP - our superclass will rethrow the error
            mResultNotifier.setException(e);
        } finally {
            int newRemaining = mRemaining.decrementAndGet();
            checkState(newRemaining >= 0, "Less than 0 remaining futures");
            if (newRemaining == 0) {
                localValues = mValues;
                if (localValues != null) {
                    mResultNotifier.set(new ArrayList<>(localValues));
                } else {
                    checkState(isDone());
                }
            }
        }
    }

    @Override
    public void addListener(@NonNull Runnable listener, @NonNull Executor executor) {
        mResult.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mFutures != null) {
            for (ListenableFuture<? extends V> f : mFutures) {
                f.cancel(mayInterruptIfRunning);
            }
        }

        return mResult.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return mResult.isCancelled();
    }

    @Override
    public boolean isDone() {
        return mResult.isDone();
    }

    @Override
    @Nullable
    public List<V> get() throws InterruptedException, ExecutionException {
        callAllGets();

        // This may still block in spite of the calls above, as the listeners may
        // be scheduled for execution in other threads.
        return mResult.get();
    }

    @Override
    public List<V> get(long timeout, @NonNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return mResult.get(timeout, unit);
    }

    /**
     * Calls the get method of all dependency futures to work around a bug in
     * some ListenableFutures where the listeners aren't called until get() is
     * called.
     */
    private void callAllGets() throws InterruptedException {
        List<? extends ListenableFuture<? extends V>> oldFutures = mFutures;
        if (oldFutures != null && !isDone()) {
            for (ListenableFuture<? extends V> future : oldFutures) {
                // We wait for a little while for the future, but if it's not done,
                // we check that no other futures caused a cancellation or failure.
                // This can introduce a delay of up to 10ms in reporting an exception.
                while (!future.isDone()) {
                    try {
                        future.get();
                    } catch (Error e) {
                        throw e;
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Throwable e) {
                        // ExecutionException / CancellationException / RuntimeException
                        if (mAllMustSucceed) {
                            return;
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
    }
}
