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
import android.telecom.CallEndpoint
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.BluetoothDeviceChecker
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_NAME
import androidx.core.telecom.test.utils.TestUtils.TEST_ADDRESS
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class should be used to test behavior in the
 * [androidx.core.telecom.internal.CallSession] object. All transactional calls are wrapped in a
 * [androidx.core.telecom.internal.CallSession] object.
 */
@SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE /* api=34 */)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalAppActions::class)
@RunWith(AndroidJUnit4::class)
class CallSessionTest : BaseTelecomTest() {
    private val mEarAndSpeakerEndpoints = listOf(mEarpieceEndpoint, mSpeakerEndpoint)
    private val mEarAndSpeakerAndBtEndpoints =
        listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)
    private val mWiredAndEarpieceEndpoints = listOf(mEarpieceEndpoint, mWiredEndpoint)

    /**
     * A fake implementation of BluetoothDeviceChecker for testing. We can control its return value
     * directly in each test.
     */
    private class FakeBluetoothDeviceChecker : BluetoothDeviceChecker {
        var hasNonWatchDevice = false

        override fun hasAvailableNonWatchDevice(
            availableEndpoints: List<CallEndpointCompat>
        ): Boolean {
            return hasNonWatchDevice
        }
    }

    private fun initVideoCallSession(
        bluetoothDeviceChecker: BluetoothDeviceChecker,
        coroutineContext: CoroutineContext,
        callChannels: CallChannels,
    ): CallSession {
        return CallSession(
            bluetoothDeviceChecker,
            coroutineContext,
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                CallAttributesCompat.SUPPORTS_STREAM,
            ),
            TestUtils.mOnAnswerLambda,
            TestUtils.mOnDisconnectLambda,
            TestUtils.mOnSetActiveLambda,
            TestUtils.mOnSetInActiveLambda,
            callChannels,
            MutableSharedFlow(),
            { _, _ -> },
            CompletableDeferred(Unit),
        )
    }

    /**
     * Verifies that the switch to speaker is avoided because the fake checker reports that a
     * non-watch Bluetooth device is present.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testSwitchToSpeaker_avoidsSwitchWhenNonWatchBluetoothDeviceIsAvailable() {
        runBlocking {
            // Arrange: Configure the fake to return true
            val fakeChecker = FakeBluetoothDeviceChecker().apply { hasNonWatchDevice = true }
            val callSession = initVideoCallSession(fakeChecker, coroutineContext, CallChannels())

            // Set initial state
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)
            callSession.setAvailableCallEndpoints(mEarAndSpeakerAndBtEndpoints)
            callSession.getIsCurrentEndpointSet().complete(Unit)
            callSession.getIsAvailableEndpointsSet().complete(Unit)

            // Act: Capture the boolean result
            val wasSwitchRequested = callSession.switchToSpeakerForVideoCallIfNeeded()

            // Assert: Check the return value
            assertFalse(wasSwitchRequested)
        }
    }

    /**
     * Verifies that the switch to speaker proceeds because the fake checker reports that no
     * non-watch Bluetooth device is present.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testSwitchToSpeaker_switchesWhenOnlyWatchIsAvailable() {
        runBlocking {
            // Arrange: Configure the fake to return false
            val fakeChecker = FakeBluetoothDeviceChecker().apply { hasNonWatchDevice = false }
            val callSession = initVideoCallSession(fakeChecker, coroutineContext, CallChannels())

            // Set initial state
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)
            callSession.setAvailableCallEndpoints(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mWatchEndpoint)
            )
            callSession.getIsCurrentEndpointSet().complete(Unit)
            callSession.getIsAvailableEndpointsSet().complete(Unit)

            // Act: Capture the boolean result
            val wasSwitchRequested = callSession.switchToSpeakerForVideoCallIfNeeded()

            // Assert: Check the return value
            assertTrue(wasSwitchRequested)
        }
    }

    /**
     * Test the helper method that removes the earpiece call endpoint if the wired headset endpoint
     * is present
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testRemovalOfEarpieceEndpointIfWiredEndpointIsPresent() {
        val res =
            EndpointUtils.maybeRemoveEarpieceIfWiredEndpointPresent(
                mWiredAndEarpieceEndpoints.toMutableList()
            )
        assertEquals(1, res.size)
        assertEquals(res[0].type, CallEndpointCompat.TYPE_WIRED_HEADSET)
    }

    /** verify the CallEvent CompletableDeferred objects complete after endpoints are echoed. */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testCompletableDeferredObjectsComplete() {
        runBlocking {
            val callChannels = CallChannels()
            val callSession = initCallSession(coroutineContext, callChannels)

            assertFalse(callSession.getIsAvailableEndpointsSet().isCompleted)
            assertFalse(callSession.getIsCurrentEndpointSet().isCompleted)

            callSession.onCallEndpointChanged(getCurrentEndpoint())
            callSession.onAvailableCallEndpointsChanged(getAvailableEndpoint())

            assertTrue(callSession.getIsAvailableEndpointsSet().isCompleted)
            assertTrue(callSession.getIsCurrentEndpointSet().isCompleted)
            callChannels.closeAllChannels()
        }
    }

    /**
     * verify the call channels are receivable given the new CompletableDeferred object logic in the
     * CallEvent callbacks.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testCallEventsEchoEndpoints() {
        runBlocking {
            val callChannels = CallChannels()
            val callSession = initCallSession(coroutineContext, callChannels)

            callSession.onCallEndpointChanged(getCurrentEndpoint())
            callSession.onAvailableCallEndpointsChanged(getAvailableEndpoint())

            assertEquals(
                getAvailableEndpoint().size,
                callChannels.availableEndpointChannel.receive().size,
            )
            assertNotNull(callChannels.currentEndpointChannel.receive())
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verify the [CallEndpoint]s echoed from the platform are re-mapped to the existing
     * [CallEndpointCompat]s the user received with
     * [androidx.core.telecom.CallsManager#getAvailableStartingCallEndpoints()]
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testPlatformEndpointsAreRemappedToExistingEndpoints() {
        runBlocking {
            val callSession = initCallSession(coroutineContext, CallChannels())

            val platformEarpiece =
                CallEndpoint(
                    mEarpieceEndpoint.name,
                    CallEndpoint.TYPE_EARPIECE,
                    getRandomParcelUuid(),
                )
            assertNotEquals(mEarpieceEndpoint.identifier, platformEarpiece.identifier)
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    getRandomParcelUuid(),
                )
            assertNotEquals(mSpeakerEndpoint.identifier, platformSpeaker.identifier)
            val platformBt =
                CallEndpoint(
                    mBluetoothEndpoint.name,
                    CallEndpoint.TYPE_BLUETOOTH,
                    getRandomParcelUuid(),
                )
            assertNotEquals(mBluetoothEndpoint.identifier, platformBt.identifier)

            val callSessionUuidRemapping = callSession.mJetpackToPlatformCallEndpoint
            assertEquals(
                mEarpieceEndpoint,
                callSession.toRemappedCallEndpointCompat(platformEarpiece),
            )
            assertTrue(callSessionUuidRemapping.containsKey(mEarpieceEndpoint.identifier))
            assertEquals(platformEarpiece, callSessionUuidRemapping[mEarpieceEndpoint.identifier])

            assertEquals(
                mSpeakerEndpoint,
                callSession.toRemappedCallEndpointCompat(platformSpeaker),
            )
            assertTrue(callSessionUuidRemapping.containsKey(mSpeakerEndpoint.identifier))
            assertEquals(platformSpeaker, callSessionUuidRemapping[mSpeakerEndpoint.identifier])

            assertEquals(mBluetoothEndpoint, callSession.toRemappedCallEndpointCompat(platformBt))
            assertTrue(callSessionUuidRemapping.containsKey(mBluetoothEndpoint.identifier))
            assertEquals(platformBt, callSessionUuidRemapping[mBluetoothEndpoint.identifier])
        }
    }

    private fun initCallSession(
        coroutineContext: CoroutineContext,
        callChannels: CallChannels,
    ): CallSession {
        return CallSession(
            FakeBluetoothDeviceChecker(),
            coroutineContext,
            TestUtils.INCOMING_CALL_ATTRIBUTES,
            TestUtils.mOnAnswerLambda,
            TestUtils.mOnDisconnectLambda,
            TestUtils.mOnSetActiveLambda,
            TestUtils.mOnSetInActiveLambda,
            callChannels,
            MutableSharedFlow(),
            { _, _ -> },
            CompletableDeferred(Unit),
        )
    }

    fun getCurrentEndpoint(): CallEndpoint {
        return CallEndpoint("EARPIECE", CallEndpoint.TYPE_EARPIECE, getRandomParcelUuid())
    }

    fun getAvailableEndpoint(): List<CallEndpoint> {
        val endpoints = mutableListOf<CallEndpoint>()
        endpoints.add(getCurrentEndpoint())
        return endpoints
    }

    private fun getRandomParcelUuid(): ParcelUuid {
        return ParcelUuid.fromString(UUID.randomUUID().toString())
    }
}
