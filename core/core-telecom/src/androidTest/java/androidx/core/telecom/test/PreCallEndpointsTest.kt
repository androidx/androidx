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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.PreCallEndpoints
import androidx.test.filters.SmallTest
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.O)
class PreCallEndpointsTest {
    private val defaultEarpiece = CallEndpointCompat("E", CallEndpointCompat.TYPE_EARPIECE)
    private val defaultSpeaker = CallEndpointCompat("S", CallEndpointCompat.TYPE_SPEAKER)
    private val defaultBluetooth = CallEndpointCompat("B", CallEndpointCompat.TYPE_BLUETOOTH)

    @SmallTest
    @Test
    fun testInitialValues() {
        val initEndpoints = mutableListOf(defaultEarpiece, defaultSpeaker, defaultBluetooth)
        val currentPreCallEndpoints = PreCallEndpoints(initEndpoints, Channel())
        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultEarpiece))
        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultSpeaker))
        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultBluetooth))
        assertTrue(currentPreCallEndpoints.mNonBluetoothEndpoints.contains(defaultEarpiece.type))
        assertTrue(currentPreCallEndpoints.mNonBluetoothEndpoints.contains(defaultSpeaker.type))
        assertTrue(currentPreCallEndpoints.mBluetoothEndpoints.contains(defaultBluetooth.name))
    }

    @SmallTest
    @Test
    fun testEndpointsAddedWithNewEndpoint() {
        val initEndpoints = mutableListOf(defaultEarpiece)
        val currentPreCallEndpoints = PreCallEndpoints(initEndpoints, Channel())

        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultEarpiece))
        assertFalse(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultSpeaker))

        val res = currentPreCallEndpoints.maybeAddCallEndpoint(defaultSpeaker)
        assertEquals(PreCallEndpoints.START_TRACKING_NEW_ENDPOINT, res)
    }

    @SmallTest
    @Test
    fun testEndpointsAddedWithNoNewEndpoints() {
        val initEndpoints = mutableListOf(defaultEarpiece, defaultSpeaker)
        val currentPreCallEndpoints = PreCallEndpoints(initEndpoints, Channel())

        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultEarpiece))
        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultSpeaker))

        val res = currentPreCallEndpoints.maybeAddCallEndpoint(defaultSpeaker)
        assertEquals(PreCallEndpoints.ALREADY_TRACKING_ENDPOINT, res)
    }

    @SmallTest
    @Test
    fun testEndpointsRemovedWithUntrackedEndpoint() {
        val initEndpoints = mutableListOf(defaultEarpiece)
        val currentPreCallEndpoints = PreCallEndpoints(initEndpoints, Channel())

        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultEarpiece))
        assertFalse(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultSpeaker))

        val res = currentPreCallEndpoints.maybeRemoveCallEndpoint(defaultSpeaker)
        assertEquals(PreCallEndpoints.NOT_TRACKING_REMOVED_ENDPOINT, res)
    }

    @SmallTest
    @Test
    fun testEndpointsRemovedWithTrackedEndpoint() {
        val initEndpoints = mutableListOf(defaultEarpiece, defaultSpeaker)
        val currentPreCallEndpoints = PreCallEndpoints(initEndpoints, Channel())

        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultEarpiece))
        assertTrue(currentPreCallEndpoints.isCallEndpointBeingTracked(defaultSpeaker))

        val res = currentPreCallEndpoints.maybeRemoveCallEndpoint(defaultSpeaker)
        assertEquals(PreCallEndpoints.STOP_TRACKING_REMOVED_ENDPOINT, res)
    }
}
