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

import androidx.core.uwb.RangingResult
import androidx.core.uwb.RangingResult.RangingResultPeerDisconnected
import androidx.core.uwb.RangingResult.RangingResultPosition
import androidx.core.uwb.common.TestCommons.Companion.COMPLEX_CHANNEL
import androidx.core.uwb.common.TestCommons.Companion.LOCAL_ADDRESS
import androidx.core.uwb.common.TestCommons.Companion.RANGING_CAPABILITIES
import androidx.core.uwb.common.TestCommons.Companion.RANGING_PARAMETERS
import androidx.core.uwb.common.TestCommons.Companion.UWB_DEVICE
import androidx.core.uwb.mock.TestUwbClient
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
    private val uwbClient = TestUwbClient(
        COMPLEX_CHANNEL, LOCAL_ADDRESS, RANGING_CAPABILITIES,
        isAvailable = true,
        isController = false
    )
    private val uwbClientSession = UwbClientSessionScopeImpl(
        uwbClient,
        androidx.core.uwb.RangingCapabilities(
            RANGING_CAPABILITIES.supportsDistance(),
            RANGING_CAPABILITIES.supportsAzimuthalAngle(),
            RANGING_CAPABILITIES.supportsElevationAngle(),
            RANGING_CAPABILITIES.getMinRangingInterval(),
            RANGING_CAPABILITIES.getSupportedChannels().toSet(),
            RANGING_CAPABILITIES.getSupportedConfigIds().toSet()
        ),
        androidx.core.uwb.UwbAddress(LOCAL_ADDRESS.address)
    )
    @Test
    public fun testInitSession_singleConsumer() {
        val sessionFlow = uwbClientSession.prepareSession(RANGING_PARAMETERS)
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
        val sharedFlow = uwbClientSession.prepareSession(RANGING_PARAMETERS)
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
        val sessionFlow = uwbClientSession.prepareSession(RANGING_PARAMETERS)
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
                UWB_DEVICE.address.address))

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
        val sharedFlow = uwbClientSession.prepareSession(RANGING_PARAMETERS)
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
                UWB_DEVICE.address.address))

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
        val sessionFlow = uwbClientSession.prepareSession(RANGING_PARAMETERS)
        val sessionFlow2 = uwbClientSession.prepareSession(RANGING_PARAMETERS)

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
        val sessionFlow = uwbClientSession.prepareSession(RANGING_PARAMETERS)

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