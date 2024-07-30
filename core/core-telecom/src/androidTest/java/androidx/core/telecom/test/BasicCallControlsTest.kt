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

import android.os.Build.VERSION_CODES
import android.telecom.Call
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestInCallService
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
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
@RequiresApi(VERSION_CODES.O)
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
        setUpV2Test()
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
        setUpV2Test()
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
        setUpV2Test()
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
        setUpV2Test()
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
    @Ignore // b/329357697  TODO:: re-enable when cache_call_audio_callbacks is enabled in builds
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testRequestEndpointChange() {
        setUpV2Test()
        runBlocking_RequestEndpointChangeAsserts()
    }

    /**
     * assert [CallsManager.addCall] can successfully add a call and verifies that requests to
     * mute/unmute the call are reflected in [CallControlScope.isMuted]. The call should use the *V2
     * platform APIs* under the hood.
     */
    @Ignore // b/323006293  TODO:: re-enable when cache_call_audio_callbacks is enabled in builds
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testIsMuted() {
        setUpV2Test()
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
                                answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
                            )
                        }
                        TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
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
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
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
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
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
                            requestEndpointChange(anotherEndpoint!!)
                        )
                    }
                    assertEquals(
                        CallControlResult.Success(),
                        disconnect(DisconnectCause(DisconnectCause.LOCAL))
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
                        // Toggle to other state
                        val setMuteStateTo = !initialMuteState
                        var muteStateChanged = false
                        // Toggle mute via ICS
                        ics.setMuted(setMuteStateTo)
                        runBlocking {
                            launch {
                                isMuted.collect {
                                    if (it != initialMuteState) {
                                        muteStateChanged = true
                                        // Cancel the coroutine to ensure we don't block on waiting
                                        // for
                                        // updates and force a timeout.
                                        cancel()
                                    }
                                }
                            }
                        }
                        // Ensure that the updated mute state was collected
                        assertTrue(muteStateChanged)
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                        )
                    }
                }
            }
        }
    }

    private fun getAnotherEndpoint(
        currentEndpoint: CallEndpointCompat,
        availableEndpoints: List<CallEndpointCompat>
    ): CallEndpointCompat? {
        for (endpoint in availableEndpoints) {
            if (endpoint.type != currentEndpoint.type) {
                return endpoint
            }
        }
        return null
    }
}
