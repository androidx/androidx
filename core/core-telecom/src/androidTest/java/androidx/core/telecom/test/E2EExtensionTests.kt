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

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.CallExtensionCreationDelegate
import androidx.core.telecom.extensions.CallExtensionsScope
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.ParticipantClientActions
import androidx.core.telecom.extensions.ParticipantClientExtensionNew
import androidx.core.telecom.extensions.addKickParticipantAction
import androidx.core.telecom.extensions.addParticipantExtension
import androidx.core.telecom.extensions.addRaiseHandAction
import androidx.core.telecom.extensions.getParticipantActions
import androidx.core.telecom.internal.CallCompat
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.internal.InCallServiceCompat
import androidx.core.telecom.test.VoipAppWithExtensions.VoipAppWithExtensionsControl
import androidx.core.telecom.test.VoipAppWithExtensions.VoipAppWithExtensionsControlLocal
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.InCallServiceType
import androidx.core.telecom.test.utils.MockInCallServiceDelegate
import androidx.core.telecom.test.utils.TestCallCallbackListener
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.ServiceTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Tests ensuring that extensions on an incoming/outgoing call work as expected for both V2 and
 * ConnSrv implementations of CallsManager.
 */
@SdkSuppress(minSdkVersion = VERSION_CODES.O /* api=26 */)
@OptIn(ExperimentalAppActions::class)
@RunWith(Parameterized::class)
class E2EExtensionTests(private val parameters: TestParameters) : BaseTelecomTest() {
    companion object {
        private const val ICS_EXTENSION_UPDATE_TIMEOUT_MS = 1000L
        // Use the VOIP service that uses V2 APIs (VoipAppExtensionControl)
        private const val SERVICE_SOURCE_V2 = 1
        // Use the VOIP service that uses bkwds compat APIs (VoipAppExtensionControl)
        private const val SERVICE_SOURCE_CONNSRV = 2
        // Set up a Capability with all actions supported.
        private val CAPABILITY_PARTICIPANT_WITH_ACTIONS =
            createCapability(
                id = CallsManager.PARTICIPANT,
                version = 1,
                actions =
                    setOf(CallsManager.RAISE_HAND_ACTION, CallsManager.KICK_PARTICIPANT_ACTION)
            )

        /** Provide all the combinations of parameters that should be tested for each run */
        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Iterable<TestParameters> {
            return arrayListOf(
                    // V2 Tests with incoming/outgoing calls
                    TestParameters(SERVICE_SOURCE_V2, CallAttributesCompat.DIRECTION_INCOMING),
                    TestParameters(SERVICE_SOURCE_V2, CallAttributesCompat.DIRECTION_OUTGOING),
                    // Bkwds compat tests with incoming/outgoing calls
                    TestParameters(SERVICE_SOURCE_CONNSRV, CallAttributesCompat.DIRECTION_INCOMING),
                    TestParameters(SERVICE_SOURCE_CONNSRV, CallAttributesCompat.DIRECTION_OUTGOING)
                )
                .toList()
        }

        fun createCapability(id: Int, version: Int, actions: Set<Int>): Capability {
            return Capability().apply {
                featureId = id
                featureVersion = version
                supportedActions = actions.toIntArray()
            }
        }
    }

    /**
     * Grant READ_PHONE_NUMBERS permission as part of testing
     * [InCallServiceCompat#resolveCallExtensionsType].
     */
    @get:Rule
    val readPhoneNumbersRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_PHONE_NUMBERS)!!

    @get:Rule val serviceRule: ServiceTestRule = ServiceTestRule()

    data class TestParameters(val serviceSource: Int, val direction: Int) {
        override fun toString(): String {
            return "${directionToString(direction)}-${sourceToString(serviceSource)}"
        }

        private fun sourceToString(source: Int): String {
            return when (source) {
                SERVICE_SOURCE_V2 -> "V2"
                SERVICE_SOURCE_CONNSRV -> "CS"
                else -> "unknown ($source)"
            }
        }

        private fun directionToString(direction: Int): String {
            return when (direction) {
                CallAttributesCompat.DIRECTION_INCOMING -> "I"
                CallAttributesCompat.DIRECTION_OUTGOING -> "O"
                else -> "unknown ($direction)"
            }
        }
    }

    /**
     * Verify that when we have a VOIP app that has extensions set up and one ICS that is not using
     * [InCallServiceCompat], no extensions are supported. In this case, the VOIP app will never
     * receive a request from the InCallService to start capability exchange, so any extensions
     * updates from the VOIP side will no-op.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipWithExtensionsAndInCallServiceWithout() = runBlocking {
        setupParameterizedTest(icsExtensionsConfig = InCallServiceType.ICS_WITHOUT_EXTENSIONS)
        val voipAppControl = bindToVoipAppWithExtensions()
        // No Capability Exchange sequence occurs between VoIP app and ICS because ICS doesn't
        // support extensions
        createAndVerifyVoipCall(
            voipAppControl,
            listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
            parameters.direction
        )
        TestUtils.waitOnInCallServiceToReachXCalls(1)
        try {
            // Send updateParticipants to ensure there is no error/exception
            voipAppControl.updateParticipants(listOf(TestUtils.getDefaultParticipant()))
        } catch (e: Exception) {
            fail("calling extension methods should not result in any exceptions: Exception: $e")
        }
    }

    /**
     * Create a new VOIP call and use [connectExtensions] in the ICS to connect to the VOIP call.
     * Once complete, use the [CallExtensionsScope.registerExtension] method to register an unknown
     * extension and ensure we get the correct null indication.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testIcsExtensionsCreationUnknownCapability() = runBlocking {
        setupParameterizedTest(icsExtensionsConfig = InCallServiceType.ICS_WITH_EXTENSIONS_NEW)
        val voipAppControl = bindToVoipAppWithExtensions()
        createAndVerifyVoipCall(
            voipAppControl,
            listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
            parameters.direction
        )

        val call = TestUtils.waitOnInCallServiceToReachXCalls(1)!!
        var hasConnected = false
        // Manually connect extensions here to exercise the CallExtensionsScope class
        with(MockInCallServiceDelegate.getServiceWithExtensions()!!) {
            connectExtensions(call) {
                // Create an extension that the VOIP app does not know about and ensure that
                // we receive a null response during negotiation so we can notify the ICS of the
                // state of that extension
                val nonexistentRemote =
                    registerExtension(
                        this,
                        icsCapability =
                            createCapability(id = 8675309, version = 42, actions = emptySet()),
                        negotiatedCapability = null
                    )
                onConnected {
                    hasConnected = true
                    assertNull(
                        "Connection to remote should be null for features with no VOIP support",
                        nonexistentRemote.await()
                    )
                    call.disconnect()
                }
            }
        }
        assertTrue("onConnected never received", hasConnected)
    }

    /**
     * Create a VOIP call with a participants extension and attach participant Call extensions.
     * Verify that all of the participant extension functions work as expected.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsWithParticipantsNew() = runBlocking {
        setupParameterizedTest(icsExtensionsConfig = InCallServiceType.ICS_WITH_EXTENSIONS_NEW)
        val voipAppControl = bindToVoipAppWithExtensions()
        val callback = TestCallCallbackListener(this)
        voipAppControl.setCallback(callback)
        val voipCallId =
            createAndVerifyVoipCall(
                voipAppControl,
                listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
                parameters.direction
            )

        val call = TestUtils.waitOnInCallServiceToReachXCalls(1)!!
        var hasConnected = false
        with(MockInCallServiceDelegate.getServiceWithExtensions()!!) {
            connectExtensions(call) {
                val participants = CachedParticipants(this)
                val raiseHandAction = CachedRaisedHands(participants.extension)
                val kickParticipantAction = participants.extension.addKickParticipantAction()
                onConnected {
                    hasConnected = true
                    // Test VOIP -> ICS connection by updating state
                    participants.waitForParticipants(emptySet())
                    participants.waitForActiveParticipant(null)

                    voipAppControl.updateParticipants(listOf(TestUtils.getDefaultParticipant()))
                    participants.waitForParticipants(setOf(TestUtils.getDefaultParticipant()))

                    voipAppControl.updateActiveParticipant(TestUtils.getDefaultParticipant())
                    participants.waitForActiveParticipant(TestUtils.getDefaultParticipant())

                    voipAppControl.updateRaisedHands(listOf(TestUtils.getDefaultParticipant()))
                    raiseHandAction.waitForRaisedHands(setOf(TestUtils.getDefaultParticipant()))

                    // Test ICS -> VOIP connection by sending events
                    raiseHandAction.action.requestRaisedHandStateChange(true)
                    callback.waitForRaiseHandState(voipCallId, true)

                    kickParticipantAction.requestKickParticipant(TestUtils.getDefaultParticipant())
                    callback.waitForKickParticipant(voipCallId, TestUtils.getDefaultParticipant())

                    call.disconnect()
                }
            }
        }
        assertTrue("onConnected never received", hasConnected)
    }

    /**
     * Validate that for a bound VOIP app, when a new call is added, the InCallService receives the
     * new call and can use all Participant extension interfaces.
     */
    // TODO: Remove in a follow up to use the new version of the API instead.
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsWithParticipants() = runBlocking {
        // test set up
        setupParameterizedTest(
            icsExtensionsConfig = InCallServiceType.ICS_WITH_EXTENSIONS_OLD,
            setOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS)
        )

        // bind to app
        val voipAppControl = bindToVoipAppWithExtensions()
        val callback = TestCallCallbackListener(this)
        voipAppControl.setCallback(callback)

        // add a call to verify capability exchange IS made with ICS
        val voipCallId =
            createAndVerifyVoipCall(
                voipAppControl,
                listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
                parameters.direction
            )
        val icsCall = getLastIcsCall()

        // verify cap exchange is complete
        val actions =
            getAndVerifyParticipantActions(
                icsCall,
                CAPABILITY_PARTICIPANT_WITH_ACTIONS.supportedActions.toSet()
            )

        // Check state updates from VOIP -> ICS are working

        // participant state updates
        // initial condition - no participants
        verifyIcsParticipantsMatch(actions, emptySet())
        // Update to include one participant
        val expectedParticipants = setOf(TestUtils.getDefaultParticipant())
        voipAppControl.updateParticipants(expectedParticipants.toList())
        verifyIcsParticipantsMatch(actions, expectedParticipants)

        // active participant updates
        // initial condition - no active participant
        verifyIcsActiveParticipantMatches(actions, CallsManager.NULL_PARTICIPANT_ID)
        // update to include one active participant
        voipAppControl.updateActiveParticipant(TestUtils.getDefaultParticipant())
        verifyIcsActiveParticipantMatches(actions, TestUtils.getDefaultParticipant().id)

        // raised hands updates
        // initial state = no raised hands
        verifyIcsRaisedHandsMatch(actions, emptySet())
        // update to include one participant raising their hand
        voipAppControl.updateRaisedHands(expectedParticipants.toList())
        verifyIcsRaisedHandsMatch(actions, expectedParticipants.map { it.id }.toSet())

        // Check actions updates from ICS -> VOIP

        actions.toggleHandRaised(true)
        callback.waitForRaiseHandState(voipCallId, true)

        actions.kickParticipant(TestUtils.getDefaultParticipant())
        callback.waitForKickParticipant(voipCallId, TestUtils.getDefaultParticipant())
    }

    /**
     * =========================================================================================
     * Helpers
     * =========================================================================================
     */
    private fun registerExtension(
        scope: CallExtensionsScope,
        icsCapability: Capability,
        negotiatedCapability: Capability?
    ): CompletableDeferred<CapabilityExchangeListenerRemote?> {
        val deferredVal = CompletableDeferred<CapabilityExchangeListenerRemote?>()
        // Register a test extension that will receive the PARTICIPANT capability
        scope.registerExtension {
            CallExtensionCreationDelegate(
                capability = icsCapability,
                receiver = { capability, remote ->
                    assertEquals(
                        "Expected PARTICIPANT capability",
                        negotiatedCapability?.featureId,
                        capability?.featureId
                    )
                    assertEquals(
                        "Expected version to equal the lowest common version",
                        negotiatedCapability?.featureVersion,
                        capability?.featureVersion
                    )
                    assertEquals(
                        "The negotiated actions should be the actions supported by both",
                        negotiatedCapability?.supportedActions?.toSet(),
                        capability?.supportedActions?.toSet()
                    )
                    deferredVal.complete(remote)
                }
            )
        }
        return deferredVal
    }

    /** Verify that the ICS state for raised hands matches the VOIP app's updates to the state */
    private suspend fun verifyIcsRaisedHandsMatch(
        actions: ParticipantClientActions,
        expectedRaisedHands: Set<Int>
    ) {
        assertResult("raised hands do not match", expectedRaisedHands) {
            actions.raisedHandsStateFlow.value
        }
    }

    /** Verify that the ICS state for participants matches the VOIP app's updates to the state */
    private suspend fun verifyIcsParticipantsMatch(
        actions: ParticipantClientActions,
        expectedVoipParticipants: Set<Participant>
    ) {
        assertResult("participants do not match", expectedVoipParticipants) {
            actions.participantsStateFlow.value
        }
    }

    /**
     * Verify that the ICS state for the active participant matches the VOIP app's updates to the
     * state.
     */
    private suspend fun verifyIcsActiveParticipantMatches(
        actions: ParticipantClientActions,
        expectedActiveParticipantId: Int
    ) {
        assertResult("active participant doesn't match", expectedActiveParticipantId) {
            actions.activeParticipantStateFlow.value
        }
    }

    /**
     * Creates a VOIP call using the specified capabilities and direction and then verifies that it
     * was set up.
     */
    private fun createAndVerifyVoipCall(
        voipAppControl: ITestAppControl,
        capabilities: List<Capability>,
        direction: Int
    ): String {
        // add a call to verify capability exchange IS made with ICS
        val voipCallId =
            voipAppControl.addCall(
                capabilities,
                direction == CallAttributesCompat.DIRECTION_OUTGOING
            )
        assertTrue("call could not be created", voipCallId.isNotEmpty())
        return voipCallId
    }

    /** Waits until there is a call in the ICS and then returns it */
    private suspend fun getLastIcsCall(): CallCompat {
        TestUtils.waitOnInCallServiceToReachXCalls(1)
        val icsCall = MockInCallServiceDelegate.getLastCall()
        assertNotNull("ICS call could not be found", icsCall)
        return icsCall!!
    }

    /**
     * Waits for capability exchange to complete for the ICS and then returns the actions interface
     * that can be used to interact with extensions.
     */
    private suspend fun getAndVerifyParticipantActions(
        icsCall: CallCompat,
        expectedActions: Set<Int>
    ): ParticipantClientActions {
        val actions: ParticipantClientActions? =
            waitForResult({ it != null }) { icsCall.getParticipantActions().getOrNull() }
        assertNotNull("actions not found", actions)
        assertResult("negotiated actions incorrect", expectedActions, actions!!::negotiatedActions)
        return actions
    }

    /** Sets up the test based on the parameters set for the run */
    private fun setupParameterizedTest(
        icsExtensionsConfig: InCallServiceType = InCallServiceType.ICS_WITHOUT_EXTENSIONS,
        icsCapabilities: Set<Capability> = emptySet()
    ) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
            assumeTrue(
                "skipping this test, must be running at least U",
                parameters.serviceSource == SERVICE_SOURCE_CONNSRV
            )
        }
        when (parameters.serviceSource) {
            SERVICE_SOURCE_V2 -> {
                when (icsExtensionsConfig) {
                    InCallServiceType.ICS_WITH_EXTENSIONS_NEW -> setUpV2TestWithExtensionsNew()
                    InCallServiceType.ICS_WITH_EXTENSIONS_OLD -> {
                        setUpV2TestWithExtensionsOld(icsCapabilities)
                    }
                    InCallServiceType.ICS_WITHOUT_EXTENSIONS -> setUpV2Test()
                }
            }
            SERVICE_SOURCE_CONNSRV -> {
                setUpBackwardsCompatTest()
                when (icsExtensionsConfig) {
                    InCallServiceType.ICS_WITH_EXTENSIONS_NEW -> {
                        setInCallService(InCallServiceType.ICS_WITH_EXTENSIONS_NEW)
                    }
                    InCallServiceType.ICS_WITH_EXTENSIONS_OLD -> {
                        setInCallService(InCallServiceType.ICS_WITH_EXTENSIONS_OLD, icsCapabilities)
                    }
                    InCallServiceType.ICS_WITHOUT_EXTENSIONS -> {
                        setInCallService(InCallServiceType.ICS_WITHOUT_EXTENSIONS)
                    }
                }
            }
        }
    }

    /** Bind to the service providing VOIP calls */
    private fun bindToVoipAppWithExtensions(): ITestAppControl {
        val serviceIntent =
            if (parameters.serviceSource == SERVICE_SOURCE_V2) {
                Intent(
                    InstrumentationRegistry.getInstrumentation().context,
                    VoipAppWithExtensionsControl::class.java
                )
            } else {
                Intent(
                    InstrumentationRegistry.getInstrumentation().context,
                    VoipAppWithExtensionsControlLocal::class.java
                )
            }
        return ITestAppControl.Stub.asInterface(serviceRule.bindService(serviceIntent))
    }

    /**
     * Tests the value returned from the [supplier] using [predicate] and retries until the criteria
     * is met. Retries every second for up to 5 seconds.
     */
    private suspend fun <R> waitForResult(predicate: (R?) -> Boolean, supplier: () -> R): R? {
        var result = supplier()
        withTimeoutOrNull(5000) {
            while (!predicate(result)) {
                yield()
                delay(1000)
                result = supplier()
            }
        }
        return result
    }

    /**
     * Assert that an expected result is received from the consumer block. Retries every second for
     * 5 seconds. If the [expectedResult] is not found, the assertion fails and [failMessage] is
     * used as a message.
     */
    private suspend fun <R> assertResult(
        failMessage: String,
        expectedResult: R,
        consumer: () -> R
    ) {
        val result = waitForResult({ it == expectedResult }, consumer)
        assertEquals(failMessage, expectedResult, result)
    }

    internal class CachedParticipants(scope: CallExtensionsScope) {
        private val participantState = MutableStateFlow<Set<Participant>>(emptySet())
        private val activeParticipantState = MutableStateFlow<Participant?>(null)
        val extension =
            scope.addParticipantExtension(
                activeParticipantsUpdate = activeParticipantState::emit,
                participantsUpdate = participantState::emit
            )

        suspend fun waitForParticipants(expected: Set<Participant>) {
            val result =
                withTimeoutOrNull(ICS_EXTENSION_UPDATE_TIMEOUT_MS) {
                    participantState.first { it == expected }
                }
            assertEquals("Never received expected participants update", expected, result)
        }

        suspend fun waitForActiveParticipant(expected: Participant?) {
            val result =
                withTimeoutOrNull(ICS_EXTENSION_UPDATE_TIMEOUT_MS) {
                    activeParticipantState.first { it == expected }
                }
            assertEquals("Never received expected active participant", expected, result)
        }
    }

    internal class CachedRaisedHands(extension: ParticipantClientExtensionNew) {
        private val raisedHands = MutableStateFlow<Set<Participant>>(emptySet())
        val action = extension.addRaiseHandAction(stateUpdate = raisedHands::emit)

        suspend fun waitForRaisedHands(expected: Set<Participant>) {
            val result =
                withTimeoutOrNull(ICS_EXTENSION_UPDATE_TIMEOUT_MS) {
                    raisedHands.first { it == expected }
                }
            assertEquals("Never received expected raised hands update", expected, result)
        }
    }
}
