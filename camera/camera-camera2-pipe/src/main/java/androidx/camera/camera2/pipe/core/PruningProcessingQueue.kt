/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.core

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope

/**
 * PruningProcessingQueue is a data structure designed and optimized for parallel, high throughput,
 * continuous element processing operations. It handles the sequential aggregation, pruning and
 * processing of a running list of elements.
 *
 * PruningProcessingQueue is constructed and instructed with these notable parameters:
 * - [prune] gets a mutable list of elements and is expected to trim, remove elements that can be
 *   dropped, or reorder elements for those that have a higher priority. It is invoked whenever
 *   there's a new batch of element ready to be processed.
 * - [onUnprocessedElements] gets the list of elements that haven't been processed. It is invoked
 *   when the coroutine scope is cancelled, or whenever there was an exception taken place during
 *   processing.
 * - [process] is continuously and sequentially invoked with the next element to process. It is
 *   invoked only after the previous processing job is done.
 *
 * Example Usage:
 * ```
 * class MyClass(scope: CoroutineScope) {
 *     private val processingQueue = ProcessingQueue<Int>(
 *         onUnprocessedElements = ::onUnprocessedElements
 *             prune = ::pruneInts
 *             process = ::processInt
 *         ).processIn(scope)
 *
 *     fun processAnInt(value: Int) {
 *         processingQueue.emitChecked(value)
 *     }
 *
 *     private fun pruneInts(items: MutableList<Int>) {
 *         println("Pruning: $items")
 *         items.removeAt(0)
 *     }
 *
 *     private suspend fun processInt(item: Int) {
 *         println("Processing: $item")
 *     }
 *
 *     private fun onUnprocessedElements(items: List<Int>) {
 *         println("Releasing unprocessed items: items")
 *     }
 * }
 * ```
 *
 * This class is thread safe.
 */
internal class PruningProcessingQueue<T>(
    val capacity: Int = Channel.UNLIMITED,
    private val prune: (MutableList<T>) -> Unit = {},
    private val onUnprocessedElements: (List<T>) -> Unit = {},
    private val process: suspend (T) -> Unit,
) {
    private val started = atomic(false)
    private val channel = Channel<T>(capacity = capacity, onUndeliveredElement = { queue.add(it) })
    private val queue = ArrayDeque<T>()

    /** Emit an element into the queue, suspending if the queue is at capacity. */
    suspend fun emit(element: T) {
        channel.send(element)
    }

    /** Emit an element into the queue, throwing an exception if it is closed or at capacity. */
    fun emitChecked(element: T) {
        val result = channel.trySend(element)
        check(result.isSuccess) { "Failed to emit item to ProcessingQueue!: $result" }
    }

    /**
     * Synchronously emit an element into the queue. Returns false if closed or if the queue is at
     * capacity.
     */
    fun tryEmit(element: T): Boolean {
        return channel.trySend(element).isSuccess
    }

    private suspend fun processingLoop() = supervisorScope {
        var processDeferred: Deferred<Unit>? = null
        var exitCause: Throwable? = null
        while (isActive) {
            try {
                // In our processing loop, one of two events can occur:
                //
                // 1. We received a new element
                // 2. `process` finished processing an element
                //
                // This means our workflow is driven by these two events. Whichever occurs, we'll
                // handle the event, and see if we're ready to process a new element.
                //
                // Using select here effectively helps us sequence these two events, as it "selects"
                // whichever comes first. This eliminates the need for locks, which reduces
                // potential resource contention in high throughput scenarios.
                select<Unit> {
                    channel.onReceive {
                        queue.add(it)

                        // Optimization: When we receive elements to process, there may be multiple
                        // elements ready to be received. To reduce the amount of `prune` calls,
                        // here we continue receiving elements until we're unable to.
                        var nextResult = channel.tryReceive()
                        while (nextResult.isSuccess) {
                            queue.add(nextResult.getOrThrow())
                            nextResult = channel.tryReceive()
                        }
                        Log.debug { "PruningProcessingQueue: Pruning $queue" }
                        prune(queue)
                    }

                    processDeferred?.onAwait { processDeferred = null }
                }
            } catch (cancellationException: CancellationException) {
                Log.debug(cancellationException) { "PruningProcessingQueue: Scope cancelled" }
                break
            } catch (throwable: Throwable) {
                Log.error(throwable) { "Encountered exception during processing" }
                exitCause = throwable
                break
            }

            if (queue.isEmpty() || processDeferred != null) continue

            val elementToProcess = queue.first()
            val deferred = async {
                Log.debug { "PruningProcessingQueue: Processing $elementToProcess" }
                process(elementToProcess)
            }
            if (deferred.isCancelled) {
                // If the Job is already cancelled, the CoroutineScope may have been cancelled.
                Log.info { "Unable to process $elementToProcess due to Job cancellation" }
                break
            }
            queue.removeFirst()
            processDeferred = deferred
        }

        closeAndReleaseUnprocessedElements(exitCause)
        exitCause?.let { throw it }
    }

    private fun closeAndReleaseUnprocessedElements(cause: Throwable?) {
        // If we reach here, it means the scope that was driving the processing loop has been
        // cancelled. It means that the last call to `process` has exited. The first time
        // that channel.close() is called, the `onUndeliveredElement` handler will be invoked
        // with the item that was pending for delivery. This, however, does not include *all*
        // of the items, and we may need to iterate and handle the remaining items that may
        // still be in the channel.
        if (channel.close(cause)) {
            // After closing the channel, there may be remaining items in the channel that were sent
            // after the receiving scope was closed. Read these items out and send them to the
            // onUnprocessedElements handler.
            var nextResult = channel.tryReceive()
            while (nextResult.isSuccess) {
                queue.add(nextResult.getOrThrow())
                nextResult = channel.tryReceive()
            }

            // Synchronously invoke the onUnprocessedElements handler with the remaining items.
            if (queue.isNotEmpty()) {
                onUnprocessedElements(queue.toMutableList())
                queue.clear()
            }
        }
    }

    internal companion object {
        /** Launch the processing loop in the provided processing scope. */
        fun <T> PruningProcessingQueue<T>.processIn(
            scope: CoroutineScope
        ): PruningProcessingQueue<T> {
            check(started.compareAndSet(expect = false, update = true)) {
                "PruningProcessingQueue cannot be re-started!"
            }

            // Launch the processing loop in the provided scope.
            val job = scope.launch { processingLoop() }

            // If the scope is already cancelled, then `process` will never be invoked. To ensure
            // items are released, attempt to close the channel and release any remaining items.
            if (job.isCancelled) {
                closeAndReleaseUnprocessedElements(null)
            }
            return this
        }
    }
}
