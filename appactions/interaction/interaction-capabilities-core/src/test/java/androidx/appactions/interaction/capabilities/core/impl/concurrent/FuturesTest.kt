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

package androidx.appactions.interaction.capabilities.core.impl.concurrent

import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.CB_TIMEOUT
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.awaitSync
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FuturesTest {
    @Test
    fun addCallback_onSuccess() {
        lateinit var integerCompleter: Completer<Int>
        val integerFuture = CallbackToFutureAdapter.getFuture<Int> {
            integerCompleter = it
            "integerFuture"
        }
        val calbackSuccessDeferred = CompletableDeferred<Boolean>()
        Futures.addCallback(
            integerFuture,
            object : FutureCallback<Int> {
                override fun onSuccess(result: Int) {
                    calbackSuccessDeferred.complete(true)
                }

                override fun onFailure(t: Throwable) {
                    calbackSuccessDeferred.complete(false)
                }
            },
            Runnable::run,
        )

        assertThat(calbackSuccessDeferred.isCompleted).isFalse()

        integerCompleter.set(25)

        assertThat(calbackSuccessDeferred.awaitSync()).isTrue()
    }

    @Test
    fun addCallback_onFailure() {
        lateinit var integerCompleter: Completer<Int>
        val integerFuture = CallbackToFutureAdapter.getFuture<Int> {
            integerCompleter = it
            "integerFuture"
        }
        val calbackSuccessDeferred = CompletableDeferred<Boolean>()
        Futures.addCallback(
            integerFuture,
            object : FutureCallback<Int> {
                override fun onSuccess(result: Int) {
                    calbackSuccessDeferred.complete(true)
                }

                override fun onFailure(t: Throwable) {
                    calbackSuccessDeferred.complete(false)
                }
            },
            Runnable::run,
        )

        assertThat(calbackSuccessDeferred.isCompleted).isFalse()

        integerCompleter.setException(IllegalStateException())

        assertThat(calbackSuccessDeferred.awaitSync()).isFalse()
    }

    @Test
    fun transform_success() {
        lateinit var integerCompleter: Completer<Int>
        val integerFuture = CallbackToFutureAdapter.getFuture<Int> {
            integerCompleter = it
            "integerFuture"
        }
        val transformedFuture = Futures.transform(
            integerFuture,
            { it + 10 },
            Runnable::run,
            "add 10",
        )
        integerCompleter.set(25)
        assertThat(transformedFuture.get(CB_TIMEOUT, MILLISECONDS)).isEqualTo(35)
    }

    @Test
    fun transformAsync_success() {
        lateinit var integerCompleter: Completer<Int>
        val integerFuture = CallbackToFutureAdapter.getFuture<Int> {
            integerCompleter = it
            "integerFuture"
        }
        val transformedFuture = Futures.transformAsync(
            integerFuture,
            { Futures.immediateFuture(it + 10) },
            Runnable::run,
            "add 10",
        )
        integerCompleter.set(25)
        assertThat(transformedFuture.get(CB_TIMEOUT, MILLISECONDS)).isEqualTo(35)
    }

    @Test
    fun immediateFuture_success() {
        val immediateFuture = Futures.immediateFuture(25)
        immediateFuture.cancel(true)
        assertThat(immediateFuture.isCancelled()).isFalse()
        assertThat(immediateFuture.isDone()).isTrue()
        assertThat(immediateFuture.get(CB_TIMEOUT, MILLISECONDS)).isEqualTo(25)
    }

    @Test
    fun immediateVoidFuture_success() {
        val immediateVoidFuture = Futures.immediateVoidFuture()
        immediateVoidFuture.cancel(true)
        assertThat(immediateVoidFuture.isCancelled()).isFalse()
        assertThat(immediateVoidFuture.isDone()).isTrue()
        assertThat(immediateVoidFuture.get(CB_TIMEOUT, MILLISECONDS)).isNull()
    }

    @Test
    fun immediateFailedFuture_failure() {
        val immediateFailedFuture =
            Futures.immediateFailedFuture<Any>(CustomException())
        val transformedFuture =
            Futures.transform(
                immediateFailedFuture,
                { it },
                Runnable::run,
                "test",
            )

        assertThat(transformedFuture.isDone()).isTrue()
        val e = assertThrows(ExecutionException::class.java, transformedFuture::get)
        assertThat(e).hasCauseThat().isInstanceOf(CustomException::class.java)
    }

    @Test
    fun transform_synchronousExceptionPropagated() {
        val transformedFuture =
            Futures.transform(
                Futures.immediateFuture(25),
                { throw IllegalStateException() },
                Runnable::run,
                "badTransform",
            )
        assertThat(transformedFuture.isDone()).isTrue()

        val errorDeferred = CompletableDeferred<Throwable>()
        Futures.addCallback(
            transformedFuture,
            object : FutureCallback<Int> {
                override fun onSuccess(result: Int) {}

                override fun onFailure(t: Throwable) {
                    errorDeferred.complete(t)
                }
            },
            Runnable::run,
        )
        assertThat(errorDeferred.awaitSync()).isInstanceOf(
            IllegalStateException::class.java,
        )
        val e = assertThrows(ExecutionException::class.java, transformedFuture::get)
        assertThat(e).hasCauseThat().isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun transformAsync_synchronousExceptionPropagated() {
        val transformedFuture =
            Futures.transformAsync<Int, Int>(
                Futures.immediateFuture(25),
                { throw IllegalStateException() },
                Runnable::run,
                "badTransform",
            )
        assertThat(transformedFuture.isDone()).isTrue()

        val errorDeferred = CompletableDeferred<Throwable>()
        Futures.addCallback(
            transformedFuture,
            object : FutureCallback<Int> {
                override fun onSuccess(result: Int) {}

                override fun onFailure(t: Throwable) {
                    errorDeferred.complete(t)
                }
            },
            Runnable::run,
        )
        assertThat(errorDeferred.awaitSync()).isInstanceOf(
            IllegalStateException::class.java,
        )
        val e = assertThrows(ExecutionException::class.java, transformedFuture::get)
        assertThat(e).hasCauseThat().isInstanceOf(IllegalStateException::class.java)
    }

    class CustomException : Exception()
}
