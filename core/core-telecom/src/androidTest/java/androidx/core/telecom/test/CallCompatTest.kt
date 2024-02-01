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
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.CapabilityExchange
import androidx.core.telecom.extensions.ParticipantClientActionsImpl
import androidx.core.telecom.extensions.voip.CapabilityExchangeListener
import androidx.core.telecom.extensions.voip.VoipExtensionManager
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallCompat
import androidx.core.telecom.internal.InCallServiceCompat
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class verifies the [CallCompat] functionality around resolving the call
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
class CallCompatTest : BaseTelecomTest() {
    private lateinit var callCompat: CallCompat
    private var mScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

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
        internal val TAG = CallCompatTest::class.simpleName
        private const val ICS_TEST_ID = 1
    }

    @Before
    fun setUp() {
        Utils.resetUtils()
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
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
     * In the case that the ICS and VOIP apps both support the Participant [Capability],
     * [CallsManager.KICK_PARTICIPANT_ACTION], and [CallsManager.RAISE_HAND_ACTION] this test
     * asserts that [CallCompat.setupSupportedCapabilities] successfully sets
     * [ParticipantClientActionsImpl.mIsParticipantExtensionSupported] to true and populates
     * [ParticipantClientActionsImpl.mNegotiatedActions] with both supported actions.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCompareSupportedCaps_ParticipantSupportedFully() {
        // Add EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED for pre-U testing
        val backwardsCompatExtra = configureCapabilityExchangeTypeTest()

        val icsParticipantCap = initializeCapability(CallsManager.PARTICIPANT,
            CallsManager.RAISE_HAND_ACTION, CallsManager.KICK_PARTICIPANT_ACTION)
        val voipParticipantCap = initializeCapability(CallsManager.PARTICIPANT,
            CallsManager.RAISE_HAND_ACTION, CallsManager.KICK_PARTICIPANT_ACTION)
        val expectedNegotiatedActions = intArrayOf(CallsManager.RAISE_HAND_ACTION,
            CallsManager.KICK_PARTICIPANT_ACTION)

        verifyCompareSupportedCapabilitiesSuccessful(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            // Waiting is not required for U+ testing
            waitForCallDetailExtras = !TestUtils.buildIsAtLeastU(),
            extraToInclude = backwardsCompatExtra,
            icsCap = icsParticipantCap,
            voipCap = voipParticipantCap,
            expectedIsParticipantExtensionSupported = true,
            expectedNegotiatedActions = expectedNegotiatedActions
        )
    }

    /**
     * In the case that the ICS and VOIP apps both support the Participant [Capability]
     * but only [CallsManager.RAISE_HAND_ACTION], this test asserts that
     * [CallCompat.setupSupportedCapabilities] successfully sets
     * [ParticipantClientActionsImpl.mIsParticipantExtensionSupported] to true and populates
     * [ParticipantClientActionsImpl.mNegotiatedActions] with the supported action.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCompareSupportedCaps_ParticipantRaiseHandSupportOnly() {
        // Add EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED for pre-U testing
        val backwardsCompatExtra = configureCapabilityExchangeTypeTest()

        val icsParticipantCap = initializeCapability(CallsManager.PARTICIPANT,
            CallsManager.RAISE_HAND_ACTION)
        val voipParticipantCap = initializeCapability(CallsManager.PARTICIPANT,
            CallsManager.RAISE_HAND_ACTION)
        val expectedNegotiatedActions = intArrayOf(CallsManager.RAISE_HAND_ACTION)

        verifyCompareSupportedCapabilitiesSuccessful(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            // Waiting is not required for U+ testing
            waitForCallDetailExtras = !TestUtils.buildIsAtLeastU(),
            extraToInclude = backwardsCompatExtra,
            icsCap = icsParticipantCap,
            voipCap = voipParticipantCap,
            expectedIsParticipantExtensionSupported = true,
            expectedNegotiatedActions = expectedNegotiatedActions
        )
    }

    /**
     * In the case that the ICS and VOIP apps both support the Participant [Capability]
     * but none of the same actions, this test asserts that
     * [CallCompat.setupSupportedCapabilities] successfully sets
     * [ParticipantClientActionsImpl.mIsParticipantExtensionSupported] to true and leaves
     * [ParticipantClientActionsImpl.mNegotiatedActions] empty.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCompareSupportedCaps_ParticipantSupportedEmptyNegotiatedActions() {
        // Add EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED for pre-U testing
        val backwardsCompatExtra = configureCapabilityExchangeTypeTest()

        val icsParticipantCap = initializeCapability(CallsManager.PARTICIPANT,
            CallsManager.KICK_PARTICIPANT_ACTION)
        val voipParticipantCap = initializeCapability(CallsManager.PARTICIPANT,
            CallsManager.RAISE_HAND_ACTION)

        verifyCompareSupportedCapabilitiesSuccessful(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            // Waiting is not required for U+ testing
            waitForCallDetailExtras = !TestUtils.buildIsAtLeastU(),
            extraToInclude = backwardsCompatExtra,
            icsCap = icsParticipantCap,
            voipCap = voipParticipantCap,
            expectedIsParticipantExtensionSupported = true,
            expectedNegotiatedActions = IntArray(0)
        )
    }

    /**
     * In the case that the ICS app supports the Participant [Capability] but the VoIP app doesn't,
     * this test asserts that [CallCompat.setupSupportedCapabilities] successfully sets
     * [ParticipantClientActionsImpl.mIsParticipantExtensionSupported] to false and leaves
     * [ParticipantClientActionsImpl.mNegotiatedActions] empty.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCompareSupportedCaps_IcsSupportsParticipantVoipDoesNot() {
        // Add EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED for pre-U testing
        val backwardsCompatExtra = configureCapabilityExchangeTypeTest()

        val icsParticipantCap = initializeCapability(CallsManager.PARTICIPANT,
            CallsManager.KICK_PARTICIPANT_ACTION)
        val voipCallSilenceCap = initializeCapability(CallsManager.CALL_SILENCE)

        verifyCompareSupportedCapabilitiesSuccessful(
            TestUtils.OUTGOING_CALL_ATTRIBUTES,
            // Waiting is not required for U+ testing
            waitForCallDetailExtras = !TestUtils.buildIsAtLeastU(),
            extraToInclude = backwardsCompatExtra,
            icsCap = icsParticipantCap,
            voipCap = voipCallSilenceCap,
            expectedIsParticipantExtensionSupported = false,
            expectedNegotiatedActions = IntArray(0)
        )
    }

    /***********************************************************************************************
     *                           Helpers
     *********************************************************************************************/

    /**
     * Helper to add a call via CallsManager#addCall and verify that the capabilities are properly
     * negotiated between the VOIP app and ICS's given that the ICS supports the exchange
     * ([InCallServiceCompat.CAPABILITY_EXCHANGE]).
     *
     * @param callAttributesCompat for the call.
     * @param waitForCallDetailExtras used for waiting on the call details extras to be non-null.
     * @param extraToInclude as part of the call extras.
     */
    private fun verifyICSCapabilitiesNegotiatedWithVoipApp(
        callAttributesCompat: CallAttributesCompat,
        waitForCallDetailExtras: Boolean = true,
        extraToInclude: Pair<String, Boolean>? = null
    ) {
        runBlocking {
            assertWithinTimeout_addCall(callAttributesCompat) {
                launch {
                    try {
                        // Enforce waiting logic to ensure that call details extras are populated.
                        val call = configureCallWithSanitizedExtras(
                            waitForCallDetailExtras, extraToInclude)

                        callCompat = CallCompat(call, mScope)

                        mScope.async {
                            callCompat.startCapabilityExchange()
                            Assert.assertTrue(callCompat.capExchangeSetupComplete)
                        }.await()
                    } finally {
                        // Always send disconnect signal if possible.
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                    }
                }
            }
        }
    }

    /**
     * Helper to verify that the capabilities are properly compared between the VOIP app and ICS's
     * given that the ICS supports the exchange ([InCallServiceCompat.CAPABILITY_EXCHANGE]).
     *
     * @param callAttributesCompat for the call.
     * @param waitForCallDetailExtras used for waiting on the call details extras to be non-null.
     * @param extraToInclude as part of the call extras.
     * @param icsCap the Capability that the ICS app supports.
     * @param voipCap the Capability that the VoIP app supports.
     * @param expectedIsParticipantExtensionSupported based the icsCap and voipCap configurations.
     * @param expectedNegotiatedActions the common actions that are supported by both ICS and VoIP.
     */
    // Todo: Expand this helper method to support verifying multiple capabilities per voip/ics app.
    private fun verifyCompareSupportedCapabilitiesSuccessful(
        callAttributesCompat: CallAttributesCompat,
        waitForCallDetailExtras: Boolean = true,
        extraToInclude: Pair<String, Boolean>? = null,
        icsCap: Capability,
        voipCap: Capability,
        expectedIsParticipantExtensionSupported: Boolean,
        expectedNegotiatedActions: IntArray
    ) {
        runBlocking {
            assertWithinTimeout_addCall(callAttributesCompat) {
                launch {
                    try {
                        // Enforce waiting logic to ensure that call details extras are populated.
                        val call = configureCallWithSanitizedExtras(
                            waitForCallDetailExtras, extraToInclude)

                        // Setup the CapExchange and CallCompat instances for testing:
                        val voipCaps: MutableList<Capability> = mutableListOf(voipCap)
                        val capExchange = createCapExchange(voipCaps)
                        callCompat = CallCompat(call, mScope)
                        callCompat.icsCapabilities.add(icsCap)
                        callCompat.addExtension { }

                        // Directly invoke the helper method that handles capability comparison:
                        callCompat.setupSupportedCapabilities(capExchange)

                        // Check all expected values:
                        val participantListener = callCompat.participantStateListener
                        assertEquals(participantListener.mIsParticipantExtensionSupported,
                            expectedIsParticipantExtensionSupported)
                        assertEquals(participantListener.mNegotiatedActions.size,
                            expectedNegotiatedActions.size)
                        for (action in expectedNegotiatedActions) {
                            assertTrue(participantListener.mNegotiatedActions.contains(action))
                        }
                    } finally {
                        // Always send disconnect signal if possible.
                        assertEquals(
                            CallControlResult.Success(),
                            disconnect(DisconnectCause(DisconnectCause.LOCAL)))
                    }
                }
            }
        }
    }

    /**
     * Helper to initialize and populate an instance of [Capability] for unit testing purposes.
     */
    private fun initializeCapability(featureId: Int, vararg actions: Int): Capability {
        val cap = Capability()
        cap.featureId = featureId
        cap.featureVersion = 1
        cap.supportedActions = actions
        return cap
    }

    /**
     * Helper to initialize and populate an instance of [CapabilityExchange] for unit testing purposes.
     */
    private fun createCapExchange(voipCaps: MutableList<Capability>): CapabilityExchange {
        val capExchange = CapabilityExchange()
        val callChannels = CallChannels()
        val emptyExtensions = emptyList<Capability>()
        val voipExtensionManager =
            VoipExtensionManager(mContext, mWorkerContext, callChannels, emptyExtensions)
        val capExchangeListener = CapabilityExchangeListener(voipExtensionManager, ICS_TEST_ID)
        capExchange.voipCapabilities = voipCaps
        capExchange.capabilityExchangeListener = capExchangeListener
        return capExchange
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
