/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Helper function for using {@link com.google.common.util.concurrent.ListenableFuture}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FuturesHelper {
    /** Returns future with immediate result. */
    @NonNull
    public static <T> ListenableFuture<T> createImmediateFuture(@NonNull T value) {
        return createAnyImmediateFuture(value);
    }

    /** Returns immediate future. */
    @NonNull
    public static ListenableFuture<Void> createImmediateVoidFuture() {
        return createAnyImmediateFuture(null);
    }

    /** Returns failed future. */
    @NonNull
    public static <T> ListenableFuture<T> createFailedFuture(@NonNull Throwable throwable) {
        ResolvableFuture<T> errorFuture = ResolvableFuture.create();
        errorFuture.setException(throwable);
        return errorFuture;
    }

    @NonNull
    private static <T> ResolvableFuture<T> createAnyImmediateFuture(@Nullable T value) {
        ResolvableFuture<T> future = ResolvableFuture.create();
        future.set(value);
        return future;
    }

    private FuturesHelper() {}
}
