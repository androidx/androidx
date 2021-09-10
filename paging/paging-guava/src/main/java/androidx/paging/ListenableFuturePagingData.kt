/*
 * Copyright 2020 The Android Open Source Project
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

@file:JvmName("PagingDataFutures")

package androidx.paging

import androidx.annotation.CheckResult
import com.google.common.util.concurrent.AsyncFunction
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

/**
 * Returns a [PagingData] containing the result of applying the given [transform] to each
 * element, as it is loaded.
 *
 * @param transform [AsyncFunction] to transform an item of type [T] to [R].
 * @param executor [Executor] to run the [AsyncFunction] in.
 */
@JvmName("map")
@CheckResult
fun <T : Any, R : Any> PagingData<T>.mapAsync(
    transform: AsyncFunction<T, R>,
    executor: Executor
): PagingData<R> = map {
    withContext(executor.asCoroutineDispatcher()) {
        transform.apply(it).await()
    }
}

/**
 * Returns a [PagingData] of all elements returned from applying the given [transform] to each
 * element, as it is loaded.
 *
 * @param transform [AsyncFunction] to transform an item of type [T] a list of items of type [R].
 * @param executor [Executor] to run the [AsyncFunction] in.
 */
@JvmName("flatMap")
@CheckResult
fun <T : Any, R : Any> PagingData<T>.flatMapAsync(
    transform: AsyncFunction<T, Iterable<R>>,
    executor: Executor
): PagingData<R> = flatMap {
    withContext(executor.asCoroutineDispatcher()) {
        transform.apply(it).await()
    }
}

/**
 * Returns a [PagingData] containing only elements matching the given [predicate].
 *
 * @param predicate [AsyncFunction] returning `false` for unmatched items which should be filtered.
 * @param executor [Executor] to run the [AsyncFunction] in.
 */
@JvmName("filter")
@CheckResult
fun <T : Any> PagingData<T>.filterAsync(
    predicate: AsyncFunction<T, Boolean>,
    executor: Executor
): PagingData<T> = filter {
    withContext(executor.asCoroutineDispatcher()) {
        predicate.apply(it).await()
    }
}

/**
 * Returns a [PagingData] containing each original element, with an optional separator generated
 * by [generator], given the elements before and after (or null, in boundary conditions).
 *
 * Note that this transform is applied asynchronously, as pages are loaded. Potential separators
 * between pages are only computed once both pages are loaded.
 *
 * @param generator [AsyncFunction] used to generate separator between two [AdjacentItems] or the
 * header or footer if either [AdjacentItems.before] or [AdjacentItems.after] is `null`.
 * @param executor [Executor] to run the [AsyncFunction] in.
 *
 * @sample androidx.paging.samples.insertSeparatorsFutureSample
 * @sample androidx.paging.samples.insertSeparatorsUiModelFutureSample
 */
@JvmName("insertSeparators")
@CheckResult
fun <T : R, R : Any> PagingData<T>.insertSeparatorsAsync(
    generator: AsyncFunction<AdjacentItems<T>, R?>,
    executor: Executor
): PagingData<R> = insertSeparators { before, after ->
    withContext(executor.asCoroutineDispatcher()) {
        generator.apply(AdjacentItems(before, after)).await()
    }
}

/**
 * Represents a pair of adjacent items, null values are used to signal boundary conditions.
 */
data class AdjacentItems<T>(val before: T?, val after: T?)
