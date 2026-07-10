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

import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallAttributesCompat.Companion.SUPPORTS_SET_INACTIVE
import androidx.core.telecom.CallAttributesCompat.Companion.SUPPORTS_STREAM
import androidx.core.telecom.CallAttributesCompat.Companion.SUPPORTS_TRANSFER
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.test.utils.TestUtils.ALL_CALL_CAPABILITIES
import androidx.core.telecom.test.utils.TestUtils.CUSTOM_TEST_APP_SCHEME
import androidx.core.telecom.test.utils.TestUtils.OUTGOING_NAME
import androidx.core.telecom.test.utils.TestUtils.TEST_ADDRESS
import androidx.core.telecom.test.utils.TestUtils.TEST_CALL_ATTRIB_NAME
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
class CallAttributesCompatTest {
    private val fakePhoneAccountHandle =
        PhoneAccountHandle(ComponentName("androidx.core.telecom.test", "TestClass"), "testId")

    @Test
    fun testCallAttributesCompatRequired() {
        val callAttributesCompat =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
            )
        assertEquals(OUTGOING_NAME, callAttributesCompat.displayName)
        assertEquals(TEST_ADDRESS, callAttributesCompat.address)
        assertEquals(CallAttributesCompat.DIRECTION_OUTGOING, callAttributesCompat.direction)
        assertEquals(CallAttributesCompat.CALL_TYPE_AUDIO_CALL, callAttributesCompat.callType)
        assertTrue(
            Utils.hasCapability(SUPPORTS_SET_INACTIVE, callAttributesCompat.callCapabilities)
        )
        assertFalse(Utils.hasCapability(SUPPORTS_STREAM, callAttributesCompat.callCapabilities))
        assertFalse(Utils.hasCapability(SUPPORTS_TRANSFER, callAttributesCompat.callCapabilities))
        assertNull(callAttributesCompat.isLogExcluded)
    }

    @Test
    fun testCallAttributesCompatAll() {
        val callAttributesCompat =
            CallAttributesCompat(
                TEST_CALL_ATTRIB_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_INCOMING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                ALL_CALL_CAPABILITIES,
                isLogExcluded = true,
            )
        assertEquals(TEST_CALL_ATTRIB_NAME, callAttributesCompat.displayName)
        assertEquals(TEST_ADDRESS, callAttributesCompat.address)
        assertEquals(CallAttributesCompat.DIRECTION_INCOMING, callAttributesCompat.direction)
        assertEquals(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, callAttributesCompat.callType)
        assertTrue(
            Utils.hasCapability(SUPPORTS_SET_INACTIVE, callAttributesCompat.callCapabilities)
        )
        assertTrue(Utils.hasCapability(SUPPORTS_STREAM, callAttributesCompat.callCapabilities))
        assertTrue(Utils.hasCapability(SUPPORTS_TRANSFER, callAttributesCompat.callCapabilities))
        assertTrue(callAttributesCompat.isLogExcluded!!)
    }

    @Test
    fun testCallAttributesCompatCompareDiff() {
        val outgoing =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
            )

        val incoming =
            CallAttributesCompat(
                TEST_CALL_ATTRIB_NAME,
                Uri.parse(CUSTOM_TEST_APP_SCHEME + "123"),
                CallAttributesCompat.DIRECTION_INCOMING,
            )

        val outgoingLogExcluded =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
            )

        assertFalse(outgoing.toString() == incoming.toString())
        assertFalse(outgoing == incoming)
        assertFalse(outgoing.hashCode() == incoming.hashCode())

        assertFalse(outgoing == outgoingLogExcluded)
        assertFalse(outgoing.hashCode() == outgoingLogExcluded.hashCode())
    }

    @Test
    fun testCallAttributesCompatCompareSame() {
        val outgoing1 =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
            )

        val outgoing2 =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
            )

        assertEquals(outgoing1, outgoing2)
        assertEquals(outgoing1.toString(), outgoing2.toString())
        assertEquals(outgoing1.hashCode(), outgoing2.hashCode())
        assertEquals(outgoing1.hashCode(), outgoing2.hashCode())
    }

    @Test
    fun testIsLogExcluded_explicitValues() {
        val attributesTrue =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
            )
        assertTrue(attributesTrue.isLogExcluded!!)

        val attributesFalse =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = false,
            )
        assertFalse(attributesFalse.isLogExcluded!!)
    }

    @Test
    fun testToString_includesIsLogExcluded() {
        val attributesTrue =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
            )
        assertTrue(attributesTrue.toString().contains("isLogExcluded=[true]"))

        val attributesFalse =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = false,
            )
        assertTrue(attributesFalse.toString().contains("isLogExcluded=[false]"))

        val attributesNull =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
            )
        assertTrue(attributesNull.toString().contains("isLogExcluded=[null]"))
    }

    // --- Tests for toCallAttributes ---

    @Test
    @SdkSuppress(
        minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdkVersion = VERSION_CODES.VANILLA_ICE_CREAM,
    )
    fun testToCallAttributes_Api34_35_isLogExcludedAlwaysIgnored() {
        // On SDKs before BAKLAVA_1, isLogExcluded should not be used.
        val attributesTrue =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
            )
        val platformAttributesTrue = attributesTrue.toCallAttributes(fakePhoneAccountHandle)
        assertNotNull(platformAttributesTrue)
        // Cannot call platform isLogExcluded() as it doesn't exist on these API levels.

        val attributesNull =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = null,
            )
        val platformAttributesNull = attributesNull.toCallAttributes(fakePhoneAccountHandle)
        assertNotNull(platformAttributesNull)
    }

    // Helper to reflectively check isLogExcluded on the platform CallAttributes

    private object Api34Helper {
        @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun platformIsLogExcluded(attributes: android.telecom.CallAttributes): Boolean {
            return try {
                val method = attributes.javaClass.getDeclaredMethod("isLogExcluded")
                method.invoke(attributes) as Boolean
            } catch (e: NoSuchMethodException) {
                throw AssertionError("Platform CallAttributes.isLogExcluded() method not found", e)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = VERSION_CODES.VANILLA_ICE_CREAM) // Run on V and above
    fun testToCallAttributes_AtLeastBaklava1_isLogExcludedApplied() {
        // This test logic is only fully effective if Build.VERSION_CODES_FULL.BAKLAVA_1 is defined
        // and the test runs on an SDK at or above that level.
        if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.BAKLAVA_1) {
            Log.w(
                "CallAttributesCompatTest",
                "testToCallAttributes_AtLeastBaklava1_isLogExcludedApplied: " +
                    "Skipping test because Build.VERSION_CODES_FULL.BAKLAVA_1 is not defined",
            )
            return // Skip test if below the required minor version
        }

        val attributesTrue =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
            )
        val platformAttributesTrue = attributesTrue.toCallAttributes(fakePhoneAccountHandle)
        assertTrue(Api34Helper.platformIsLogExcluded(platformAttributesTrue))

        val attributesFalse =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = false,
            )
        val platformAttributesFalse = attributesFalse.toCallAttributes(fakePhoneAccountHandle)
        assertFalse(Api34Helper.platformIsLogExcluded(platformAttributesFalse))
    }

    @Test
    @SdkSuppress(minSdkVersion = VERSION_CODES.VANILLA_ICE_CREAM) // Run on V and above
    fun testToCallAttributes_AtLeastBaklava1_isLogExcludedNull() {
        if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.BAKLAVA_1) {
            Log.w(
                "CallAttributesCompatTest",
                "testToCallAttributes_AtLeastBaklava1_isLogExcludedNull: " +
                    "Skipping test because Build.VERSION_CODES_FULL.BAKLAVA_1 is not defined",
            )
            return // Skip test if below the required minor version
        }

        val attributesNull =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = null,
            )
        val platformAttributesNull = attributesNull.toCallAttributes(fakePhoneAccountHandle)
        assertNotNull(platformAttributesNull)
        // When isLogExcluded is null, the code uses Api34PlusImpl, which doesn't call
        // setLogExcluded.
        // The isLogExcluded property on the platform object will return its default value (false).
        assertFalse(Api34Helper.platformIsLogExcluded(platformAttributesNull))
    }
}
