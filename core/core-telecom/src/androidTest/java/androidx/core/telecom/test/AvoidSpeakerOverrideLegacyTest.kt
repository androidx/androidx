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
}
