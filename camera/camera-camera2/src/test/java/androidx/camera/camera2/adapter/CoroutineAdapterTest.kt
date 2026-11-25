/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.adapter

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class CoroutineAdapterTest {
    @Test
    fun propagateCompleteResult(): Unit = runBlocking {
        // Arrange.
        val resultValue = 123
        val sourceDeferred = CompletableDeferred<Int>()
        val resultDeferred = CompletableDeferred<Int>()
        sourceDeferred.propagateTo(resultDeferred)

        // Act.
        sourceDeferred.complete(resultValue)

        // Assert.
        assertThat(resultDeferred.await()).isEqualTo(resultValue)
    }

    @Test
    fun propagateTransformedCompleteResult(): Unit = runBlocking {
        // Arrange.
        val resultValue = 123
        val resultValueTransformed = resultValue.toString()

        val sourceDeferred = CompletableDeferred<Int>()
        val resultDeferred = CompletableDeferred<String>()
        sourceDeferred.propagateTo(resultDeferred) { res -> res.toString() }

        // Act.
        sourceDeferred.complete(resultValue)

        // Assert.
        assertThat(resultDeferred.await()).isEqualTo(resultValueTransformed)
    }

    @Test
    fun propagateCancelResult() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred)

        // Act.
        sourceDeferred.cancel()

        // Assert.
        assertThat(resultDeferred.isCancelled).isTrue()
    }

    @Test
    fun propagateCancelResult_whenTransformFunctionIsUsed() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred) { res -> res.toString() }

        // Act.
        sourceDeferred.cancel()

        // Assert.
        assertThat(resultDeferred.isCancelled).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun propagateExceptionResult() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred)
        val testThrowable = Throwable()

        // Act.
        sourceDeferred.completeExceptionally(testThrowable)

        // Assert.
        assertThat(resultDeferred.getCompletionExceptionOrNull()).isSameInstanceAs(testThrowable)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun propagateExceptionResult_whenTransformFunctionIsUsed() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred) { res -> res.toString() }
        val testThrowable = Throwable()

        // Act.
        sourceDeferred.completeExceptionally(testThrowable)

        // Assert.
        assertThat(resultDeferred.getCompletionExceptionOrNull()).isSameInstanceAs(testThrowable)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun future_completesWithResult() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)

        val future = scope.future { 42 }
        testScheduler.advanceUntilIdle()

        assertThat(future.isDone).isTrue()
        assertThat(future.isCancelled).isFalse()
        assertThat(future.get()).isEqualTo(42)
        scope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun future_propagatesExceptionWithoutUncaughtException() = runTest {
        val uncaughtExceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, throwable ->
            uncaughtExceptions.add(throwable)
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(Job() + dispatcher + handler)
        val expected = IllegalStateException("expected failure")

        val future = scope.future<Int> { throw expected }
        testScheduler.advanceUntilIdle()

        assertThat(future.isDone).isTrue()
        assertThat(future.isCancelled).isFalse()
        val thrown = assertThrows(ExecutionException::class.java) { future.get() }
        assertThat(thrown.cause).isSameInstanceAs(expected)
        assertThat(uncaughtExceptions).isEmpty()
        assertThat(scope.isActive).isTrue()
        scope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun future_cancelsWhenScopeAlreadyCancelled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scope.cancel()

        val future = scope.future { 42 }
        testScheduler.advanceUntilIdle()

        assertThat(future.isCancelled).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun future_cancelsWhenScopeCancelledDuringSuspension() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val started = CompletableDeferred<Unit>()

        val future =
            scope.future<Int> {
                started.complete(Unit)
                awaitCancellation()
            }
        testScheduler.advanceUntilIdle()
        assertThat(started.isCompleted).isTrue()

        scope.cancel()
        testScheduler.advanceUntilIdle()

        assertThat(future.isCancelled).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun future_cancellingReturnedFutureDoesNotCancelUnderlyingCoroutine() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val gate = CompletableDeferred<Unit>()
        var sideEffectCompleted = false

        val future = scope.future {
            gate.await()
            sideEffectCompleted = true
        }
        testScheduler.advanceUntilIdle()

        future.cancel(true)
        assertThat(future.isCancelled).isTrue()

        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertThat(sideEffectCompleted).isTrue()
        scope.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun future_cancelsWhenScopeCancelledBeforeDispatch() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)

        val future = scope.future { 42 }
        scope.cancel()
        testScheduler.advanceUntilIdle()

        assertThat(future.isCancelled).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun future_cancelsJobAndReturnedFutureWhenBlockThrowsCancellationException() = runTest {
        val uncaughtExceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, throwable ->
            uncaughtExceptions.add(throwable)
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(Job() + dispatcher + handler)
        lateinit var launchedJob: Job

        val future =
            scope.future<Int> {
                launchedJob = currentCoroutineContext().job
                throw CancellationException("cancelled inside block")
            }
        testScheduler.advanceUntilIdle()

        assertThat(future.isCancelled).isTrue()
        assertThat(launchedJob.isCancelled).isTrue()
        assertThat(uncaughtExceptions).isEmpty()
        assertThat(scope.isActive).isTrue()
        scope.cancel()
    }
}
