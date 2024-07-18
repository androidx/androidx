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
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.voip.CallControlScopeExtensionSingleton
import androidx.core.telecom.extensions.voip.activeParticipant
import androidx.core.telecom.extensions.voip.participants
import androidx.core.telecom.extensions.voip.raisedHandParticipants
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class VoipExtensionRegistrationTest : BaseTelecomTest() {
    private lateinit var extensionRegistrationMap: Map<Int, (CallControlScope) -> Unit>
    private lateinit var extensionCleanupMap: Map<Int, (CallControlScope) -> Unit>

    @Before
    fun setUp() {
        Utils.resetUtils()
        extensionRegistrationMap = mapOf(
            CallsManager.PARTICIPANT to ::verifyParticipantExtensionsInitialized
//            CallsManager.CALL_ICON to ::verifyCallIconExtensionsInitialized
        )
        extensionCleanupMap = mapOf(
            CallsManager.PARTICIPANT to ::verifyParticipantExtensionsCleanup
//            CallsManager.CALL_ICON to ::verifyCallIconExtensionsCleanup
        )
    }

    @After
    fun onDestroy() {
        Utils.resetUtils()
    }

    /***********************************************************************************************
     *                           V2 APIs (Android U and above) tests
     *********************************************************************************************/

    /**
     * Verify that the VOIP app is able to register the participant extension if defined via
     * [CallsManager#addCall].
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    @MediumTest
    fun testRegisterParticipantExtension() {
        setUpV2Test()
        runBlocking_addCallWithVoipExtension(buildParticipantExtension())
    }

    /**
     * Verify that the VOIP app is able to register the call details extension if it defines
     * support for call icons via [CallsManager#addCall].
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    @MediumTest
    @Ignore
    fun testRegisterCallDetailsExtension_CallIcon() {
        setUpV2Test()
        runBlocking_addCallWithVoipExtension(buildCallDetailsExtension_CallIcon())
    }

    /***********************************************************************************************
     *                           Backwards Compatibility Layer tests
     *********************************************************************************************/

    /**
     * Verify that the VOIP app is able to register the participant extension if defined via
     * [CallsManager#addCall].
     */
    @Test
    @MediumTest
    fun testRegisterParticipantExtension_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking_addCallWithVoipExtension(buildParticipantExtension())
    }

    /**
     * Verify that the VOIP app is able to register the call details extension if it defines
     * support for call icons via [CallsManager#addCall].
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @MediumTest
    @Ignore
    fun testRegisterCallDetailsExtension_CallIcon_BackwardsCompat() {
        setUpBackwardsCompatTest()
        runBlocking_addCallWithVoipExtension(buildCallDetailsExtension_CallIcon())
    }

    /***********************************************************************************************
     *                           Helpers
     *********************************************************************************************/

    /**
     * Helper to add call with the given capability. Verify that the extension has been properly
     * registered by the VOIP app.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun runBlocking_addCallWithVoipExtension(capability: Capability) {
        lateinit var scope: CallControlScope
        mCallsManager.setVoipCapabilities(listOf(capability))
        runBlocking {
            mCallsManager.addCall(
                TestUtils.OUTGOING_CALL_ATTRIBUTES,
                TestUtils.mOnAnswerLambda,
                TestUtils.mOnDisconnectLambda,
                TestUtils.mOnSetActiveLambda,
                TestUtils.mOnSetInActiveLambda,
            ) {
                launch {
                    scope = this@addCall
                    // Verify that the extension fields have been set up
                    extensionRegistrationMap[capability.featureId]!!.invoke(scope)
                    assertEquals(
                        CallControlResult.Success(),
                        disconnect(DisconnectCause(DisconnectCause.LOCAL))
                    )
                }
            }
        }
        // Verify cleanup of extensions
        extensionCleanupMap[capability.featureId]!!.invoke(scope)
    }

    private fun buildParticipantExtension(): Capability {
        val participantCapability = Capability()
        participantCapability.featureId = CallsManager.PARTICIPANT
        participantCapability.featureVersion = 1
        participantCapability.supportedActions = intArrayOf(
            CallsManager.RAISE_HAND_ACTION,
            CallsManager.KICK_PARTICIPANT_ACTION
        )
        return participantCapability
    }

    private fun buildCallDetailsExtension_CallIcon(): Capability {
        val callIconCapability = Capability()
        callIconCapability.featureId = CallsManager.CALL_ICON
        callIconCapability.featureVersion = 1
        callIconCapability.supportedActions = IntArray(0)
        return callIconCapability
    }

    private fun verifyParticipantExtensionsInitialized(session: CallControlScope) {
        assertNotNull(session.participants)
        assertEquals(session.participants!!.value, setOf<Participant>())
        assertNotNull(session.activeParticipant)
        assertNull(session.activeParticipant!!.value)
        assertNotNull(session.raisedHandParticipants)
        assertEquals(session.raisedHandParticipants!!.value, setOf<Participant>())
    }

//    private fun verifyCallIconExtensionsInitialized(session: CallControlScope) {
//        assertNotNull(session.activeCallIcon)
//        assertNull(session.activeCallIcon!!.value)
//    }

    private fun verifyParticipantExtensionsCleanup(session: CallControlScope) {
        assertNotNull(session)
        assertNull(session.participants)
        assertNull(session.activeParticipant)
        assertNull(session.raisedHandParticipants)
        assertNull(CallControlScopeExtensionSingleton.getInstance()
            .PARTICIPANT_DELEGATE[session.getCallId()])
    }

//    private fun verifyCallIconExtensionsCleanup(session: CallControlScope) {
//        assertNotNull(session)
//        assertNull(session.activeCallIcon)
//        assertNull(CallControlScopeExtensionSingleton.getInstance()
//            .CALL_DETAILS_DELEGATE[session.getCallId()])
//    }
}
