/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.test

import android.os.Build.VERSION_CODES
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.CallEndpointUuidTracker
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RequiresApi(VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class CallEndpointUuidTrackerTest {
    /** Verify that the non bluetooth [CallEndpointCompat] types all return the expected UUID */
    @SmallTest
    @Test
    fun testNonBtEndpointUuid() {
        val mockFlowSession = 1
        assertEquals(
            CallEndpointUuidTracker.EARPIECE_UUID,
            CallEndpointUuidTracker.getUuid(mockFlowSession, CallEndpointCompat.TYPE_EARPIECE)
        )
        assertEquals(
            CallEndpointUuidTracker.SPEAKER_UUID,
            CallEndpointUuidTracker.getUuid(mockFlowSession, CallEndpointCompat.TYPE_SPEAKER)
        )
        assertEquals(
            CallEndpointUuidTracker.WIRED_HEADSET_UUID,
            CallEndpointUuidTracker.getUuid(mockFlowSession, CallEndpointCompat.TYPE_WIRED_HEADSET)
        )
        assertEquals(
            CallEndpointUuidTracker.UNKNOWN_ENDPOINT_UUID,
            CallEndpointUuidTracker.getUuid(mockFlowSession, CallEndpointCompat.TYPE_UNKNOWN)
        )
    }

    /**
     * Verify the CallEndpointUuidTracker mappings are correctly tracking and cleaning up a SINGLE
     * Bluetooth device on a SINGLE session.
     */
    @SmallTest
    @Test
    fun testTestSingleSessionWithBluetooth() {
        val btDeviceName = "Pixel Buds"
        val mockFlowSession = 2
        val btEndpointUuid: ParcelUuid =
            CallEndpointUuidTracker.getUuid(
                mockFlowSession,
                CallEndpointCompat.TYPE_BLUETOOTH,
                btDeviceName
            )
        assertBluetoothNameMappingEntry(btDeviceName, btEndpointUuid, mockFlowSession)
        assertSessionMapping(mockFlowSession, mutableSetOf(btDeviceName))
        CallEndpointUuidTracker.endSession(mockFlowSession)
        assertSessionCleanup(mockFlowSession, btDeviceName)
    }

    /**
     * Verify the CallEndpointUuidTracker mappings are correctly tracking and cleaning up a SINGLE
     * Bluetooth device on a MULTIPLE session.
     */
    @SmallTest
    @Test
    fun testTestMultipleSessionWithBluetooth() {
        val btDeviceName = "Pixel Buds"
        val mockFlowSession = 3
        val callSession = 4

        val flowBtUuid: ParcelUuid =
            CallEndpointUuidTracker.getUuid(
                mockFlowSession,
                CallEndpointCompat.TYPE_BLUETOOTH,
                btDeviceName
            )

        val callSessionBtUuid: ParcelUuid =
            CallEndpointUuidTracker.getUuid(
                callSession,
                CallEndpointCompat.TYPE_BLUETOOTH,
                btDeviceName
            )

        // All sessions should reference the same UUID. This ensures that the same device is
        // represented by with the same identifier.
        assertEquals(flowBtUuid, callSessionBtUuid)
        assertBluetoothNameMappingEntry(btDeviceName, flowBtUuid, mockFlowSession)
        assertBluetoothNameMappingEntry(btDeviceName, callSessionBtUuid, callSession)

        assertSessionMapping(mockFlowSession, mutableSetOf(btDeviceName))
        assertSessionMapping(callSession, mutableSetOf(btDeviceName))

        CallEndpointUuidTracker.endSession(mockFlowSession)
        assertSessionCleanup(mockFlowSession, btDeviceName)
        CallEndpointUuidTracker.endSession(callSession)
        assertSessionCleanup(callSession, btDeviceName)
    }

    /**
     * Verify the CallEndpointUuidTracker mappings are correctly tracking and cleaning up MULTIPLE
     * bluetooth devices on MULTIPLE sessions.
     */
    @SmallTest
    @Test
    fun testTestMultipleSessionWithMultipleBluetoothDevices() {
        val initialBtDevice = "Pixel Buds"
        val secondaryBtDevice = "Pixel Watch"
        val mockFlowSession = 5
        val callSession = 6

        val flowBtUuid: ParcelUuid =
            CallEndpointUuidTracker.getUuid(
                mockFlowSession,
                CallEndpointCompat.TYPE_BLUETOOTH,
                initialBtDevice
            )

        val callSessionBtUuid: ParcelUuid =
            CallEndpointUuidTracker.getUuid(
                callSession,
                CallEndpointCompat.TYPE_BLUETOOTH,
                initialBtDevice
            )

        assertEquals(flowBtUuid, callSessionBtUuid)
        assertBluetoothNameMappingEntry(initialBtDevice, flowBtUuid, mockFlowSession)
        assertBluetoothNameMappingEntry(initialBtDevice, callSessionBtUuid, callSession)

        assertSessionMapping(mockFlowSession, mutableSetOf(initialBtDevice))
        assertSessionMapping(callSession, mutableSetOf(initialBtDevice))

        val flowBtUuid2: ParcelUuid =
            CallEndpointUuidTracker.getUuid(
                mockFlowSession,
                CallEndpointCompat.TYPE_BLUETOOTH,
                secondaryBtDevice
            )

        val callSessionBtUuid2: ParcelUuid =
            CallEndpointUuidTracker.getUuid(
                callSession,
                CallEndpointCompat.TYPE_BLUETOOTH,
                secondaryBtDevice
            )

        assertEquals(flowBtUuid2, callSessionBtUuid2)
        assertBluetoothNameMappingEntry(secondaryBtDevice, flowBtUuid2, mockFlowSession)
        assertBluetoothNameMappingEntry(secondaryBtDevice, callSessionBtUuid2, callSession)

        assertSessionMapping(mockFlowSession, mutableSetOf(initialBtDevice, secondaryBtDevice))
        assertSessionMapping(callSession, mutableSetOf(initialBtDevice, secondaryBtDevice))

        CallEndpointUuidTracker.endSession(mockFlowSession)
        assertSessionCleanup(mockFlowSession, initialBtDevice)
        assertSessionCleanup(mockFlowSession, secondaryBtDevice)

        CallEndpointUuidTracker.endSession(callSession)
        assertSessionCleanup(callSession, initialBtDevice)
        assertSessionCleanup(callSession, secondaryBtDevice)
    }

    /**
     * -------------------------------------------------------------------------------------------
     * Helpers
     * -------------------------------------------------------------------------------------------
     */
    private fun assertBluetoothNameMappingEntry(
        btDeviceName: String,
        btEndpointUuid: ParcelUuid,
        sessionId: Int
    ) {
        assertTrue(CallEndpointUuidTracker.getBluetoothMapping().containsKey(btDeviceName))
        val ids = CallEndpointUuidTracker.getBluetoothMapping()[btDeviceName]!!
        val jetpackUuid = ids.first
        val trackingSessions = ids.second
        assertEquals(jetpackUuid, btEndpointUuid)
        assertTrue(trackingSessions.contains(sessionId))
    }

    private fun assertSessionMapping(sessionId: Int, btDeviceNames: MutableSet<String>) {
        val sessionMapping = CallEndpointUuidTracker.getSessionMapping()
        assertTrue(sessionMapping.containsKey(sessionId))
        val trackedBtNames: Set<String> = sessionMapping[sessionId]!!
        for (name in btDeviceNames) {
            assertTrue(trackedBtNames.contains(name))
        }
    }

    private fun assertSessionCleanup(sessionId: Int, btDeviceName: String) {
        if (CallEndpointUuidTracker.getBluetoothMapping().containsKey(btDeviceName)) {
            val ids = CallEndpointUuidTracker.getBluetoothMapping()[btDeviceName]!!
            val trackingSessions = ids.second
            assertFalse(trackingSessions.contains(sessionId))
        }
        val sessionMapping = CallEndpointUuidTracker.getSessionMapping()
        assertFalse(sessionMapping.containsKey(sessionId))
    }
}
