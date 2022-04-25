/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * An {@link Observable} whose value is set at construction time and never changes.
 * @param <T> The observed type.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ConstantObservable<T> implements Observable<T> {
    private static final ConstantObservable<Object> NULL_OBSERVABLE =
            new ConstantObservable<>(null);
    private static final String TAG = "ConstantObservable";

    private final ListenableFuture<T> mValueFuture;

    /**
     * Creates an {@link Observable} with constant given value.
     * @param value The value which will immediately be sent to observers and is always returned
     *              by {@link #fetchData()}.
     */
    @NonNull
    public static <U> Observable<U> withValue(@Nullable U value) {
        if (value == null) {
            @SuppressWarnings({"unchecked", "rawtypes"}) // Safe since null can be cast to any type
            Observable<U> typedNull = (Observable) NULL_OBSERVABLE;
            return typedNull;
        }
        return new ConstantObservable<>(value);
    }

    private ConstantObservable(@Nullable T value) {
        mValueFuture = Futures.immediateFuture(value);
    }

    @NonNull
    @Override
    public ListenableFuture<T> fetchData() {
        return mValueFuture;
    }

    @Override
    public void addObserver(@NonNull Executor executor, @NonNull Observer<? super T> observer) {
        // Since the Observable has a constant value, we only will have a one-shot call to the
        // observer, so we don't need to store the observer.
        // ImmediateFuture does not actually store listeners since it is already complete, so it
        // is safe to call addListener() here without leaking.
        mValueFuture.addListener(() -> {
            try {
                observer.onNewData(mValueFuture.get());
            } catch (ExecutionException | InterruptedException e) {
                // Note: this should not be possible as Futures.immediateFuture() should return a
                // future that is already complete and has not failed with an exception.
                observer.onError(e);
            }
        }, executor);
    }

    @Override
    public void removeObserver(@NonNull Observer<? super T> observer) {
        // no-op. addObserver() does not need to store observers.
    }
}
