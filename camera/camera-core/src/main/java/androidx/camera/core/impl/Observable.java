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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * An observable stream which contains data or errors.
 *
 * @param <T> The type of the data in the stream.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface Observable<T> {

    /**
     * Fetch the latest piece of data asynchronously.
     *
     * <p>The returned future may also complete with an exception if the observable currently
     * contains an error. If
     * the observable has not yet been initialized with a value, the future may contain an
     * {@link IllegalStateException}.
     *
     * @return A future which will contain the latest value or an error.
     */
    @NonNull
    ListenableFuture<T> fetchData();

    /**
     * Adds an observer which will receive the stream of data.
     *
     * <p>This is an asynchronous operation. Once the observer has been added, it will
     * immediately be called with the latest value contained in the observable if it contains a
     * value, or will be called once a value has been set on the observable.
     *
     * <p>All added observers should be removed with {@link #removeObserver(Observer)} when no
     * longer needed.
     *
     * <p>If the same observer is added twice, it will only be called on the last executor it was
     * registered with.
     * @param executor The executor which will be used to notify the observer of new data.
     * @param observer The observer which will receive new data.
     */
    void addObserver(@NonNull Executor executor, @NonNull Observer<? super T> observer);

    /**
     * Removes a previously added observer.
     *
     * <p>Once removed, the observer will no longer receive data.
     *
     * <p>If the observer was not previously added, this operation will be a no-op.
     *
     * @param observer The observer to remove.
     */
    void removeObserver(@NonNull Observer<? super T> observer);

    /**
     * A callback that can receive new values and errors from an {@link Observable}.
     *
     * @param <T> The type of the data being reported.
     */
    interface Observer<T> {
        /**
         * Called when the stream emits a new piece of data.
         *
         * @param value The new data value
         */
        void onNewData(@Nullable T value);

        /**
         * Called when the stream emits an error.
         *
         * @param t The error.
         */
        void onError(@NonNull Throwable t);
    }
}
