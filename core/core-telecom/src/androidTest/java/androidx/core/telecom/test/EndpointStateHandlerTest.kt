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
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.CallEndpointUuidTracker
import androidx.core.telecom.internal.EndpointStateHandler
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
class EndpointStateHandlerTest {
    private val sessionId: Int = 111

    // Test data reused across multiple tests
    private val earpiece =
        CallEndpointCompat("Earpiece", CallEndpointCompat.TYPE_EARPIECE, sessionId)
    private val speaker = CallEndpointCompat("Speaker", CallEndpointCompat.TYPE_SPEAKER, sessionId)
    private val bluetooth =
        CallEndpointCompat("Bluetooth", CallEndpointCompat.TYPE_BLUETOOTH, sessionId)
    private val wiredHeadset =
        CallEndpointCompat("Wired", CallEndpointCompat.TYPE_WIRED_HEADSET, sessionId)
    private val bluetooth_A =
        CallEndpointCompat("A_Headset", CallEndpointCompat.TYPE_BLUETOOTH, sessionId)
    private val bluetooth_B =
        CallEndpointCompat("B_Headset", CallEndpointCompat.TYPE_BLUETOOTH, sessionId)
    private val bluetooth_C =
        CallEndpointCompat("C_Headset", CallEndpointCompat.TYPE_BLUETOOTH, sessionId)

    @After
    fun tearDown() {
        // Clean up the static tracker after each test to ensure isolation.
        CallEndpointUuidTracker.endSession(sessionId)
    }

    @Test
    fun initialState_isCorrectlySet() {
        val initialEndpoints = mutableSetOf(earpiece, speaker)
        val handler = EndpointStateHandler(initialEndpoints)

        val internalSet = handler.getInternalSet()
        assertEquals(2, internalSet.size)
        assertTrue(internalSet.contains(earpiece))
        assertTrue(internalSet.contains(speaker))
    }

    @Test
    fun add_whenSetChanges_returnsTrueAndStateIsSorted() {
        val handler = EndpointStateHandler(mutableSetOf(earpiece))

        // Action
        val stateChanged = handler.add(listOf(speaker))

        // Assertion
        assertTrue("add() should return true when the set changes", stateChanged)
        val updatedList = handler.getSortedEndpoints()
        assertEquals("List should have 2 endpoints after addition", 2, updatedList.size)
        assertEquals("Speaker should be the first element (higher rank)", speaker, updatedList[0])
        assertEquals("Earpiece should be the second element (lower rank)", earpiece, updatedList[1])
    }

    @Test
    fun add_whenSetDoesNotChange_returnsFalse() {
        val handler = EndpointStateHandler(mutableSetOf(earpiece))

        // Action
        val stateChanged = handler.add(listOf(earpiece))

        // Assertion
        assertFalse("add() should return false when the set does not change", stateChanged)
        assertThat(handler.getSortedEndpoints()).containsExactly(earpiece)
    }

    @Test
    fun remove_whenSetChanges_returnsTrueAndStateIsSorted() {
        val handler = EndpointStateHandler(mutableSetOf(earpiece, speaker, bluetooth))

        // Action
        val stateChanged = handler.remove(listOf(speaker))

        // Assertion
        assertTrue("remove() should return true when the set changes", stateChanged)
        val updatedList = handler.getSortedEndpoints()
        assertEquals("List should have 2 endpoints after removal", 2, updatedList.size)
        assertEquals(bluetooth, updatedList[0])
        assertEquals(earpiece, updatedList[1])
    }

    @Test
    fun getSortedEndpoints_returnsListThatDoesNotAffectInternalState() {
        val handler = EndpointStateHandler(mutableSetOf(earpiece, speaker))

        // Action: Get the list from the handler
        val receivedList = handler.getSortedEndpoints()

        val mutableCopy = receivedList.toMutableList()
        mutableCopy.clear()

        // Assertion: The handler's internal state should still be unchanged.
        val listAfterModification = handler.getSortedEndpoints()
        assertThat(listAfterModification).hasSize(2)
        assertThat(listAfterModification).containsExactly(speaker, earpiece).inOrder()
    }

    @Test
    fun getSortedEndpoints_returnsCorrectlySortedList() {
        // Initialize with an unsorted set
        val handler = EndpointStateHandler(mutableSetOf(earpiece, speaker))

        // Action
        handler.add(listOf(wiredHeadset, bluetooth))

        // Assertion
        val updatedList = handler.getSortedEndpoints()
        val expectedOrder = listOf(wiredHeadset, bluetooth, speaker, earpiece)
        assertThat(updatedList).containsExactlyElementsIn(expectedOrder).inOrder()
    }

    @Test
    fun add_withMultipleNewBluetoothDevices_updatesStateCorrectly() {
        val handler = EndpointStateHandler(mutableSetOf(earpiece))

        // Action
        handler.add(listOf(bluetooth_C, bluetooth_A))

        // Assertion
        val updatedList = handler.getSortedEndpoints()
        val expectedOrder = listOf(bluetooth_A, bluetooth_C, earpiece)
        assertEquals("List should contain 3 endpoints after addition", 3, updatedList.size)
        assertThat(updatedList).containsExactlyElementsIn(expectedOrder).inOrder()
    }

    @Test
    fun remove_withMultipleBluetoothDevices_updatesStateCorrectly() {
        val initialSet = mutableSetOf(earpiece, bluetooth_A, speaker, bluetooth_C)
        val handler = EndpointStateHandler(initialSet)

        // Action
        handler.remove(listOf(bluetooth_C, bluetooth_A))

        // Assertion
        val updatedList = handler.getSortedEndpoints()
        val expectedOrder = listOf(speaker, earpiece)
        assertEquals("List should contain 2 endpoints after removal", 2, updatedList.size)
        assertThat(updatedList).containsExactlyElementsIn(expectedOrder).inOrder()
    }

    @Test
    fun multipleBluetoothDevices_areSortedAlphabeticallyByName() {
        val initialSet = mutableSetOf(speaker, bluetooth_C, earpiece, bluetooth_A)
        val handler = EndpointStateHandler(initialSet)

        // Action
        handler.add(listOf(bluetooth_B))

        // Assertion
        val updatedList = handler.getSortedEndpoints()
        val expectedOrder = listOf(bluetooth_A, bluetooth_B, bluetooth_C, speaker, earpiece)
        assertThat(updatedList).containsExactlyElementsIn(expectedOrder).inOrder()
    }

    @Test
    fun addBluetoothDevice_whenAnotherAlreadyExists_maintainsSortOrder() {
        val handler = EndpointStateHandler(mutableSetOf(bluetooth_B, earpiece))

        // Action
        handler.add(listOf(bluetooth_A))

        // Assertion
        val updatedList = handler.getSortedEndpoints()
        val expectedOrder = listOf(bluetooth_A, bluetooth_B, earpiece)
        assertEquals("List should contain 3 endpoints after addition", 3, updatedList.size)
        assertThat(updatedList).containsExactlyElementsIn(expectedOrder).inOrder()
    }

    @Test
    fun removeBluetoothDevice_whenMultipleExist_updatesListCorrectly() {
        val handler = EndpointStateHandler(mutableSetOf(bluetooth_A, bluetooth_C, speaker))

        // Action
        handler.remove(listOf(bluetooth_A))

        // Assertion
        val updatedList = handler.getSortedEndpoints()
        val expectedOrder = listOf(bluetooth_C, speaker)
        assertEquals("List should contain 2 endpoints after removal", 2, updatedList.size)
        assertThat(updatedList).containsExactlyElementsIn(expectedOrder).inOrder()
    }
}
