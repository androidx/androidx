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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
class CallAttributesCompatTest {
    private val fakePhoneAccountHandle =
        PhoneAccountHandle(ComponentName("androidx.core.telecom.test", "TestClass"), "testId")
    private val testContactUri: Uri = Uri.parse("content://com.android.contacts/contacts/lookup/2")

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
        assertNull(callAttributesCompat.contactUri)
        assertFalse(callAttributesCompat.isGroupCall)
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
                contactUri = testContactUri,
                isGroupCall = true,
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
        assertEquals(testContactUri, callAttributesCompat.contactUri)
        assertTrue(callAttributesCompat.isGroupCall)
    }

    @Test
    fun testCallAttributesCompatSecondaryConstructors() {
        val attributes7Param =
            CallAttributesCompat(
                TEST_CALL_ATTRIB_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_INCOMING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                ALL_CALL_CAPABILITIES,
                null,
                true,
            )
        assertEquals(TEST_CALL_ATTRIB_NAME, attributes7Param.displayName)
        assertEquals(TEST_ADDRESS, attributes7Param.address)
        assertEquals(CallAttributesCompat.DIRECTION_INCOMING, attributes7Param.direction)
        assertEquals(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, attributes7Param.callType)
        assertEquals(ALL_CALL_CAPABILITIES, attributes7Param.callCapabilities)
        assertNull(attributes7Param.preferredStartingCallEndpoint)
        assertTrue(attributes7Param.isLogExcluded!!)
        assertNull(attributes7Param.contactUri)
        assertFalse(attributes7Param.isGroupCall)

        val attributes8Param =
            CallAttributesCompat(
                TEST_CALL_ATTRIB_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_INCOMING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                ALL_CALL_CAPABILITIES,
                null,
                true,
                testContactUri,
            )
        assertEquals(TEST_CALL_ATTRIB_NAME, attributes8Param.displayName)
        assertEquals(TEST_ADDRESS, attributes8Param.address)
        assertEquals(CallAttributesCompat.DIRECTION_INCOMING, attributes8Param.direction)
        assertEquals(CallAttributesCompat.CALL_TYPE_VIDEO_CALL, attributes8Param.callType)
        assertEquals(ALL_CALL_CAPABILITIES, attributes8Param.callCapabilities)
        assertNull(attributes8Param.preferredStartingCallEndpoint)
        assertTrue(attributes8Param.isLogExcluded!!)
        assertEquals(testContactUri, attributes8Param.contactUri)
        assertFalse(attributes8Param.isGroupCall)
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

        val outgoingWithContactUri =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = testContactUri,
            )

        val outgoingGroupCall =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isGroupCall = true,
            )

        assertFalse(outgoing.toString() == incoming.toString())
        assertFalse(outgoing == incoming)
        assertFalse(outgoing.hashCode() == incoming.hashCode())

        assertFalse(outgoing == outgoingLogExcluded)
        assertFalse(outgoing.hashCode() == outgoingLogExcluded.hashCode())

        assertFalse(outgoing == outgoingWithContactUri)
        assertFalse(outgoing.hashCode() == outgoingWithContactUri.hashCode())

        assertFalse(outgoing == outgoingGroupCall)
        assertFalse(outgoing.hashCode() == outgoingGroupCall.hashCode())
    }

    @Test
    fun testCallAttributesCompatCompareSame() {
        val outgoing1 =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
                contactUri = testContactUri,
                isGroupCall = true,
            )

        val outgoing2 =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
                contactUri = testContactUri,
                isGroupCall = true,
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
    fun testContactUri_explicitValues() {
        val attributesWithUri =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = testContactUri,
            )
        assertEquals(testContactUri, attributesWithUri.contactUri)

        val attributesNullUri =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = null,
            )
        assertNull(attributesNullUri.contactUri)
    }

    @Test
    fun testIsGroupCall_explicitValues() {
        val attributesGroupCall =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isGroupCall = true,
            )
        assertTrue(attributesGroupCall.isGroupCall)

        val attributesNotGroupCall =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isGroupCall = false,
            )
        assertFalse(attributesNotGroupCall.isGroupCall)
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

    @Test
    fun testToString_includesContactUri() {
        val attributesWithUri =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = testContactUri,
            )
        assertTrue(attributesWithUri.toString().contains("contactUri=[$testContactUri]"))

        val attributesNull =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
            )
        assertTrue(attributesNull.toString().contains("contactUri=[null]"))
    }

    @Test
    fun testToString_includesIsGroupCallAndBalancedCapabilities() {
        val attributesGroupCall =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                callCapabilities = SUPPORTS_STREAM or SUPPORTS_TRANSFER,
                isGroupCall = true,
            )
        assertTrue(attributesGroupCall.toString().contains("capabilities=[Stream, Transfer], "))
        assertTrue(attributesGroupCall.toString().contains("isGroupCall=[true])"))

        val attributesDefault =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
            )
        assertTrue(attributesDefault.toString().contains("capabilities=[SetInactive], "))
        assertTrue(attributesDefault.toString().contains("isGroupCall=[false])"))
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

    @Test
    @SdkSuppress(
        minSdkVersion = VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdkVersion = VERSION_CODES.BAKLAVA,
    )
    fun testToCallAttributes_Api34_36_contactUriAndIsGroupCallAlwaysIgnored() {
        // On SDKs before CINNAMON_BUN, contactUri and isGroupCall should not be used on the
        // platform object.
        val attributesWithUriAndGroup =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = testContactUri,
                isGroupCall = true,
            )
        val platformAttributesWithUri =
            attributesWithUriAndGroup.toCallAttributes(fakePhoneAccountHandle)
        assertNotNull(platformAttributesWithUri)

        val attributesDefault =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = null,
                isGroupCall = false,
            )
        val platformAttributesDefault = attributesDefault.toCallAttributes(fakePhoneAccountHandle)
        assertNotNull(platformAttributesDefault)
    }

    // Helper to reflectively check platform CallAttributes methods
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

        @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun platformGetContactUri(attributes: android.telecom.CallAttributes): Uri? {
            return try {
                val method = attributes.javaClass.getDeclaredMethod("getContactUri")
                method.invoke(attributes) as Uri?
            } catch (e: NoSuchMethodException) {
                throw AssertionError("Platform CallAttributes.getContactUri() method not found", e)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun platformIsGroupCall(attributes: android.telecom.CallAttributes): Boolean {
            return try {
                val method = attributes.javaClass.getDeclaredMethod("isGroupCall")
                method.invoke(attributes) as Boolean
            } catch (e: NoSuchMethodException) {
                throw AssertionError("Platform CallAttributes.isGroupCall() method not found", e)
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
        // When isLogExcluded is null, the platform object returns its default value (false).
        assertFalse(Api34Helper.platformIsLogExcluded(platformAttributesNull))
    }

    @Test
    @SdkSuppress(minSdkVersion = VERSION_CODES.CINNAMON_BUN)
    fun testToCallAttributes_AtLeastCinnamonBun_contactUriApplied() {
        val attributesWithUri =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = testContactUri,
            )
        val platformAttributesWithUri = attributesWithUri.toCallAttributes(fakePhoneAccountHandle)
        assertEquals(testContactUri, Api34Helper.platformGetContactUri(platformAttributesWithUri))
        assertFalse(Api34Helper.platformIsGroupCall(platformAttributesWithUri))

        // Verify setting isLogExcluded, contactUri, and isGroupCall applies all three on platform
        // CallAttributes
        val attributesWithAll =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isLogExcluded = true,
                contactUri = testContactUri,
                isGroupCall = true,
            )
        val platformAttributesWithAll = attributesWithAll.toCallAttributes(fakePhoneAccountHandle)
        assertTrue(Api34Helper.platformIsLogExcluded(platformAttributesWithAll))
        assertEquals(testContactUri, Api34Helper.platformGetContactUri(platformAttributesWithAll))
        assertTrue(Api34Helper.platformIsGroupCall(platformAttributesWithAll))
    }

    @Test
    @SdkSuppress(minSdkVersion = VERSION_CODES.CINNAMON_BUN)
    fun testToCallAttributes_AtLeastCinnamonBun_contactUriNull() {
        val attributesNull =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = null,
            )
        val platformAttributesNull = attributesNull.toCallAttributes(fakePhoneAccountHandle)
        assertNotNull(platformAttributesNull)
        assertNull(Api34Helper.platformGetContactUri(platformAttributesNull))
        assertFalse(Api34Helper.platformIsGroupCall(platformAttributesNull))
    }

    @Test
    @SdkSuppress(minSdkVersion = VERSION_CODES.CINNAMON_BUN)
    fun testToCallAttributes_AtLeastCinnamonBun_isGroupCallApplied() {
        val attributesGroupCall =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isGroupCall = true,
            )
        val platformAttributesGroupCall =
            attributesGroupCall.toCallAttributes(fakePhoneAccountHandle)
        assertTrue(Api34Helper.platformIsGroupCall(platformAttributesGroupCall))
        assertNull(Api34Helper.platformGetContactUri(platformAttributesGroupCall))

        val attributesNotGroupCall =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                isGroupCall = false,
            )
        val platformAttributesNotGroupCall =
            attributesNotGroupCall.toCallAttributes(fakePhoneAccountHandle)
        assertFalse(Api34Helper.platformIsGroupCall(platformAttributesNotGroupCall))
    }

    @Test
    @SdkSuppress(minSdkVersion = VERSION_CODES.CINNAMON_BUN)
    fun testToCallAttributes_AtLeastCinnamonBun_invalidContactUriThrows() {
        val attributesInvalidScheme =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = Uri.parse("tel:6506959001"),
            )
        assertThrows(IllegalArgumentException::class.java) {
            attributesInvalidScheme.toCallAttributes(fakePhoneAccountHandle)
        }

        val attributesInvalidAuthority =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
                contactUri = Uri.parse("content://com.example.invalid/contacts/1"),
            )
        assertThrows(IllegalArgumentException::class.java) {
            attributesInvalidAuthority.toCallAttributes(fakePhoneAccountHandle)
        }
    }
}
