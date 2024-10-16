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
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallEndpointUuidTracker
import androidx.core.telecom.internal.CallSessionLegacy
import androidx.core.telecom.internal.utils.EndpointUtils
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@RequiresApi(VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class CallSessionLegacyTest : BaseTelecomTest() {
    val mSessionId: Int = 444

    @After
    fun tearDown() {
        CallEndpointUuidTracker.endSession(mSessionId)
    }

    /**
     * Verify the [CallEndpoint]s echoed from the platform are re-mapped to the existing
     * [CallEndpointCompat]s the user received with
     * [androidx.core.telecom.CallsManager#getAvailableStartingCallEndpoints()]
     */
    @SmallTest
    @Test
    fun testPlatformEndpointsAreRemappedToExistingEndpoints() {
        setUpBackwardsCompatTest()
        runBlocking {
            val callSession =
                initCallSessionLegacy(
                    coroutineContext,
                    null,
                )
            val supportedRouteMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER

            val platformEndpoints =
                EndpointUtils.toCallEndpointsCompat(
                    CallAudioState(false, CallAudioState.ROUTE_EARPIECE, supportedRouteMask),
                    mSessionId
                )

            val platformEarpiece = platformEndpoints[0]
            assertEquals(CallEndpointCompat.TYPE_EARPIECE, platformEarpiece.type)
            assertEquals(
                mEarpieceEndpoint,
                callSession.toRemappedCallEndpointCompat(platformEarpiece)
            )

            val platformSpeaker = platformEndpoints[1]
            assertEquals(CallEndpointCompat.TYPE_SPEAKER, platformSpeaker.type)
            assertEquals(
                mSpeakerEndpoint,
                callSession.toRemappedCallEndpointCompat(platformSpeaker)
            )
        }
    }

    /**
     * Ensure that if the platform returns a null active bluetooth device, the jetpack layer does
     * not crash the client application or destroy the call session
     */
    @SmallTest
    @Test
    fun testOnCallAudioStateChangedWithNullActiveDevice() {
        setUpBackwardsCompatTest()
        runBlocking {
            val callSession =
                initCallSessionLegacy(
                    coroutineContext,
                    null,
                )

            val supportedRouteMask =
                CallAudioState.ROUTE_BLUETOOTH or
                    CallAudioState.ROUTE_WIRED_HEADSET or
                    CallAudioState.ROUTE_SPEAKER

            val cas = CallAudioState(false, CallAudioState.ROUTE_BLUETOOTH, supportedRouteMask)

            callSession.onCallAudioStateChanged(cas)

            val currentCallEndpoint = callSession.getCurrentCallEndpointForSession()
            assertNotNull(currentCallEndpoint)
            assertEquals(CallEndpointCompat.TYPE_BLUETOOTH, currentCallEndpoint!!.type)
            assertEquals(
                EndpointUtils.endpointTypeToString(CallEndpointCompat.TYPE_BLUETOOTH),
                currentCallEndpoint.name
            )
        }
    }

    private fun initCallSessionLegacy(
        coroutineContext: CoroutineContext,
        preferredStartingEndpoint: CallEndpointCompat?,
    ): CallSessionLegacy {
        return CallSessionLegacy(
            getRandomParcelUuid(),
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
}
