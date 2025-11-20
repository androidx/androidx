/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.telecom.test

import android.os.Build.VERSION_CODES
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.BluetoothDeviceChecker
import androidx.core.telecom.internal.VideoCallSpeakerManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for the [VideoCallSpeakerManager] class.
 *
 * These tests verify the logic for determining whether to switch to speakerphone during a video
 * call under various endpoint conditions.
 */
@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class VideoCallSpeakerManagerTest {
    private val sessionId: Int = 987

    /**
     * A fake implementation of [BluetoothDeviceChecker] for testing. We can control its return
     * value directly in each test and check if it was invoked.
     */
    private class FakeBluetoothDeviceChecker : BluetoothDeviceChecker {
        var hasNonWatchDeviceReturnValue = false
        var wasChecked = false

        override fun hasAvailableNonWatchDevice(
            availableEndpoints: List<CallEndpointCompat>
        ): Boolean {
            wasChecked = true
            return hasNonWatchDeviceReturnValue
        }
    }

    private lateinit var fakeBluetoothDeviceChecker: FakeBluetoothDeviceChecker
    private lateinit var speakerManager: VideoCallSpeakerManager

    // Reusable test data
    private val earpiece =
        CallEndpointCompat("Earpiece", CallEndpointCompat.TYPE_EARPIECE, sessionId)
    private val speaker = CallEndpointCompat("Speaker", CallEndpointCompat.TYPE_SPEAKER, sessionId)
    private val watch = CallEndpointCompat("Watch", CallEndpointCompat.TYPE_BLUETOOTH, sessionId)
    private val bluetoothHeadset =
        CallEndpointCompat("Headset", CallEndpointCompat.TYPE_BLUETOOTH, sessionId)
    private val wiredHeadset =
        CallEndpointCompat("Wired", CallEndpointCompat.TYPE_WIRED_HEADSET, sessionId)

    @Before
    fun setUp() {
        fakeBluetoothDeviceChecker = FakeBluetoothDeviceChecker()
        speakerManager = VideoCallSpeakerManager(fakeBluetoothDeviceChecker)
    }

    @Test
    fun shouldSwitchToSpeaker_allConditionsMet_noBluetooth_returnsTrue() {
        val availableEndpoints = listOf(earpiece, speaker)

        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = true,
                currentEndpoint = earpiece,
                availableEndpoints = availableEndpoints,
            )

        assertThat(result).isTrue()
        // Verify the costly check was NOT performed
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isFalse()
    }

    @Test
    fun shouldSwitchToSpeaker_allConditionsMet_onlyWatchBluetooth_returnsTrue() {
        // Arrange: Configure the fake to return false (no non-watch devices)
        fakeBluetoothDeviceChecker.hasNonWatchDeviceReturnValue = false
        val availableEndpoints = listOf(earpiece, speaker, watch)

        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = true,
                currentEndpoint = earpiece,
                availableEndpoints = availableEndpoints,
            )

        assertThat(result).isTrue()
        // Verify the costly check WAS performed
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isTrue()
    }

    @Test
    fun shouldSwitchToSpeaker_isNotVideoCall_returnsFalse() {
        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = false, // Condition failed
                currentEndpoint = earpiece,
                availableEndpoints = listOf(earpiece, speaker),
            )

        assertThat(result).isFalse()
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isFalse()
    }

    @Test
    fun shouldSwitchToSpeaker_audioNotOnEarpiece_returnsFalse() {
        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = true,
                currentEndpoint = speaker, // Condition failed
                availableEndpoints = listOf(earpiece, speaker),
            )

        assertThat(result).isFalse()
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isFalse()
    }

    @Test
    fun shouldSwitchToSpeaker_noSpeakerAvailable_returnsFalse() {
        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = true,
                currentEndpoint = earpiece,
                availableEndpoints = listOf(earpiece, wiredHeadset), // Condition failed
            )

        assertThat(result).isFalse()
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isFalse()
    }

    @Test
    fun shouldSwitchToSpeaker_nonWatchBluetoothDeviceAvailable_returnsFalse() {
        // Arrange: Configure the fake to return true (a non-watch device is present)
        fakeBluetoothDeviceChecker.hasNonWatchDeviceReturnValue = true
        val availableEndpoints = listOf(earpiece, speaker, bluetoothHeadset)

        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = true,
                currentEndpoint = earpiece,
                availableEndpoints = availableEndpoints,
            )

        assertThat(result).isFalse()
        // Verify the costly check WAS performed
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isTrue()
    }

    @Test
    fun shouldSwitchToSpeaker_multipleBluetoothDevices_nonWatchPresent_returnsFalse() {
        // Arrange: Configure the fake to return true (a non-watch device is present)
        fakeBluetoothDeviceChecker.hasNonWatchDeviceReturnValue = true
        val availableEndpoints = listOf(earpiece, speaker, watch, bluetoothHeadset)

        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = true,
                currentEndpoint = earpiece,
                availableEndpoints = availableEndpoints,
            )

        assertThat(result).isFalse()
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isTrue()
    }

    @Test
    fun shouldSwitchToSpeaker_currentEndpointIsNull_returnsFalse() {
        val result =
            speakerManager.shouldSwitchToSpeaker(
                isVideoCall = true,
                currentEndpoint = null, // Condition failed
                availableEndpoints = listOf(earpiece, speaker),
            )

        assertThat(result).isFalse()
        assertThat(fakeBluetoothDeviceChecker.wasChecked).isFalse()
    }
}
