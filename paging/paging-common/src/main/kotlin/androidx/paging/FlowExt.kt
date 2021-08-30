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

package androidx.paging

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * This File includes custom flow operators that we implement to avoid using experimental APIs
 * from coroutines. Eventually, this file should be deleted once those APIs become stable.
 */

private val NULL = Any()

/**
 * Temporary `scan` operator on Flow without experimental APIs.
 */
internal fun <T, R> Flow<T>.simpleScan(
    initial: R,
    operation: suspend (accumulator: R, value: T) -> R
): Flow<R> = flow {
    var accumulator: R = initial
    emit(accumulator)
    collect { value ->
        accumulator = operation(accumulator, value)
        emit(accumulator)
    }
}

/**
 * Temporary `runningReduce` operator on Flow without experimental APIs.
 */
internal fun <T> Flow<T>.simpleRunningReduce(
    operation: suspend (accumulator: T, value: T) -> T
): Flow<T> = flow {
    var accumulator: Any? = NULL
    collect { value ->
        accumulator = if (accumulator === NULL) {
            value
        } else {
            @Suppress("UNCHECKED_CAST")
            operation(accumulator as T, value)
        }
        @Suppress("UNCHECKED_CAST")
        emit(accumulator as T)
    }
}

/**
 * This is a similar implementation to transformLatest using a channel Flow.
 */
internal fun <T, R> Flow<T>.simpleTransformLatest(
    transform: suspend FlowCollector<R>.(value: T) -> Unit
): Flow<R> = simpleChannelFlow {
    val origin = this@simpleTransformLatest
    val collector = ChannelFlowCollector(this@simpleChannelFlow)
    origin.collectLatest { value ->
        collector.transform(value)
    }
}

/**
 * flatMapLatest without experimental APIs
 */
internal inline fun <T, R> Flow<T>.simpleFlatMapLatest(
    crossinline transform: suspend (value: T) -> Flow<R>
): Flow<R> = simpleTransformLatest { emitAll(transform(it)) }

/**
 * mapLatest without experimental APIs
 */
internal inline fun <T, R> Flow<T>.simpleMapLatest(
    crossinline transform: suspend (value: T) -> R
): Flow<R> = simpleTransformLatest { emit(transform(it)) }

internal class ChannelFlowCollector<T>(
    val channel: SendChannel<T>
) : FlowCollector<T> {
    override suspend fun emit(value: T) {
        channel.send(value)
    }
}