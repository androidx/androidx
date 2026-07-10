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
}
