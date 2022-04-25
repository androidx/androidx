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
import androidx.annotation.RequiresApi;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Cloned from concurrent-futures package in Guava to AndroidX namespace since we would need
 * ListenableFuture related implementation but not want to include whole Guava library.
 *
 * Transforms a value, possibly asynchronously. For an example usage and more information, see
 * {@link Futures#transformAsync(ListenableFuture, AsyncFunction, Executor)}.
 *
 * @author Chris Povirk
 * @since 11.0
 * @param <I>
 * @param <O>
 */
@FunctionalInterface
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface AsyncFunction<I, O> {
    /**
     * Returns an output {@code Future} to use in place of the given {@code input}. The output
     * {@code Future} need not be {@linkplain Future#isDone done}, making {@code AsyncFunction}
     * suitable for asynchronous derivations.
     *
     * <p>Throwing an exception from this method is equivalent to returning a failing {@code
     * Future}.
     */
    ListenableFuture<O> apply(@Nullable I input) throws Exception;
}
