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

import android.Manifest
import android.os.Build
import android.telecom.Call
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.InCallServiceCompat
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class verifies the [InCallServiceCompat] functionality around resolving the call
 * extension type in order to determine the supported extensions between the VOIP app and the
 * associated InCallServices. This test constructs calls via TelecomManager and modifies the call
 * details (if required) to test each scenario. This is explained in more detail at the test level
 * for each of the applicable cases below.
 *
 * Note: [Call] is package-private so we still need to leverage Telecom to create calls on our
 * behalf for testing. The call properties and extras fields aren't mutable so we need to ensure
 * that we wait for them to become available before accessing them.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class InCallServiceCompatTest : BaseTelecomTest() {
    private lateinit var inCallServiceCompat: InCallServiceCompat

    /**
     * Grant READ_PHONE_NUMBERS permission as part of testing
     * [InCallServiceCompat#resolveCallExtensionsType].
     */
    @get:Rule
    val readPhoneNumbersRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_PHONE_NUMBERS)!!

    companion object {
        /**
         * Logging for within the test class.
         */
        internal val TAG = InCallServiceCompatTest::class.simpleName
    }

    @Before
    fun setUp() {
        Utils.resetUtils()
        inCallServiceCompat = InCallServiceCompat(mContext)
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
    }

    /**
     * Assert that EXTRAS is the extension type for calls made using the V1.5 ConnectionService +
     * Extensions Library (Auto). The call should have the [CallsManager.EXTRA_VOIP_API_VERSION]
     * defined in the extras.
     *
     * The contents of the call detail extras need to be modified to test calls using the V1.5
     * ConnectionService + Extensions library (until E2E testing can be supported for it). This
     * requires us to manually insert the [CallsManager.EXTRA_VOIP_API_VERSION] key into the bundle.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testResolveCallExtension_Extra() {
        setUpBackwardsCompatTest()
        val voipApiExtra = Pair(CallsManager.EXTRA_VOIP_API_VERSION, true)
        addAndVerifyCallExtensionType(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            InCallServiceCompat.EXTRAS,
            extraToInclude = voipApiExtra)
    }

    /**
     * Assert that CAPABILITY_EXCHANGE is the extension type for calls that either have the
     * [CallsManager.PROPERTY_IS_TRANSACTIONAL] (V) defined as a property or the phone account
     * supports transactional ops (U+). For pre-U devices, the call extras would define the
     * [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED] key.
     *
     * Note: The version codes for V is not available so we need to enforce a strict manual check
     * to ensure the V test path is not executed by incompatible devices.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testResolveCallExtension_CapabilityExchange() {
        // Add EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED for pre-U testing
        val backwardsCompatExtra = configureCapabilityExchangeTypeTest()
        addAndVerifyCallExtensionType(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            InCallServiceCompat.CAPABILITY_EXCHANGE,
            // Waiting is not required for U+ testing
            waitForCallDetailExtras = !TestUtils.buildIsAtLeastU(),
            extraToInclude = backwardsCompatExtra
        )
    }

    /**
     * Assert that for calls supporting [InCallServiceCompat.CAPABILITY_EXCHANGE] that capability
     * exchange between the VOIP app and associated ICS is successful. This is signaled from the
     * ICS side when the feature setup is completed via CapabilityExchange#featureSetupComplete.
     *
     * Note: The version codes for V is not available so we need to enforce a strict manual check
     * to ensure the V test path is not executed by incompatible devices.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCapabilityExchangeNegotiationSuccessful() {
        // Add EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED for pre-U testing
        val backwardsCompatExtra = configureCapabilityExchangeTypeTest()
        verifyICSCapabilitiesNegotiatedWithVoipApp(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            // Waiting is not required for U+ testing
            waitForCallDetailExtras = !TestUtils.buildIsAtLeastU(),
            extraToInclude = backwardsCompatExtra
        )
    }

    /**
     * Assert that NONE is the extension type for calls with phone accounts that do not support
     * transactional ops. Note that the caller must have had the read phone numbers permission.
     *
     * Note: Ensure that all extras are cleared before asserting extension type so that the phone
     * account can be checked. For backwards compatibility tests, calls define the
     * [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED] key in the details extras so this
     * needs to be disregarded.
     *
     * We need to ensure that all extras/properties are ignored for testing so that the phone
     * account can be checked to see if it supports transactional ops. In jetpack, this can only be
     * verified on pre-U devices as those phone accounts are registered in Telecom without
     * transactional ops. Keep in mind that because these calls are set up for backwards
     * compatibility, they will have the [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED]
     * extra in the details (which will need to be ignored during testing).
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testResolveCallExtension_TransactionalOpsNotSupported() {
        // Phone accounts that don't use the v2 APIs don't support transactional ops.
        setUpBackwardsCompatTest()
        addAndVerifyCallExtensionType(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            InCallServiceCompat.NONE,
            waitForCallDetailExtras = false
        )
    }

    /***********************************************************************************************
     *                           Helpers
     *********************************************************************************************/

    /**
     * Helper to add a call via CallsManager#addCall and verify the extension type depending on
     * the APIs that are leveraged.
     *
     * Note: The connection extras are not added into the call until the connection is successfully
     * created. This is usually the case when the call moves from the CONNECTING state into either
     * the DIALING/RINGING state. This would be the case for [CallsManager.EXTRA_VOIP_API_VERSION]
     * (handled by auto) as well as for [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED]
     * (see JetpackConnectionService#createSelfManagedConnection). Keep in mind that these extras
     * would not be available in [InCalLService#onCallAdded], but after
     * [Call#handleCreateConnectionSuccess] is invoked and the connection service extras are
     * propagated into the call details via [Call#putConnectionServiceExtras].
     *
     * @param callAttributesCompat for the call.
     * @param expectedType for call extension type.
     * @param waitForCallDetailExtras used for waiting on the call details extras to be non-null.
     * @param extraToInclude as part of the call extras.
     */
    private fun addAndVerifyCallExtensionType(
        callAttributesCompat: CallAttributesCompat,
        @InCallServiceCompat.Companion.CapabilityExchangeType expectedType: Int,
        waitForCallDetailExtras: Boolean = true,
        extraToInclude: Pair<String, Boolean>? = null
    ) {
        runBlocking {
            assertWithinTimeout_addCall(callAttributesCompat) {
                launch {
                    // Enforce waiting logic to ensure that the call details extras are populated.
                    val call = configureCallWithSanitizedExtras(
                        waitForCallDetailExtras, extraToInclude)

                    // Assert call extension type.
                    assertEquals(expectedType, inCallServiceCompat.resolveCallExtensionsType(call))
                    // Always send disconnect signal if possible.
                    assertEquals(
                        CallControlResult.Success(),
                        disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                }
            }
        }
    }

    /**
     * Helper to add a call via CallsManager#addCall and verify that the capabilities are properly
     * negotiated between the VOIP app and ICS's given that the ICS supports the exchange
     * ([InCallServiceCompat.CAPABILITY_EXCHANGE]).
     *
     * @param callAttributesCompat for the call.
     * @param waitForCallDetailExtras used for waiting on the call details extras to be non-null.
     * @param extraToInclude as part of the call extras.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun verifyICSCapabilitiesNegotiatedWithVoipApp(
        callAttributesCompat: CallAttributesCompat,
        waitForCallDetailExtras: Boolean = true,
        extraToInclude: Pair<String, Boolean>? = null
    ) {
        runBlocking {
            assertWithinTimeout_addCall(callAttributesCompat) {
                launch {
                    // Enforce waiting logic to ensure that the call details extras are populated.
                    val call = configureCallWithSanitizedExtras(
                        waitForCallDetailExtras, extraToInclude)
                    // Initiate capability negotiation from ICS side with a new coroutine via
                    // GlobalScope. This will ensure that the client/server tasks can operate
                    // independently. Note that runBlocking uses an internal dispatcher (not the
                    // same as Dispatchers.Main).
                    GlobalScope.async {
                        Assert.assertTrue(inCallServiceCompat.initiateICSCapabilityExchange(call))
                    }.await()
                    // Always send disconnect signal if possible.
                    assertEquals(
                        CallControlResult.Success(),
                        disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                }
            }
        }
    }

    /**
     * Helper to retrieve the call from MockInCallService and wait for any call detail extras
     * to be populated, if needed.
     */
    private suspend fun configureCallWithSanitizedExtras(
        waitForCallDetailExtras: Boolean,
        extraToInclude: Pair<String, Boolean>? = null
    ): Call {
        val call = TestUtils.waitOnInCallServiceToReachXCalls(1)
        Assert.assertNotNull("The returned Call object is <NULL>", call!!)

        // Enforce waiting logic to ensure that the call details extras are populated.
        if (waitForCallDetailExtras) {
            TestUtils.waitOnCallExtras(call)
        }

        val callDetails = call.details
        // Clear out extras to isolate the testing scenarios.
        call.details.extras?.clear()
        // Add extraToInclude for testing.
        if (extraToInclude != null) {
            callDetails.extras?.putBoolean(extraToInclude.first, extraToInclude.second)
        }
        return call
    }

    private fun configureCapabilityExchangeTypeTest(): Pair<String, Boolean>? {
        if (TestUtils.buildIsAtLeastU()) {
            Log.w(TAG, "Setting up v2 tests for U+ device")
            setUpV2Test()
        } else {
            Log.w(TAG, "Setting up backwards compatibility tests for pre-U device")
            setUpBackwardsCompatTest()
        }

        // Add EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED for pre-U testing
        return if (!TestUtils.buildIsAtLeastU())
            Pair(CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED, true)
        else null
    }
}
