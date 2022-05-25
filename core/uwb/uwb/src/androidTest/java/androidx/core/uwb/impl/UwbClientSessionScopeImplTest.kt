/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb.impl

import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.RangingResult.RangingResultPeerDisconnected
import androidx.core.uwb.RangingResult.RangingResultPosition
import androidx.core.uwb.UwbDevice
import androidx.core.uwb.mock.TestUwbClient
import com.google.android.gms.nearby.uwb.RangingCapabilities
import com.google.android.gms.nearby.uwb.UwbAddress
import com.google.android.gms.nearby.uwb.UwbComplexChannel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class UwbClientSessionScopeImplTest {
    private val complexChannel = UwbComplexChannel.Builder()
        .setPreambleIndex(0)
        .setChannel(0)
        .build()
    private val localAddress = UwbAddress(ByteArray(0))
    private val rangingCapabilities = RangingCapabilities(true, false, false)
    private val uwbClient = TestUwbClient(complexChannel, localAddress, rangingCapabilities, true)
    private val uwbClientSessionScopeImpl = UwbClientSessionScopeImpl(
        uwbClient,
        androidx.core.uwb.RangingCapabilities(
            rangingCapabilities.supportsDistance(),
            rangingCapabilities.supportsAzimuthalAngle(),
            rangingCapabilities.supportsElevationAngle()),
        androidx.core.uwb.UwbAddress(localAddress.address))
    private val uwbDevice = UwbDevice.createForAddress(ByteArray(0))
    private val rangingParameters = RangingParameters(
        RangingParameters.UWB_CONFIG_ID_1,
        0,
        null,
        null,
        listOf(uwbDevice),
        RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
    )

    @Test
    public fun testInitSession_singleConsumer() {
        val sessionFlow = uwbClientSessionScopeImpl.prepareSession(rangingParameters)
        var rangingResult: RangingResult? = null
        val job = sessionFlow
            .cancellable()
            .onEach { rangingResult = it }
            .launchIn(CoroutineScope(Dispatchers.Main.immediate))

        runBlocking {
            // wait for the coroutines for UWB to get launched.
            delay(500)
        }
        // a non-null RangingResult should return from the TestUwbClient.
        if (rangingResult != null) {
            assertThat(rangingResult is RangingResultPosition).isTrue()
        } else {
            job.cancel()
            Assert.fail()
        }

        // cancel and wait for the job to terminate.
        job.cancel()
        runBlocking {
            job.join()
        }
        // StopRanging should have been called after the coroutine scope completed.
        assertThat(uwbClient.stopRangingCalled).isTrue()
    }

    @Test
    public fun testInitSession_multipleSharedConsumers() {
        var passed1 = false
        var passed2 = false
        val sharedFlow = uwbClientSessionScopeImpl.prepareSession(rangingParameters)
            .shareIn(CoroutineScope(Dispatchers.Main.immediate), SharingStarted.WhileSubscribed(),
                replay = 1)
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            sharedFlow
                .onEach {
                    if (it is RangingResultPosition) {
                        passed1 = true
                    }
                }
                .collect()
        }
        val job2 = CoroutineScope(Dispatchers.Main.immediate).launch {
            sharedFlow
                .onEach {
                    if (it is RangingResultPosition) {
                        passed2 = true
                    }
                }
                .collect()
        }

        runBlocking {
            // wait for coroutines for flow to start.
            delay(500)
        }

        // a non-null RangingResult should return from the TestUwbClient.
        assertThat(passed1).isTrue()
        assertThat(passed2).isTrue()

        // cancel and wait for the first job to terminate.
        job.cancel()
        runBlocking {
            job.join()
        }
        // StopRanging should not have been called because not all consumers have finished.
        assertThat(uwbClient.stopRangingCalled).isFalse()

        // cancel and wait for the second job to terminate.
        job2.cancel()
        runBlocking {
            job2.join()
        }
        // StopRanging should have been called because all consumers have finished.
        assertThat(uwbClient.stopRangingCalled).isTrue()
    }

    @Test
    public fun testInitSession_singleConsumer_disconnectPeerDevice() {
        val sessionFlow = uwbClientSessionScopeImpl.prepareSession(rangingParameters)
        var peerDisconnected = false
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            sessionFlow
                .cancellable()
                .onEach {
                    if (it is RangingResultPeerDisconnected) {
                        peerDisconnected = true
                    }
                }
                .collect()
        }

        runBlocking {
            // wait for coroutines for flow to start.
            delay(500)
            uwbClient.disconnectPeer(com.google.android.gms.nearby.uwb.UwbDevice.createForAddress(
                uwbDevice.address.address))

            // wait for rangingResults to get filled.
            delay(500)
        }

        // a peer disconnected event should have occurred.
        assertThat(peerDisconnected).isTrue()

        // cancel and wait for the job to terminate.
        job.cancel()
        runBlocking {
            job.join()
        }
        // StopRanging should have been called after the coroutine scope completed.
        assertThat(uwbClient.stopRangingCalled).isTrue()
    }

    @Test
    public fun testInitSession_multipleSharedConsumers_disconnectPeerDevice() {
        val sharedFlow = uwbClientSessionScopeImpl.prepareSession(rangingParameters)
            .shareIn(CoroutineScope(Dispatchers.Main.immediate), SharingStarted.WhileSubscribed())

        var peerDisconnected = false
        var peerDisconnected2 = false
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            sharedFlow
                .onEach {
                    if (it is RangingResultPeerDisconnected) {
                        peerDisconnected = true
                    }
                }
                .collect()
        }
        val job2 = CoroutineScope(Dispatchers.Main.immediate).launch {
            sharedFlow
                .onEach {
                    if (it is RangingResultPeerDisconnected) {
                        peerDisconnected2 = true
                    }
                }
                .collect()
        }

        runBlocking {
            // wait for coroutines for flow to start.
            delay(500)
            uwbClient.disconnectPeer(com.google.android.gms.nearby.uwb.UwbDevice.createForAddress(
                uwbDevice.address.address))

            // wait for rangingResults to get filled.
            delay(500)
        }

        // a peer disconnected event should have occurred.
        assertThat(peerDisconnected).isTrue()
        assertThat(peerDisconnected2).isTrue()

        // cancel and wait for the job to terminate.
        job.cancel()
        runBlocking {
            job.join()
        }
        // StopRanging should not have been called because not all consumers have finished.
        assertThat(uwbClient.stopRangingCalled).isFalse()

        // cancel and wait for the job to terminate.
        job2.cancel()
        runBlocking {
            job2.join()
        }
        // StopRanging should have been called because all consumers have finished.
        assertThat(uwbClient.stopRangingCalled).isTrue()
    }

    @Test
    public fun testInitSession_multipleSessions_throwsUwbApiException() {
        val sessionFlow = uwbClientSessionScopeImpl.prepareSession(rangingParameters)
        val sessionFlow2 = uwbClientSessionScopeImpl.prepareSession(rangingParameters)

        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            sessionFlow.collect()
        }
        runBlocking {
            // wait for coroutines for flow to start.
            delay(500)
        }
        val job2 = CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                sessionFlow2.collect()
                Assert.fail()
            } catch (e: IllegalStateException) {
                // verified the exception was thrown.
            }
        }
        job.cancel()
        job2.cancel()
    }

    @Test
    public fun testInitSession_reusingSession_throwsUwbApiException() {
        val sessionFlow = uwbClientSessionScopeImpl.prepareSession(rangingParameters)

        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            sessionFlow.collect()
        }
        runBlocking {
            // wait for coroutines for flow to start.
            delay(500)
        }
        // cancel and wait for the job to terminate.
        job.cancel()
        runBlocking {
            job.join()
        }
        // StopRanging should not have been called because not all consumers have finished.
        assertThat(uwbClient.stopRangingCalled).isTrue()
        val job2 = CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                // Reuse the same session after it was closed.
                sessionFlow.collect()
                Assert.fail()
            } catch (e: IllegalStateException) {
                // verified the exception was thrown.
            }
        }
        job2.cancel()
    }
}