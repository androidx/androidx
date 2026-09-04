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

package androidx.core.telecom.test

import android.os.Build.VERSION_CODES
import android.telecom.CallEndpoint
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.BluetoothDeviceChecker
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalAppActions::class)
@RunWith(AndroidJUnit4::class)
class AvoidSpeakerOverrideTest : BaseTelecomTest() {

    private fun initCallSession(
        coroutineContext: CoroutineContext,
        callChannels: CallChannels,
        attributes: CallAttributesCompat = TestUtils.INCOMING_CALL_ATTRIBUTES,
    ): CallSession {
        return CallSession(
            object : BluetoothDeviceChecker {
                override fun hasAvailableNonWatchDevice(
                    availableEndpoints: List<CallEndpointCompat>
                ): Boolean = false
            },
            coroutineContext,
            attributes,
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
     * Verifies that if the user explicitly requested the speaker endpoint at the start of the call
     * (where prevEndpoint is null), the stabilization logic correctly identifies the intent and
     * does NOT revert to the preferred starting endpoint.
     */
    @SmallTest
    @Test
    fun testAvoidSpeakerOverride_UserRequestedSpeaker_NoReversion() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes = TestUtils.INCOMING_CALL_ATTRIBUTES
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Set the preferred starting endpoint (e.g., EARPIECE)
            callSession.mPreferredStartingCallEndpoint = mEarpieceEndpoint

            // Simulate user explicitly requesting speaker
            callSession.mLastClientRequestedEndpoint = mSpeakerEndpoint
            callSession.mAlreadyRequestedStartingEndpointSwitch = true

            // Simulate first platform update to SPEAKER
            // In the real world, previousCallEndpoint would be null here
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    mSpeakerEndpoint.identifier,
                )

            // Act: call the change
            callSession.onCallEndpointChanged(platformSpeaker)

            // Wait for coroutines
            yield()

            // Assert: mLastClientRequestedEndpoint is cleared (confirmed) and NO new request was
            // made
            // If a reversion happened, mLastClientRequestedEndpoint would be mEarpieceEndpoint
            assertNull(
                "Reversion should not have occurred",
                callSession.mLastClientRequestedEndpoint,
            )

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that if the platform unexpectedly switches to SPEAKER from the preferred endpoint,
     * and it was NOT a user request, the logic reverts it back.
     */
    @SmallTest
    @Test
    fun testAvoidSpeakerOverride_PlatformIncorrectlyOverrides_Reverts() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes = TestUtils.INCOMING_CALL_ATTRIBUTES
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Set the preferred starting endpoint (e.g., EARPIECE)
            callSession.mPreferredStartingCallEndpoint = mEarpieceEndpoint

            // Initial state is EARPIECE
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)

            // Simulate platform unexpectedly switching to SPEAKER
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    mSpeakerEndpoint.identifier,
                )

            // Act
            callSession.onCallEndpointChanged(platformSpeaker)

            // Wait for coroutines
            yield()

            // Assert: A reversion request to EARPIECE should have been made
            assertEquals(
                "Should have reverted to earpiece",
                CallEndpointCompat.TYPE_EARPIECE,
                callSession.mLastClientRequestedEndpoint?.type,
            )

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that for a VIDEO call, if the user selected a preferred starting endpoint (e.g.,
     * EARPIECE) and the platform unexpectedly switches to SPEAKER, the logic reverts the endpoint
     * back to the preferred EARPIECE endpoint.
     */
    @SmallTest
    @Test
    fun testAvoidSpeakerOverride_videoCall_preferredEarpiece_revertsToEarpiece() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes =
                CallAttributesCompat(
                    TestUtils.OUTGOING_NAME,
                    TestUtils.TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_OUTGOING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    TestUtils.ALL_CALL_CAPABILITIES,
                )
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Set the preferred starting endpoint (EARPIECE)
            callSession.mPreferredStartingCallEndpoint = mEarpieceEndpoint

            // Initial state settled on EARPIECE
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)

            // Simulate platform unexpectedly switching to SPEAKER (e.g. video call defaulting)
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    mSpeakerEndpoint.identifier,
                )

            // Act: platform changes to SPEAKER
            callSession.onCallEndpointChanged(platformSpeaker)

            // Wait for coroutines
            yield()

            // Assert: A reversion request to EARPIECE should have been made
            assertEquals(
                "Video call should have reverted to preferred earpiece",
                CallEndpointCompat.TYPE_EARPIECE,
                callSession.mLastClientRequestedEndpoint?.type,
            )

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that for a VIDEO call, enforceVideoCallSpeakerFallback respects the user's preferred
     * starting endpoint (EARPIECE) and does NOT force it to SPEAKER, while still enforcing SPEAKER
     * when preferred starting endpoint is null.
     */
    @SmallTest
    @Test
    fun testEnforceVideoCallSpeakerFallback_preferredEarpiece_doesNotForceSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes =
                CallAttributesCompat(
                    TestUtils.OUTGOING_NAME,
                    TestUtils.TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_OUTGOING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    TestUtils.ALL_CALL_CAPABILITIES,
                )
            val callSession = initCallSession(coroutineContext, callChannels, attributes)
            callSession.setAvailableCallEndpoints(listOf(mEarpieceEndpoint, mSpeakerEndpoint))

            // Case A: User explicitly preferred EARPIECE as starting endpoint
            callSession.mPreferredStartingCallEndpoint = mEarpieceEndpoint
            callSession.mLastClientRequestedEndpoint = null // Already cleared after initial ACK

            val platformEarpiece =
                CallEndpoint(
                    mEarpieceEndpoint.name,
                    CallEndpoint.TYPE_EARPIECE,
                    mEarpieceEndpoint.identifier,
                )

            // Act: Platform endpoint set to EARPIECE
            callSession.onCallEndpointChanged(platformEarpiece)
            yield()

            // Assert: Must NOT force switch to SPEAKER
            assertNull(
                "Should not force speaker when preferred starting endpoint is earpiece",
                callSession.mLastClientRequestedEndpoint,
            )

            // Case B: No preferred starting endpoint set (null) on video call
            callSession.mPreferredStartingCallEndpoint = null
            callSession.onCallEndpointChanged(platformEarpiece)
            yield()

            // Assert: Must force switch to SPEAKER for standard video calls
            assertEquals(
                "Default video call on earpiece must force speaker",
                CallEndpointCompat.TYPE_SPEAKER,
                callSession.mLastClientRequestedEndpoint?.type,
            )

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that if the user started the video call with a preferred endpoint of EARPIECE, but
     * subsequently transitions to BLUETOOTH, disconnecting the Bluetooth headset will properly
     * route to SPEAKER rather than falling back to the starting EARPIECE endpoint.
     */
    @SmallTest
    @Test
    fun testPreferredEarpiece_thenMovesToBluetooth_disconnectRoutesToSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes =
                CallAttributesCompat(
                    TestUtils.OUTGOING_NAME,
                    TestUtils.TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_OUTGOING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    TestUtils.ALL_CALL_CAPABILITIES,
                )
            val callSession = initCallSession(coroutineContext, callChannels, attributes)
            callSession.setAvailableCallEndpoints(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint)
            )

            // 1. Call starts on EARPIECE (preferred starting endpoint)
            callSession.mPreferredStartingCallEndpoint = mEarpieceEndpoint
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)
            callSession.startPreferredEndpointStabilizationTimer()

            // Once the call-start stabilization window elapses, preferred starting endpoint is
            // cleared
            delay(3500)
            assertNull(
                "Preferred starting endpoint should be cleared once stabilization timer expires",
                callSession.mPreferredStartingCallEndpoint,
            )

            // 2. User moves to BLUETOOTH
            val platformBt =
                CallEndpoint(
                    mBluetoothEndpoint.name,
                    CallEndpoint.TYPE_BLUETOOTH,
                    mBluetoothEndpoint.identifier,
                )
            callSession.onCallEndpointChanged(platformBt)
            yield()

            // 3. Bluetooth headset disconnects, platform defaults to EARPIECE
            val platformEarpiece =
                CallEndpoint(
                    mEarpieceEndpoint.name,
                    CallEndpoint.TYPE_EARPIECE,
                    mEarpieceEndpoint.identifier,
                )
            callSession.onCallEndpointChanged(platformEarpiece)
            yield()

            // 4. Assert: Video call must request switch to SPEAKER, NOT stay on EARPIECE!
            assertEquals(
                "Headset disconnect in video call must switch to speaker",
                CallEndpointCompat.TYPE_SPEAKER,
                callSession.mLastClientRequestedEndpoint?.type,
            )

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that when a video call with a preferred starting endpoint of EARPIECE starts:
     * 1. The platform initially routes to SPEAKER (default for video calls).
     * 2. Core-Telecom switches the call to the preferred EARPIECE endpoint.
     * 3. The platform's delayed speaker echo arrives (switching back to SPEAKER).
     * 4. Core-Telecom catches this delayed override and correctly requests a switch back to
     *    EARPIECE.
     */
    @SmallTest
    @Test
    fun testAvoidSpeakerOverride_videoCall_delayedSpeakerEcho_revertsToEarpiece() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes =
                CallAttributesCompat(
                    TestUtils.OUTGOING_NAME,
                    TestUtils.TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_OUTGOING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    TestUtils.ALL_CALL_CAPABILITIES,
                )
            val callSession = initCallSession(coroutineContext, callChannels, attributes)
            callSession.setAvailableCallEndpoints(listOf(mEarpieceEndpoint, mSpeakerEndpoint))

            callSession.mPreferredStartingCallEndpoint = mEarpieceEndpoint

            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    mSpeakerEndpoint.identifier,
                )
            val platformEarpiece =
                CallEndpoint(
                    mEarpieceEndpoint.name,
                    CallEndpoint.TYPE_EARPIECE,
                    mEarpieceEndpoint.identifier,
                )

            // 1. Platform starts video call on SPEAKER
            callSession.onCallEndpointChanged(platformSpeaker)
            yield()

            // 2. Core-Telecom requests EARPIECE, and platform switches to EARPIECE
            callSession.mLastClientRequestedEndpoint = mEarpieceEndpoint
            callSession.onCallEndpointChanged(platformEarpiece)
            yield()

            // At this point, the route is EARPIECE and mLastClientRequestedEndpoint is cleared
            assertNull(callSession.mLastClientRequestedEndpoint)

            // 3. Platform's delayed speaker echo arrives (~1s later in real world)
            callSession.onCallEndpointChanged(platformSpeaker)
            yield()

            // 4. Assert: avoidSpeakerOverrideOnCallStart must NOT have been disarmed by the
            // earlier SPEAKER -> EARPIECE transition, and must request a switch back to EARPIECE!
            assertEquals(
                "Delayed speaker echo must be caught and reverted back to earpiece",
                CallEndpointCompat.TYPE_EARPIECE,
                callSession.mLastClientRequestedEndpoint?.type,
            )

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that when the call-start stabilization timer expires:
     * 1. The preferred starting endpoint is cleared (set to null).
     * 2. Any subsequent switch to SPEAKER (e.g. via System UI) is NOT overridden.
     */
    @SmallTest
    @Test
    fun testAvoidSpeakerOverride_stabilizationTimerExpires_disarmsGuard() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes =
                CallAttributesCompat(
                    TestUtils.OUTGOING_NAME,
                    TestUtils.TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_OUTGOING,
                    CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                    TestUtils.ALL_CALL_CAPABILITIES,
                )
            val callSession = initCallSession(coroutineContext, callChannels, attributes)
            callSession.setAvailableCallEndpoints(listOf(mEarpieceEndpoint, mSpeakerEndpoint))

            callSession.mPreferredStartingCallEndpoint = mEarpieceEndpoint
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)

            // Start the non-blocking stabilization timer
            callSession.startPreferredEndpointStabilizationTimer()

            // Advance time past the 3-second stabilization window
            delay(3500)

            // 1. Preferred starting endpoint should be disarmed (cleared to null)
            assertNull(
                "Preferred starting endpoint should be cleared once stabilization timer expires",
                callSession.mPreferredStartingCallEndpoint,
            )

            // 2. User or System UI now switches to SPEAKER mid-call
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    mSpeakerEndpoint.identifier,
                )
            callSession.onCallEndpointChanged(platformSpeaker)
            yield()

            // 3. Assert: Core-Telecom must NOT try to switch back to EARPIECE
            assertNull(
                "Mid-call switch to speaker after timer expires must not be overridden",
                callSession.mLastClientRequestedEndpoint,
            )

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that different Bluetooth devices (e.g. a Watch vs. a Headset) are NOT treated as
     * equivalent by avoidSpeakerOverrideOnCallStart, even though both share TYPE_BLUETOOTH.
     */
    @SmallTest
    @Test
    fun testAvoidSpeakerOverride_watchVsHeadset_doesNotConflateBluetoothEndpoints() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes =
                CallAttributesCompat(
                    TestUtils.OUTGOING_NAME,
                    TestUtils.TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_OUTGOING,
                    CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                    TestUtils.ALL_CALL_CAPABILITIES,
                )
            val callSession = initCallSession(coroutineContext, callChannels, attributes)
            callSession.setAvailableCallEndpoints(
                listOf(mEarpieceEndpoint, mSpeakerEndpoint, mBluetoothEndpoint, mWatchEndpoint)
            )

            // User preferred BLUETOOTH headset (e.g. Pixel Buds)
            callSession.mPreferredStartingCallEndpoint = mBluetoothEndpoint

            // Call was active on WATCH (a different Bluetooth device)
            callSession.setCurrentCallEndpoint(mWatchEndpoint)

            // Platform transitions from WATCH to SPEAKER
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    mSpeakerEndpoint.identifier,
                )
            callSession.onCallEndpointChanged(platformSpeaker)
            yield()

            // Assert: Because WATCH is NOT the preferred BLUETOOTH headset,
            // avoidSpeakerOverrideOnCallStart must NOT treat them as equal based on type,
            // and must NOT trigger a reversion.
            assertNull(
                "Different Bluetooth endpoints must not match on type alone",
                callSession.mLastClientRequestedEndpoint,
            )

            callChannels.closeAllChannels()
        }
    }
}
