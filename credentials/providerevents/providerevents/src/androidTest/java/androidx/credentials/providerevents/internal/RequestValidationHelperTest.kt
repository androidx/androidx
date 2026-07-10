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

package androidx.credentials.providerevents.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RequestValidationHelperTest {

    @Test
    fun isValidJSON_validObject_returnsTrue() {
        val json = "{\"key\": \"value\"}"
        assertTrue(RequestValidationHelper.isValidJSON(json))
    }

    @Test
    fun isValidJSON_validArray_returnsTrue() {
        val json = "[\"item1\", \"item2\"]"
        assertTrue(RequestValidationHelper.isValidJSON(json))
    }

    @Test
    fun isValidJSON_invalidJson_returnsFalse() {
        val json = "not a json"
        assertFalse(RequestValidationHelper.isValidJSON(json))
    }

    @Test
    fun isValidJSON_emptyString_returnsFalse() {
        val json = ""
        assertFalse(RequestValidationHelper.isValidJSON(json))
    }

    @Test
    fun isValidJSON_whitespaceObject_returnsTrue() {
        val json = "   {\"key\": \"value\"}   "
        assertTrue(RequestValidationHelper.isValidJSON(json))
    }

    @Test
    fun isValidJSON_nestedObject_returnsTrue() {
        val json = "{\"key\": {\"nested\": \"value\"}}"
        assertTrue(RequestValidationHelper.isValidJSON(json))
    }
}
