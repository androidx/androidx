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

import androidx.core.telecom.CallException
import androidx.core.telecom.internal.AddCallResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AddCallResultTest {

    @Test
    fun testAddCallResultErrorCodesAreSetProperly() {
        assertEquals(
            CallException.ERROR_UNKNOWN,
            AddCallResult.Error(CallException.ERROR_UNKNOWN).errorCode
        )
        assertEquals(
            CallException.ERROR_CALL_DOES_NOT_SUPPORT_HOLD,
            AddCallResult.Error(CallException.ERROR_CALL_DOES_NOT_SUPPORT_HOLD).errorCode
        )
        assertEquals(
            CallException.ERROR_CALL_IS_NOT_BEING_TRACKED,
            AddCallResult.Error(CallException.ERROR_CALL_IS_NOT_BEING_TRACKED).errorCode
        )
    }

    @Test
    fun testAddCallResultCallSession() {
        val callSessionSuccess1 = AddCallResult.SuccessCallSession()
        val callSessionSuccess2 = AddCallResult.SuccessCallSession()
        assertEquals(callSessionSuccess1, callSessionSuccess2)
        assertNotEquals(callSessionSuccess1, AddCallResult.Error(CallException.ERROR_UNKNOWN))
    }
}
