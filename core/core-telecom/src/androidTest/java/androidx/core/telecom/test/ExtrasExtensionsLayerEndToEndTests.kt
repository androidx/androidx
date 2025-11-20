/*
 * Copyright 2025 The Android Open Source Project
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

import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.extensions.CallExtensionScope
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.AutoVoipConnection
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.test.utils.TestUtils.ALL_CALL_CAPABILITIES
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_NAME
import androidx.core.telecom.test.utils.VoipConnectionService
import androidx.core.telecom.test.utils.VoipConnectionService.Companion.DEFAULT_ADDRESS
import androidx.core.telecom.test.utils.VoipConnectionService.Companion.SINGLETON_PHONE_ACCOUNT_HANDLE
import androidx.core.telecom.test.utils.VoipConnectionService.VoipPendingConnectionRequest
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end tests for the Extras Extensions Layer. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ExtrasExtensionsLayerEndToEndTests : BaseTelecomTest() {

    companion object {
        const val ICON_URI = "CoreTelecomFakeUriPath"
        val OUTGOING_AUTO_CALL_ATTRIBUTES =
            CallAttributesCompat(
                OUTGOING_NAME,
                DEFAULT_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                ALL_CALL_CAPABILITIES,
            )
    }

    private val mAutoConnectionService = VoipConnectionService()
    private val mPhoneAccount =
        PhoneAccount.builder(SINGLETON_PHONE_ACCOUNT_HANDLE, "AutoTestJetpackAcct")
            .setAddress(DEFAULT_ADDRESS)
            .setSubscriptionAddress(DEFAULT_ADDRESS)
            .setCapabilities(
                PhoneAccount.CAPABILITY_SELF_MANAGED or PhoneAccount.CAPABILITY_VIDEO_CALLING
            )
            .build()

    @Before
    fun setUp() {
        Utils.resetUtils()
        mTelecomManager.registerPhoneAccount(mPhoneAccount)
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
        mTelecomManager.unregisterPhoneAccount(SINGLETON_PHONE_ACCOUNT_HANDLE)
    }

    /**
     * Tests the meeting summary extension.
     *
     * This test verifies that the meeting summary extension can successfully update the participant
     * count and active speaker information in the InCallService.
     */
    @OptIn(ExperimentalAppActions::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @LargeTest
    @Test
    fun testMeetingSummaryExtension() {
        setUpBackwardsCompatTest()
        val deferredConnection = CompletableDeferred<AutoVoipConnection>()
        runBlocking {
            usingIcs { ics ->
                val connection = addVoipCall(OUTGOING_AUTO_CALL_ATTRIBUTES, deferredConnection)
                val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
                var hasConnected = false
                with(ics) {
                    connectExtensions(call) {
                        val extension = CachedMeetingSummary(this)
                        onConnected {
                            hasConnected = true
                            assertTrue(extension.extension.isSupported)
                            // set the number of participants
                            connection.setParticipantCount(2)
                            extension.waitForParticipantCount(2)
                            // set the active speaker
                            connection.setCurrentSpeaker(OUTGOING_NAME)
                            extension.waitForActiveParticipant(OUTGOING_NAME)
                            // clear meeting summary properties and assert the ICS updates
                            // -- speaker clear
                            connection.clearCurrentSpeaker()
                            extension.waitForActiveParticipant(null)
                            // -- count clear
                            connection.clearParticipantCount()
                            extension.waitForParticipantCount(0)
                            disconnectAndDestroyConnection(connection)
                        }
                    }
                }
                assertTrue(hasConnected)
            }
        }
    }

    /**
     * Tests the call icon extension.
     *
     * This test verifies that the call icon extension can successfully update the call image URI in
     * the InCallService.
     */
    @OptIn(ExperimentalAppActions::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @LargeTest
    @Test
    fun testIconExtension() {
        setUpBackwardsCompatTest()
        val deferredConnection = CompletableDeferred<AutoVoipConnection>()
        runBlocking {
            usingIcs { ics ->
                val connection = addVoipCall(OUTGOING_AUTO_CALL_ATTRIBUTES, deferredConnection)
                val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
                var hasConnected = false
                with(ics) {
                    connectExtensions(call) {
                        val callIconExtension = CachedCallIcon(this)
                        onConnected {
                            hasConnected = true
                            val uri = Uri.parse(ICON_URI)
                            assertTrue(callIconExtension.extension.isSupported)
                            // -- set an initial icon and assert the ICS receives it
                            connection.setCallImageUri(uri)
                            callIconExtension.waitForLocalCallSilenceState(uri)
                            // -- clear the icon and assert change
                            connection.clearCallImageUri()
                            callIconExtension.waitForLocalCallSilenceState(Uri.EMPTY)
                            disconnectAndDestroyConnection(connection)
                        }
                    }
                }
                assertTrue(hasConnected)
            }
        }
    }

    /**
     * Tests the local call silence extension.
     *
     * This test verifies that the local call silence extension can successfully update the local
     * call silence state in the InCallService and that the InCallService can request updates to the
     * local call silence state.
     */
    @OptIn(ExperimentalAppActions::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @LargeTest
    @Test
    fun testLocalCallSilenceExtension() {
        setUpBackwardsCompatTest()
        val deferredConnection = CompletableDeferred<AutoVoipConnection>()
        runBlocking {
            usingIcs { ics ->
                val connection = addVoipCall(OUTGOING_AUTO_CALL_ATTRIBUTES, deferredConnection)
                val call = TestUtils.waitOnInCallServiceToReachXCalls(ics, 1)!!
                var hasConnected = false
                with(ics) {
                    connectExtensions(call) {
                        val localSilenceExtension = CachedLocalSilence(this)
                        onConnected {
                            hasConnected = true
                            // assert start state
                            assertTrue(localSilenceExtension.extension.isSupported)
                            // signal that the connection is changeable
                            connection.setCallMicrophoneSilenceAvailability(true)
                            assertFalse(connection.getCallMicrophoneSilenceState())

                            // locally silence the call from the VoIP app and verify the ICS gets
                            // the update
                            connection.setCallMicrophoneSilenceState(true)
                            assertTrue(connection.getCallMicrophoneSilenceState())
                            localSilenceExtension.waitForLocalCallSilenceState(true)

                            // Toggle the LCS from the ICS side and ensure the VoIP app can
                            // receive the change
                            localSilenceExtension.extension.requestLocalCallSilenceUpdate(false)
                            delay(500)
                            assertFalse(connection.getCallMicrophoneSilenceState())

                            // simulate the switch to global mute
                            connection.setCallMicrophoneSilenceAvailability(false)
                            connection.setCallMicrophoneSilenceState(true)
                            delay(500)
                            // verify state is not changeable
                            localSilenceExtension.waitForLocalCallSilenceState(false)

                            disconnectAndDestroyConnection(connection)
                        }
                    }
                }
                assertTrue(hasConnected)
            }
        }
    }

    /**
     * ********************************************************************************************
     * Helpers
     * *******************************************************************************************
     */
    private suspend fun addVoipCall(
        callAttributes: CallAttributesCompat,
        deferredConnection: CompletableDeferred<AutoVoipConnection>,
    ): AutoVoipConnection {
        val request = VoipPendingConnectionRequest(callAttributes, deferredConnection)
        mAutoConnectionService.createConnectionRequest(
            mTelecomManager,
            SINGLETON_PHONE_ACCOUNT_HANDLE,
            request,
        )
        deferredConnection.await()
        val connection = deferredConnection.getCompleted()
        delay(10)
        connection.setActive()
        delay(10)
        return connection
    }

    @OptIn(ExperimentalAppActions::class)
    internal class CachedCallIcon(scope: CallExtensionScope) {
        private val callIconUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
        val extension = scope.addCallIconSupport(callIconUri::emit)

        suspend fun waitForLocalCallSilenceState(expected: Uri) {
            val result = withTimeoutOrNull(5000) { callIconUri.first { it == expected } }
            assertEquals("Never received Call Icon state", expected, result)
        }
    }

    @OptIn(ExperimentalAppActions::class)
    internal class CachedMeetingSummary(scope: CallExtensionScope) {
        private val participantState = MutableStateFlow<Int>(0)
        private val activeParticipantState = MutableStateFlow<CharSequence?>("")
        val extension =
            scope.addMeetingSummaryExtension(
                onCurrentSpeakerChanged = activeParticipantState::emit,
                onParticipantCountChanged = participantState::emit,
            )

        suspend fun waitForParticipantCount(expected: Int) {
            val result = withTimeoutOrNull(5000) { participantState.first { it == expected } }
            assertEquals("Never received expected participant count update", expected, result)
        }

        suspend fun waitForActiveParticipant(expected: String?) {
            val result = withTimeoutOrNull(5000) { activeParticipantState.first { it == expected } }
            assertEquals("Never received expected active participant", expected, result)
        }
    }

    @OptIn(ExperimentalAppActions::class)
    internal class CachedLocalSilence(scope: CallExtensionScope) {
        private val isLocallySilenced = MutableStateFlow(false)

        val extension =
            scope.addLocalCallSilenceExtension(onIsLocallySilencedUpdated = isLocallySilenced::emit)

        suspend fun waitForLocalCallSilenceState(expected: Boolean) {
            val result = withTimeoutOrNull(5000) { isLocallySilenced.first { it == expected } }
            assertEquals("Never received local call silence state", expected, result)
        }
    }

    private fun disconnectAndDestroyConnection(connection: AutoVoipConnection) {
        connection.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        connection.destroy()
    }
}
