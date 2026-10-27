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

import android.media.ToneGenerator
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.ParcelUuid
import android.telecom.CallAudioState
import android.telecom.CallAudioState.ROUTE_EARPIECE
import android.telecom.CallAudioState.ROUTE_WIRED_HEADSET
import android.telecom.CallEndpoint
import android.telecom.DisconnectCause
import androidx.core.telecom.CallControlResult
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    /**
     * Test that if a call is upgraded to a video call mid-session, disconnecting the headset will
     * fall back to the speakerphone instead of the earpiece.
     */
    @SmallTest
    @Test
    fun testHeadsetDisconnectDefaultsToSpeakerWhenUpgradedToVideoCall() {
        runBlocking {
            val callSession = initCallSessionLegacy(coroutineContext, null)

            // 1. Start on wired headset
            val initialSupportedRouteMask =
                CallAudioState.ROUTE_EARPIECE or
                    CallAudioState.ROUTE_SPEAKER or
                    CallAudioState.ROUTE_WIRED_HEADSET
            callSession.onCallAudioStateChanged(
                CallAudioState(false, CallAudioState.ROUTE_WIRED_HEADSET, initialSupportedRouteMask)
            )

            callSession.mLastClientRequestedEndpoint = null

            // 2. Simulate call upgrade to video
            callSession.requestVideoState(
                androidx.core.telecom.CallAttributesCompat.CALL_TYPE_VIDEO_CALL
            )

            // 3. Simulate wired headset disconnecting and platform defaulting to EARPIECE
            val disconnectedRouteMask =
                CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
            callSession.onCallAudioStateChanged(
                CallAudioState(false, CallAudioState.ROUTE_EARPIECE, disconnectedRouteMask)
            )

            kotlinx.coroutines.yield()

            // Verify a change to speaker endpoint was requested
            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                callSession.mLastClientRequestedEndpoint?.type,
            )
        }
    }

    /**
     * Verify that on SDK < 38, calling setConnectionDisconnect with DisconnectCause.ERROR maps the
     * cause to DisconnectCause.LOCAL while preserving metadata, and on SDK >= 38 retains
     * DisconnectCause.ERROR.
     */
    @SmallTest
    @Test
    fun testDisconnect_remapsErrorToLocal() {
        runBlocking {
            val callSession = initCallSessionLegacy(coroutineContext, null)
            val errorCause =
                DisconnectCause(
                    DisconnectCause.ERROR,
                    "label",
                    "description",
                    "reason",
                    ToneGenerator.TONE_PROP_BEEP,
                )
            callSession.setConnectionDisconnect(errorCause)
            val disconnectCause = callSession.disconnectCause
            assertNotNull("DisconnectCause on Connection should not be null", disconnectCause)
            if (Build.VERSION.SDK_INT < 38) {
                assertEquals(
                    "DisconnectCause should be remapped to LOCAL",
                    DisconnectCause.LOCAL,
                    disconnectCause.code,
                )
            } else {
                assertEquals(
                    "DisconnectCause should remain ERROR",
                    DisconnectCause.ERROR,
                    disconnectCause.code,
                )
            }
            assertEquals("label", disconnectCause.label)
            assertEquals("description", disconnectCause.description)
            assertEquals("reason", disconnectCause.reason)
            assertEquals(ToneGenerator.TONE_PROP_BEEP, disconnectCause.tone)
        }
    }

    /**
     * Verify that when the platform invokes [CallSessionLegacy.onAbort], the disconnect callback is
     * invoked with [DisconnectCause.CANCELED] to match the transactional [CallSession] path.
     */
    @SmallTest
    @Test
    fun testOnAbort_invokesOnDisconnectWithCanceled() {
        runBlocking {
            var receivedCause: DisconnectCause? = null
            val blockingSessionExecution = CompletableDeferred<Unit>()
            val callSession =
                CallSessionLegacy(
                    getRandomParcelUuid(),
                    mContext,
                    TestUtils.INCOMING_CALL_ATTRIBUTES,
                    CallChannels(),
                    coroutineContext,
                    TestUtils.mOnAnswerLambda,
                    { cause -> receivedCause = cause },
                    TestUtils.mOnSetActiveLambda,
                    TestUtils.mOnSetInActiveLambda,
                    { _, _ -> },
                    MutableSharedFlow(),
                    null,
                    blockingSessionExecution,
                )

            callSession.onAbort()
            blockingSessionExecution.await()

            assertNotNull("onDisconnectCallback should be invoked on abort", receivedCause)
            assertEquals(DisconnectCause.CANCELED, receivedCause?.code)
            assertEquals(DisconnectCause.CANCELED, callSession.disconnectCause?.code)
            callSession.close()
        }
    }

    /**
     * Verify that seeded endpoints from AudioManager are normalized to match platform
     * CallAudioState endpoint names and deduplicated (e.g. when a USB headset reports multiple
     * AudioDeviceInfo types).
     */
    @SmallTest
    @Test
    fun testNormalizeSeededEndpoints_matchesPlatformNamesAndDeduplicates() {
        runBlocking {
            val callSession = initCallSessionLegacy(coroutineContext, null)
            val rawEndpoints =
                listOf(
                    CallEndpointCompat(
                        "Earpiece",
                        CallEndpointCompat.TYPE_EARPIECE,
                        CallEndpointUuidTracker.EARPIECE_UUID,
                    ),
                    CallEndpointCompat(
                        "Speaker",
                        CallEndpointCompat.TYPE_SPEAKER,
                        CallEndpointUuidTracker.SPEAKER_UUID,
                    ),
                    CallEndpointCompat(
                        "Wired Headset",
                        CallEndpointCompat.TYPE_WIRED_HEADSET,
                        CallEndpointUuidTracker.WIRED_HEADSET_UUID,
                    ),
                    CallEndpointCompat(
                        "Wired Headset",
                        CallEndpointCompat.TYPE_WIRED_HEADSET,
                        CallEndpointUuidTracker.WIRED_HEADSET_UUID,
                    ),
                )

            val normalized = callSession.normalizeSeededEndpoints(rawEndpoints)
            assertEquals(3, normalized.size)
            assertEquals(
                CallEndpointCompat(
                    "WIRED_HEADSET",
                    CallEndpointCompat.TYPE_WIRED_HEADSET,
                    CallEndpointUuidTracker.WIRED_HEADSET_UUID,
                ),
                normalized[0],
            )
            assertEquals(mSpeakerEndpoint, normalized[1])
            assertEquals(mEarpieceEndpoint, normalized[2])
            callSession.close()
        }
    }

    /**
     * Verify that seedInitialEndpointsFromAudioManagerIfNeeded does not overwrite endpoints once
     * onCallAudioStateChanged has already delivered the platform CallAudioState.
     */
    @SmallTest
    @Test
    fun testSeedInitialEndpoints_doesNotOverwritePlatformCallAudioState() {
        runBlocking {
            val callSession = initCallSessionLegacy(coroutineContext, null)
            val supportedRouteMask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
            callSession.onCallAudioStateChanged(
                CallAudioState(false, CallAudioState.ROUTE_SPEAKER, supportedRouteMask)
            )
            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                callSession.getCurrentCallEndpointForSession()?.type,
            )

            callSession.seedInitialEndpointsFromAudioManagerIfNeeded()
            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                callSession.getCurrentCallEndpointForSession()?.type,
            )
            callSession.close()
        }
    }

    /**
     * Verify that on API 28+, requesting a Bluetooth endpoint when the Bluetooth device cache is
     * still empty (before onCallAudioStateChanged arrives) falls back to ROUTE_BLUETOOTH and
     * returns Success instead of ERROR_BLUETOOTH_DEVICE_IS_NULL.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.P)
    @SmallTest
    @Test
    fun testSetAudio_bluetoothWithEmptyCache_fallsBackToRouteBluetooth() {
        runBlocking {
            val callSession = initCallSessionLegacy(coroutineContext, null)
            val btEndpoint =
                CallEndpointCompat(
                    "Pixel Buds",
                    CallEndpointCompat.TYPE_BLUETOOTH,
                    getRandomParcelUuid(),
                )
            val result =
                CallSessionLegacy.Api28PlusImpl.setAudio(btEndpoint, callSession, ArrayList())
            assertEquals(CallControlResult.Success(), result)
            callSession.close()
        }
    }

    /**
     * Verify that when onCallAudioStateChanged is not invoked by the platform within the initial
     * window, the fallback timer seeds and emits normalized endpoints to availableEndpointChannel
     * only (the current endpoint is left unseeded), and that the platform CallAudioState takes over
     * when it arrives later.
     */
    @SmallTest
    @Test
    fun testFallbackEndpointSeedTimer_emitsToChannelsAndTransitionsToPlatformState() {
        runBlocking {
            val callChannels = CallChannels()
            val callSession =
                CallSessionLegacy(
                    getRandomParcelUuid(),
                    mContext,
                    TestUtils.INCOMING_CALL_ATTRIBUTES,
                    callChannels,
                    coroutineContext,
                    TestUtils.mOnAnswerLambda,
                    TestUtils.mOnDisconnectLambda,
                    TestUtils.mOnSetActiveLambda,
                    TestUtils.mOnSetInActiveLambda,
                    { _, _ -> },
                    MutableSharedFlow(),
                    null,
                    CompletableDeferred(Unit),
                )

            // 1. Without calling onCallAudioStateChanged, wait for the fallback timer in init to
            // emit seeded available endpoints from AudioManager.
            val seededAvailable =
                withTimeout(2000L) {
                    callChannels.availableEndpointChannel.receiveAsFlow().first { it.isNotEmpty() }
                }

            assertTrue(
                "Seeded available endpoints should not be empty",
                seededAvailable.isNotEmpty(),
            )
            for (endpoint in seededAvailable) {
                if (!endpoint.isBluetoothType()) {
                    assertEquals(
                        EndpointUtils.endpointTypeToString(endpoint.type),
                        endpoint.name.toString(),
                    )
                }
            }
            // The current endpoint cannot be reliably inferred, so it must NOT be seeded.
            assertNull(
                "Current endpoint must not be seeded from AudioManager",
                callChannels.currentEndpointChannel.tryReceive().getOrNull(),
            )
            assertNull(callSession.getCurrentCallEndpointForSession())

            // 2. Now simulate platform onCallAudioStateChanged arriving later with WIRED_HEADSET.
            val supportedRouteMask = ROUTE_WIRED_HEADSET or CallAudioState.ROUTE_SPEAKER
            callSession.onCallAudioStateChanged(
                CallAudioState(false, ROUTE_WIRED_HEADSET, supportedRouteMask)
            )

            val platformCurrent =
                withTimeout(2000L) {
                    callChannels.currentEndpointChannel.receiveAsFlow().first {
                        it.type == CallEndpointCompat.TYPE_WIRED_HEADSET
                    }
                }
            assertEquals(CallEndpointCompat.TYPE_WIRED_HEADSET, platformCurrent.type)

            callSession.close()
            callChannels.closeAllChannels()
        }
    }
}
