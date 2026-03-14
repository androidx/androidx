/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build
import android.os.Build.VERSION_CODES
import android.telecom.Call
import android.telecom.DisconnectCause
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class verifies the [CallControlScope] functionality is working as intended when adding
 * a VoIP call. Each test should add a call via [CallsManager.addCall] and changes the call state
 * via the [CallControlScope].
 *
 * Note: Be careful with using a delay in a runBlocking scope to avoid missing flows. ex:
 * runBlocking { addCall(...){ delay(x time) // The flow will be emitted here and missed
 * currentCallEndpoint.counter.getFirst() // The flow may never be collected } }
 */
@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class BasicCallControlsTest : BaseTelecomTest() {
    private val NUM_OF_TIMES_TO_TOGGLE = 3

    @Before
    fun setUp() {
        Utils.resetUtils()
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
    }

    companion object {
        val TAG = BasicCallControlsTest::class.simpleName
    }

    /**
     * ********************************************************************************************
     * V2 APIs (Android U and above) tests
     * *******************************************************************************************
     */

    /**
     * assert [CallsManager.addCall] can successfully add an *OUTGOING* call and set it active. The
     * call should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testBasicOutgoingCall() {
        runBlocking_addCallAndSetActive(TestUtils.OUTGOING_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and answer it. The call
     * should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testBasicIncomingCall() {
        runBlocking_addCallAndSetActive(TestUtils.INCOMING_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call and **TOGGLE** active and inactive.
     * The call should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testTogglingHoldOnActiveCall() {
        runBlocking_ToggleCallAsserts(TestUtils.OUTGOING_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call that does NOT support setting the
     * call inactive and when the setInactive is called, the transaction fails. The call should use
     * the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testTogglingHoldOnActiveCall_NoHoldCapabilities() {
        assertFalse(
            TestUtils.OUTGOING_NO_HOLD_CAP_CALL_ATTRIBUTES.hasSupportsSetInactiveCapability()
        )
        runBlocking_ShouldFailHold(TestUtils.OUTGOING_NO_HOLD_CAP_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call and request a new
     * [CallEndpointCompat] via [CallControlScope.requestEndpointChange]. The call should use the
     * *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testRequestEndpointChange() {
        runBlocking_RequestEndpointChangeAsserts()
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call and verifies that requests to
     * mute/unmute the call are reflected in [CallControlScope.isMuted]. The call should use the *V2
     * platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testIsMuted() {
        verifyMuteStateChange()
    }

    /**
     * ********************************************************************************************
     * Backwards Compatibility Layer tests
     * *******************************************************************************************
     */

    /**
     * assert [CallsManager.addCall] can successfully add an *OUTGOING* call and set it active. The
     * call should use the *[android.telecom.ConnectionService] and [android.telecom.Connection]
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 10000)
    fun testBasicOutgoingCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking_addCallAndSetActive(TestUtils.OUTGOING_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and answer it. The call
     * should use the *[android.telecom.ConnectionService] and [android.telecom.Connection] APIs*
     * under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 10000)
    fun testBasicIncomingCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking_addCallAndSetActive(TestUtils.INCOMING_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call and **TOGGLE** active and inactive.
     * The call should use the *[android.telecom.ConnectionService] and [android.telecom.Connection]
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 10000)
    fun testTogglingHoldOnActiveCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking_ToggleCallAsserts(TestUtils.OUTGOING_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call that does NOT support setting the
     * call inactive and when the setInactive is called, the transaction fails. The call should use
     * the *[android.telecom.ConnectionService] and [android.telecom.Connection] APIs* under the
     * hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 10000)
    fun testTogglingHoldOnActiveCall_NoHoldCapabilities_BackwardsCompat() {
        setUpBackwardsCompatTest()
        assertFalse(
            TestUtils.OUTGOING_NO_HOLD_CAP_CALL_ATTRIBUTES.hasSupportsSetInactiveCapability()
        )
        runBlocking_ShouldFailHold(TestUtils.OUTGOING_NO_HOLD_CAP_CALL_ATTRIBUTES)
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call and request a new
     * [CallEndpointCompat] via [CallControlScope.requestEndpointChange]. The call should use the
     * *[android.telecom.ConnectionService] and [android.telecom.Connection] APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 10000)
    fun testRequestEndpointChange_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking_RequestEndpointChangeAsserts()
        // TODO:: tracking bug: b/283324578. This test passes when the request is sent off and does
        // not actually verify the request was successful. Need to change the impl. details.
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call and verifies that requests to
     * mute/unmute the call are reflected in [CallControlScope.isMuted]. The call should use the
     * *[android.telecom.ConnectionService] and [android.telecom.Connection] APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 20000)
    fun testIsMuted_BackwardsCompat() {
        setUpBackwardsCompatTest()
        verifyMuteStateChange()
    }

    /**
     * Verify that the [androidx.core.telecom.CallsManager.addCall] blocks until the session is
     * disconnected
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testTiming() {
        setUpBackwardsCompatTest()
        var flag = false
        runBlocking {
            mCallsManager.addCall(
                TestUtils.OUTGOING_CALL_ATTRIBUTES,
                TestUtils.mOnAnswerLambda,
                TestUtils.mOnDisconnectLambda,
                TestUtils.mOnSetActiveLambda,
                TestUtils.mOnSetInActiveLambda,
            ) {
                launch {
                    delay(10)
                    disconnect(DisconnectCause(DisconnectCause.LOCAL))
                    flag = true
                }
            }
            assertTrue(flag)
        }
    }

    /** Add test coverage for [CallControlScope.getCallId] */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testGetCallId() {
        runBlocking {
            assertWithinTimeout_addCall(TestUtils.OUTGOING_CALL_ATTRIBUTES) {
                launch {
                    assertNotNull(getCallId())
                    disconnect(DisconnectCause(DisconnectCause.LOCAL))
                }
            }
        }
    }

    /**
     * Add test coverage for [CallControlScope.requestCallType] and [CallControlScope.callTypeFlow]
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testCallType() {
        runBlocking {
            // 1. Add an audio call
            assertWithinTimeout_addCall(
                attributes =
                    CallAttributesCompat(
                        OUTGOING_NAME,
                        TEST_ADDRESS,
                        CallAttributesCompat.DIRECTION_OUTGOING,
                        CallAttributesCompat.CALL_TYPE_AUDIO_CALL, // Start as audio
                        ALL_CALL_CAPABILITIES,
                    )
            ) {
                launch {
                    // 2. Collect the initial call type from the flow and verify it's audio
                    val callTypeFlow = callTypeFlow()
                    if (Build.VERSION.SDK_INT != VERSION_CODES.VANILLA_ICE_CREAM) {
                        val initialCallType = callTypeFlow.first()
                        assertEquals(
                            "Initial call type should be audio",
                            CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                            initialCallType,
                        )
                    }
                    // 3. Launch a collector for the next value *before* triggering it.
                    //    This ensures we are listening for the change when it happens.
                    val collectorJob = launch {
                        waitForVideoState(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, callTypeFlow)
                    }
                    // 4. Request the upgrade to a video call, which triggers the emission.
                    assertEquals(
                        "Request to change video state should succeed",
                        CallControlResult.Success(),
                        requestCallType(CallAttributesCompat.CALL_TYPE_VIDEO_CALL),
                    )
                    // 5. Wait for the collector coroutine to complete, which confirms the
                    //    assertion in waitForVideoState has passed.
                    collectorJob.join()
                    // 6. Clean up the call
                    disconnect(DisconnectCause(DisconnectCause.LOCAL))
                }
            }
        }
    }

    /**
     * Assert that an incoming video call avoids the platform earpiece routing bug and successfully
     * requests a switch to the speaker endpoint on start. The call should use the *V2 platform
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test(timeout = 15000)
    fun testIncomingVideoCall_EnforcesSpeakerOnStart() {
        runBlocking {
            val videoIncomingAttributes =
                CallAttributesCompat(
                    OUTGOING_NAME,
                    TEST_ADDRESS,
                    CallAttributesCompat.DIRECTION_INCOMING,
                    CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                    ALL_CALL_CAPABILITIES,
                )

            usingIcs { ics ->
                assertWithinTimeout_addCall(videoIncomingAttributes) {
                    // We MUST use launch here to provide a coroutine context for suspend functions
                    // like delay() and disconnect(), and to run concurrently with the call session.
                    launch {
                        // Wait for the platform to become aware of the call
                        val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                        assertNotNull("The returned Call object is <NULL>", call)

                        // 1. Keep track of the most recent endpoint the platform emits
                        var settledEndpoint: CallEndpointCompat? = null
                        val endpointCollectorJob = launch {
                            currentCallEndpoint.collect { endpoint -> settledEndpoint = endpoint }
                        }

                        // 2. Allow the platform time to run through its noisy initial routing
                        //    (e.g. SPEAKER -> EARPIECE) and allow our Jetpack workaround time
                        //    to intercept and correct it back to SPEAKER.
                        delay(3000)

                        // Stop collecting now that the route should be stable
                        endpointCollectorJob.cancel()

                        // 3. Assert that the FINAL, settled state is SPEAKER, catching the bug
                        //    where it ends on EARPIECE.
                        assertNotNull(
                            "Never received an endpoint update from the platform",
                            settledEndpoint,
                        )
                        assertEquals(
                            "Video call routing failed to settle on the SPEAKER endpoint",
                            CallEndpointCompat.TYPE_SPEAKER,
                            settledEndpoint?.type,
                        )

                        // Clean up
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL)),
                        )
                    }
                }
            }
        }
    }

    /**
     * ********************************************************************************************
     * Helpers
     * *******************************************************************************************
     */

    /**
     * This helper facilitates adding a call, setting it active or answered, and disconnecting.
     *
     * Note: delays are inserted to simulate more natural calling. Otherwise the call dumpsys does
     * not reflect realistic transitions.
     *
     * Note: This helper blocks the TestRunner from finishing until all asserts and async functions
     * have finished or the timeout has been reached.
     */
    private fun runBlocking_addCallAndSetActive(callAttributesCompat: CallAttributesCompat) {
        runBlocking {
            usingIcs { ics ->
                assertWithinTimeout_addCall(callAttributesCompat) {
                    launch {
                        val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                        assertNotNull("The returned Call object is <NULL>", call)
                        if (callAttributesCompat.isOutgoingCall()) {
                            assertEquals(CallControlResult.Success(), setActive())
                        } else {
                            assertEquals(
                                CallControlResult.Success(),
                                answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL),
                            )
                        }
                        TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL)),
                        )
                    }
                }
            }
        }
    }

    // similar to runBlocking_addCallAndSetActive except for toggling
    private fun runBlocking_ToggleCallAsserts(callAttributesCompat: CallAttributesCompat) {
        runBlocking {
            usingIcs { ics ->
                assertWithinTimeout_addCall(callAttributesCompat) {
                    launch {
                        val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                        assertNotNull("The returned Call object is <NULL>", call)
                        repeat(NUM_OF_TIMES_TO_TOGGLE) {
                            assertEquals(CallControlResult.Success(), setActive())
                            TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                            assertEquals(CallControlResult.Success(), setInactive())
                            TestUtils.waitOnCallState(call, Call.STATE_HOLDING)
                        }
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL)),
                        )
                    }
                }
            }
        }
    }

    private fun runBlocking_ShouldFailHold(callAttributesCompat: CallAttributesCompat) {
        runBlocking {
            usingIcs { ics ->
                assertWithinTimeout_addCall(callAttributesCompat) {
                    launch {
                        val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                        assertNotNull("The returned Call object is <NULL>", call)
                        assertEquals(CallControlResult.Success(), setActive())
                        TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                        assertNotEquals(CallControlResult.Success(), setInactive())
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL)),
                        )
                    }
                }
            }
        }
    }

    // similar to runBlocking_addCallAndSetActive except for requesting a new call endpoint
    private fun runBlocking_RequestEndpointChangeAsserts() {
        runBlocking {
            assertWithinTimeout_addCall(TestUtils.OUTGOING_CALL_ATTRIBUTES) {
                launch {
                    // ============================================================================
                    //   NOTE:: DO NOT DELAY BEFORE COLLECTING FLOWS OR THEY COULD BE MISSED!!
                    // ============================================================================
                    val currentEndpoint = currentCallEndpoint.first()
                    assertNotNull("currentEndpoint is null", currentEndpoint)
                    val availableEndpointsList = availableEndpoints.first()
                    // only run the following asserts if theres another endpoint available
                    // (This will most likely the speaker endpoint)
                    if (availableEndpointsList.size > 1) {
                        // grab another endpoint
                        val anotherEndpoint =
                            getAnotherEndpoint(currentEndpoint, availableEndpointsList)
                        assertNotNull(anotherEndpoint)
                        // set the call active
                        assertEquals(CallControlResult.Success(), setActive())
                        // request an endpoint switch
                        assertEquals(
                            CallControlResult.Success(),
                            requestEndpointChange(anotherEndpoint!!),
                        )
                    }
                    assertEquals(
                        CallControlResult.Success(),
                        disconnect(DisconnectCause(DisconnectCause.LOCAL)),
                    )
                }
            }
        }
    }

    /**
     * This helper verifies that [CallControlScope.isMuted] properly collects updates to the mute
     * state via [TestInCallService.setMuted].
     *
     * Note: Due to the possibility that the channel can receive stale updates, it's necessary to
     * keep receiving those updates until the state does change. To prevent the test execution from
     * blocking on additional updates, the coroutine scope needs to be cancelled.
     */
    @Suppress("deprecation")
    private fun verifyMuteStateChange() {
        runBlocking {
            usingIcs { ics ->
                assertWithinTimeout_addCall(TestUtils.OUTGOING_CALL_ATTRIBUTES) {
                    launch {
                        val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
                        assertNotNull("The returned Call object is <NULL>", call)
                        assertEquals(CallControlResult.Success(), setActive())
                        TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                        // Grab initial mute state
                        val initialMuteState = isMuted.first()
                        // Toggle mute via ICS
                        ics.setMuted(!initialMuteState)
                        waitForMuteStateChange(!initialMuteState, isMuted)
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL)),
                        )
                    }
                }
            }
        }
    }

    private suspend fun waitForMuteStateChange(isMuted: Boolean, isMutedFlow: Flow<Boolean>) {
        Log.i(TAG, "waitForGlobalMuteState: v=[$isMuted]")
        val result =
            withTimeoutOrNull(5000) {
                isMutedFlow
                    .filter {
                        Log.i(TAG, "it=[$isMuted], isMuted=[$isMuted]")
                        it == isMuted
                    }
                    .firstOrNull()
            }
        assertEquals("Global Mute State never reached the expected state", isMuted, result)
    }

    /**
     * Collects from the videoStateFlow until the expected call type is emitted or a 5-second
     * timeout is reached.
     */
    private suspend fun waitForVideoState(expectedCallType: Int, videoStateFlow: Flow<Int>) {
        // withTimeoutOrNull will return the result of the block or null if it times out.
        val finalState =
            withTimeoutOrNull(5000) {
                // Use the 'first' operator with a predicate. It's more concise and achieves the
                // same goal as filtering and then taking the first element.
                videoStateFlow.first { it == expectedCallType }
            }

        // This assertion now has a much clearer failure message. If 'finalState' is null
        // (due to timeout), the message will clearly explain what the test was waiting for.
        assertEquals(
            "Timeout: Video state did not change to the expected" +
                " value of [$expectedCallType] within 5s.",
            expectedCallType,
            finalState,
        )
    }

    private fun getAnotherEndpoint(
        currentEndpoint: CallEndpointCompat,
        availableEndpoints: List<CallEndpointCompat>,
    ): CallEndpointCompat? {
        for (endpoint in availableEndpoints) {
            if (endpoint.type != currentEndpoint.type) {
                return endpoint
            }
        }
        return null
    }
}
