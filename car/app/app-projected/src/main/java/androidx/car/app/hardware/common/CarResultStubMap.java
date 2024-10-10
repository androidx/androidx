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
package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import androidx.annotation.RestrictTo;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Convenience class wrapping calls to the car host that involve subscribing and unsubscribing to
 * an ongoing listener.
 *
 * @param <T> represents the result data type which this stub is returning
 * @param <U> represents the parameter data type which this stub is indexing on
 */
@RestrictTo(LIBRARY_GROUP)
public class CarResultStubMap<T, U> {
    private final HashMap<U, CarResultStub<T>> mStubMap = new HashMap<>();
    private final T mUnsupportedValue;
    private final int mResultType;
    private final CarHardwareHostDispatcher mHostDispatcher;

    /**
     * Creates an instance of the result stub map.
     *
     * @param resultType       the result type to fetch
     * @param unsupportedValue value to be returned if the host does not support the result type
     * @param hostDispatcher   dispatcher to be used for host calls
     *
     * @throws NullPointerException if {@code unsupportedValue} is {@code null} or if
     *                              {@code hostDispatcher} is {@code null}
     */
    public CarResultStubMap(int resultType, @NonNull T unsupportedValue,
            @NonNull CarHardwareHostDispatcher hostDispatcher) {
        mResultType = resultType;
        mUnsupportedValue = requireNonNull(unsupportedValue);
        mHostDispatcher = requireNonNull(hostDispatcher);
    }

    /**
     * Adds a listener for the given result type and parameter.
     *
     * <p>This call also kicks off the initial call to the host if needed. If the
     * {@code listener} was added previously then the executor is updated.
     *
     * @param params   the requested listener params or {@code null}
     * @param executor the executor which will be used for invoking the listener
     * @param listener listener for the results
     *
     * @throws NullPointerException if {@code executor} is {@code null} or if {@code listener} is
     *                              {@code null}
     */
    public void addListener(@Nullable U params,
            @NonNull Executor executor, @NonNull OnCarDataAvailableListener<T> listener) {
        requireNonNull(executor);
        requireNonNull(listener);

        CarResultStub<T> stub = mStubMap.get(params);
        if (stub != null) {
            stub.addListener(executor, listener);
            return;
        }
        try {
            Bundleable bundle = params == null ? null : Bundleable.create(params);
            stub = new CarResultStub<T>(mResultType,
                    bundle, false,
                    mUnsupportedValue,
                    mHostDispatcher);
            stub.addListener(executor, listener);
            mStubMap.put(params, stub);
        } catch (BundlerException e) {
            throw new IllegalArgumentException("Invalid params");
        }
    }

    /**
     * Removes a listener which was previously added from all params for which is it associated.
     *
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeListener(@NonNull OnCarDataAvailableListener<T> listener) {
        requireNonNull(listener);
        Iterator<Map.Entry<U, CarResultStub<T>>> iter = mStubMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<U, CarResultStub<T>> entry = iter.next();
            if (entry.getValue().removeListener(listener)) {
                iter.remove();
            }
        }
    }
}
