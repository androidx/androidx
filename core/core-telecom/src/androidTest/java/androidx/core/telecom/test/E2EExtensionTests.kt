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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.InCallServiceCompat
import androidx.core.telecom.extensions.CallExtensionScope
import androidx.core.telecom.extensions.CallIconExtensionImpl
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.Extensions
import androidx.core.telecom.extensions.LocalCallSilenceExtensionImpl
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.ParticipantExtensionImpl
import androidx.core.telecom.extensions.ParticipantExtensionRemote
import androidx.core.telecom.test.VoipAppWithExtensions.VoipAppWithExtensionsControl
import androidx.core.telecom.test.VoipAppWithExtensions.VoipAppWithExtensionsControlLocal
import androidx.core.telecom.test.VoipAppWithExtensions.VoipCall.Companion.INITIAL_CALL_ICON_URI
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestCallCallbackListener
import androidx.core.telecom.test.utils.TestMuteStateReceiver
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.ServiceTestRule
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
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
        private val LOG_TAG = E2EExtensionTests::class.simpleName
        private const val ICS_EXTENSION_UPDATE_TIMEOUT_MS = 1000L
        // Use the VOIP service that uses V2 APIs (VoipAppExtensionControl)
        private const val SERVICE_SOURCE_V2 = 1
        // Use the VOIP service that uses bkwds compat APIs (VoipAppExtensionControl)
        private const val SERVICE_SOURCE_CONNSRV = 2
        // Set up a Capability with all actions supported.
        private val CAPABILITY_PARTICIPANT_WITH_ACTIONS =
            createCapability(
                id = Extensions.PARTICIPANT,
                version = ParticipantExtensionImpl.VERSION,
                actions =
                    setOf(
                        ParticipantExtensionImpl.RAISE_HAND_ACTION,
                        ParticipantExtensionImpl.KICK_PARTICIPANT_ACTION
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

    internal class CachedParticipants(scope: CallExtensionScope) {
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

    // TODO:: b/364316364 should assert on a per call basis
    internal class CachedLocalSilence(scope: CallExtensionScope) {
        private val isLocallySilenced = MutableStateFlow(false)

        val extension =
            scope.addLocalCallSilenceExtension(onIsLocallySilencedUpdated = isLocallySilenced::emit)

        suspend fun waitForLocalCallSilenceState(expected: Boolean) {
            val result =
                withTimeoutOrNull(ICS_EXTENSION_UPDATE_TIMEOUT_MS) {
                    isLocallySilenced.first { it == expected }
                }
            assertEquals("Never received local call silence state", expected, result)
        }
    }

    internal class CachedCallIcon(scope: CallExtensionScope) {
        private val callIconUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
        val extension = scope.addCallIconSupport(callIconUri::emit)

        suspend fun waitForLocalCallSilenceState(expected: Uri) {
            val result =
                withTimeoutOrNull(ICS_EXTENSION_UPDATE_TIMEOUT_MS) {
                    callIconUri.first { it == expected }
                }
            assertEquals("Never received Call Icon state", expected, result)
        }
    }

    internal class CachedRaisedHands(extension: ParticipantExtensionRemote) {
        private val raisedHands = MutableStateFlow<List<Participant>>(emptyList())
        val action = extension.addRaiseHandAction(raisedHands::emit)

        suspend fun waitForRaisedHands(expected: List<Participant>) {
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
    @get:Rule
    val modifyAudioRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.MODIFY_AUDIO_SETTINGS)!!

    @get:Rule val voipAppServiceRule: ServiceTestRule = ServiceTestRule()

    private val mRequestIdGenerator = AtomicInteger(0)

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
            val callback = TestCallCallbackListener(this)
            voipAppControl.setCallback(callback)
            // No Capability Exchange sequence occurs between VoIP app and ICS because ICS doesn't
            // support extensions
            createAndVerifyVoipCall(
                voipAppControl,
                callback,
                listOf(CAPABILITY_PARTICIPANT_WITH_ACTIONS),
                parameters.direction
            )
            TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)
            try {
                // Send updateParticipants to ensure there is no error/exception
                voipAppControl.updateParticipants(
                    listOf(TestUtils.getDefaultParticipantParcelable())
                )
            } catch (e: Exception) {
                fail("calling extension methods should not result in any exceptions: Exception: $e")
            }
        }
    }

    /**
     * Create a VOIP call with a participants extension and no actions Verify that all of the
     * participant extension functions work as expected.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsWithParticipants() = runBlocking {
        usingIcs { ics ->
            val voipAppControl = bindToVoipAppWithExtensions()
            val callback = TestCallCallbackListener(this)
            voipAppControl.setCallback(callback)
            createAndVerifyVoipCall(
                voipAppControl,
                callback,
                listOf(getParticipantCapability(emptySet())),
                parameters.direction
            )

            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
            var hasConnected = false
            with(ics) {
                connectExtensions(call) {
                    val participants = CachedParticipants(this)
                    onConnected {
                        hasConnected = true
                        assertTrue(
                            "Participants are not supported",
                            participants.extension.isSupported
                        )
                        // Wait for initial state
                        participants.waitForParticipants(emptySet())
                        participants.waitForActiveParticipant(null)
                        // Test VOIP -> ICS connection by updating state
                        val currentParticipants = TestUtils.generateParticipants(2)
                        voipAppControl.updateParticipants(
                            currentParticipants.map { it.toParticipantParcelable() }
                        )
                        participants.waitForParticipants(currentParticipants.toSet())
                        voipAppControl.updateActiveParticipant(
                            currentParticipants[0].toParticipantParcelable()
                        )
                        participants.waitForActiveParticipant(currentParticipants[0])

                        call.disconnect()
                    }
                }
            }
            assertTrue("onConnected never received", hasConnected)
        }
    }

    /**
     * On some Android versions (U & V), setting up an extension quickly after the ICS receives the
     * new call can cause the CAPABILITY_EXCHANGE event to drop internally in Telecom.
     *
     * Run 10 iterations of adding a new call + setting up extensions to test that we do not hit
     * this condition.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsWithParticipantsRace() = runBlocking {
        usingIcs { ics ->
            val iterations = 10
            val voipAppControl = bindToVoipAppWithExtensions()
            val callback = TestCallCallbackListener(this)
            voipAppControl.setCallback(callback)
            val failedTries = ArrayList<Int>()
            for (i in 1..iterations) {
                Log.i(LOG_TAG, "testVoipAndIcsWithParticipantsStress: try#$i")
                val requestId = mRequestIdGenerator.getAndIncrement()
                // Only wait for call setup on ICS side to stress extensions setup
                createVoipCallAsync(
                    voipAppControl,
                    requestId,
                    listOf(getParticipantCapability(emptySet())),
                    parameters.direction
                )
                var hasConnected = false
                with(ics) {
                    val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
                    connectExtensions(call) {
                        val participants = CachedParticipants(this)
                        onConnected {
                            hasConnected = true
                            if (!participants.extension.isSupported) {
                                failedTries.add(i)
                            }
                            call.disconnect()
                        }
                    }
                }
                assertTrue("onConnected never received", hasConnected)
                // Ensure the ICS mCalls list is updated with the newly removed call so we don't
                // accidentally grab the stale call when starting the next round.
                TestUtils.waitOnInCallServiceToReachXCalls(ics, 0)
            }
            if (failedTries.isNotEmpty()) {
                fail("Failed to set up extensions on ${failedTries.size}/$iterations tries")
            }
        }
    }

    /**
     * Create a VOIP call with a participants extension and attach participant Call extensions.
     * Verify raised hands functionality works as expected
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsRaisedHands() = runBlocking {
        usingIcs { ics ->
            val voipAppControl = bindToVoipAppWithExtensions()
            val callback = TestCallCallbackListener(this)
            voipAppControl.setCallback(callback)
            val voipCallId =
                createAndVerifyVoipCall(
                    voipAppControl,
                    callback,
                    listOf(
                        getParticipantCapability(setOf(ParticipantExtensionImpl.RAISE_HAND_ACTION))
                    ),
                    parameters.direction
                )

            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
            var hasConnected = false
            with(ics) {
                connectExtensions(call) {
                    val participants = CachedParticipants(this)
                    val raiseHandAction = CachedRaisedHands(participants.extension)
                    onConnected {
                        hasConnected = true
                        val currentParticipants = TestUtils.generateParticipants(3)
                        voipAppControl.updateParticipants(
                            currentParticipants.map { it.toParticipantParcelable() }
                        )
                        participants.waitForParticipants(currentParticipants.toSet())

                        // Reverse the ordering of the list to ensure that ordering is maintained
                        // across the binder.
                        val raisedHands = currentParticipants.reversed()
                        voipAppControl.updateRaisedHands(
                            raisedHands.map { it.toParticipantParcelable() }
                        )
                        raiseHandAction.waitForRaisedHands(raisedHands)
                        val raisedHandsAndInvalid = ArrayList(raisedHands)
                        raisedHandsAndInvalid.add(Participant("INVALID", "INVALID"))
                        voipAppControl.updateRaisedHands(
                            raisedHandsAndInvalid.map { it.toParticipantParcelable() }
                        )
                        // action should not contain the invalid Participant
                        raiseHandAction.waitForRaisedHands(raisedHands)

                        // Test ICS -> VOIP connection by sending events
                        raiseHandAction.action.requestRaisedHandStateChange(true)
                        callback.waitForRaiseHandState(voipCallId, true)

                        call.disconnect()
                    }
                }
            }
            assertTrue("onConnected never received", hasConnected)
        }
    }

    /**
     * This test verifies the functionality of the Call Icon extension. It checks if the extension
     * can successfully connect to the In-Call Service (ICS), receive and update the call icon URI,
     * and handle URI updates sent by the VoIP app.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testCallIconExtension(): Unit = runBlocking {
        usingIcs { ics ->
            val voipAppControl = bindToVoipAppWithExtensions()
            val callback = TestCallCallbackListener(this)
            voipAppControl.setCallback(callback)
            createAndVerifyVoipCall(
                voipAppControl,
                callback,
                listOf(getCallIconCapability(setOf())),
                parameters.direction
            )
            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
            var hasConnected = false
            with(ics) {
                connectExtensions(call) {
                    val callIconExtension = CachedCallIcon(this)
                    onConnected {
                        hasConnected = true
                        assertTrue(callIconExtension.extension.isSupported)
                        // verify the remote service gets the initial URI
                        callIconExtension.waitForLocalCallSilenceState(INITIAL_CALL_ICON_URI)
                        // create a fake URI file
                        val uniqueFileName = UUID.randomUUID().toString() + ".jpg"
                        val file = File(mContext.filesDir, uniqueFileName)
                        val fileUri = Uri.fromFile(file)
                        // Send new Uri update to the remote surface
                        voipAppControl.updateCallIcon(fileUri)
                        callIconExtension.waitForLocalCallSilenceState(fileUri)
                        call.disconnect()
                    }
                }
            }
            assertTrue("onConnected never received", hasConnected)
        }
    }

    /**
     * This is an end to end test that verifies a VoIP application and InCallService can add the
     * LocalCallSilenceExtension and toggle the value.
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsTogglingTheLocalCallSilenceExtension(): Unit = runBlocking {
        usingIcs { ics ->
            val globalMuteStateReceiver = TestMuteStateReceiver()
            try {
                mContext.registerReceiver(
                    globalMuteStateReceiver,
                    IntentFilter(AudioManager.ACTION_MICROPHONE_MUTE_CHANGED)
                )
                val voipAppControl = bindToVoipAppWithExtensions()
                val callback = TestCallCallbackListener(this)
                voipAppControl.setCallback(callback)
                val voipCallId =
                    createAndVerifyVoipCall(
                        voipAppControl,
                        callback,
                        listOf(getLocalSilenceCapability(setOf())),
                        parameters.direction
                    )

                val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
                var hasConnected = false

                val am = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                with(ics) {
                    connectExtensions(call) {
                        val localSilenceExtension = CachedLocalSilence(this)
                        onConnected {
                            hasConnected = true
                            // simulate an external global mute while the local call silence
                            // extension is connected.  The expected behavior is that telecom will
                            // undo this op
                            if (VERSION.SDK_INT >= VERSION_CODES.P) {
                                // a delay is needed so the call goes active first and then
                                // the mute is called. Otherwise, telecom will unmute during the
                                // call setup
                                delay(500)
                                Log.i("LCS_Test", "manually muting the mic")
                                am.setMicrophoneMute(true)
                                assertTrue(am.isMicrophoneMute)
                                waitForGlobalMuteState(true, "1", callback, globalMuteStateReceiver)
                                // LocalCallSilenceExtensionImpl handles globally unmuting the
                                // microphone
                                waitForGlobalMuteState(
                                    false,
                                    "2",
                                    callback,
                                    globalMuteStateReceiver
                                )
                            }

                            // VoIP --> ICS
                            voipAppControl.updateIsLocallySilenced(false)
                            localSilenceExtension.waitForLocalCallSilenceState(false)

                            // signal that the app wants to locally silence the call
                            voipAppControl.updateIsLocallySilenced(true)
                            localSilenceExtension.waitForLocalCallSilenceState(true)

                            // ICS -> VOIP
                            localSilenceExtension.extension.requestLocalCallSilenceUpdate(false)
                            callback.waitForIsLocalSilenced(voipCallId, false)

                            // set the call state via voip app control
                            if (VERSION.SDK_INT >= VERSION_CODES.R) {
                                call.hold()
                                waitForGlobalMuteState(true, "3", callback, globalMuteStateReceiver)
                                call.unhold()
                                waitForGlobalMuteState(
                                    false,
                                    "4",
                                    callback,
                                    globalMuteStateReceiver
                                )
                            }
                            call.disconnect()
                        }
                    }
                }
                assertTrue("onConnected never received", hasConnected)
            } finally {
                mContext.unregisterReceiver(globalMuteStateReceiver)
            }
        }
    }

    private suspend fun waitForGlobalMuteState(
        expectedValue: Boolean,
        tag: String,
        cb: TestCallCallbackListener,
        receiver: TestMuteStateReceiver
    ) {
        if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            cb.waitForGlobalMuteState(expectedValue, tag)
        } else if (VERSION.SDK_INT >= VERSION_CODES.P) {
            receiver.waitForGlobalMuteState(expectedValue, tag)
        }
    }

    /**
     * Create a VOIP call with a participants extension and attach participant Call extensions.
     * Verify kick participant functionality works as expected
     */
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipAndIcsKickParticipant() = runBlocking {
        usingIcs { ics ->
            val voipAppControl = bindToVoipAppWithExtensions()
            val callback = TestCallCallbackListener(this)
            voipAppControl.setCallback(callback)
            val voipCallId =
                createAndVerifyVoipCall(
                    voipAppControl,
                    callback,
                    listOf(
                        getParticipantCapability(
                            setOf(ParticipantExtensionImpl.KICK_PARTICIPANT_ACTION)
                        )
                    ),
                    parameters.direction
                )

            val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
            var hasConnected = false
            with(ics) {
                connectExtensions(call) {
                    val participants = CachedParticipants(this)
                    val kickParticipant = participants.extension.addKickParticipantAction()
                    onConnected {
                        hasConnected = true
                        val currentParticipants = TestUtils.generateParticipants(3)
                        voipAppControl.updateParticipants(
                            currentParticipants.map { it.toParticipantParcelable() }
                        )
                        participants.waitForParticipants(currentParticipants.toSet())
                        // Kick a valid participant
                        assertEquals(
                            "Never received response to kickParticipant request",
                            CallControlResult.Success(),
                            kickParticipant.requestKickParticipant(currentParticipants[0])
                        )
                        callback.waitForKickParticipant(voipCallId, currentParticipants[0])

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
    private fun createVoipCallAsync(
        voipAppControl: ITestAppControl,
        requestId: Int,
        capabilities: List<Capability>,
        direction: Int
    ) {
        // add a call to verify capability exchange IS made with ICS
        voipAppControl.addCall(
            requestId,
            capabilities,
            direction == CallAttributesCompat.DIRECTION_OUTGOING
        )
    }

    /**
     * Creates a VOIP call using the specified capabilities and direction and then verifies that it
     * was set up.
     */
    private suspend fun createAndVerifyVoipCall(
        voipAppControl: ITestAppControl,
        callback: TestCallCallbackListener,
        capabilities: List<Capability>,
        direction: Int
    ): String {
        val requestId = mRequestIdGenerator.getAndIncrement()
        createVoipCallAsync(voipAppControl, requestId, capabilities, direction)
        val callId = callback.waitForCallAdded(requestId)
        assertTrue("call could not be created", !callId.isNullOrEmpty())
        return callId!!
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

    private fun getParticipantCapability(actions: Set<Int>): Capability {
        return createCapability(
            id = Extensions.PARTICIPANT,
            version = ParticipantExtensionImpl.VERSION,
            actions = actions
        )
    }

    private fun getLocalSilenceCapability(actions: Set<Int>): Capability {
        return createCapability(
            id = Extensions.LOCAL_CALL_SILENCE,
            version = LocalCallSilenceExtensionImpl.VERSION,
            actions = actions
        )
    }

    private fun getCallIconCapability(actions: Set<Int>): Capability {
        return createCapability(
            id = Extensions.CALL_ICON,
            version = CallIconExtensionImpl.VERSION,
            actions = actions
        )
    }
}
