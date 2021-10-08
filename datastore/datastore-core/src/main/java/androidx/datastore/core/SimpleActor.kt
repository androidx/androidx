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

package androidx.datastore.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

internal class SimpleActor<T>(
    /**
     * The scope in which to consume messages.
     */
    private val scope: CoroutineScope,
    /**
     * Function that will be called when scope is cancelled. Should *not* throw exceptions.
     */
    onComplete: (Throwable?) -> Unit,
    /**
     * Function that will be called for each element when the scope is cancelled. Should *not*
     * throw exceptions.
     */
    onUndeliveredElement: (T, Throwable?) -> Unit,
    /**
     * Function that will be called once for each message.
     *
     * Must *not* throw an exception (other than CancellationException if scope is cancelled).
     */
    private val consumeMessage: suspend (T) -> Unit
) {
    private val messageQueue = Channel<T>(capacity = UNLIMITED)

    /**
     * Count of the number of remaining messages to process. When the messageQueue is closed,
     * this is no longer used.
     */
    private val remainingMessages = AtomicInteger(0)

    init {
        // If the scope doesn't have a job, it won't be cancelled, so we don't need to register a
        // callback.
        scope.coroutineContext[Job]?.invokeOnCompletion { ex ->
            onComplete(ex)

            // TODO(rohitsat): replace this with Channel(onUndeliveredElement) when it
            // is fixed: https://github.com/Kotlin/kotlinx.coroutines/issues/2435

            messageQueue.close(ex)

            while (true) {
                messageQueue.tryReceive().getOrNull()?.let { msg ->
                    onUndeliveredElement(msg, ex)
                } ?: break
            }
        }
    }

    /**
     * Sends a message to a message queue to be processed by [consumeMessage] in [scope].
     *
     * If [offer] completes successfully, the msg *will* be processed either by
     * consumeMessage or
     * onUndeliveredElement. If [offer] throws an exception, the message may or may not be
     * processed.
     */
    fun offer(msg: T) {
        /**
         * Possible states:
         * 1) remainingMessages = 0
         *   All messages have been consumed, so there is no active consumer
         * 2) remainingMessages > 0, no active consumer
         *   One of the senders is responsible for triggering the consumer
         * 3) remainingMessages > 0, active consumer
         *   Consumer will continue to consume until remainingMessages is 0
         * 4) messageQueue is closed, there are remaining messages to consume
         *   Attempts to offer messages will fail, onComplete() will consume remaining messages
         *   with onUndelivered. The Consumer has already completed since close() is called by
         *   onComplete().
         * 5) messageQueue is closed, there are no remaining messages to consume
         *   Attempts to offer messages will fail.
         */

        // should never return false bc the channel capacity is unlimited
        check(
            messageQueue.trySend(msg)
                .onClosed { throw it ?: ClosedSendChannelException("Channel was closed normally") }
                .isSuccess
        )

        // If the number of remaining messages was 0, there is no active consumer, since it quits
        // consuming once remaining messages hits 0. We must kick off a new consumer.
        if (remainingMessages.getAndIncrement() == 0) {
            scope.launch {
                // We shouldn't have started a new consumer unless there are remaining messages...
                check(remainingMessages.get() > 0)

                do {
                    // We don't want to try to consume a new message unless we are still active.
                    // If ensureActive throws, the scope is no longer active, so it doesn't
                    // matter that we have remaining messages.
                    scope.ensureActive()

                    consumeMessage(messageQueue.receive())
                } while (remainingMessages.decrementAndGet() != 0)
            }
        }
    }
}