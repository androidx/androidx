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
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
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
@RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
class CallSessionTest : BaseTelecomTest() {
    private val mEarAndSpeakerEndpoints = listOf(mEarpieceEndpoint, mSpeakerEndpoint)
    private val mEarAndSpeakerAndBtEndpoints =
        listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)

    /**
     * verify maybeDelaySwitchToSpeaker does NOT switch to speakerphone if the bluetooth device
     * connects after 1 second
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testDelayedSwitchToSpeakerBluetoothConnects() {
        setUpV2Test()
        runBlocking {
            val callSession = initCallSession(coroutineContext, CallChannels())
            callSession.setCurrentCallEndpoint(mBluetoothEndpoint)
            callSession.setAvailableCallEndpoints(mEarAndSpeakerAndBtEndpoints)
            assertFalse(callSession.maybeDelaySwitchToSpeaker(mSpeakerEndpoint))
        }
    }

    /**
     * verify maybeDelaySwitchToSpeaker switches to speaker if a BT device is not in the available
     * list of call endpoints
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testDelayedSwitchToSpeakerNoBluetoothAvailable() {
        setUpV2Test()
        runBlocking {
            val callSession = initCallSession(coroutineContext, CallChannels())
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            assertTrue(callSession.maybeDelaySwitchToSpeaker(mSpeakerEndpoint))
        }
    }

    /** verify maybeDelaySwitchToSpeaker switches to speaker if a BT failed to connect in time */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testDelayedSwitchToSpeakerBluetoothDidNotConnectInTime() {
        setUpV2Test()
        runBlocking {
            val callSession = initCallSession(coroutineContext, CallChannels())
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)
            callSession.setAvailableCallEndpoints(mEarAndSpeakerAndBtEndpoints)
            assertTrue(callSession.maybeDelaySwitchToSpeaker(mSpeakerEndpoint))
        }
    }

    /** verify the CallEvent CompletableDeferred objects complete after endpoints are echoed. */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testCompletableDeferredObjectsComplete() {
        setUpV2Test()
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
        setUpV2Test()
        runBlocking {
            val callChannels = CallChannels()
            val callSession = initCallSession(coroutineContext, callChannels)

            callSession.onCallEndpointChanged(getCurrentEndpoint())
            callSession.onAvailableCallEndpointsChanged(getAvailableEndpoint())

            assertEquals(
                getAvailableEndpoint().size,
                callChannels.availableEndpointChannel.receive().size
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
        setUpV2Test()
        runBlocking {
            val callSession =
                initCallSession(
                    coroutineContext,
                    CallChannels(),
                )

            val platformEarpiece =
                CallEndpoint(
                    mEarpieceEndpoint.name,
                    CallEndpoint.TYPE_EARPIECE,
                    getRandomParcelUuid()
                )
            assertNotEquals(mEarpieceEndpoint.identifier, platformEarpiece.identifier)
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    getRandomParcelUuid()
                )
            assertNotEquals(mSpeakerEndpoint.identifier, platformSpeaker.identifier)
            val platformBt =
                CallEndpoint(
                    mBluetoothEndpoint.name,
                    CallEndpoint.TYPE_BLUETOOTH,
                    getRandomParcelUuid()
                )
            assertNotEquals(mBluetoothEndpoint.identifier, platformBt.identifier)

            val callSessionUuidRemapping = callSession.mJetpackToPlatformCallEndpoint
            assertEquals(
                mEarpieceEndpoint,
                callSession.toRemappedCallEndpointCompat(platformEarpiece)
            )
            assertTrue(callSessionUuidRemapping.containsKey(mEarpieceEndpoint.identifier))
            assertEquals(platformEarpiece, callSessionUuidRemapping[mEarpieceEndpoint.identifier])

            assertEquals(
                mSpeakerEndpoint,
                callSession.toRemappedCallEndpointCompat(platformSpeaker)
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
            coroutineContext,
            TestUtils.INCOMING_CALL_ATTRIBUTES,
            TestUtils.mOnAnswerLambda,
            TestUtils.mOnDisconnectLambda,
            TestUtils.mOnSetActiveLambda,
            TestUtils.mOnSetInActiveLambda,
            callChannels,
            { _, _ -> },
            CompletableDeferred(Unit)
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
