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

package androidx.core.telecom

import android.os.Build.VERSION_CODES
import android.telecom.Call
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.utils.BaseTelecomTest
import androidx.core.telecom.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class verifies the [CallControlCallback] functionality is working as intended when
 * adding a VoIP call.  Each test should add a call via [CallsManager.addCall] but should be
 * manipulated via the [androidx.core.telecom.utils.MockInCallService].  The MockInCallService will
 * create a [CallControlCallback] request before changing the call state.
 */
@RunWith(AndroidJUnit4::class)
@RequiresApi(VERSION_CODES.O)
class BasicCallControlCallbacksTest : BaseTelecomTest() {

    @Before
    fun setUp() {
        Utils.resetUtils()
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
    }

    /***********************************************************************************************
     *                           V2 APIs (Android U and above) tests
     *********************************************************************************************/

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and answer it via
     * an InCallService that requires the [CallControlCallback.onAnswer] to accept the request. The
     * call should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackAnswerCall() {
        setUpV2Test()
        verifyAnswerCall()
    }

    /**
     * assert that when an *INCOMING* call is answered via [CallsManager.addCall] and the client
     * rejects the request in [CallControlCallback.onAnswer], that we disconnect the call. The call
     * should go into the active state before being disconnected. The call should use the *V2
     * platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test
    fun testRejectCallControlCallbackAnswerCall() {
        setUpV2Test()
        verifyRejectAnswerCall(Call.STATE_ACTIVE)
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and disconnect it via
     * an InCallService that requires the [CallControlCallback.onDisconnect] to accept the request.
     * The call should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackDisconnectCall() {
        setUpV2Test()
        verifyDisconnectCall()
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and hold it via
     * an InCallService that requires the [CallControlCallback.onSetInactive] to accept the request.
     * The call should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackHoldCall() {
        setUpV2Test()
        verifyHoldCall()
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and un-hold it via
     * an InCallService that requires the [CallControlCallback.onSetActive] to accept the request.
     * The call should use the *V2 platform APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackUnholdCall() {
        setUpV2Test()
        verifyUnholdCall()
    }

    /***********************************************************************************************
     *                           Backwards Compatibility Layer tests
     *********************************************************************************************/

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and answer it via
     * an InCallService that requires the [CallControlCallback.onAnswer] to accept the request.
     * The call should use the *[android.telecom.ConnectionService] and [android.telecom.Connection]
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackAnswerCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        verifyAnswerCall()
    }

    /**
     * assert that when an *INCOMING* call is answered via [CallsManager.addCall] and the client
     * rejects the request in [CallControlCallback.onAnswer], that we disconnect the call. The call
     * should never go active and simply be disconnected when it is in the ringing state. The call
     * should use the *[android.telecom.ConnectionService] and [android.telecom.Connection] APIs*
     * under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testRejectCallControlCallbackAnswerCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        verifyRejectAnswerCall(Call.STATE_DISCONNECTED)
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and disconnect it via
     * an InCallService that requires the [CallControlCallback.onDisconnect] to accept the request.
     * The call should use the *[android.telecom.ConnectionService] and [android.telecom.Connection]
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackDisconnectCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        verifyDisconnectCall()
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and hold it via
     * an InCallService that requires the [CallControlCallback.onSetInactive] to accept the request.
     * The call should use the *[android.telecom.ConnectionService] and [android.telecom.Connection]
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackHoldCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        verifyHoldCall()
    }

    /**
     * assert [CallsManager.addCall] can successfully add an *INCOMING* call and un-hold it via
     * an InCallService that requires the [CallControlCallback.onSetActive] to accept the request.
     * The call should use the *[android.telecom.ConnectionService] and [android.telecom.Connection]
     * APIs* under the hood.
     */
    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @LargeTest
    @Test
    fun testBasicCallControlCallbackUnholdCall_BackwardsCompat() {
        setUpBackwardsCompatTest()
        verifyUnholdCall()
    }

    /***********************************************************************************************
     *                           Helpers
     *********************************************************************************************/

    @Suppress("deprecation")
    private fun verifyAnswerCall() {
        assertFalse(TestUtils.mOnAnswerCallbackCalled)
        runBlocking {
            mCallsManager.addCall(TestUtils.INCOMING_CALL_ATTRIBUTES) {
                setCallback(TestUtils.mCallControlCallbacksImpl)
                launch {
                    val call = TestUtils.waitOnInCallServiceToReachXCalls(1)
                    assertNotNull("The returned Call object is <NULL>", call)
                    call!!.answer(0) // API under test
                    TestUtils.waitOnCallState(call, Call.STATE_ACTIVE)
                    // Always send the disconnect signal if possible:
                    assertTrue(disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                }
            }
        }
        // Assert that the correct callback was invoked
        assertTrue(TestUtils.mOnAnswerCallbackCalled)
    }

    @Suppress("deprecation")
    private fun verifyDisconnectCall() {
        assertFalse(TestUtils.mOnDisconnectCallbackCalled)
        runBlocking {
            mCallsManager.addCall(TestUtils.INCOMING_CALL_ATTRIBUTES) {
                setCallback(TestUtils.mCallControlCallbacksImpl)
                launch {
                    val call = TestUtils.waitOnInCallServiceToReachXCalls(1)
                    assertNotNull("The returned Call object is <NULL>", call)
                    // Disconnect the call and ensure the disconnect callback is received:
                    call!!.disconnect()
                    TestUtils.waitOnCallState(call, Call.STATE_DISCONNECTED)
                }
            }
        }
        // Assert that the correct callback was invoked
        assertTrue(TestUtils.mOnDisconnectCallbackCalled)
    }

    @Suppress("deprecation")
    private fun verifyHoldCall() {
        assertFalse(TestUtils.mOnSetInactiveCallbackCalled)
        runBlocking {
            mCallsManager.addCall(TestUtils.INCOMING_CALL_ATTRIBUTES) {
                setCallback(TestUtils.mCallControlCallbacksImpl)
                launch {
                    val call = TestUtils.waitOnInCallServiceToReachXCalls(1)
                    assertNotNull("The returned Call object is <NULL>", call)
                    assertTrue(setActive())
                    // Wait for the call to be set to ACTIVE:
                    TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                    // Place the call on hold and ensure the onSetInactive callback is received:
                    call.hold()
                    TestUtils.waitOnCallState(call, Call.STATE_HOLDING)
                    // Always send the disconnect signal if possible:
                    assertTrue(disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                }
            }
        }
        // Assert that the correct callback was invoked
        assertTrue(TestUtils.mOnSetInactiveCallbackCalled)
    }

    @Suppress("deprecation")
    private fun verifyUnholdCall() {
        assertFalse(TestUtils.mOnSetActiveCallbackCalled)
        runBlocking {
            mCallsManager.addCall(TestUtils.INCOMING_CALL_ATTRIBUTES) {
                setCallback(TestUtils.mCallControlCallbacksImpl)
                launch {
                    val call = TestUtils.waitOnInCallServiceToReachXCalls(1)
                    assertNotNull("The returned Call object is <NULL>", call)
                    assertTrue(setActive())
                    // Wait for the call to be set to ACTIVE:
                    TestUtils.waitOnCallState(call!!, Call.STATE_ACTIVE)
                    assertTrue(setInactive())
                    // Wait for the call to be set to HOLDING (aka inactive):
                    TestUtils.waitOnCallState(call, Call.STATE_HOLDING)
                    // Request to un-hold the call and ensure the onSetActive callback is received:
                    call.unhold()
                    TestUtils.waitOnCallState(call, Call.STATE_ACTIVE)
                    // Always send the disconnect signal if possible:
                    assertTrue(disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                }
            }
        }
        // Assert that the correct callback was invoked
        assertTrue(TestUtils.mOnSetActiveCallbackCalled)
    }

    @Suppress("deprecation")
    private fun verifyRejectAnswerCall(callState: Int) {
        assertFalse(TestUtils.mOnAnswerCallbackCalled)
        runBlocking {
            mCallsManager.addCall(TestUtils.INCOMING_CALL_ATTRIBUTES) {
                setCallback(TestUtils.mCallControlCallbacksImpl)
                // Note that this is reset in BaseTelecomTest in setUp/destroy
                TestUtils.mCompleteOnAnswer = false
                launch {
                    val call = TestUtils.waitOnInCallServiceToReachXCalls(1)
                    assertNotNull("The returned Call object is <NULL>", call)
                    call!!.answer(0) // API under test
                    // Ensure that call has moved to the expected state
                    TestUtils.waitOnCallState(call, callState)
                    // Ensure that call is automatically disconnected.
                    TestUtils.waitOnCallState(call, Call.STATE_DISCONNECTED)
                }
            }
        }
        // Assert that the correct callback was invoked
        assertTrue(TestUtils.mOnAnswerCallbackCalled)
    }
}