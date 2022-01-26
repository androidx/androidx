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

import androidx.annotation.RequiresApi
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

 @RunWith(JUnit4::class)
class BuildLiveDataTest {
    @get:Rule
    val scopes = ScopesRule()
    private val mainScope = scopes.mainScope
    private val testScope = scopes.testScope

    @Test
    fun oneShot() {
        val liveData = liveData {
            emit(3)
        }
        scopes.triggerAllActions()
        assertThat(liveData.value).isNull()
        liveData.addObserver().assertItems(3)
    }

    @Test
    fun removeObserverInBetween() {
        val ld = liveData(timeoutInMs = 10) {
            emit(1)
            emit(2)
            delay(1000)
            emit(3)
        }
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
    fun removeObserverInBetween_largeTimeout() {
        val ld = liveData(timeoutInMs = 10000) {
            emit(1)
            emit(2)
            delay(1000)
            emit(3)
        }
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

    @RequiresApi(26)
    @Test
    fun timeoutViaDuration() {
        val running = CompletableDeferred<Unit>()
        val ld = liveData(timeout = Duration.ofSeconds(5)) {
            try {
                emit(1)
                delay(5_001)
                emit(2)
            } finally {
                running.complete(Unit)
            }
        }
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
    fun ignoreCancelledYields() {
        val cancelMutex = Mutex(true)
        val ld = liveData(timeoutInMs = 0, context = testScope.coroutineContext) {
            emit(1)
            cancelMutex.withLock {
                emit(2)
            }
        }
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(1)
            unsubscribe()
            cancelMutex.unlock()
        }
        // let cancellation take place
        scopes.triggerAllActions()
        // emit should immediately trigger cancellation to happen
        assertThat(ld.value).isEqualTo(1)
        assertThat(ld.hasActiveObservers()).isFalse()
        // now because it was cancelled, re-observing should dispatch 1,1,2
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(1, 1, 2)
        }
    }

    @Test
    fun readLatestValue() {
        val latest = AtomicReference<Int?>()
        val ld = liveData<Int>(testScope.coroutineContext) {
            latest.set(latestValue)
        }
        scopes.runOnMain {
            ld.value = 3
        }
        ld.addObserver()
        scopes.triggerAllActions()
        assertThat(latest.get()).isEqualTo(3)
    }

    @Test
    fun readLatestValue_readWithinBlock() {
        val latest = AtomicReference<Int?>()
        val ld = liveData<Int>(testScope.coroutineContext) {
            emit(5)
            latest.set(latestValue)
        }
        ld.addObserver()
        scopes.triggerAllActions()
        assertThat(latest.get()).isEqualTo(5)
    }

    @Test
    fun readLatestValue_keepYieldedFromBefore() {
        val latest = AtomicReference<Int?>()
        val ld = liveData<Int>(testScope.coroutineContext, 10) {
            if (latestValue == null) {
                emit(5)
                delay(500000) // wait for cancellation
            }

            latest.set(latestValue)
        }
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(5)
            unsubscribe()
        }
        scopes.triggerAllActions()
        // wait for it to be cancelled
        scopes.advanceTimeBy(10)
        assertThat(latest.get()).isNull()
        ld.addObserver()
        scopes.triggerAllActions()
        assertThat(latest.get()).isEqualTo(5)
    }

    @Test
    fun yieldSource_simple() {
        val odds = liveData {
            (1..9 step 2).forEach {
                emit(it)
            }
        }
        val ld = liveData {
            emitSource(odds)
        }
        ld.addObserver().apply {
            assertItems(1, 3, 5, 7, 9)
        }
    }

    @Test
    fun yieldSource_switchTwo() {
        val doneOddsYield = Mutex(true)
        val odds = liveData {
            (1..9 step 2).forEach {
                emit(it)
            }
            doneOddsYield.unlock()
            delay(1)
            emit(-1)
        }
        val evens = liveData {
            (2..10 step 2).forEach {
                emit(it)
            }
        }
        val ld = liveData(testScope.coroutineContext) {
            emitSource(odds)
            doneOddsYield.lock()
            emitSource(evens)
        }
        ld.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(1, 3, 5, 7, 9, 2, 4, 6, 8, 10)
        }
    }

    @Test
    fun yieldSource_yieldValue() {
        val doneOddsYield = Mutex(true)
        val odds = liveData(timeoutInMs = 0) {
            (1..9 step 2).forEach {
                emit(it)
            }
            doneOddsYield.unlock()
            delay(1)
            emit(-1)
        }
        val ld = liveData(testScope.coroutineContext) {
            emitSource(odds)
            doneOddsYield.lock()
            emit(10)
        }
        ld.addObserver().apply {
            scopes.triggerAllActions()
            scopes.advanceTimeBy(100)
            assertItems(1, 3, 5, 7, 9, 10)
        }
    }

    @Test
    fun yieldSource_dispose() {
        val doneOddsYield = Mutex(true)
        val odds = liveData {
            (1..9 step 2).forEach {
                emit(it)
            }
            doneOddsYield.lock()
            emit(2)
        }
        val ld = liveData {
            val disposable = emitSource(odds)
            scopes.triggerAllActions()
            assertThat(odds.hasActiveObservers()).isEqualTo(true)
            disposable.dispose()
            scopes.triggerAllActions()
            assertThat(odds.hasActiveObservers()).isEqualTo(false)
            doneOddsYield.unlock()
        }
        ld.addObserver().apply {
            assertItems(1, 3, 5, 7, 9)
        }
    }

    @Test
    fun yieldSource_disposeTwice() {
        val odds = liveData {
            (1..9 step 2).forEach {
                emit(it)
            }
        }
        val ld = liveData {
            val disposable = emitSource(odds)
            scopes.triggerAllActions()
            disposable.dispose()
            scopes.triggerAllActions()
            assertThat(odds.hasActiveObservers()).isEqualTo(false)
            // add observer via side channel.
            (this as LiveDataScopeImpl<Int>).target.addSource(odds) {}
            scopes.triggerAllActions()
            // redispose previous one should not impact
            disposable.dispose()
            scopes.triggerAllActions()
            // still has the observer we added from the side channel
            assertThat(odds.hasActiveObservers()).isEqualTo(true)
        }
        ld.addObserver().apply {
            assertItems(1, 3, 5, 7, 9)
        }
    }

    @Test
    fun blockThrows() {
        // use an exception handler instead of the test context exception handler to ensure that
        // we do not re-run the block if its exception is gracefully caught
        // TODO should we consider doing that ? But if we do, what is the rule? do we retry when
        // it becomes active again or do we retry ourselves? better no do anything to be consistent.
        val exception = CompletableDeferred<Throwable>()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            exception.complete(throwable)
        }
        val ld = liveData(testScope.coroutineContext + exceptionHandler, 10) {
            if (exception.isActive) {
                throw IllegalArgumentException("i like to fail")
            } else {
                emit(3)
            }
        }
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
    fun blockCancelsItself() {
        val didCancel = AtomicBoolean(false)
        val unexpected = AtomicBoolean(false)

        val ld = liveData<Int>(testScope.coroutineContext, 10) {
            if (didCancel.compareAndSet(false, true)) {
                coroutineContext.cancel()
            } else {
                unexpected.set(true)
            }
        }
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
    fun blockThrows_switchMap() {
        val exception = CompletableDeferred<Throwable>()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            exception.complete(throwable)
        }
        val src = MutableLiveData<Int>()
        val ld = src.switchMap {
            liveData(testScope.coroutineContext + exceptionHandler) {
                if (exception.isActive) {
                    throw IllegalArgumentException("i like to fail")
                } else {
                    emit(3)
                }
            }
        }
        ld.addObserver().apply {
            assertItems()
            scopes.runOnMain {
                src.value = 1
            }
            scopes.triggerAllActions()
            runBlocking {
                assertThat(exception.await()).hasMessageThat().contains("i like to fail")
            }
            scopes.runOnMain {
                src.value = 2
            }
            scopes.triggerAllActions()
            assertItems(3)
        }
    }

    @Test
    fun multipleValuesAndObservers() {
        val ld = liveData {
            emit(3)
            emit(4)
        }
        ld.addObserver().assertItems(3, 4)
        // re-observe, get latest value only
        ld.addObserver().assertItems(4)
    }

    @Test
    fun raceTest() {
        val subLiveData = MutableLiveData(1)
        val subject = liveData(testScope.coroutineContext) {
            emitSource(subLiveData)
            emitSource(subLiveData)
            emit(2)
        }
        subject.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(1, 1, 2)
            subLiveData.value = 3
            scopes.triggerAllActions()
            // we do not expect 3 because it is disposed
            assertItems(1, 1, 2)
        }
    }

    @Test
    fun raceTest_withCustomDispose() {
        val subLiveData = MutableLiveData(1)
        val subject = liveData(testScope.coroutineContext) {
            emitSource(subLiveData).dispose()
            emitSource(subLiveData)
        }
        subject.addObserver().apply {
            scopes.triggerAllActions()
            assertItems(1, 1)
            subLiveData.value = 3
            scopes.triggerAllActions()
            assertItems(1, 1, 3)
        }
    }

    private fun <T> LiveData<T>.addObserver() = this.addObserver(scopes)
}