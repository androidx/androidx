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

import androidx.paging.CombineSource.INITIAL
import androidx.paging.CombineSource.OTHER
import androidx.paging.CombineSource.RECEIVER
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger

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

/**
 * Similar to [kotlinx.coroutines.flow.combine], except it never batches reads from its Flows, so
 * [transform] is always guaranteed to get called for every emission from either Flow after the
 * initial call (which waits for the first emission from both Flows).
 *
 * The emissions for both Flows are also guaranteed to get buffered, so if one Flow emits
 * multiple times before the other does, [transform] will get called for each emission from the
 * first Flow instead of just once with the latest values.
 *
 * @param transform The transform to apply on each update. This is first called after awaiting an
 * initial emission from both Flows, and then is guaranteed to be called for every emission from
 * either Flow.
 *
 * For convenience, [CombineSource] is also passed to the transform, which indicates the
 * origin of the update with the following possible values:
 *   * [INITIAL]: Initial emission from both Flows
 *   * [RECEIVER]: Triggered by new emission from receiver
 *   * [OTHER]: Triggered by new emission from [otherFlow]
 */
internal suspend inline fun <T1, T2, R> Flow<T1>.combineWithoutBatching(
    otherFlow: Flow<T2>,
    crossinline transform: suspend (T1, T2, updateFrom: CombineSource) -> R,
): Flow<R> {
    return simpleChannelFlow {
        val incompleteFlows = AtomicInteger(2)
        val unbatchedFlowCombiner = UnbatchedFlowCombiner<T1, T2> { t1, t2, updateFrom ->
            send(transform(t1, t2, updateFrom))
        }
        val parentJob = Job()
        arrayOf(this@combineWithoutBatching, otherFlow).forEachIndexed { index, flow ->
            launch(parentJob) {
                try {
                    flow.collect { value ->
                        unbatchedFlowCombiner.onNext(index, value)

                        // Make this more fair, giving the other flow a chance to emit.
                        yield()
                    }
                } finally {
                    if (incompleteFlows.decrementAndGet() == 0) {
                        close()
                    }
                }
            }
        }

        awaitClose { parentJob.cancel() }
    }
}

/**
 * Helper class for [UnbatchedFlowCombiner], which handles dispatching the combined values in the
 * correct order, and with [CombineSource].
 *
 * NOTE: This implementation relies on the fact that [onNext] is called in-order for emissions
 * from the same Flow. This means that concurrently calling [onNext] with the same index will not
 * work.
 *
 * @see combineWithoutBatching
 */
internal class UnbatchedFlowCombiner<T1, T2>(
    private val send: suspend (t1: T1, t2: T2, updateFrom: CombineSource) -> Unit
) {
    private val initialDispatched = CompletableDeferred<Unit>()
    private val lock = Mutex()
    private val valueReceived = Array(2) { CompletableDeferred<Unit>() }
    private val values = Array<Any?>(2) { NULL }

    suspend fun onNext(index: Int, value: Any?) {
        // Allow the first value to dispatch immediately, but for subsequent values, we should
        // wait until the other flow emits, so that we don't overwrite the previous value.
        if (valueReceived[index].isCompleted) {
            // NOTE: We use a separate Completable here because just awaiting
            // valueReceived[1 - index] could potentially allow multiple calls to onNext from the
            // same Flow to trigger out of order.
            initialDispatched.await()
        } else {
            valueReceived[index].complete(Unit)
        }

        lock.withLock {
            val isInitial = values.any { it === NULL }
            values[index] = value

            if (values.none { it === NULL }) {
                val updateFrom = when {
                    isInitial -> INITIAL
                    index == 0 -> RECEIVER
                    else -> OTHER
                }

                @Suppress("UNCHECKED_CAST")
                send(values[0] as T1, values[1] as T2, updateFrom)
                initialDispatched.complete(Unit)
            }
        }
    }
}

/**
 * Used to indicate which Flow emission triggered the transform block in [combineWithoutBatching].
 *
 * @see combineWithoutBatching
 */
internal enum class CombineSource {
    INITIAL,
    RECEIVER,
    OTHER,
}