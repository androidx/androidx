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
import androidx.core.telecom.extensions.ICapabilityExchange
import androidx.core.telecom.extensions.ICapabilityExchangeListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not very useful for now, but tests the visibility of the AIDL files and ensures that they can
 * be used as described.
 */
@RunWith(AndroidJUnit4::class)
class ExtensionAidlTest {

    class CapabilityExchangeImpl(
        val onSetListener: (ICapabilityExchangeListener?) -> Unit = {},
        val onNegotiateCapabilities: (MutableList<Capability>) -> Unit = {},
        val onFeatureSetupComplete: () -> Unit = {}
    ) : ICapabilityExchange.Stub() {
        override fun setListener(l: ICapabilityExchangeListener?) {
            onSetListener(l)
        }

        override fun negotiateCapabilities(capabilities: MutableList<Capability>?) {
            capabilities?.let {
                onNegotiateCapabilities(capabilities)
            }
        }

        override fun featureSetupComplete() {
            onFeatureSetupComplete()
        }
    }

    class CapabilityExchangeListenerImpl(
        val capabilitiesNegotiated: (MutableList<Capability>) -> Unit = {}
    ) : ICapabilityExchangeListener.Stub() {
        override fun onCapabilitiesNegotiated(filteredCapabilities: MutableList<Capability>?) {
            filteredCapabilities?.let {
                capabilitiesNegotiated(filteredCapabilities)
            }
        }
    }

    @SmallTest
    @Test
    fun testSetListener() {
        // setup interfaces
        var listener: ICapabilityExchangeListener? = null
        val capExchange = CapabilityExchangeImpl(onSetListener = {
            listener = it
        })
        val capExchangeListener = CapabilityExchangeListenerImpl()

        // set the listener to non-null value
        capExchange.setListener(capExchangeListener)
        assertEquals(capExchangeListener, listener)

        // set back to null value
        capExchange.setListener(null)
        assertNull(listener)
    }

    @SmallTest
    @Test
    fun testNegotiateCapabilities() {
        // setup
        var listener: ICapabilityExchangeListener? = null
        var capabilities: MutableList<Capability>? = null
        var filteredCapabilities: MutableList<Capability>? = null
        val capExchange = CapabilityExchangeImpl(onSetListener = {
             listener = it
        }, onNegotiateCapabilities = {
            capabilities = it
        })
        capExchange.onSetListener(CapabilityExchangeListenerImpl {
            filteredCapabilities = it
        })
        val testCapability = Capability()
        testCapability.featureVersion = 2
        testCapability.featureId = 1
        testCapability.supportedActions = intArrayOf(1, 2)
        val testCapabilities = mutableListOf(testCapability)

        // Send caps
        capExchange.negotiateCapabilities(testCapabilities)
        assertEquals(testCapabilities, capabilities)
        assertNotNull(listener)

        // Receive filtered caps
        listener?.onCapabilitiesNegotiated(testCapabilities)
        assertEquals(testCapabilities, filteredCapabilities)
    }

    @SmallTest
    @Test
    fun testSetupComplete() {
        // setup
        var isComplete = false
        val capExchange = CapabilityExchangeImpl(onFeatureSetupComplete = {
            isComplete = true
        })

        // ensure feature setup complete is called properly
        capExchange.featureSetupComplete()
        assertTrue(isComplete)
    }
}
