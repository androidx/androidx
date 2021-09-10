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

import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
class SimpleActorTest {
    @get:Rule
    val timeout = Timeout(10, TimeUnit.SECONDS)

    @Test
    fun testSimpleActor() = runBlockingTest {
        val msgs = mutableListOf<Int>()

        val actor = SimpleActor<Int>(
            this,
            onComplete = {},
            onUndeliveredElement = { _, _ -> }
        ) {
            msgs.add(it)
        }

        actor.offer(1)
        actor.offer(2)
        actor.offer(3)
        actor.offer(4)

        assertThat(msgs).isEqualTo(listOf(1, 2, 3, 4))
    }

    @Test
    fun testOnCompleteIsCalledWhenScopeIsCancelled() = runBlocking<Unit> {
        val scope = CoroutineScope(Job())
        val called = AtomicBoolean(false)

        val actor = SimpleActor<Int>(
            scope,
            onComplete = {
                assertThat(called.compareAndSet(false, true)).isTrue()
            },
            onUndeliveredElement = { _, _ -> }
        ) {
            // do nothing
        }

        actor.offer(123)

        scope.coroutineContext.job.cancelAndJoin()

        assertThat(called.get()).isTrue()
    }

    @Test
    fun testManyConcurrentCalls() = runBlocking<Unit> {
        val scope = CoroutineScope(Job() + Executors.newFixedThreadPool(4).asCoroutineDispatcher())
        val numCalls = 100000
        val volatileIntHolder = VolatileIntHolder()

        val latch = CountDownLatch(numCalls)
        val actor = SimpleActor<Int>(
            scope,
            onComplete = {},
            onUndeliveredElement = { _, _ -> }
        ) {
            val newValue = volatileIntHolder.int + 1
            // This should be safe because there shouldn't be any concurrent calls
            volatileIntHolder.int = newValue
            latch.countDown()
        }

        repeat(numCalls) {
            scope.launch {
                actor.offer(it)
            }
        }

        latch.await(5, TimeUnit.SECONDS)

        assertThat(volatileIntHolder.int).isEqualTo(numCalls)
    }

    @Test
    fun testManyConcurrentCalls_withDelayBeforeSettingValue() = runBlocking<Unit> {
        val scope = CoroutineScope(Job() + Executors.newFixedThreadPool(4).asCoroutineDispatcher())
        val numCalls = 500
        val volatileIntHolder = VolatileIntHolder()

        val latch = CountDownLatch(numCalls)
        val actor = SimpleActor<Int>(
            scope,
            onComplete = {},
            onUndeliveredElement = { _, _ -> }
        ) {
            val newValue = volatileIntHolder.int + 1
            delay(1)
            // This should be safe because there shouldn't be any concurrent calls
            volatileIntHolder.int = newValue
            latch.countDown()
        }

        repeat(numCalls) {
            scope.launch {
                actor.offer(it)
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(volatileIntHolder.int).isEqualTo(numCalls)
    }

    @Test
    fun testMessagesAreConsumedInProvidedScope() = runBlocking {
        val scope = CoroutineScope(TestElement("test123"))
        val latch = CompletableDeferred<Unit>()

        val actor = SimpleActor<Int>(
            scope,
            onComplete = {},
            onUndeliveredElement = { _, _ -> }
        ) {
            assertThat(getTestElement().name).isEqualTo("test123")
            latch.complete(Unit)
        }

        actor.offer(123)

        latch.await()
    }

    @Test
    fun testOnUndeliveredElementsCallback() = runBlocking<Unit> {
        val scope = CoroutineScope(Job())

        val actor = SimpleActor<CompletableDeferred<Unit>>(
            scope,
            onComplete = {},
            onUndeliveredElement = { msg, ex ->
                msg.completeExceptionally(ex!!)
            }
        ) {
            awaitCancellation()
        }

        actor.offer(CompletableDeferred()) // first one won't be completed...

        val deferreds = mutableListOf<CompletableDeferred<Unit>>()

        repeat(100) {
            CompletableDeferred<Unit>().also {
                deferreds.add(it)
                actor.offer(it)
            }
        }

        scope.coroutineContext.job.cancelAndJoin()

        deferreds.forEach {
            assertThrows<CancellationException> { it.await() }
        }
    }

    @Test
    fun testAllMessagesAreConsumedIfOfferSucceeds() = runBlocking<Unit> {
        val actorScope = CoroutineScope(Job())

        val actor = SimpleActor<CompletableDeferred<Unit>>(
            actorScope,
            onComplete = {},
            onUndeliveredElement = { msg, _ -> msg.complete(Unit) }
        ) {
            try {
                awaitCancellation()
            } finally {
                it.complete(Unit)
            }
        }

        val senderScope =
            CoroutineScope(Job() + Executors.newFixedThreadPool(4).asCoroutineDispatcher())

        val sender = senderScope.async {
            repeat(500) {
                launch {
                    try {
                        val msg = CompletableDeferred<Unit>()
                        // If `offer` doesn't throw CancellationException, the msg must be processed.
                        actor.offer(msg)
                        msg.await() // This must complete even though we've completed.
                    } catch (canceled: CancellationException) {
                        // This is OK.
                    }
                }
            }
        }

        actorScope.coroutineContext.job.cancelAndJoin()
        sender.await()
    }

    @Test
    fun testAllMessagesAreRespondedTo() = runBlocking<Unit> {
        val myScope =
            CoroutineScope(Job() + Executors.newFixedThreadPool(4).asCoroutineDispatcher())

        val actorScope = CoroutineScope(Job())
        val actor = SimpleActor<CompletableDeferred<Unit?>>(
            actorScope,
            onComplete = {},
            onUndeliveredElement = { msg, _ ->
                msg.complete(null)
            }
        ) {
            it.complete(Unit)
        }

        val waiters = myScope.async {
            repeat(100_000) { _ ->
                launch {
                    try {
                        CompletableDeferred<Unit?>().also {
                            actor.offer(it)
                        }.await()
                    } catch (cancelled: CancellationException) {
                        // This is OK
                    }
                }
            }
        }

        delay(100)
        actorScope.coroutineContext.job.cancelAndJoin()
        waiters.await()
    }

    class TestElement(val name: String) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<TestElement>
    }

    private suspend fun getTestElement(): TestElement {
        return coroutineContext[TestElement]!!
    }

    private class VolatileIntHolder {
        @Volatile
        var int = 0
    }
}