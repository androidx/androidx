/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals

import android.content.Context
import android.os.CancellationSignal
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AgeSignalManagerTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val testRequest = GetAgeRangeRequest()
    private val expectedResponse =
        GetAgeRangeResponse(
            lowerAgeBound = 13,
            upperAgeBound = 15,
            assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_B,
        )

    private lateinit var factory: AgeSignalProviderFactory

    @Before
    fun setUp() {
        factory = AgeSignalProviderFactory(context)
        factory.testMode = true
    }

    @Test
    fun create_returnsNonNullInstance() {
        val manager = AgeSignalManager.create(context)
        assertThat(manager).isNotNull()
    }

    @Test
    fun getAgeRange_coroutine_success() = runBlocking {
        val fakeProvider =
            FakeAgeSignalProvider(context, isAvailableResponse = true, response = expectedResponse)
        factory.testProvider = fakeProvider
        val manager = AgeSignalManagerImpl(factory)

        val actualResponse = manager.getAgeRange(context, testRequest)

        assertThat(actualResponse).isEqualTo(expectedResponse)
        assertThat(fakeProvider.lastReceivedRequest).isEqualTo(testRequest)
        assertThat(fakeProvider.lastReceivedContext).isSameInstanceAs(context)
    }

    @Test
    fun getAgeRange_coroutine_propagatesProviderError() {
        val expectedError = GetAgeRangeUnknownException("Internal error")
        val fakeProvider =
            FakeAgeSignalProvider(context, isAvailableResponse = true, exception = expectedError)
        factory.testProvider = fakeProvider
        val manager = AgeSignalManagerImpl(factory)

        val thrown =
            assertThrows(GetAgeRangeUnknownException::class.java) {
                runBlocking { manager.getAgeRange(context, testRequest) }
            }

        assertThat(thrown.message).isEqualTo("Internal error")
    }

    @Test
    fun getAgeRange_coroutine_noProvider_throwsConfigurationException() {
        factory.testProvider = null
        val manager = AgeSignalManagerImpl(factory)

        assertThrows(GetAgeRangeProviderConfigurationException::class.java) {
            runBlocking { manager.getAgeRange(context, testRequest) }
        }
    }

    @Test
    fun getAgeRange_coroutine_cancellationPropagatesToSignal() = runBlocking {
        val signalReceived = AtomicReference<CancellationSignal>()
        val unblockFake = CountDownLatch(1)

        val fakeProvider =
            object : AgeSignalProvider {
                override fun onGetAgeRange(
                    context: Context,
                    request: GetAgeRangeRequest,
                    cancellationSignal: CancellationSignal?,
                    executor: Executor,
                    callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
                ) {
                    signalReceived.set(cancellationSignal)
                    unblockFake.countDown()
                }

                override fun isAvailable(): Boolean = true
            }

        factory.testProvider = fakeProvider
        val manager = AgeSignalManagerImpl(factory)

        val job =
            launch(start = CoroutineStart.UNDISPATCHED) {
                manager.getAgeRange(context, testRequest)
            }

        assertThat(unblockFake.await(1, TimeUnit.SECONDS)).isTrue()
        job.cancel()

        assertThat(signalReceived.get()?.isCanceled).isTrue()
    }

    @Test
    fun getAgeRangeAsync_successCallbackDeliveredOnExecutor() {
        val fakeProvider =
            FakeAgeSignalProvider(context, isAvailableResponse = true, response = expectedResponse)
        factory.testProvider = fakeProvider
        val manager = AgeSignalManagerImpl(factory)

        val latch = CountDownLatch(1)
        val executedOnCustomThread = AtomicBoolean(false)
        val receivedResponse = AtomicReference<GetAgeRangeResponse>()

        val customExecutor = Executor { command ->
            executedOnCustomThread.set(true)
            command.run()
        }

        manager.getAgeRangeAsync(
            context,
            testRequest,
            null,
            customExecutor,
            object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                override fun onResult(result: GetAgeRangeResponse) {
                    receivedResponse.set(result)
                    latch.countDown()
                }

                override fun onError(error: GetAgeRangeException) {
                    latch.countDown()
                }
            },
        )

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(executedOnCustomThread.get()).isTrue()
        assertThat(receivedResponse.get()).isEqualTo(expectedResponse)
    }

    @Test
    fun getAgeRangeAsync_noProvider_dispatchesConfigurationExceptionOnExecutor() {
        factory.testProvider = null
        val manager = AgeSignalManagerImpl(factory)

        val latch = CountDownLatch(1)
        val executedOnCustomThread = AtomicBoolean(false)
        val receivedError = AtomicReference<GetAgeRangeException>()

        val customExecutor = Executor { command ->
            executedOnCustomThread.set(true)
            command.run()
        }

        manager.getAgeRangeAsync(
            context,
            testRequest,
            null,
            customExecutor,
            object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                override fun onResult(result: GetAgeRangeResponse) {
                    latch.countDown()
                }

                override fun onError(error: GetAgeRangeException) {
                    receivedError.set(error)
                    latch.countDown()
                }
            },
        )

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(executedOnCustomThread.get()).isTrue()
        assertThat(receivedError.get())
            .isInstanceOf(GetAgeRangeProviderConfigurationException::class.java)
    }

    @Test
    fun getAgeRangeAsync_providerError_dispatchesErrorOnExecutor() {
        val expectedError = GetAgeRangeUnknownException("Custom provider error")
        val fakeProvider =
            FakeAgeSignalProvider(context, isAvailableResponse = true, exception = expectedError)
        factory.testProvider = fakeProvider
        val manager = AgeSignalManagerImpl(factory)

        val latch = CountDownLatch(1)
        val executedOnCustomThread = AtomicBoolean(false)
        val receivedError = AtomicReference<GetAgeRangeException>()

        val customExecutor = Executor { command ->
            executedOnCustomThread.set(true)
            command.run()
        }

        manager.getAgeRangeAsync(
            context,
            testRequest,
            null,
            customExecutor,
            object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                override fun onResult(result: GetAgeRangeResponse) {
                    latch.countDown()
                }

                override fun onError(error: GetAgeRangeException) {
                    receivedError.set(error)
                    latch.countDown()
                }
            },
        )

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(executedOnCustomThread.get()).isTrue()
        assertThat(receivedError.get()).isEqualTo(expectedError)
    }
}
