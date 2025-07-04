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

import android.net.Uri
import android.os.Build.VERSION_CODES
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
import org.junit.Assert.assertTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
class CallAttributesCompatTest {

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

        assertFalse(outgoing.toString() == incoming.toString())
        assertFalse(outgoing == incoming)
        assertFalse(outgoing.hashCode() == incoming.hashCode())
    }

    @Test
    fun testCallAttributesCompatCompareSame() {
        val outgoing1 =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
            )

        val outgoing2 =
            CallAttributesCompat(
                OUTGOING_NAME,
                TEST_ADDRESS,
                CallAttributesCompat.DIRECTION_OUTGOING,
            )

        assertEquals(outgoing1, outgoing2)
        assertEquals(outgoing1.toString(), outgoing2.toString())
        assertEquals(outgoing1.hashCode(), outgoing2.hashCode())
    }
}
