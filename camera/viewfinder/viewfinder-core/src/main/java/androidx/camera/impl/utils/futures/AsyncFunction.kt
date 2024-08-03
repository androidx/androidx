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

package androidx.camera.impl.utils.futures

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

/**
 * Cloned from concurrent-futures package in Guava to AndroidX namespace since we would need
 * ListenableFuture related implementation but not want to include whole Guava library.
 *
 * Transforms a value, possibly asynchronously. For an example usage and more information, see
 * [Futures.transformAsync].
 *
 * @param <I>
 * @param <O> </O></I>
 * @author Chris Povirk
 * @since 11.0
 */
fun interface AsyncFunction<I, O> {
    /**
     * Returns an output `Future` to use in place of the given `input`. The output `Future` need not
     * be [done][Future.isDone], making `AsyncFunction` suitable for asynchronous derivations.
     *
     * Throwing an exception from this method is equivalent to returning a failing `Future`.
     */
    @Throws(Exception::class) fun apply(input: I?): ListenableFuture<O>
}
