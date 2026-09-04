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
import android.os.ParcelUuid
import android.telecom.CallAudioState
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSessionLegacy
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.util.UUID
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

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class AvoidSpeakerOverrideLegacyTest : BaseTelecomTest() {

    private fun initCallSessionLegacy(
        coroutineContext: CoroutineContext,
        preferredStartingEndpoint: CallEndpointCompat?,
    ): CallSessionLegacy {
        return CallSessionLegacy(
            getRandomParcelUuid(),
            mContext,
            TestUtils.INCOMING_CALL_ATTRIBUTES,
            CallChannels(),
            coroutineContext,
            TestUtils.mOnAnswerLambda,
            TestUtils.mOnDisconnectLambda,
            TestUtils.mOnSetActiveLambda,
            TestUtils.mOnSetInActiveLambda,
            { _, _ -> },
            MutableSharedFlow(),
            preferredStartingEndpoint,
            CompletableDeferred(Unit),
        )
    }

    private fun getRandomParcelUuid(): ParcelUuid {
        return ParcelUuid.fromString(UUID.randomUUID().toString())
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
            val callSession = initCallSessionLegacy(coroutineContext, mEarpieceEndpoint)

            // Simulate user explicitly requesting speaker
            callSession.mLastClientRequestedEndpoint = mSpeakerEndpoint
            callSession.mAlreadyRequestedStartingEndpointSwitch = true

            // Simulate first platform update to SPEAKER
            val supportedRouteMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
            val cas = CallAudioState(false, CallAudioState.ROUTE_SPEAKER, supportedRouteMask)

            // Act: call the change
            callSession.onCallAudioStateChanged(cas)

            // Wait for coroutines
            yield()

            // Assert: mLastClientRequestedEndpoint is cleared (confirmed) and NO new request was
            // made
            assertNull(
                "Reversion should not have occurred",
                callSession.mLastClientRequestedEndpoint,
            )
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
            val callSession = initCallSessionLegacy(coroutineContext, mEarpieceEndpoint)

            // Initial state is EARPIECE
            val supportedRouteMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
            val initialCas =
                CallAudioState(false, CallAudioState.ROUTE_EARPIECE, supportedRouteMask)
            callSession.onCallAudioStateChanged(initialCas)

            // Simulate platform unexpectedly switching to SPEAKER
            val overrideCas =
                CallAudioState(false, CallAudioState.ROUTE_SPEAKER, supportedRouteMask)

            // Act
            callSession.onCallAudioStateChanged(overrideCas)

            // Wait for coroutines
            yield()

            // Assert: A reversion request to EARPIECE should have been made
            assertEquals(
                "Should have reverted to earpiece",
                CallEndpointCompat.TYPE_EARPIECE,
                callSession.mLastClientRequestedEndpoint?.type,
            )
        }
    }

    /**
     * Verifies that if platform Telecom starts on SPEAKER, Core-Telecom reverts to EARPIECE, and
     * then a delayed speaker switch event arrives from the platform,
     * avoidSpeakerOverrideOnCallStart catches it and reverts back to EARPIECE.
     */
    @SmallTest
    @Test
    fun testAvoidSpeakerOverride_DelayedEchoToSpeaker_StillReverts() {
        runBlocking {
            val callSession = initCallSessionLegacy(coroutineContext, mEarpieceEndpoint)
            val supportedRouteMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER

            val platformSpeaker =
                CallAudioState(false, CallAudioState.ROUTE_SPEAKER, supportedRouteMask)
            val platformEarpiece =
                CallAudioState(false, CallAudioState.ROUTE_EARPIECE, supportedRouteMask)

            // 1. Platform starts call on SPEAKER
            callSession.onCallAudioStateChanged(platformSpeaker)
            yield()

            // 2. Core-Telecom requested EARPIECE, and platform switches to EARPIECE
            callSession.mLastClientRequestedEndpoint = mEarpieceEndpoint
            callSession.onCallAudioStateChanged(platformEarpiece)
            yield()

            // At this point, the route is EARPIECE and mLastClientRequestedEndpoint is cleared
            assertNull(callSession.mLastClientRequestedEndpoint)

            // 3. Platform's delayed speaker echo arrives (~1s later in real world)
            callSession.onCallAudioStateChanged(platformSpeaker)
            yield()

            // 4. Assert: avoidSpeakerOverrideOnCallStart must NOT have been disarmed by the
            // earlier SPEAKER -> EARPIECE transition, and must request a switch back to EARPIECE!
            assertEquals(
                "Delayed speaker echo must be caught and reverted back to earpiece",
                CallEndpointCompat.TYPE_EARPIECE,
                callSession.mLastClientRequestedEndpoint?.type,
            )
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
            val callSession = initCallSessionLegacy(coroutineContext, mEarpieceEndpoint)
            val supportedRouteMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER

            val platformEarpiece =
                CallAudioState(false, CallAudioState.ROUTE_EARPIECE, supportedRouteMask)
            callSession.onCallAudioStateChanged(platformEarpiece)
            yield()
            // Platform confirms EARPIECE route, clearing in-flight request
            callSession.onCallAudioStateChanged(platformEarpiece)
            yield()
            assertNull(callSession.mLastClientRequestedEndpoint)

            // Start the non-blocking stabilization timer
            callSession.startPreferredEndpointStabilizationTimer()

            // Advance time past the 3-second stabilization window
            delay(3500)

            // 1. Preferred starting endpoint should be disarmed (cleared to null)
            assertNull(
                "Preferred starting endpoint should be cleared once stabilization timer expires",
                callSession.preferredStartingCallEndpoint,
            )

            // 2. User or System UI now switches to SPEAKER mid-call
            val platformSpeaker =
                CallAudioState(false, CallAudioState.ROUTE_SPEAKER, supportedRouteMask)
            callSession.onCallAudioStateChanged(platformSpeaker)
            yield()

            // 3. Assert: Core-Telecom must NOT try to switch back to EARPIECE
            assertNull(
                "Mid-call switch to speaker after timer expires must not be overridden",
                callSession.mLastClientRequestedEndpoint,
            )
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
            val callSession = initCallSessionLegacy(coroutineContext, mBluetoothEndpoint)

            // User preferred BLUETOOTH headset (e.g. Pixel Buds)
            // Call was active on WATCH (a different Bluetooth device)
            // Transition from WATCH to SPEAKER is evaluated
            callSession.avoidSpeakerOverrideOnCallStart(
                prevEndpoint = mWatchEndpoint,
                nextEndpoint = mSpeakerEndpoint,
            )
            yield()

            // Assert: Because WATCH is NOT the preferred BLUETOOTH headset,
            // avoidSpeakerOverrideOnCallStart must NOT treat them as equal based on type,
            // and must NOT trigger a reversion.
            assertNull(
                "Different Bluetooth endpoints must not match on type alone",
                callSession.mLastClientRequestedEndpoint,
            )
        }
    }
}
