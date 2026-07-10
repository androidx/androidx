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

package androidx.camera.core.impl;

import android.os.SystemClock;

import androidx.annotation.GuardedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * An observable implemented using {@link LiveData}.
 *
 * <p>While this class can provide error reporting, it is prone to other issues. First, all updates
 * will originate from the main thread before being sent to the observer's executor. Second, there
 * exists the possibility of error and value elision. This means that some posted values and some
 * posted errors may be ignored if a newer error/value is posted before the observers can be
 * updated. If it is important for observers to receive all updates, then this class should not be
 * used.
 *
 * @param <T> The data type used for
 *            {@link Observable.Observer#onNewData(Object)}.
 */
public final class LiveDataObservable<T> implements Observable<T> {

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final MutableLiveData<Result<T>> mLiveData = new MutableLiveData<>();
    /** The observers from {@link #addObserver(Executor, Observer)} and their executors. */
    @GuardedBy("mObservers")
    private final Map<Observer<? super T>, Executor> mObservers = new HashMap<>();
    /** The single observer that is attached to the {@link LiveData}. */
    private androidx.lifecycle.@Nullable Observer<Result<T>> mLiveDataObserver;

    /**
     * Posts a new value to be used as the current value of this Observable.
     */
    public void postValue(@Nullable T value) {
        mLiveData.postValue(Result.fromValue(value));
    }

    /**
     * Posts a new error to be used as the current error state of this Observable.
     */
    public void postError(@NonNull Throwable error) {
        mLiveData.postValue(Result.fromError(error));
    }

    /**
     * Returns the underlying {@link LiveData} used to store and update {@link Result Results}.
     */
    public @NonNull LiveData<Result<T>> getLiveData() {
        return mLiveData;
    }

    @Override
    @SuppressWarnings("ObjectToString")
    public @NonNull ListenableFuture<T> fetchData() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            CameraXExecutors.mainThreadExecutor().execute(() -> {
                Result<T> result = mLiveData.getValue();
                if (result == null) {
                    completer.setException(new IllegalStateException(
                            "Observable has not yet been initialized with a value."));
                } else if (result.completedSuccessfully()) {
                    completer.set(result.getValue());
                } else {
                    Preconditions.checkNotNull(result.getError());
                    completer.setException(result.getError());
                }
            });

            return LiveDataObservable.this + " [fetch@" + SystemClock.uptimeMillis() + "]";
        });
    }

    @Override
    public void addObserver(@NonNull Executor executor, @NonNull Observer<? super T> observer) {
        synchronized (mObservers) {
            boolean wasEmpty = mObservers.isEmpty();
            mObservers.put(observer, executor);

            // If this is the first observer, we need to create and attach the internal observer.
            if (wasEmpty) {
                enableInternalObserver();
            } else {
                // Pass the current value to the new observer.
                executor.execute(() -> {
                    Result<T> result = mLiveData.getValue();

                    if (result == null) {
                        return;
                    }

                    if (result.completedSuccessfully()) {
                        observer.onNewData(result.getValue());
                    } else {
                        Preconditions.checkNotNull(result.getError());
                        observer.onError(result.getError());
                    }
                });
            }
        }
    }

    @Override
    public void removeObserver(@NonNull Observer<? super T> observer) {
        synchronized (mObservers) {
            mObservers.remove(observer);

            // If this was the last observer, we can detach the internal observer.
            if (mObservers.isEmpty()) {
                disableInternalObserver();
            }
        }
    }

    /**
     * Creates the internal observer which will dispatch results to all registered observers and
     * attaches it to the {@link LiveData}.
     */
    private void enableInternalObserver() {
        CameraXExecutors.mainThreadExecutor().execute(() -> {
            if (mLiveDataObserver == null) {
                // The internal observer dispatches results to all registered observers.
                mLiveDataObserver = result -> {
                    Map<Observable.Observer<? super T>, Executor> observersCopy;
                    synchronized (mObservers) {
                        // Make a copy to iterate outside the synchronized block. This
                        // prevents holding the lock while dispatching, which could cause
                        // deadlocks.
                        observersCopy = new HashMap<>(mObservers);
                    }

                    for (Map.Entry<Observable.Observer<? super T>, Executor> entry :
                            observersCopy.entrySet()) {
                        entry.getValue().execute(() -> {
                            Observable.Observer<? super T> observer = entry.getKey();
                            if (result.completedSuccessfully()) {
                                observer.onNewData(result.getValue());
                            } else {
                                Preconditions.checkNotNull(result.getError());
                                observer.onError(result.getError());
                            }
                        });
                    }
                };
            }
            mLiveData.observeForever(mLiveDataObserver);
        });
    }

    /** Detaches the internal observer from the {@link LiveData}. */
    private void disableInternalObserver() {
        CameraXExecutors.mainThreadExecutor().execute(() -> {
            if (mLiveDataObserver != null) {
                mLiveData.removeObserver(mLiveDataObserver);
            }
        });
    }

    /**
     * A wrapper class that allows error reporting.
     *
     * A Result can contain either a value or an error, but not both.
     *
     * @param <T> The data type used for
     *            {@link Observable.Observer#onNewData(Object)}.
     */
    public static final class Result<T> {
        private final @Nullable T mValue;
        private final @Nullable Throwable mError;

        private Result(@Nullable T value, @Nullable Throwable error) {
            mValue = value;
            mError = error;
        }

        /**
         * Creates a successful result that contains a value.
         */
        static <T> Result<T> fromValue(@Nullable T value) {
            return new Result<>(value, null);
        }

        /**
         * Creates a failed result that contains an error.
         */
        static <T> Result<T> fromError(@NonNull Throwable error) {
            return new Result<>(null, Preconditions.checkNotNull(error));
        }

        /**
         * Returns whether this result contains a value or an error.
         *
         * <p>A successful result will contain a value.
         */
        public boolean completedSuccessfully() {
            return mError == null;
        }

        /**
         * Returns the value contained within this result.
         *
         * @throws IllegalStateException if the result contains an error rather than a value.
         */
        public @Nullable T getValue() {
            if (!completedSuccessfully()) {
                throw new IllegalStateException(
                        "Result contains an error. Does not contain a value.");
            }

            return mValue;
        }

        /**
         * Returns the error contained within this result, or {@code null} if the result contains
         * a value.
         */
        public @Nullable Throwable getError() {
            return mError;
        }

        @Override
        public @NonNull String toString() {
            return "[Result: <" + (completedSuccessfully() ? "Value: " + mValue :
                    "Error: " + mError) + ">]";
        }
    }
}
