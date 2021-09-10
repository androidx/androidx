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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.lifecycle

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

@RunWith(JUnit4::class)
class FlowAsLiveDataTest {
    @get:Rule
    val scopes = ScopesRule()
    private val mainScope = scopes.mainScope
    private val testScope = scopes.testScope

    private fun <T> LiveData<T>.addObserver() = this.addObserver(scopes)

    @Test
    fun oneShot() {
        val liveData = flowOf(3).asLiveData()
        scopes.triggerAllActions()
        assertThat(liveData.value).isNull()
        liveData.addObserver().assertItems(3)
    }

    @Test
    fun removeObserverInBetween() {
        val ld = flow {
            emit(1)
            emit(2)
            delay(1000)
            emit(3)
        }.asLiveData(timeoutInMs = 10)

        ld.addObserver().apply {
            assertItems(1, 2)
            unsubscribe()
        }
        // trigger cancellation
        mainScope.advanceTimeBy(100)
        assertThat(ld.hasActiveObservers()).isFalse()
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(2, 1, 2)
            mainScope.advanceTimeBy(1001)
            assertItems(2, 1, 2, 3)
        }
    }

    @Test
    fun callbackFlow_cancelled() {
        var closeCalled = false
        val ld = callbackFlow {
            testScope.launch {
                trySend(1)
                trySend(2)
                delay(1000)
                trySend(3)
            }
            awaitClose {
                closeCalled = true
            }
        }.asLiveData(timeoutInMs = 10)

        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(1, 2)
            unsubscribe()
        }
        assertThat(closeCalled).isFalse()
        // trigger cancellation
        mainScope.advanceTimeBy(100)
        assertThat(ld.hasActiveObservers()).isFalse()
        assertThat(closeCalled).isTrue()
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(2, 1, 2)
            scopes.advanceTimeBy(1001)
            assertItems(2, 1, 2, 3)
        }
    }

    @Test
    fun removeObserverInBetween_largeTimeout() {
        val ld = flow {
            emit(1)
            emit(2)
            delay(1000)
            emit(3)
        }.asLiveData(timeoutInMs = 10000)

        ld.addObserver().apply {
            assertItems(1, 2)
            unsubscribe()
        }
        // advance some but not enough to cover the delay
        mainScope.advanceTimeBy(500)
        assertThat(ld.hasActiveObservers()).isFalse()
        assertThat(ld.value).isEqualTo(2)
        ld.addObserver().apply {
            assertItems(2)
            // advance enough to cover the rest of the delay
            mainScope.advanceTimeBy(501)
            assertItems(2, 3)
        }
    }

    @Test
    fun timeoutViaDuration() {
        val running = CompletableDeferred<Unit>()
        val ld = flow {
            try {
                emit(1)
                delay(5_001)
                emit(2)
            } finally {
                running.complete(Unit)
            }
        }.asLiveData(timeout = Duration.ofSeconds(5))

        ld.addObserver().apply {
            assertItems(1)
            unsubscribe()
        }
        // advance some but not enough to cover the delay
        mainScope.advanceTimeBy(4_000)
        assertThat(running.isActive).isTrue()
        assertThat(ld.hasActiveObservers()).isFalse()
        assertThat(ld.value).isEqualTo(1)
        // advance time to finish
        mainScope.advanceTimeBy(1_000)
        // ensure it is not running anymore
        assertThat(running.isActive).isFalse()
        assertThat(ld.value).isEqualTo(1)
    }

    @Test
    fun flowThrows() {
        // use an exception handler instead of the test context exception handler to ensure that
        // we do not re-run the block if its exception is gracefully caught
        // TODO should we consider doing that ? But if we do, what is the rule? do we retry when
        // it becomes active again or do we retry ourselves? better no do anything to be consistent.
        val exception = CompletableDeferred<Throwable>()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            exception.complete(throwable)
        }
        val ld = flow {
            if (exception.isActive) {
                throw IllegalArgumentException("i like to fail")
            } else {
                emit(3)
            }
        }.asLiveData(testScope.coroutineContext + exceptionHandler, 10)
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems()
            runBlocking {
                assertThat(exception.await()).hasMessageThat().contains("i like to fail")
            }
            unsubscribe()
        }
        scopes.triggerAllActions()
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems()
        }
    }

    @Test
    fun flowCancelsItself() {
        val didCancel = AtomicBoolean(false)
        val unexpected = AtomicBoolean(false)

        val ld = flow<Int> {
            if (didCancel.compareAndSet(false, true)) {
                coroutineContext.cancel()
            } else {
                unexpected.set(true)
            }
        }.asLiveData(testScope.coroutineContext, 10)
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems()
            unsubscribe()
        }
        assertThat(didCancel.get()).isTrue()
        ld.addObserver()
        // trigger cancelation
        scopes.advanceTimeBy(11)
        assertThat(unexpected.get()).isFalse()
    }

    @Test
    fun multipleValuesAndObservers() {
        val ld = flowOf(3, 4).asLiveData()
        ld.addObserver().assertItems(3, 4)
        // re-observe, get latest value only
        ld.addObserver().assertItems(4)
    }
}