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
import androidx.core.telecom.InCallServiceCompat
import androidx.core.telecom.extensions.CallExtensionCreator
import androidx.core.telecom.extensions.CallExtensionsScope
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.Extensions
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.ParticipantExtension
import androidx.core.telecom.extensions.ParticipantExtensionRemote
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.test.VoipAppWithExtensions.VoipAppWithExtensionsControl
import androidx.core.telecom.test.VoipAppWithExtensions.VoipAppWithExtensionsControlLocal
import androidx.core.telecom.test.utils.BaseTelecomTest
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
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
                id = Extensions.PARTICIPANT,
                version = ParticipantExtension.VERSION,
                actions =
                    setOf(
                        ParticipantExtension.RAISE_HAND_ACTION,
                        ParticipantExtension.KICK_PARTICIPANT_ACTION
                    )
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

    internal class CachedParticipants(scope: CallExtensionsScope) {
        private val participantState = MutableStateFlow<Set<Participant>>(emptySet())
        private val activeParticipantState = MutableStateFlow<Participant?>(null)
        val extension =
            scope.addParticipantExtension(
                onActiveParticipantChanged = activeParticipantState::emit,
                onParticipantsUpdated = participantState::emit
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

    internal class CachedRaisedHands(extension: ParticipantExtensionRemote) {
        private val raisedHands = MutableStateFlow<Set<Participant>>(emptySet())
        val action = extension.addRaiseHandAction(raisedHands::emit)

        suspend fun waitForRaisedHands(expected: Set<Participant>) {
            val result =
                withTimeoutOrNull(ICS_EXTENSION_UPDATE_TIMEOUT_MS) {
                    raisedHands.first { it == expected }
                }
            assertEquals("Never received expected raised hands update", expected, result)
        }
    }

    /**
     * Grant READ_PHONE_NUMBERS permission as part of testing
     * [InCallServiceCompat#resolveCallExtensionsType].
     */
    @get:Rule
    val readPhoneNumbersRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_PHONE_NUMBERS)!!

    @get:Rule val voipAppServiceRule: ServiceTestRule = ServiceTestRule()

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

    @Before
    fun beforeTest() {
        setupParameterizedTest()
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
        usingIcs { ics ->
            val voipAppControl = bindToVoipAppWithExtensions()
            // No Capability Exchange sequence occurs between VoIP app and ICS because ICS doesn't
            // support extensions
            createAndVerifyVoipCall(
                voipAppControl,
                listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
                parameters.direction
            )
            TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
            try {
                // Send updateParticipants to ensure there is no error/exception
                voipAppControl.updateParticipants(listOf(TestUtils.getDefaultParticipant()))
            } catch (e: Exception) {
                fail("calling extension methods should not result in any exceptions: Exception: $e")
            }
        }
    }

    /**
     * Create a new VOIP call and use [InCallServiceCompat.connectExtensions] in the ICS to connect
     * to the VOIP call. Once complete, use the [CallExtensionsScope.registerExtension] method to
     * register an unknown extension and ensure we get the correct null indication.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testIcsExtensionsCreationUnknownCapability() = runBlocking {
        usingIcs { ics ->
            val voipAppControl = bindToVoipAppWithExtensions()
            createAndVerifyVoipCall(
                voipAppControl,
                listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
                parameters.direction
            )

            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
            var hasConnected = false
            // Manually connect extensions here to exercise the CallExtensionsScope class
            with(ics) {
                connectExtensions(call) {
                    // Create an extension that the VOIP app does not know about and ensure that
                    // we receive a null response during negotiation so we can notify the ICS of the
                    // state of that extension
                    val nonexistentRemote = registerInvalidExtension(this)
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
    }

    /**
     * Create a VOIP call with a participants extension and attach participant Call extensions.
     * Verify that all of the participant extension functions work as expected.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsWithParticipants() = runBlocking {
        usingIcs { ics ->
            val voipAppControl = bindToVoipAppWithExtensions()
            val callback = TestCallCallbackListener(this)
            voipAppControl.setCallback(callback)
            val voipCallId =
                createAndVerifyVoipCall(
                    voipAppControl,
                    listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
                    parameters.direction
                )

            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
            var hasConnected = false
            with(ics) {
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

                        kickParticipantAction.requestKickParticipant(
                            TestUtils.getDefaultParticipant()
                        )
                        callback.waitForKickParticipant(
                            voipCallId,
                            TestUtils.getDefaultParticipant()
                        )

                        call.disconnect()
                    }
                }
            }
            assertTrue("onConnected never received", hasConnected)
        }
    }

    /**
     * =========================================================================================
     * Helpers
     * =========================================================================================
     */
    private fun registerInvalidExtension(
        scope: CallExtensionsScope,
    ): CompletableDeferred<CapabilityExchangeListenerRemote?> {
        val deferredVal = CompletableDeferred<CapabilityExchangeListenerRemote?>()
        scope.registerExtension {
            CallExtensionCreator(
                extensionCapability =
                    createCapability(id = 8675309, version = 42, actions = emptySet()),
                onExchangeComplete = { capability, remote ->
                    assertNull("Expected null capability", capability)
                    deferredVal.complete(remote)
                }
            )
        }
        return deferredVal
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

    /** Sets up the test based on the parameters set for the run */
    private fun setupParameterizedTest() {
        if (Build.VERSION.SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
            assumeTrue(
                "skipping this test, must be running at least U",
                parameters.serviceSource == SERVICE_SOURCE_CONNSRV
            )
        }
        when (parameters.serviceSource) {
            SERVICE_SOURCE_V2 -> setUpV2Test()
            SERVICE_SOURCE_CONNSRV -> setUpBackwardsCompatTest()
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
        return ITestAppControl.Stub.asInterface(voipAppServiceRule.bindService(serviceIntent))
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
}
