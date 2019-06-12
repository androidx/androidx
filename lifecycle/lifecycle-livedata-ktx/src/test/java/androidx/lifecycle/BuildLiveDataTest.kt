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

package androidx.lifecycle

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestCoroutineContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

@ObsoleteCoroutinesApi
@RunWith(JUnit4::class)
class BuildLiveDataTest {
    @ObsoleteCoroutinesApi
    private val mainContext = TestCoroutineContext("test-main-context")
    private val mainScope = CoroutineScope(mainContext)
    @ObsoleteCoroutinesApi
    private val testContext = TestCoroutineContext("test-other-context")

    @ExperimentalCoroutinesApi
    @Before
    fun initMain() {
        lateinit var mainThread: Thread
        runBlocking(mainContext) {
            mainThread = Thread.currentThread()
        }
        Dispatchers.setMain(
            mainContext[ContinuationInterceptor.Key] as CoroutineDispatcher
        )
        ArchTaskExecutor.getInstance().setDelegate(
            object : TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) {
                    error("unsupported")
                }

                override fun postToMainThread(runnable: Runnable) {
                    mainScope.launch {
                        runnable.run()
                    }
                }

                override fun isMainThread(): Boolean {
                    return mainThread == Thread.currentThread()
                }
            }
        )
    }

    @ExperimentalCoroutinesApi
    @After
    fun clear() {
        advanceTimeBy(100000)
        mainContext.assertExceptions("shouldn't have any exceptions") {
            it.isEmpty()
        }
        testContext.assertExceptions("shouldn't have any exceptions") {
            it.isEmpty()
        }
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    @Test
    fun oneShot() {
        val liveData = liveData {
            emit(3)
        }
        triggerAllActions()
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
        mainContext.advanceTimeBy(100)
        assertThat(ld.hasActiveObservers()).isFalse()
        ld.addObserver().apply {
            triggerAllActions()
            assertItems(2, 1, 2)
            mainContext.advanceTimeBy(1001)
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
        mainContext.advanceTimeBy(500)
        assertThat(ld.hasActiveObservers()).isFalse()
        assertThat(ld.value).isEqualTo(2)
        ld.addObserver().apply {
            assertItems(2)
            // advance enough to cover the rest of the delay
            mainContext.advanceTimeBy(501)
            assertItems(2, 3)
        }
    }

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
        mainContext.advanceTimeBy(4_000)
        assertThat(running.isActive).isTrue()
        assertThat(ld.hasActiveObservers()).isFalse()
        assertThat(ld.value).isEqualTo(1)
        // advance time to finish
        mainContext.advanceTimeBy(1_000)
        // ensure it is not running anymore
        assertThat(running.isActive).isFalse()
        assertThat(ld.value).isEqualTo(1)
    }

    @Test
    fun ignoreCancelledYields() {
        val cancelMutex = Mutex(true)
        val ld = liveData(timeoutInMs = 0, context = testContext) {
            emit(1)
            cancelMutex.withLock {
                emit(2)
            }
        }
        ld.addObserver().apply {
            triggerAllActions()
            assertItems(1)
            unsubscribe()
            cancelMutex.unlock()
        }
        // let cancellation take place
        triggerAllActions()
        // emit should immediately trigger cancellation to happen
        assertThat(ld.value).isEqualTo(1)
        assertThat(ld.hasActiveObservers()).isFalse()
        // now because it was cancelled, re-observing should dispatch 1,1,2
        ld.addObserver().apply {
            triggerAllActions()
            assertItems(1, 1, 2)
        }
    }

    @Test
    fun readLatestValue() {
        val latest = AtomicReference<Int?>()
        val ld = liveData<Int>(testContext) {
            latest.set(latestValue)
        }
        runOnMain {
            ld.value = 3
        }
        ld.addObserver()
        triggerAllActions()
        assertThat(latest.get()).isEqualTo(3)
    }

    @Test
    fun readLatestValue_readWithinBlock() {
        val latest = AtomicReference<Int?>()
        val ld = liveData<Int>(testContext) {
            emit(5)
            latest.set(latestValue)
        }
        ld.addObserver()
        triggerAllActions()
        assertThat(latest.get()).isEqualTo(5)
    }

    @Test
    fun readLatestValue_keepYieldedFromBefore() {
        val latest = AtomicReference<Int?>()
        val ld = liveData<Int>(testContext, 10) {
            if (latestValue == null) {
                emit(5)
                delay(500000) // wait for cancellation
            }

            latest.set(latestValue)
        }
        ld.addObserver().apply {
            triggerAllActions()
            assertItems(5)
            unsubscribe()
        }
        triggerAllActions()
        // wait for it to be cancelled
        advanceTimeBy(10)
        assertThat(latest.get()).isNull()
        ld.addObserver()
        triggerAllActions()
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
        val ld = liveData(testContext) {
            emitSource(odds)
            doneOddsYield.lock()
            emitSource(evens)
        }
        ld.addObserver().apply {
            triggerAllActions()
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
        val ld = liveData(testContext) {
            emitSource(odds)
            doneOddsYield.lock()
            emit(10)
        }
        ld.addObserver().apply {
            triggerAllActions()
            advanceTimeBy(100)
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
            triggerAllActions()
            assertThat(odds.hasActiveObservers()).isEqualTo(true)
            disposable.dispose()
            triggerAllActions()
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
            triggerAllActions()
            disposable.dispose()
            triggerAllActions()
            assertThat(odds.hasActiveObservers()).isEqualTo(false)
            // add observer via side channel.
            (this as LiveDataScopeImpl<Int>).target.addSource(odds) {}
            triggerAllActions()
            // redispose previous one should not impact
            disposable.dispose()
            triggerAllActions()
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
        val ld = liveData(testContext + exceptionHandler, 10) {
            if (exception.isActive) {
                throw IllegalArgumentException("i like to fail")
            } else {
                emit(3)
            }
        }
        ld.addObserver().apply {
            triggerAllActions()
            assertItems()
            runBlocking {
                assertThat(exception.await()).hasMessageThat().contains("i like to fail")
            }
            unsubscribe()
        }
        triggerAllActions()
        ld.addObserver().apply {
            triggerAllActions()
            assertItems()
        }
    }

    @Test
    fun blockCancelsItself() {
        val didCancel = AtomicBoolean(false)
        val unexpected = AtomicBoolean(false)

        val ld = liveData<Int>(testContext, 10) {
            if (didCancel.compareAndSet(false, true)) {
                coroutineContext.cancel()
            } else {
                unexpected.set(true)
            }
        }
        ld.addObserver().apply {
            triggerAllActions()
            assertItems()
            unsubscribe()
        }
        assertThat(didCancel.get()).isTrue()
        ld.addObserver()
        // trigger cancelation
        advanceTimeBy(11)
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
            liveData(testContext + exceptionHandler) {
                if (exception.isActive) {
                    throw IllegalArgumentException("i like to fail")
                } else {
                    emit(3)
                }
            }
        }
        ld.addObserver().apply {
            assertItems()
            runOnMain {
                src.value = 1
            }
            triggerAllActions()
            runBlocking {
                assertThat(exception.await()).hasMessageThat().contains("i like to fail")
            }
            runOnMain {
                src.value = 2
            }
            triggerAllActions()
            assertItems(3)
        }
    }

    private fun triggerAllActions() {
        do {
            mainContext.triggerActions()
            testContext.triggerActions()
            val allIdle = listOf(mainContext, testContext).all {
                it.isIdle()
            }
        } while (!allIdle)
    }

    private fun advanceTimeBy(time: Long) {
        mainContext.advanceTimeBy(time)
        testContext.advanceTimeBy(time)
        triggerAllActions()
    }

    private fun TestCoroutineContext.isIdle(): Boolean {
        val queueField = this::class.java
            .getDeclaredField("queue")
        queueField.isAccessible = true
        val queue = queueField.get(this)
        val peekMethod = queue::class.java
            .getDeclaredMethod("peek")
        val nextTask = peekMethod.invoke(queue) ?: return true
        val timeField = nextTask::class.java.getDeclaredField("time")
        timeField.isAccessible = true
        val time = timeField.getLong(nextTask)
        return time > now()
    }

    private fun <T> runOnMain(block: () -> T): T {
        return runBlocking {
            val async = mainScope.async {
                block()
            }
            mainContext.triggerActions()
            async.await()
        }
    }

    private fun <T> LiveData<T>.addObserver(): CollectingObserver<T> {
        return runOnMain {
            val observer = CollectingObserver(this)
            observeForever(observer)
            observer
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

    inner class CollectingObserver<T>(
        private val liveData: LiveData<T>
    ) : Observer<T> {
        private var items = mutableListOf<T>()
        override fun onChanged(t: T) {
            items.add(t)
        }

        fun assertItems(vararg expected: T) {
            assertThat(items).containsExactly(*expected)
        }

        fun unsubscribe() = runOnMain {
            liveData.removeObserver(this)
        }
    }
}