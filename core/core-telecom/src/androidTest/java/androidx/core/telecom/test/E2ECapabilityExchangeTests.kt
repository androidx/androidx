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

import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.internal.CallCompat
import androidx.core.telecom.internal.InCallServiceCompat
import androidx.core.telecom.test.VoipAppWithExtensions.VoipAppWithExtensionsControl
import androidx.core.telecom.test.utils.BaseTelecomTest
import androidx.core.telecom.test.utils.InCallServiceType
import androidx.core.telecom.test.utils.MockInCallServiceDelegate
import androidx.core.telecom.test.utils.TestUtils
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
class E2ECapabilityExchangeTests : BaseTelecomTest() {
    @get:Rule val serviceRule: ServiceTestRule = ServiceTestRule()

    /**
     * In this test, we have a VOIP app that has extensions set up and one ICS that is not using
     * [InCallServiceCompat], so no extensions are supported. In this case, the VOIP app will never
     * receive a request to start capability exchange, so any extensions updates will no-op.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipWithExtensionsAndInCallServiceWithout() {
        runBlocking {
            // test set up
            setUpV2Test()
            setInCallService(InCallServiceType.ICS_WITHOUT_EXTENSIONS)
            // bind to app
            val voipAppControl = bindToVoipAppWithExtensions()
            // configure and verify app capabilities
            voipAppControl.setVoipCapabilities(getParticipantCapability())
            assertCapabilities(getParticipantCapability(), voipAppControl.voipCapabilities)
            // No Capability Exchange sequence occurs between VoIP app and ICS because ICS doesn't
            // support extensions
            voipAppControl.addCall(true /* isOutgoing*/)
            TestUtils.waitOnInCallServiceToReachXCalls(1)
            voipAppControl.updateParticipants(listOf(TestUtils.getDefaultParticipant()))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @LargeTest
    @Test(timeout = 10000)
    fun testVoipWithExtensionsAndInCallServiceWithExtensions() {
        runBlocking {
            // test set up
            setUpV2TestWithExtensions()
            // bind to app
            val voipAppControl = bindToVoipAppWithExtensions()
            // configure and verify app capabilities
            voipAppControl.setVoipCapabilities(getParticipantCapability())
            assertCapabilities(getParticipantCapability(), voipAppControl.voipCapabilities)
            // add a call to verify capability exchange IS made with ICS
            voipAppControl.addCall(true /* isOutgoing*/)
            TestUtils.waitOnInCallServiceToReachXCalls(1)
            val call = MockInCallServiceDelegate.getLastCall()
            // verify capability exchange is made
            assertEquals(1, getInCallServiceCallCompatList().size)
            assertEquals(
                InCallServiceCompat.CAPABILITY_EXCHANGE,
                getInCallServiceExtensionLevelSupport()
            )
            assertEquals(
                InCallServiceCompat.CAPABILITY_EXCHANGE,
                getCallCapabilityType(call!!.toCall())
            )
            val callCompat = getInCallServiceCallCompatList()[0]
            // wait for cap exchange setup to complete
            withTimeout(5000) {
                while (isActive && !callCompat.capExchangeSetupComplete) {
                    yield() // another mechanism to stop the while loop if the coroutine is dead
                    delay(1) // sleep x millisecond(s) instead of spamming check
                }
            }
            // verify cap exchange is complete
            assertTrue(callCompat.capExchangeSetupComplete)
            voipAppControl.updateParticipants(listOf(TestUtils.getDefaultParticipant()))
            // TODO:: confirm the ICS received the participant update
        }
    }

    /**
     * =========================================================================================
     * Helpers
     * =========================================================================================
     */
    private fun bindToVoipAppWithExtensions(): ITestAppControl {
        val serviceIntent =
            Intent(
                InstrumentationRegistry.getInstrumentation().context,
                VoipAppWithExtensionsControl::class.java
            )
        return ITestAppControl.Stub.asInterface(serviceRule.bindService(serviceIntent))
    }

    private fun assertCapabilities(capList: List<Capability>, otherCapList: List<Capability>) {
        assertEquals(capList.size, otherCapList.size)
        for (i in capList.indices) {
            assertCapabilitiesAreSame(capList[i], otherCapList[i])
        }
    }

    private fun assertCapabilitiesAreSame(capability: Capability, other: Capability) {
        assertEquals(capability.featureId, other.featureId)
        assertEquals(capability.featureVersion, other.featureVersion)
        assertEquals(capability.supportedActions.size, other.supportedActions.size)
        for (i in capability.supportedActions.indices) {
            assertEquals(capability.supportedActions[i], other.supportedActions[i])
        }
    }

    private fun getParticipantCapability(): List<Capability> {
        val participantCapability = Capability()
        participantCapability.featureId = CallsManager.PARTICIPANT
        participantCapability.featureVersion = 0
        participantCapability.supportedActions = TestUtils.getDefaultParticipantSupportedActions()
        return listOf(participantCapability)
    }

    private fun getCallCapabilityType(call: Call): Int {
        return MockInCallServiceDelegate.getServiceWithExtensions()!!.resolveCallExtensionsType(
            call
        )
    }

    private fun getInCallServiceCallCompatList(): MutableList<CallCompat> {
        return MockInCallServiceDelegate.getServiceWithExtensions()!!.mCallCompats
    }

    private fun getInCallServiceExtensionLevelSupport(): Int {
        return MockInCallServiceDelegate.getServiceWithExtensions()!!.mExtensionLevelSupport
    }
}
