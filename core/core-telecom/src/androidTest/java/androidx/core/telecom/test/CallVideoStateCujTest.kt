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
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.ALL_CALL_CAPABILITIES
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_NAME
import androidx.core.telecom.test.utils.TestUtils.TEST_ADDRESS
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CUJ Test class focusing on incoming and outgoing audio and video VoIP calls, verifying the
 * correct remapping of video states and enforcing expected default audio routing (speaker for
 * video, earpiece for audio).
 */
@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class CallVideoStateCujTest : BaseTelecomTest() {

    @Before
    fun setUp() {
        Utils.resetUtils()
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
    }

    companion object {
        val TAG = CallVideoStateCujTest::class.simpleName
        private const val ENDPOINT_SETTLE_TIMEOUT_MS = 3000L
    }

    // ==========================================================================
    // Incoming Calls
    // ==========================================================================

    // --- Audio Calls: Client Answer ---

    /**
     * Test incoming audio call answer to verify correct video state translation and earpiece audio
     * routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallAnswer_AudioStateTranslation() {
        runIncomingCallAnswer_AudioStateTranslation()
    }

    /**
     * Test incoming audio call answer in backwards compatibility mode to verify correct video state
     * translation and earpiece audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallAnswer_AudioStateTranslation_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runIncomingCallAnswer_AudioStateTranslation()
    }

    // --- Audio Calls: Platform / InCallService Answer ---

    /**
     * Test platform answering incoming call as audio to verify correct CallType translation in
     * Jetpack callback and earpiece audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallPlatformAnswer_AudioCallTypeTranslation() {
        runIncomingCallPlatformAnswer_AudioCallTypeTranslation()
    }

    /**
     * Test platform answering incoming call as audio in backwards compatibility mode to verify
     * correct CallType translation in Jetpack callback and earpiece audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallPlatformAnswer_AudioCallTypeTranslation_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runIncomingCallPlatformAnswer_AudioCallTypeTranslation()
    }

    // --- Video Calls: Client Answer ---

    /**
     * Test incoming video call answer to verify correct video state translation and speaker audio
     * routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallAnswer_VideoStateTranslation() {
        runIncomingCallAnswer_VideoStateTranslation()
    }

    /**
     * Test incoming video call answer in backwards compatibility mode to verify correct video state
     * translation and speaker audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallAnswer_VideoStateTranslation_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runIncomingCallAnswer_VideoStateTranslation()
    }

    // --- Video Calls: Platform / InCallService Answer ---

    /**
     * Test platform answering incoming call as video to verify correct CallType translation in
     * Jetpack callback and speaker audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallPlatformAnswer_CallTypeTranslation() {
        runIncomingCallPlatformAnswer_CallTypeTranslation()
    }

    /**
     * Test platform answering incoming call as video in backwards compatibility mode to verify
     * correct CallType translation in Jetpack callback and speaker audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingCallPlatformAnswer_CallTypeTranslation_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runIncomingCallPlatformAnswer_CallTypeTranslation()
    }

    // ==========================================================================
    // Outgoing Calls
    // ==========================================================================

    // --- Audio Calls ---

    /**
     * Test outgoing audio call to verify correct video state translation and earpiece audio
     * routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testOutgoingCall_AudioCall() {
        runOutgoingCall_AudioCall()
    }

    /**
     * Test outgoing audio call in backwards compatibility mode to verify correct video state
     * translation and earpiece audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testOutgoingCall_AudioCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runOutgoingCall_AudioCall()
    }

    // --- Video Calls ---

    /**
     * Test outgoing video call to verify correct video state translation and speaker audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testOutgoingCall_VideoCall() {
        runOutgoingCall_VideoCall()
    }

    /**
     * Test outgoing video call in backwards compatibility mode to verify correct video state
     * translation and speaker audio routing.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testOutgoingCall_VideoCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runOutgoingCall_VideoCall()
    }

    // ==========================================================================
    // Incoming Call Runners
    // ==========================================================================

    /**
     * Steps:
     * 1. Start an incoming AUDIO call.
     * 2. Assert the call type flow is echoing AUDIO.
     * 3. Answer the call from the client side via CallControlScope#answer with AUDIO.
     * 4. Verify the platform video state is showing audio (STATE_AUDIO_ONLY or STATE_TX_ENABLED).
     * 5. Verify the call type flow is echoing AUDIO.
     * 6. Verify the call audio route settles on EARPIECE (if other endpoints available).
     */
    private fun runIncomingCallAnswer_AudioStateTranslation() {
        runBlocking {
            val audioIncomingAttributes =
                createIncomingCallAttributes(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            Log.i(TAG, "runIncomingCallAnswer_AudioStateTranslation: start")

            usingIcs { ics ->
                withTimeout(TestUtils.WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT) {
                    Log.i(TAG, "runIncomingCallAnswer_AudioStateTranslation: adding call")
                    mCallsManager.addCall(
                        audioIncomingAttributes,
                        TestUtils.mOnAnswerLambda,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                    ) {
                        Log.i(
                            TAG,
                            "runIncomingCallAnswer_AudioStateTranslation: call added, scope active",
                        )
                        launch {
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_AudioStateTranslation: waiting for platform call",
                            )
                            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                            assertNotNull("The returned Call object is <NULL>", call)
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_AudioStateTranslation: platform call received: $call",
                            )

                            // Verify initial call type matches
                            val initialCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_AudioStateTranslation: initial callType=$initialCallType",
                            )
                            assertEquals(CallAttributesCompat.CALL_TYPE_AUDIO_CALL, initialCallType)

                            // Answer as AUDIO from client side
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_AudioStateTranslation: answering call",
                            )
                            assertEquals(
                                CallControlResult.Success(),
                                answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL),
                            )
                            Log.i(TAG, "runIncomingCallAnswer_AudioStateTranslation: answered call")

                            TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_AudioStateTranslation: call state is ACTIVE",
                            )

                            // Verify platform call has correct translated video state (AUDIO_ONLY
                            // or TX_ENABLED)
                            val videoState = call.details.videoState
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_AudioStateTranslation: platform videoState=$videoState",
                            )
                            assertTrue(
                                "Platform call should have STATE_AUDIO_ONLY or STATE_TX_ENABLED video state, actual: $videoState",
                                videoState == VideoProfile.STATE_AUDIO_ONLY ||
                                    videoState == VideoProfile.STATE_TX_ENABLED,
                            )

                            // Verify call type still matches
                            val postAnswerCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_AudioStateTranslation: post-answer callType=$postAnswerCallType",
                            )
                            assertEquals(
                                CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                                postAnswerCallType,
                            )

                            // Verify audio route settles on earpiece for audio calls
                            verifyAudioRouteSettle(
                                CallEndpointCompat.TYPE_EARPIECE,
                                "runIncomingCallAnswer_AudioStateTranslation",
                            )

                            Log.i(TAG, "runIncomingCallAnswer_AudioStateTranslation: disconnecting")
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            Log.i(TAG, "runIncomingCallAnswer_AudioStateTranslation: disconnected")
                        }
                    }
                }
            }
        }
    }

    /**
     * Steps:
     * 1. Start an incoming AUDIO call.
     * 2. Assert the call type flow is echoing AUDIO.
     * 3. Answer the call from the platform side (InCallService) with STATE_AUDIO_ONLY.
     * 4. Verify the Jetpack callback (onAnswer) receives CALL_TYPE_AUDIO_CALL.
     * 5. Verify the call type flow is echoing AUDIO.
     * 6. Verify the call audio route settles on EARPIECE.
     */
    private fun runIncomingCallPlatformAnswer_AudioCallTypeTranslation() {
        runBlocking {
            val audioIncomingAttributes =
                createIncomingCallAttributes(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            Log.i(TAG, "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: start")

            val receivedCallType = CompletableDeferred<Int>()
            val customOnAnswer: suspend (Int) -> Unit = { type ->
                Log.i(TAG, "customOnAnswer: type=$type")
                receivedCallType.complete(type)
            }

            usingIcs { ics ->
                withTimeout(TestUtils.WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT) {
                    Log.i(
                        TAG,
                        "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: adding call",
                    )
                    mCallsManager.addCall(
                        audioIncomingAttributes,
                        customOnAnswer,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                    ) {
                        Log.i(
                            TAG,
                            "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: call added",
                        )
                        launch {
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: waiting for platform call",
                            )
                            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                            assertNotNull("The returned Call object is <NULL>", call)
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: platform call received: $call",
                            )

                            // Verify initial call type matches
                            val initialCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: initial callType=$initialCallType",
                            )
                            assertEquals(CallAttributesCompat.CALL_TYPE_AUDIO_CALL, initialCallType)

                            // Answer as AUDIO from platform side (using platform VideoProfile)
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: answering call from platform",
                            )
                            call!!.answer(VideoProfile.STATE_AUDIO_ONLY)

                            // Verify Jetpack callback received translated CallType
                            // (CALL_TYPE_AUDIO_CALL)
                            val callType = receivedCallType.await()
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: onAnswer callback received callType=$callType",
                            )
                            assertEquals(
                                "Jetpack callback should receive CALL_TYPE_AUDIO_CALL",
                                CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                                callType,
                            )

                            // Verify call type still matches
                            val postAnswerCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: post-answer callType=$postAnswerCallType",
                            )
                            assertEquals(
                                CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                                postAnswerCallType,
                            )

                            // Verify audio route settles on earpiece for audio calls
                            verifyAudioRouteSettle(
                                CallEndpointCompat.TYPE_EARPIECE,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation",
                            )

                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: disconnecting",
                            )
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_AudioCallTypeTranslation: disconnected",
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Steps:
     * 1. Start an incoming VIDEO call.
     * 2. Assert the call type flow is echoing VIDEO.
     * 3. Answer the call from the client side via CallControlScope#answer with VIDEO.
     * 4. Verify the platform video state is showing video (STATE_BIDIRECTIONAL or
     *    CALL_TYPE_VIDEO_CALL).
     * 5. Verify the call type flow is echoing VIDEO.
     * 6. Verify the call audio route settles on SPEAKER.
     */
    private fun runIncomingCallAnswer_VideoStateTranslation() {
        runBlocking {
            val videoIncomingAttributes =
                createIncomingCallAttributes(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)
            Log.i(TAG, "runIncomingCallAnswer_VideoStateTranslation: start")

            usingIcs { ics ->
                withTimeout(TestUtils.WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT) {
                    Log.i(TAG, "runIncomingCallAnswer_VideoStateTranslation: adding call")
                    mCallsManager.addCall(
                        videoIncomingAttributes,
                        TestUtils.mOnAnswerLambda,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                    ) {
                        Log.i(TAG, "runIncomingCallAnswer_VideoStateTranslation: call added")
                        launch {
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_VideoStateTranslation: waiting for platform call",
                            )
                            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                            assertNotNull("The returned Call object is <NULL>", call)
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_VideoStateTranslation: platform call received: $call",
                            )

                            // Verify initial call type matches
                            val initialCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_VideoStateTranslation: initial callType=$initialCallType",
                            )
                            assertEquals(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, initialCallType)

                            // Answer as VIDEO from client side
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_VideoStateTranslation: answering call",
                            )
                            assertEquals(
                                CallControlResult.Success(),
                                answer(CallAttributesCompat.CALL_TYPE_VIDEO_CALL),
                            )
                            Log.i(TAG, "runIncomingCallAnswer_VideoStateTranslation: answered call")

                            TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_VideoStateTranslation: call state is ACTIVE",
                            )

                            // Verify platform call has correct translated video state
                            // (STATE_BIDIRECTIONAL) or fallback video state (CALL_TYPE_VIDEO_CALL)
                            val videoState = call.details.videoState
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_VideoStateTranslation: platform videoState=$videoState",
                            )
                            assertTrue(
                                "Platform call should have bidirectional (3) or TX/RX enabled (2) video state, actual: $videoState",
                                videoState == VideoProfile.STATE_BIDIRECTIONAL ||
                                    videoState == CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                            )

                            // Verify call type still matches
                            val postAnswerCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallAnswer_VideoStateTranslation: post-answer callType=$postAnswerCallType",
                            )
                            assertEquals(
                                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                                postAnswerCallType,
                            )

                            // Verify audio route settles on speaker for video calls
                            verifyAudioRouteSettle(
                                CallEndpointCompat.TYPE_SPEAKER,
                                "runIncomingCallAnswer_VideoStateTranslation",
                            )

                            Log.i(TAG, "runIncomingCallAnswer_VideoStateTranslation: disconnecting")
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            Log.i(TAG, "runIncomingCallAnswer_VideoStateTranslation: disconnected")
                        }
                    }
                }
            }
        }
    }

    /**
     * Steps:
     * 1. Start an incoming VIDEO call.
     * 2. Assert the call type flow is echoing VIDEO.
     * 3. Answer the call from the platform side (InCallService) with STATE_BIDIRECTIONAL.
     * 4. Verify the Jetpack callback (onAnswer) receives CALL_TYPE_VIDEO_CALL.
     * 5. Verify the call type flow is echoing VIDEO.
     * 6. Verify the call audio route settles on SPEAKER.
     */
    private fun runIncomingCallPlatformAnswer_CallTypeTranslation() {
        runBlocking {
            val videoIncomingAttributes =
                createIncomingCallAttributes(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)
            Log.i(TAG, "runIncomingCallPlatformAnswer_CallTypeTranslation: start")

            val receivedCallType = CompletableDeferred<Int>()
            val customOnAnswer: suspend (Int) -> Unit = { type ->
                Log.i(TAG, "customOnAnswer: type=$type")
                receivedCallType.complete(type)
            }

            usingIcs { ics ->
                withTimeout(TestUtils.WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT) {
                    Log.i(TAG, "runIncomingCallPlatformAnswer_CallTypeTranslation: adding call")
                    mCallsManager.addCall(
                        videoIncomingAttributes,
                        customOnAnswer,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                    ) {
                        Log.i(TAG, "runIncomingCallPlatformAnswer_CallTypeTranslation: call added")
                        launch {
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: waiting for platform call",
                            )
                            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                            assertNotNull("The returned Call object is <NULL>", call)
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: platform call received: $call",
                            )

                            // Verify initial call type matches
                            val initialCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: initial callType=$initialCallType",
                            )
                            assertEquals(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, initialCallType)

                            // Answer as VIDEO from platform side (using platform VideoProfile)
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: answering call from platform",
                            )
                            call!!.answer(VideoProfile.STATE_BIDIRECTIONAL)

                            // Verify Jetpack callback received translated CallType
                            // (CALL_TYPE_VIDEO_CALL)
                            val callType = receivedCallType.await()
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: onAnswer callback received callType=$callType",
                            )
                            assertEquals(
                                "Jetpack callback should receive CALL_TYPE_VIDEO_CALL",
                                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                                callType,
                            )

                            // Verify call type still matches
                            val postAnswerCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: post-answer callType=$postAnswerCallType",
                            )
                            assertEquals(
                                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                                postAnswerCallType,
                            )

                            // Verify audio route settles on speaker for video calls
                            verifyAudioRouteSettle(
                                CallEndpointCompat.TYPE_SPEAKER,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation",
                            )

                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: disconnecting",
                            )
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            Log.i(
                                TAG,
                                "runIncomingCallPlatformAnswer_CallTypeTranslation: disconnected",
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================================================
    // Outgoing Call Runners
    // ==========================================================================

    /**
     * Steps:
     * 1. Start an outgoing AUDIO call.
     * 2. Assert the call type flow is echoing AUDIO.
     * 3. Verify the platform video state is showing audio (STATE_AUDIO_ONLY or STATE_TX_ENABLED).
     * 4. Verify the call audio route settles on EARPIECE.
     */
    private fun runOutgoingCall_AudioCall() {
        runBlocking {
            val audioOutgoingAttributes =
                createOutgoingCallAttributes(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            Log.i(TAG, "runOutgoingCall_AudioCall: start")

            usingIcs { ics ->
                withTimeout(TestUtils.WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT) {
                    Log.i(TAG, "runOutgoingCall_AudioCall: adding call")
                    mCallsManager.addCall(
                        audioOutgoingAttributes,
                        TestUtils.mOnAnswerLambda,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                    ) {
                        Log.i(TAG, "runOutgoingCall_AudioCall: call added")
                        launch {
                            Log.i(TAG, "runOutgoingCall_AudioCall: waiting for platform call")
                            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                            assertNotNull("The returned Call object is <NULL>", call)
                            Log.i(TAG, "runOutgoingCall_AudioCall: platform call received: $call")

                            // Verify initial call type matches
                            val initialCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runOutgoingCall_AudioCall: initial callType=$initialCallType",
                            )
                            assertEquals(CallAttributesCompat.CALL_TYPE_AUDIO_CALL, initialCallType)

                            // Verify platform call has correct translated video state
                            val videoState = call!!.details.videoState
                            Log.i(TAG, "runOutgoingCall_AudioCall: platform videoState=$videoState")
                            assertTrue(
                                "Platform call should have STATE_AUDIO_ONLY or STATE_TX_ENABLED video state, actual: $videoState",
                                videoState == VideoProfile.STATE_AUDIO_ONLY ||
                                    videoState == VideoProfile.STATE_TX_ENABLED,
                            )

                            // Verify audio route settles on earpiece for audio calls
                            verifyAudioRouteSettle(
                                CallEndpointCompat.TYPE_EARPIECE,
                                "runOutgoingCall_AudioCall",
                            )

                            Log.i(TAG, "runOutgoingCall_AudioCall: disconnecting")
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            Log.i(TAG, "runOutgoingCall_AudioCall: disconnected")
                        }
                    }
                }
            }
        }
    }

    /**
     * Steps:
     * 1. Start an outgoing VIDEO call.
     * 2. Assert the call type flow is echoing VIDEO.
     * 3. Verify the platform video state is showing video (STATE_BIDIRECTIONAL or
     *    CALL_TYPE_VIDEO_CALL).
     * 4. Verify the call audio route settles on SPEAKER.
     */
    private fun runOutgoingCall_VideoCall() {
        runBlocking {
            val videoOutgoingAttributes =
                createOutgoingCallAttributes(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)
            Log.i(TAG, "runOutgoingCall_VideoCall: start")

            usingIcs { ics ->
                withTimeout(TestUtils.WAIT_ON_ASSERTS_TO_FINISH_TIMEOUT) {
                    Log.i(TAG, "runOutgoingCall_VideoCall: adding call")
                    mCallsManager.addCall(
                        videoOutgoingAttributes,
                        TestUtils.mOnAnswerLambda,
                        TestUtils.mOnDisconnectLambda,
                        TestUtils.mOnSetActiveLambda,
                        TestUtils.mOnSetInActiveLambda,
                    ) {
                        Log.i(TAG, "runOutgoingCall_VideoCall: call added")
                        launch {
                            Log.i(TAG, "runOutgoingCall_VideoCall: waiting for platform call")
                            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                            assertNotNull("The returned Call object is <NULL>", call)
                            Log.i(TAG, "runOutgoingCall_VideoCall: platform call received: $call")

                            // Verify initial call type matches
                            val initialCallType = callTypeFlow().first()
                            Log.i(
                                TAG,
                                "runOutgoingCall_VideoCall: initial callType=$initialCallType",
                            )
                            assertEquals(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, initialCallType)

                            // Verify platform call has correct translated video state
                            val videoState = call!!.details.videoState
                            Log.i(TAG, "runOutgoingCall_VideoCall: platform videoState=$videoState")
                            assertTrue(
                                "Platform call should have bidirectional (3) or TX/RX enabled (2) video state, actual: $videoState",
                                videoState == VideoProfile.STATE_BIDIRECTIONAL ||
                                    videoState == CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                            )

                            // Verify audio route settles on speaker for video calls
                            verifyAudioRouteSettle(
                                CallEndpointCompat.TYPE_SPEAKER,
                                "runOutgoingCall_VideoCall",
                            )

                            Log.i(TAG, "runOutgoingCall_VideoCall: disconnecting")
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                            Log.i(TAG, "runOutgoingCall_VideoCall: disconnected")
                        }
                    }
                }
            }
        }
    }

    // ==========================================================================
    // Helpers
    // ==========================================================================
    private fun createIncomingCallAttributes(callType: Int): CallAttributesCompat {
        return CallAttributesCompat(
            OUTGOING_NAME,
            TEST_ADDRESS,
            CallAttributesCompat.DIRECTION_INCOMING,
            callType,
            ALL_CALL_CAPABILITIES,
        )
    }

    private fun createOutgoingCallAttributes(callType: Int): CallAttributesCompat {
        return CallAttributesCompat(
            OUTGOING_NAME,
            TEST_ADDRESS,
            CallAttributesCompat.DIRECTION_OUTGOING,
            callType,
            ALL_CALL_CAPABILITIES,
        )
    }

    private suspend fun CoroutineScope.getSettledEndpoint(
        endpointFlow: Flow<CallEndpointCompat>
    ): CallEndpointCompat? {
        var settledEndpoint: CallEndpointCompat? = null
        val endpointCollectorJob = launch {
            endpointFlow.collect { endpoint -> settledEndpoint = endpoint }
        }
        delay(ENDPOINT_SETTLE_TIMEOUT_MS)
        endpointCollectorJob.cancel()
        return settledEndpoint
    }

    private suspend fun CallControlScope.verifyAudioRouteSettle(
        expectedRouteType: Int,
        methodNameForLog: String,
    ) {
        val availableEndpointsList = availableEndpoints.first()
        Log.i(TAG, "$methodNameForLog: availableEndpoints=$availableEndpointsList")
        val hasTargetRoute = availableEndpointsList.any { it.type == expectedRouteType }
        if (availableEndpointsList.size > 1 && hasTargetRoute) {
            Log.i(TAG, "$methodNameForLog: waiting for endpoint to settle")
            val settledEndpoint = getSettledEndpoint(currentCallEndpoint)
            Log.i(TAG, "$methodNameForLog: settledEndpoint=$settledEndpoint")

            assertNotNull("Never received an endpoint update", settledEndpoint)
            assertEquals(
                "Call routing failed to settle on the expected endpoint type: $expectedRouteType",
                expectedRouteType,
                settledEndpoint?.type,
            )
        }
    }
}
