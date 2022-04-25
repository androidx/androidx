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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class LiveDataObservable<T> implements Observable<T> {


    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final MutableLiveData<Result<T>> mLiveData = new MutableLiveData<>();
    @GuardedBy("mObservers")
    private final Map<Observer<? super T>, LiveDataObserverAdapter<T>> mObservers = new HashMap<>();

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
    @NonNull
    public LiveData<Result<T>> getLiveData() {
        return mLiveData;
    }

    @NonNull
    @Override
    @SuppressWarnings("ObjectToString")
    public ListenableFuture<T> fetchData() {
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
            final LiveDataObserverAdapter<T> oldAdapter = mObservers.get(observer);
            if (oldAdapter != null) {
                oldAdapter.disable();
            }

            final LiveDataObserverAdapter<T> newAdapter = new LiveDataObserverAdapter<>(executor,
                    observer);
            mObservers.put(observer, newAdapter);

            CameraXExecutors.mainThreadExecutor().execute(() -> {
                if (oldAdapter != null) {
                    mLiveData.removeObserver(oldAdapter);
                }
                mLiveData.observeForever(newAdapter);
            });
        }
    }

    @Override
    public void removeObserver(@NonNull Observer<? super T> observer) {
        synchronized (mObservers) {
            LiveDataObserverAdapter<T> adapter = mObservers.remove(observer);

            if (adapter != null) {
                adapter.disable();
                CameraXExecutors.mainThreadExecutor().execute(
                        () -> mLiveData.removeObserver(adapter));
            }
        }
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
        @Nullable
        private final T mValue;
        @Nullable
        private final Throwable mError;

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
        @Nullable
        public T getValue() {
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
        @Nullable
        public Throwable getError() {
            return mError;
        }

        @Override
        @NonNull
        public String toString() {
            return "[Result: <" + (completedSuccessfully() ? "Value: " + mValue :
                    "Error: " + mError) + ">]";
        }
    }

    private static final class LiveDataObserverAdapter<T> implements
            androidx.lifecycle.Observer<Result<T>> {

        final AtomicBoolean mActive = new AtomicBoolean(true);
        final Observer<? super T> mObserver;
        final Executor mExecutor;

        LiveDataObserverAdapter(@NonNull Executor executor, @NonNull Observer<? super T> observer) {
            mExecutor = executor;
            mObserver = observer;
        }

        void disable() {
            mActive.set(false);
        }

        @Override
        public void onChanged(@NonNull final Result<T> result) {
            mExecutor.execute(() -> {
                if (!mActive.get()) {
                    // Observer has been disabled.
                    return;
                }

                if (result.completedSuccessfully()) {
                    mObserver.onNewData(result.getValue());
                } else {
                    Preconditions.checkNotNull(result.getError());
                    mObserver.onError(result.getError());
                }
            });
        }
    }
}
