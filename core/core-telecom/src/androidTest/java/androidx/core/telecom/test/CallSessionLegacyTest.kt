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
import android.telecom.CallAudioState.ROUTE_EARPIECE
import android.telecom.CallAudioState.ROUTE_WIRED_HEADSET
import android.telecom.CallEndpoint
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@RunWith(AndroidJUnit4::class)
class CallSessionLegacyTest : BaseTelecomTest() {
    val mSessionId: Int = 444

    @After
    fun tearDown() {
        CallEndpointUuidTracker.endSession(mSessionId)
    }

    /**
     * Verify that if the MAC address is not set for the [CallEndpointCompat], the name is used
     * instead to determine if the two devices are the same
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.P)
    @SmallTest
    @Test
    fun testMatchingOnEndpointName() {
        setUpBackwardsCompatTest()
        runBlocking {
            // Represent a BluetoothDevice since the object cannot be mocked
            val btDeviceName = "Pixel Buds"
            val btDeviceAddress = "abcd"
            // Mirror the platform CallEndpointCompat that is created in the jetpack layer
            val endpoint =
                CallEndpointCompat(
                    btDeviceName,
                    CallEndpointCompat.TYPE_BLUETOOTH,
                    ParcelUuid.fromString(UUID.randomUUID().toString()),
                )
            // verify the matching function evaluates that as equal even though the MAC
            // address was not set in the CallEndpointCompat
            assertTrue(
                CallSessionLegacy.Api28PlusImpl.bluetoothDeviceMatchesEndpoint(
                    btName = btDeviceName,
                    btAddress = btDeviceAddress,
                    endpoint,
                )
            )
        }
    }

    /**
     * Verify that if the MAC address is populated for two devices but the name is the same, the
     * devices are not equal
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.P)
    @SmallTest
    @Test
    fun testMatchingOnEndpointNameWithDifferentAddresses() {
        setUpBackwardsCompatTest()
        runBlocking {
            // Represent a BluetoothDevice since the object cannot be mocked
            val btDeviceName = "Pixel Buds"
            val btDeviceAddress = "abcd"
            // create an endpoint with the same name but DIFFERENT MAC addresses
            val endpoint =
                CallEndpointCompat(
                    btDeviceName,
                    CallEndpointCompat.TYPE_BLUETOOTH,
                    ParcelUuid.fromString(UUID.randomUUID().toString()),
                )
            endpoint.mMackAddress = "1234"
            // assert different MAC addresses
            assertNotEquals(btDeviceAddress, endpoint.mMackAddress)
            // assert the endpoints do not match
            assertFalse(
                CallSessionLegacy.Api28PlusImpl.bluetoothDeviceMatchesEndpoint(
                    btName = btDeviceName,
                    btAddress = btDeviceAddress,
                    endpoint,
                )
            )
        }
    }

    /**
     * Test the setter for available endpoints removes the earpiece endpoint if the wired headset
     * endpoint is available
     */
    @SmallTest
    @Test
    fun testRemovalOfEarpieceEndpointIfWiredEndpointIsPresent() {
        setUpBackwardsCompatTest()
        runBlocking {
            val callSession = initCallSessionLegacy(coroutineContext, null)
            val supportedRouteMask = ROUTE_EARPIECE or ROUTE_WIRED_HEADSET
            callSession.setAvailableCallEndpoints(
                CallAudioState(false, ROUTE_WIRED_HEADSET, supportedRouteMask)
            )
            val res = callSession.getAvailableCallEndpointsForSession()
            assertEquals(1, res.size)
            assertEquals(res[0].type, CallEndpointCompat.TYPE_WIRED_HEADSET)
        }
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
            val callSession = initCallSessionLegacy(coroutineContext, null)
            val supportedRouteMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER

            val platformEndpoints =
                EndpointUtils.toCallEndpointsCompat(
                    CallAudioState(false, CallAudioState.ROUTE_EARPIECE, supportedRouteMask),
                    mSessionId,
                )

            val platformEarpiece = platformEndpoints[0]
            assertEquals(CallEndpointCompat.TYPE_EARPIECE, platformEarpiece.type)
            assertEquals(
                mEarpieceEndpoint,
                callSession.toRemappedCallEndpointCompat(platformEarpiece),
            )

            val platformSpeaker = platformEndpoints[1]
            assertEquals(CallEndpointCompat.TYPE_SPEAKER, platformSpeaker.type)
            assertEquals(
                mSpeakerEndpoint,
                callSession.toRemappedCallEndpointCompat(platformSpeaker),
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
            val callSession = initCallSessionLegacy(coroutineContext, null)

            val supportedRouteMask =
                CallAudioState.ROUTE_BLUETOOTH or
                    ROUTE_WIRED_HEADSET or
                    CallAudioState.ROUTE_SPEAKER

            val cas = CallAudioState(false, CallAudioState.ROUTE_BLUETOOTH, supportedRouteMask)

            callSession.onCallAudioStateChanged(cas)

            val currentCallEndpoint = callSession.getCurrentCallEndpointForSession()
            assertNotNull(currentCallEndpoint)
            assertEquals(CallEndpointCompat.TYPE_BLUETOOTH, currentCallEndpoint!!.type)
            assertEquals(
                EndpointUtils.endpointTypeToString(CallEndpointCompat.TYPE_BLUETOOTH),
                currentCallEndpoint.name,
            )
        }
    }

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
}
