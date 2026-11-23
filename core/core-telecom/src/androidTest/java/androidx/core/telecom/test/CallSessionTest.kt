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
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.ParcelUuid
import android.telecom.CallEndpoint
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
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
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
        attributes: CallAttributesCompat = TestUtils.INCOMING_CALL_ATTRIBUTES,
    ): CallSession {
        return CallSession(
            FakeBluetoothDeviceChecker(),
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

    private fun createAudioCallAttributes(): CallAttributesCompat {
        return CallAttributesCompat(
            OUTGOING_NAME,
            TEST_ADDRESS,
            CallAttributesCompat.DIRECTION_OUTGOING,
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            CallAttributesCompat.SUPPORTS_STREAM,
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

    /**
     * Verifies that when the platform unexpectedly upgrades an audio call to a video call, the
     * Jetpack layer intercepts this, remains in an audio call state, and forces the audio route
     * back to the earpiece.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.VANILLA_ICE_CREAM, maxSdkVersion = 36)
    @SmallTest
    @Test
    fun testUnrequestedVideoStateUpgrade_AudioCall() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes = createAudioCallAttributes()
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Set initial state
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            callSession.setCurrentCallEndpoint(mSpeakerEndpoint)

            // Wait for coroutines to execute
            yield()

            // Simulate platform upgrading to video unexpectedly
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)

            // Wait for coroutines to execute
            yield()

            val lastEndpoint = callSession.mLastClientRequestedEndpoint

            // Should fall back to EARPIECE
            assertNotNull(lastEndpoint)
            assertEquals(CallEndpointCompat.TYPE_EARPIECE, lastEndpoint?.type)

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that if the user explicitly requested the speaker endpoint, the fallback to earpiece
     * does not occur even during an unrequested video upgrade.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.VANILLA_ICE_CREAM, maxSdkVersion = 36)
    @SmallTest
    @Test
    fun testUnrequestedVideoStateUpgrade_UserRequestedSpeaker() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes = createAudioCallAttributes()
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Set initial state
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            callSession.setCurrentCallEndpoint(mSpeakerEndpoint)

            // Simulate user explicitly requesting speaker
            callSession.mLastClientRequestedEndpoint = mSpeakerEndpoint

            // Simulate platform upgrading to video unexpectedly
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)

            // Wait for coroutines to execute
            yield()

            // Should NOT fallback to EARPIECE because user requested speaker
            val lastEndpoint = callSession.mLastClientRequestedEndpoint
            assertEquals(CallEndpointCompat.TYPE_SPEAKER, lastEndpoint?.type)

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that if the application initiates the video state upgrade, the fallback logic is not
     * triggered.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.VANILLA_ICE_CREAM)
    @SmallTest
    @Test
    fun testAppInitiatedVideoStateUpgrade_DoesNotFallback() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes = createAudioCallAttributes()
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Set initial state
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            callSession.setCurrentCallEndpoint(mSpeakerEndpoint)

            // App requests video upgrade
            callSession.requestVideoState(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)

            // Simulate platform upgrading to video as expected
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)

            // Wait for coroutines to execute
            yield()

            // Tracking should be false since it was app initiated
            assertFalse(callSession.mUnrequestedVideoManager.mTrackingUnrequestedVideoStateUpgrade)

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that if the platform upgrades the video state without request, and subsequently
     * changes the audio route to SPEAKER, the Jetpack layer catches it and forces the route back to
     * EARPIECE.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.VANILLA_ICE_CREAM, maxSdkVersion = 36)
    @SmallTest
    @Test
    fun testUnrequestedVideoStateUpgrade_EndpointChangedToSpeakerAfterwards() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes = createAudioCallAttributes()
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Set initial state
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            callSession.setCurrentCallEndpoint(mEarpieceEndpoint)

            // Wait for coroutines to execute
            yield()

            // Simulate first audio state flow emission
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)

            // Simulate platform upgrading to video unexpectedly
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)

            // Now platform changes endpoint to SPEAKER
            val platformSpeaker =
                CallEndpoint(
                    mSpeakerEndpoint.name,
                    CallEndpoint.TYPE_SPEAKER,
                    getRandomParcelUuid(),
                )
            callSession.onCallEndpointChanged(platformSpeaker)

            // Wait for coroutines to execute
            yield()

            // Should fall back to EARPIECE
            val reqEndpoint = callSession.mLastClientRequestedEndpoint
            assertEquals(CallEndpointCompat.TYPE_EARPIECE, reqEndpoint?.type)

            // Tracking should be cleared
            assertFalse(callSession.mUnrequestedVideoManager.mTrackingUnrequestedVideoStateUpgrade)

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that [CallSession.launchStartingEndpointSwitch] launches asynchronously in the
     * background so call setup can return immediately without waiting for starting endpoint
     * resolution.
     */
    @Test
    fun testLaunchStartingEndpointSwitch_runsAsynchronouslyWithoutBlocking() {
        runBlocking {
            val callChannels = CallChannels()
            val attributes = createAudioCallAttributes()
            val callSession = initCallSession(coroutineContext, callChannels, attributes)

            // Launch starting endpoint switch with a preferred endpoint.
            // Because mIsAvailableEndpointsSet is NOT completed yet, this job is GUARANTEED to stay
            // suspended at awaitAll() without finishing!
            callSession.launchStartingEndpointSwitch(mEarpieceEndpoint)

            // Verify that launchStartingEndpointSwitch returned immediately and
            // mStartingEndpointJob
            // is actively running in the background.
            val audioJob = callSession.mStartingEndpointJob
            assertNotNull("mStartingEndpointJob should have been launched", audioJob)
            assertTrue(
                "launchStartingEndpointSwitch must return immediately while audio setup job suspends in background!",
                audioJob!!.isActive,
            )

            // Cleanup
            audioJob.cancel()
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that when the platform invokes [CallSession.onDisconnect] with
     * [DisconnectCause.UNKNOWN] (e.g., on API 34-36 during local Call.disconnect()), the Jetpack
     * layer normalizes the cause code to [DisconnectCause.LOCAL] to match [CallSessionLegacy] while
     * preserving metadata.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testOnDisconnect_remapsUnknownToLocal() {
        runBlocking {
            val callChannels = CallChannels()
            var receivedCause: DisconnectCause? = null
            val completionDeferred = CompletableDeferred<Boolean>()
            val callSession =
                CallSession(
                    FakeBluetoothDeviceChecker(),
                    coroutineContext,
                    TestUtils.INCOMING_CALL_ATTRIBUTES,
                    TestUtils.mOnAnswerLambda,
                    { cause -> receivedCause = cause },
                    TestUtils.mOnSetActiveLambda,
                    TestUtils.mOnSetInActiveLambda,
                    callChannels,
                    MutableSharedFlow(),
                    { _, _ -> },
                    CompletableDeferred(Unit),
                )

            val unknownCause =
                DisconnectCause(
                    DisconnectCause.UNKNOWN,
                    "label",
                    "description",
                    "reason",
                    ToneGenerator.TONE_PROP_BEEP,
                )
            callSession.onDisconnect(unknownCause) { completed ->
                completionDeferred.complete(completed)
            }

            assertTrue(completionDeferred.await())
            assertNotNull(receivedCause)
            assertEquals(DisconnectCause.LOCAL, receivedCause?.code)
            assertEquals("label", receivedCause?.label)
            assertEquals("description", receivedCause?.description)
            assertEquals("reason", receivedCause?.reason)
            assertEquals(ToneGenerator.TONE_PROP_BEEP, receivedCause?.tone)
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that when the platform invokes [CallSession.onDisconnect] with an explicit
     * non-UNKNOWN [DisconnectCause] (such as [DisconnectCause.REJECTED]), the cause is passed
     * through unchanged.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testOnDisconnect_preservesNonUnknownDisconnectCause() {
        runBlocking {
            val callChannels = CallChannels()
            var receivedCause: DisconnectCause? = null
            val completionDeferred = CompletableDeferred<Boolean>()
            val callSession =
                CallSession(
                    FakeBluetoothDeviceChecker(),
                    coroutineContext,
                    TestUtils.INCOMING_CALL_ATTRIBUTES,
                    TestUtils.mOnAnswerLambda,
                    { cause -> receivedCause = cause },
                    TestUtils.mOnSetActiveLambda,
                    TestUtils.mOnSetInActiveLambda,
                    callChannels,
                    MutableSharedFlow(),
                    { _, _ -> },
                    CompletableDeferred(Unit),
                )

            val rejectedCause =
                DisconnectCause(
                    DisconnectCause.REJECTED,
                    "label",
                    "description",
                    "reason",
                    ToneGenerator.TONE_PROP_BEEP,
                )
            callSession.onDisconnect(rejectedCause) { completed ->
                completionDeferred.complete(completed)
            }

            assertTrue(completionDeferred.await())
            assertNotNull(receivedCause)
            assertEquals(DisconnectCause.REJECTED, receivedCause?.code)
            assertEquals("label", receivedCause?.label)
            assertEquals("description", receivedCause?.description)
            assertEquals("reason", receivedCause?.reason)
            assertEquals(ToneGenerator.TONE_PROP_BEEP, receivedCause?.tone)
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that when the platform answers an incoming video call via [CallSession.onAnswer]
     * with [VideoProfile.STATE_BIDIRECTIONAL] and immediately emits an unrequested
     * [CallAttributesCompat.CALL_TYPE_AUDIO_CALL] downgrade, [CallSession] suppresses the bogus
     * downgrade, keeps video call state, and enforces speaker fallback if routed to earpiece.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testUnrequestedVideoStateDowngrade_AfterPlatformVideoAnswer() {
        runBlocking {
            val callChannels = CallChannels()
            val incomingVideoAttributes =
                CallAttributesCompat(
                    OUTGOING_NAME,
                    TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_INCOMING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    CallAttributesCompat.SUPPORTS_STREAM,
                )
            val callSession =
                initCallSession(coroutineContext, callChannels, incomingVideoAttributes)
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            callSession.setCurrentCallEndpoint(mSpeakerEndpoint)
            yield()

            // Drain initial callType emission from init block
            assertEquals(
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                callChannels.callTypeChannel.receive(),
            )

            // Platform answers incoming call as video (STATE_BIDIRECTIONAL)
            val answerAck = CompletableDeferred<Boolean>()
            callSession.onAnswer(VideoProfile.STATE_BIDIRECTIONAL) { completed ->
                answerAck.complete(completed)
            }
            assertTrue(answerAck.await())
            assertEquals(
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                callChannels.callTypeChannel.receive(),
            )
            assertTrue(callSession.mUnrequestedVideoManager.mAwaitingPlatformVideoAnswerSync)

            // Simulate platform getSetAnswerReceiver() bug downgrading call to AUDIO_CALL
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            yield()

            // One-shot tracking should be consumed and bogus AUDIO_CALL must not be emitted
            assertFalse(callSession.mUnrequestedVideoManager.mAwaitingPlatformVideoAnswerSync)
            assertTrue(callChannels.callTypeChannel.tryReceive().isFailure)

            // Subsequent platform route drop to EARPIECE should still trigger SPEAKER fallback
            val platformEarpiece =
                CallEndpoint(
                    mEarpieceEndpoint.name,
                    CallEndpoint.TYPE_EARPIECE,
                    getRandomParcelUuid(),
                )
            callSession.onCallEndpointChanged(platformEarpiece)
            yield()

            assertEquals(
                CallEndpointCompat.TYPE_SPEAKER,
                callSession.mLastClientRequestedEndpoint?.type,
            )
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that if the client app explicitly requests a downgrade to audio-only after a
     * platform-initiated video answer, the downgrade is respected and not treated as a platform
     * bug.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testClientInitiatedVideoStateDowngrade_AfterPlatformVideoAnswer_IsRespected() {
        runBlocking {
            val callChannels = CallChannels()
            val incomingVideoAttributes =
                CallAttributesCompat(
                    OUTGOING_NAME,
                    TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_INCOMING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    CallAttributesCompat.SUPPORTS_STREAM,
                )
            val callSession =
                initCallSession(coroutineContext, callChannels, incomingVideoAttributes)
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            callSession.setCurrentCallEndpoint(mSpeakerEndpoint)
            yield()

            // Drain initial callType emission
            callChannels.callTypeChannel.receive()

            // Platform answers incoming call as video
            val answerAck = CompletableDeferred<Boolean>()
            callSession.onAnswer(VideoProfile.STATE_BIDIRECTIONAL) { completed ->
                answerAck.complete(completed)
            }
            assertTrue(answerAck.await())
            callChannels.callTypeChannel.receive()

            // Client explicitly downgrades call to audio-only
            callSession.requestVideoState(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            assertFalse(callSession.mUnrequestedVideoManager.mAwaitingPlatformVideoAnswerSync)

            // Platform echoes AUDIO_CALL and switches route to EARPIECE
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            val platformEarpiece =
                CallEndpoint(
                    mEarpieceEndpoint.name,
                    CallEndpoint.TYPE_EARPIECE,
                    getRandomParcelUuid(),
                )
            callSession.onCallEndpointChanged(platformEarpiece)
            yield()

            // Earpiece should be preserved without forcing back to SPEAKER
            assertEquals(
                CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                callChannels.callTypeChannel.receive(),
            )
            assertEquals(null, callSession.mLastClientRequestedEndpoint)
            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies [CallSession.clearStaleRequestedEndpoint] clears
     * [CallSession.mLastClientRequestedEndpoint] on failures and exact active-endpoint no-ops.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testClearStaleRequestedEndpoint_onlyClearsOnFailureOrExactNoOp() {
        runBlocking {
            val callChannels = CallChannels()
            val callSession = initCallSession(coroutineContext, callChannels)
            val btBuds =
                CallEndpointCompat(
                    "Buds",
                    CallEndpointCompat.TYPE_BLUETOOTH,
                    getRandomParcelUuid(),
                )

            // 1. Exact no-op Success clears the matching requested endpoint
            callSession.mCurrentCallEndpoint = mSpeakerEndpoint
            callSession.mLastClientRequestedEndpoint = mSpeakerEndpoint
            callSession.clearStaleRequestedEndpoint(
                androidx.core.telecom.CallControlResult.Success(),
                mSpeakerEndpoint,
            )
            assertEquals(null, callSession.mLastClientRequestedEndpoint)

            // 2. Two Bluetooth endpoints share TYPE_BLUETOOTH but are not equal; keep pending
            callSession.mCurrentCallEndpoint = mWatchEndpoint
            callSession.mLastClientRequestedEndpoint = btBuds
            callSession.clearStaleRequestedEndpoint(
                androidx.core.telecom.CallControlResult.Success(),
                btBuds,
            )
            assertEquals(btBuds, callSession.mLastClientRequestedEndpoint)

            // 3. Newer in-flight request must not be cleared by an older no-op completion
            callSession.mCurrentCallEndpoint = mSpeakerEndpoint
            callSession.mLastClientRequestedEndpoint = mEarpieceEndpoint
            callSession.clearStaleRequestedEndpoint(
                androidx.core.telecom.CallControlResult.Success(),
                mSpeakerEndpoint,
            )
            assertEquals(mEarpieceEndpoint, callSession.mLastClientRequestedEndpoint)

            // 4. Failed platform result clears the pending requested endpoint
            callSession.clearStaleRequestedEndpoint(
                androidx.core.telecom.CallControlResult.Error(
                    androidx.core.telecom.CallException.ERROR_UNKNOWN
                ),
                mEarpieceEndpoint,
            )
            assertEquals(null, callSession.mLastClientRequestedEndpoint)

            callChannels.closeAllChannels()
        }
    }

    /**
     * Verifies that if the client app re-requests [CallAttributesCompat.CALL_TYPE_VIDEO_CALL] right
     * after a platform-initiated video answer (e.g., syncing video state on ACTIVE), the
     * post-answer downgrade guard stays armed and still suppresses the bogus platform audio
     * downgrade.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SmallTest
    @Test
    fun testClientVideoSyncRequest_AfterPlatformVideoAnswer_KeepsDowngradeGuardArmed() {
        runBlocking {
            val callChannels = CallChannels()
            val incomingVideoAttributes =
                CallAttributesCompat(
                    OUTGOING_NAME,
                    TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_INCOMING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    CallAttributesCompat.SUPPORTS_STREAM,
                )
            val callSession =
                initCallSession(coroutineContext, callChannels, incomingVideoAttributes)
            callSession.setAvailableCallEndpoints(mEarAndSpeakerEndpoints)
            callSession.setCurrentCallEndpoint(mSpeakerEndpoint)
            yield()
            callChannels.callTypeChannel.receive()

            // Platform answers incoming call as video
            val answerAck = CompletableDeferred<Boolean>()
            callSession.onAnswer(VideoProfile.STATE_BIDIRECTIONAL) { completed ->
                answerAck.complete(completed)
            }
            assertTrue(answerAck.await())
            callChannels.callTypeChannel.receive()

            // Client syncs/re-confirms VIDEO_CALL before the bogus platform AUDIO_CALL arrives.
            // On API 34 (< VANILLA_ICE_CREAM), requestVideoState echoes locally to callTypeChannel
            // because CallControl.requestVideoState was added in API 35; drain that echo first.
            callSession.requestVideoState(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)
            if (VERSION.SDK_INT < VERSION_CODES.VANILLA_ICE_CREAM) {
                assertEquals(
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    callChannels.callTypeChannel.receive(),
                )
            }
            assertTrue(callSession.mUnrequestedVideoManager.mAwaitingPlatformVideoAnswerSync)

            // Bogus platform AUDIO_CALL downgrade arrives and must still be suppressed
            callSession.onVideoStateChanged(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            yield()

            assertFalse(callSession.mUnrequestedVideoManager.mAwaitingPlatformVideoAnswerSync)
            assertTrue(callChannels.callTypeChannel.tryReceive().isFailure)
            callChannels.closeAllChannels()
        }
    }
}
