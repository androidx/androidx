/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.common.TestCommons.Companion.COMPLEX_CHANNEL
import androidx.core.uwb.common.TestCommons.Companion.LOCAL_ADDRESS
import androidx.core.uwb.common.TestCommons.Companion.NEIGHBOR_1
import androidx.core.uwb.common.TestCommons.Companion.NEIGHBOR_2
import androidx.core.uwb.common.TestCommons.Companion.RANGING_CAPABILITIES
import androidx.core.uwb.common.TestCommons.Companion.RANGING_PARAMETERS
import androidx.core.uwb.mock.TestUwbClient
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class UwbControllerSessionScopeImplTest {
    private val uwbClient = TestUwbClient(
        COMPLEX_CHANNEL, LOCAL_ADDRESS, RANGING_CAPABILITIES,
        isAvailable = true,
        isController = true
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
        UwbAddress(LOCAL_ADDRESS.address)
    )
    private val uwbControllerSession = UwbControllerSessionScopeImpl(
        uwbClient,
        androidx.core.uwb.RangingCapabilities(
            RANGING_CAPABILITIES.supportsDistance(),
            RANGING_CAPABILITIES.supportsAzimuthalAngle(),
            RANGING_CAPABILITIES.supportsElevationAngle(),
            RANGING_CAPABILITIES.getMinRangingInterval(),
            RANGING_CAPABILITIES.getSupportedChannels().toSet(),
            RANGING_CAPABILITIES.getSupportedConfigIds().toSet()
        ),
        UwbAddress(LOCAL_ADDRESS.address),
        androidx.core.uwb.UwbComplexChannel(
            COMPLEX_CHANNEL.channel,
            COMPLEX_CHANNEL.preambleIndex)
    )

    private var rangingResult: RangingResult? = null
    @Test
    public fun testAddControlee_success() {
        val job = startRanging()

        // a non-null RangingResult should return from the TestUwbClient.
        if (rangingResult != null) {
            assertThat(rangingResult is RangingResult.RangingResultPosition).isTrue()
            assertThat(rangingResult!!.device.address.address).isEqualTo(NEIGHBOR_1)
        } else {
            stopRanging(job)
            Assert.fail()
        }

        runBlocking {
            uwbControllerSession.addControlee(UwbAddress(NEIGHBOR_2))
            delay(500)
        }
        assertThat(rangingResult is RangingResult.RangingResultPosition).isTrue()
        assertThat(rangingResult!!.device.address.address).isEqualTo(NEIGHBOR_2)
        stopRanging(job)
    }

    @Test
    public fun testAddControlee_beforeStartingRanging() {
        runBlocking {
            var correctExceptionThrown = false
            try {
                uwbControllerSession.addControlee(UwbAddress(NEIGHBOR_2))
            } catch (e: IllegalStateException) {
                // IllegalStateException was thrown as expected
                correctExceptionThrown = true
            }
            assertThat(correctExceptionThrown).isTrue()
        }
    }

    @Test
    public fun testRemoveControlee_success() {
        val job = startRanging()

        // a non-null RangingResult should return from the TestUwbClient.
        if (rangingResult != null) {
            assertThat(rangingResult is RangingResult.RangingResultPosition).isTrue()
            assertThat(rangingResult!!.device.address.address).isEqualTo(NEIGHBOR_1)
        } else {
            stopRanging(job)
            Assert.fail()
        }

        runBlocking {
            uwbControllerSession.removeControlee(UwbAddress(NEIGHBOR_1))
            delay(500)
        }
        assertThat(rangingResult is RangingResult.RangingResultPeerDisconnected).isTrue()
        assertThat(rangingResult!!.device.address.address).isEqualTo(NEIGHBOR_1)
        stopRanging(job)
    }

    @Test
    public fun testRemoveControlee_beforeStartingRanging() {
        runBlocking {
            var correctExceptionThrown = false
            try {
                uwbControllerSession.removeControlee(UwbAddress(NEIGHBOR_2))
            } catch (e: IllegalStateException) {
                // IllegalStateException was thrown as expected
                correctExceptionThrown = true
            }
            assertThat(correctExceptionThrown).isTrue()
        }
    }

    private fun startRanging(): Job {
        val sessionFlow = uwbControllerSession.prepareSession(RANGING_PARAMETERS)
        val job = sessionFlow
            .cancellable()
            .onEach { rangingResult = it }
            .launchIn(CoroutineScope(Dispatchers.Main.immediate))

        runBlocking {
            // wait for the coroutines for UWB to get launched.
            delay(500)
        }
        return job
    }

    private fun stopRanging(job: Job) {
        // cancel and wait for the job to terminate.
        job.cancel()
        runBlocking {
            job.join()
        }
    }
}