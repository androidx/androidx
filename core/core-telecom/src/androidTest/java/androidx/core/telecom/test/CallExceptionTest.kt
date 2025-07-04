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

import android.os.Build.VERSION_CODES
import androidx.core.telecom.CallException
import androidx.core.telecom.CallException.Companion.ERROR_BLUETOOTH_DEVICE_IS_NULL
import androidx.core.telecom.CallException.Companion.ERROR_CALL_CANNOT_BE_SET_TO_ACTIVE
import androidx.core.telecom.CallException.Companion.ERROR_CALL_DOES_NOT_SUPPORT_HOLD
import androidx.core.telecom.CallException.Companion.ERROR_CALL_IS_NOT_BEING_TRACKED
import androidx.core.telecom.CallException.Companion.ERROR_CALL_NOT_PERMITTED_AT_PRESENT_TIME
import androidx.core.telecom.CallException.Companion.ERROR_CANNOT_HOLD_CURRENT_ACTIVE_CALL
import androidx.core.telecom.CallException.Companion.ERROR_OPERATION_TIMED_OUT
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
class CallExceptionTest {

    @Test
    fun testCallExceptionUnknown() {
        val e = CallException()
        assertCallExceptionProperties(e, e.code)
    }

    @Test
    fun testCallExceptionCannotHold() {
        val e = CallException(ERROR_CANNOT_HOLD_CURRENT_ACTIVE_CALL)
        assertCallExceptionProperties(e, ERROR_CANNOT_HOLD_CURRENT_ACTIVE_CALL)
    }

    @Test
    fun testCallExceptionCallIsNotBeingTracked() {
        val e = CallException(ERROR_CALL_IS_NOT_BEING_TRACKED)
        assertCallExceptionProperties(e, ERROR_CALL_IS_NOT_BEING_TRACKED)
    }

    @Test
    fun testCallExceptionCallCannotBeSetToActive() {
        val e = CallException(ERROR_CALL_CANNOT_BE_SET_TO_ACTIVE)
        assertCallExceptionProperties(e, ERROR_CALL_CANNOT_BE_SET_TO_ACTIVE)
    }

    @Test
    fun testCallExceptionCallNotPermittedAtPresentTime() {
        val e = CallException(ERROR_CALL_NOT_PERMITTED_AT_PRESENT_TIME)
        assertCallExceptionProperties(e, ERROR_CALL_NOT_PERMITTED_AT_PRESENT_TIME)
    }

    @Test
    fun testCallExceptionTimedOut() {
        val e = CallException(ERROR_OPERATION_TIMED_OUT)
        assertCallExceptionProperties(e, ERROR_OPERATION_TIMED_OUT)
    }

    @Test
    fun testCallExceptionErrorCallDoesNotSupportHold() {
        val e = CallException(ERROR_CALL_DOES_NOT_SUPPORT_HOLD)
        assertCallExceptionProperties(e, ERROR_CALL_DOES_NOT_SUPPORT_HOLD)
    }

    @Test
    fun testCallExceptionErrorBluetoothDeviceIsNull() {
        val e = CallException(ERROR_BLUETOOTH_DEVICE_IS_NULL)
        assertCallExceptionProperties(e, ERROR_BLUETOOTH_DEVICE_IS_NULL)
    }

    private fun assertCallExceptionProperties(e: CallException, code: Int) {
        assertEquals(e.code, code)
        assertNull(e.message)
        assertEquals(e, CallException(code))
        assertNotNull(e.toString())
        assertEquals(e.hashCode(), CallException(code).hashCode())
    }
}
