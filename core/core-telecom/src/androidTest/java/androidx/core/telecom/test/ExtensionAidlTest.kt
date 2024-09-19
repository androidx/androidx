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

import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.ICallDetailsListener
import androidx.core.telecom.extensions.ICapabilityExchange
import androidx.core.telecom.extensions.ICapabilityExchangeListener
import androidx.core.telecom.extensions.ILocalSilenceStateListener
import androidx.core.telecom.extensions.IParticipantStateListener
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not very useful for now, but tests the visibility of the AIDL files and ensures that they can be
 * used as described.
 */
@OptIn(ExperimentalAppActions::class)
@RunWith(AndroidJUnit4::class)
class ExtensionAidlTest {

    class CapabilityExchangeImpl(
        val onBeginExchange: (MutableList<Capability>?, ICapabilityExchangeListener?) -> Unit =
            { _: MutableList<Capability>?, _: ICapabilityExchangeListener? ->
            }
    ) : ICapabilityExchange.Stub() {

        override fun beginExchange(
            capabilities: MutableList<Capability>?,
            l: ICapabilityExchangeListener?
        ) {
            capabilities?.let { l?.let { onBeginExchange(capabilities, l) } }
        }
    }

    class CapabilityExchangeListenerImpl(
        val createParticipantExtension: (Int, IntArray?, IParticipantStateListener?) -> Unit =
            { _: Int, _: IntArray?, _: IParticipantStateListener? ->
            },
        val createCallDetailsExtension: (Int, IntArray?, ICallDetailsListener?, String) -> Unit =
            { _: Int, _: IntArray?, _: ICallDetailsListener?, _: String ->
            },
        val unsubscribeFromParticipantExtensionUpdatse: () -> Unit = {},
        val unsubscribeFromCallDetailsExtensionUpdates: () -> Unit = {}
    ) : ICapabilityExchangeListener.Stub() {
        override fun onCreateParticipantExtension(
            version: Int,
            actions: IntArray?,
            l: IParticipantStateListener?
        ) {
            createParticipantExtension(version, actions, l)
        }

        override fun onCreateCallDetailsExtension(
            version: Int,
            actions: IntArray?,
            l: ICallDetailsListener?,
            packageName: String
        ) {
            createCallDetailsExtension(version, actions, l, packageName)
        }

        override fun onCreateLocalCallSilenceExtension(
            version: Int,
            actions: IntArray?,
            l: ILocalSilenceStateListener?
        ) {
            TODO("Not yet implemented")
        }

        override fun onRemoveExtensions() {
            unsubscribeFromParticipantExtensionUpdatse()
            unsubscribeFromCallDetailsExtensionUpdates()
        }
    }

    @SmallTest
    @Test
    fun testNegotiateCapabilities() {
        // setup
        var listener: ICapabilityExchangeListener? = null
        var capabilities: MutableList<Capability>? = null
        var supportedParticipantActions: IntArray? = null
        var participantStateListener: IParticipantStateListener? = null
        var versionNumber = -1
        val capExchange =
            CapabilityExchangeImpl(
                onBeginExchange = {
                    caps: MutableList<Capability>?,
                    iCapabilityExchangeListener: ICapabilityExchangeListener? ->
                    listener = iCapabilityExchangeListener
                    capabilities = caps
                }
            )
        val testCapability = Capability()
        testCapability.featureVersion = 2
        testCapability.featureId = 1
        testCapability.supportedActions = intArrayOf(1, 2)
        val testCapabilities = mutableListOf(testCapability)
        val capExchangeListener =
            CapabilityExchangeListenerImpl(
                createParticipantExtension = {
                    version: Int,
                    actions: IntArray?,
                    iParticipantStateListener: IParticipantStateListener? ->
                    versionNumber = version
                    supportedParticipantActions = actions
                    participantStateListener = iParticipantStateListener
                }
            )

        // Send caps
        capExchange.beginExchange(testCapabilities, capExchangeListener)
        assertEquals(testCapabilities, capabilities)
        assertNotNull(listener)

        val testActions = IntArray(3) { 0 }
        val testVersion = 1
        capExchangeListener.onCreateParticipantExtension(testVersion, testActions, null)
        assertNotNull(supportedParticipantActions)
        assertEquals(testVersion, versionNumber)
        assertNull(participantStateListener)
    }
}
