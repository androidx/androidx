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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * ProcessingQueue handles the sequential aggregation and processing of a running list of elements.
 * It is designed to iteratively invoke the provided [process] function with a [MutableList] of
 * elements that have been aggregated from the previous iteration. [process] is guaranteed to be
 * invoked sequentially. [process] is expected to remove items from the [MutableList] that are no
 * longer needed (or that have been processed). Items that are not removed will be present in
 * subsequent [process] invocations.
 *
 * A key design consideration for this class, and the reason it does not operate on generic flows,
 * is the handling of unprocessed elements, which may need to be handled and/or closed. This is
 * non-trivial for buffered flows. [onUnprocessedElements] will be invoked synchronously with all
 * un-processed items exactly once if there is a non-zero number of unprocessed elements when the
 * ProcessingQueue scope is closed.
 *
 * Example Usage:
 * ```
 * class MyClass(scope: CoroutineScope) {
 *   private val processingQueue = ProcessingQueue<Int>(
 *     onUnprocessedElements = ::onUnprocessedElements
 *     process = ::processInts
 *   ).processIn(scope)
 *
 *   fun processAnInt(value: Int) {
 *     processingQueue.emitChecked(value)
 *   }
 *
 *   private suspend fun processInts(items: MutableList<Int>) {
 *     val first = items.removeFirst()
 *     println("Processing: $first")
 *   }
 *
 *   private fun onUnprocessedElements(items: List<Int>) {
 *     println("Releasing unprocessed items: items")
 *   }
 * }
 * ```
 *
 * This class is thread safe.
 */
internal class ProcessingQueue<T>(
    val capacity: Int = Channel.UNLIMITED,
    private val onUnprocessedElements: (List<T>) -> Unit = {},
    private val process: suspend (MutableList<T>) -> Unit
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

    private suspend fun processingLoop() {
        try {
            // The core loop is:
            // 1. Wait for a new item in the channel.
            // 2. Add all items that can be immediately received from the channel into queue.
            // 3. Process items (maybe suspend)
            // 4. If the queue of items is the same, assume processing did nothing and jump to 1.
            // 5. If the queue of items is different, assume processing did something and jump to 2.

            while (true) {
                // Suspend until we receive a element from the channel
                val element = channel.receive()
                queue.add(element)

                while (queue.isNotEmpty()) {
                    // Buffer any additional elements from the inputChannel that may have been sent
                    // during the last call to process
                    var nextResult = channel.tryReceive()
                    while (nextResult.isSuccess) {
                        queue.add(nextResult.getOrThrow())
                        nextResult = channel.tryReceive()
                    }

                    // Emit the list of elements. This may suspend, and the consumer may modify the
                    // list, which will be updated and sent back on the next iteration.
                    val size = queue.size
                    process(queue)
                    if (size == queue.size) {
                        break
                    }
                }
            }
        } catch (e: Throwable) {
            releaseUnprocessedElements(e)
            throw e
        }
    }

    private fun releaseUnprocessedElements(cause: Throwable?) {
        // If we reach here, it means the scope that was driving the processing loop has been
        // cancelled. It means that the last call to `processor` has exited. The first time
        // that channel.close() is called, the `onUndeliveredElement` handler will be invoked
        // with the item that was pending for delivery. This, however, does not include *all*
        // of the items, and we may need to iterate and handle the remaining items that may
        // still be in the channel.
        if (channel.close(cause)) {

            // After closing the channel, there may be remaining items in the channel that
            // were sent after the receiving scope was closed. Read these items out and send
            // them to the onUnpressedElements handler.
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
        fun <T> ProcessingQueue<T>.processIn(scope: CoroutineScope): ProcessingQueue<T> {
            check(started.compareAndSet(expect = false, update = true)) {
                "ProcessingQueue cannot be re-started!"
            }

            // Launch the processing loop in the provided scope.
            val job = scope.launch { processingLoop() }

            // If the scope is already cancelled, then `process` will never be invoked. To ensure
            // items are released, attempt to close the channel and release any remaining items.
            if (job.isCancelled) {
                releaseUnprocessedElements(null)
            }
            return this
        }
    }
}
