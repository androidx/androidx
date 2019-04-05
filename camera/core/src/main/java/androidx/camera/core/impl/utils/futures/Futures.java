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

import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Utility class for generating specific implementations of {@link ListenableFuture}.
 */
public final class Futures {

    /**
     * Returns an implementation of {@link ListenableFuture} which immediately contains a result.
     * @param value The result that is immediately set on the future.
     * @param <V> The type of the result.
     * @return A future which immediately contains the result.
     */
    public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
        if (value == null) {
            return ImmediateFuture.nullFuture();
        }

        return new ImmediateFuture<>(value);
    }

    /**
     * Should not be instantiated.
     */
    private Futures() {}
}
