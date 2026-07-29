/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.model.catalog.functions

import androidx.a2ui.model.protocol.A2uiException
import com.google.common.truth.Truth.assertThat
import kotlin.text.get
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiFunctionArgParserTest {

    // --- getArg Tests ---

    @Test
    fun getArg_exists_returnsValue() {
        val args = mapOf(ARG_VAL to "some-raw-value")
        val result = A2uiFunctionArgParser.getArg(args, ARG_VAL)
        assertThat(result).isEqualTo("some-raw-value")
    }

    @Test
    fun getArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getArg(emptyMap(), ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    // --- getStringArg Tests ---

    @Test
    fun getStringArg_exists_returnsString() {
        val args = mapOf(ARG_VAL to "hello")
        val result = A2uiFunctionArgParser.getStringArg(args, ARG_VAL)
        assertThat(result).isEqualTo("hello")
    }

    @Test
    fun getStringArg_nonString_convertsAndReturns() {
        val args = mapOf(ARG_VAL to 123)
        val result = A2uiFunctionArgParser.getStringArg(args, ARG_VAL)
        assertThat(result).isEqualTo("123")
    }

    @Test
    fun getStringArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getStringArg(emptyMap(), ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    @Test
    fun getStringArg_empty_returnsEmptyString() {
        val args = mapOf(ARG_VAL to "")
        val result = A2uiFunctionArgParser.getStringArg(args, ARG_VAL)
        assertThat(result).isEqualTo("")
    }

    // --- getStringListArg Tests ---

    @Test
    fun getStringListArg_validList_parsesCorrectly() {
        val args = mapOf(ARG_LIST to listOf("a", 123, "b"))
        val result = A2uiFunctionArgParser.getStringListArg(args, ARG_LIST)
        assertThat(result).containsExactly("a", "123", "b").inOrder()
    }

    @Test
    fun getStringListArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getStringListArg(emptyMap(), ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST")
    }

    @Test
    fun getStringListArg_notAList_throwsValidationException() {
        val args = mapOf(ARG_LIST to "not-a-list")
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getStringListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST")
    }

    @Test
    fun getStringListArg_containsNull_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf("a", null, "b"))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getStringListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    // --- getDoubleArg Tests ---

    @Test
    fun getDoubleArg_withDouble_parsesCorrectly() {
        val args = mapOf(ARG_VAL to 12.34)
        val result = A2uiFunctionArgParser.getDoubleArg(args, ARG_VAL)
        assertThat(result).isEqualTo(12.34)
    }

    @Test
    fun getDoubleArg_withInt_parsesCorrectly() {
        val args = mapOf(ARG_VAL to 42)
        val result = A2uiFunctionArgParser.getDoubleArg(args, ARG_VAL)
        assertThat(result).isEqualTo(42.0)
    }

    @Test
    fun getDoubleArg_withString_parsesCorrectly() {
        val args = mapOf(ARG_VAL to "3.14")
        val result = A2uiFunctionArgParser.getDoubleArg(args, ARG_VAL)
        assertThat(result).isEqualTo(3.14)
    }

    @Test
    fun getDoubleArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getDoubleArg(emptyMap(), ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    @Test
    fun getDoubleArg_invalidValue_throwsValidationException() {
        val args = mapOf(ARG_VAL to "not-a-number")
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getDoubleArg(args, ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    // --- getDoubleListArg Tests ---

    @Test
    fun getDoubleListArg_validList_parsesCorrectly() {
        val args = mapOf(ARG_LIST to listOf(1.5, 42, "3.14"))
        val result = A2uiFunctionArgParser.getDoubleListArg(args, ARG_LIST)
        assertThat(result).containsExactly(1.5, 42.0, 3.14).inOrder()
    }

    @Test
    fun getDoubleListArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getDoubleListArg(emptyMap(), ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST")
    }

    @Test
    fun getDoubleListArg_invalidItem_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(1.5, "not-a-double"))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getDoubleListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    @Test
    fun getDoubleListArg_containsNull_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(1.5, null))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getDoubleListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    // --- getIntArg Tests ---

    @Test
    fun getIntArg_withInt_parsesCorrectly() {
        val args = mapOf(ARG_VAL to 42)
        val result = A2uiFunctionArgParser.getIntArg(args, ARG_VAL)
        assertThat(result).isEqualTo(42)
    }

    @Test
    fun getIntArg_withDouble_parsesCorrectly() {
        val args = mapOf(ARG_VAL to 42.9)
        val result = A2uiFunctionArgParser.getIntArg(args, ARG_VAL)
        assertThat(result).isEqualTo(42)
    }

    @Test
    fun getIntArg_withString_parsesCorrectly() {
        val args = mapOf(ARG_VAL to "100")
        val result = A2uiFunctionArgParser.getIntArg(args, ARG_VAL)
        assertThat(result).isEqualTo(100)
    }

    @Test
    fun getIntArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getIntArg(emptyMap(), ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    @Test
    fun getIntArg_invalidValue_throwsValidationException() {
        val args = mapOf(ARG_VAL to "not-an-integer")
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getIntArg(args, ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    // --- getIntListArg Tests ---

    @Test
    fun getIntListArg_withInts_parsesCorrectly() {
        val args = mapOf(ARG_LIST to listOf(1, "2", 3.5))
        val result = A2uiFunctionArgParser.getIntListArg(args, ARG_LIST)
        assertThat(result).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun getIntListArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getIntListArg(emptyMap(), ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST")
    }

    @Test
    fun getIntListArg_invalidItem_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(1, "invalid", 3))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getIntListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    @Test
    fun getIntListArg_containsNull_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(1, null, 3))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getIntListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    // --- getLongArg Tests ---

    @Test
    fun getLongArg_withLong_parsesCorrectly() {
        val args = mapOf(ARG_VAL to 1234567890123L)
        val result = A2uiFunctionArgParser.getLongArg(args, ARG_VAL)
        assertThat(result).isEqualTo(1234567890123L)
    }

    @Test
    fun getLongArg_withInt_parsesCorrectly() {
        val args = mapOf(ARG_VAL to 42)
        val result = A2uiFunctionArgParser.getLongArg(args, ARG_VAL)
        assertThat(result).isEqualTo(42L)
    }

    @Test
    fun getLongArg_withString_parsesCorrectly() {
        val args = mapOf(ARG_VAL to "9876543210")
        val result = A2uiFunctionArgParser.getLongArg(args, ARG_VAL)
        assertThat(result).isEqualTo(9876543210L)
    }

    @Test
    fun getLongArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getLongArg(emptyMap(), ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    @Test
    fun getLongArg_invalidValue_throwsValidationException() {
        val args = mapOf(ARG_VAL to "not-a-long")
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getLongArg(args, ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    // --- getLongListArg Tests ---

    @Test
    fun getLongListArg_withLongs_parsesCorrectly() {
        val args = mapOf(ARG_LIST to listOf(10L, "20", 30.5))
        val result = A2uiFunctionArgParser.getLongListArg(args, ARG_LIST)
        assertThat(result).containsExactly(10L, 20L, 30L).inOrder()
    }

    @Test
    fun getLongListArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getLongListArg(emptyMap(), ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST")
    }

    @Test
    fun getLongListArg_invalidItem_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(10L, "invalid-long"))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getLongListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    @Test
    fun getLongListArg_containsNull_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(10L, null))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getLongListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    // --- getBooleanArg Tests ---

    @Test
    fun getBooleanArg_withBoolean_parsesCorrectly() {
        val args = mapOf(ARG_VAL to true)
        val result = A2uiFunctionArgParser.getBooleanArg(args, ARG_VAL)
        assertThat(result).isTrue()
    }

    @Test
    fun getBooleanArg_withTrueString_parsesCorrectly() {
        val args = mapOf(ARG_VAL to "TRUE")
        val result = A2uiFunctionArgParser.getBooleanArg(args, ARG_VAL)
        assertThat(result).isTrue()
    }

    @Test
    fun getBooleanArg_withFalseString_parsesCorrectly() {
        val args = mapOf(ARG_VAL to "False")
        val result = A2uiFunctionArgParser.getBooleanArg(args, ARG_VAL)
        assertThat(result).isFalse()
    }

    @Test
    fun getBooleanArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getBooleanArg(emptyMap(), ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    @Test
    fun getBooleanArg_invalidValue_throwsValidationException() {
        val args = mapOf(ARG_VAL to "not-a-boolean")
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getBooleanArg(args, ARG_VAL)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_VAL")
    }

    // --- getBooleanListArg Tests ---

    @Test
    fun getBooleanListArg_validList_parsesCorrectly() {
        val args = mapOf(ARG_LIST to listOf(true, "FALSE", false))
        val result = A2uiFunctionArgParser.getBooleanListArg(args, ARG_LIST)
        assertThat(result).containsExactly(true, false, false).inOrder()
    }

    @Test
    fun getBooleanListArg_missing_throwsValidationException() {
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getBooleanListArg(emptyMap(), ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST")
    }

    @Test
    fun getBooleanListArg_invalidItem_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(true, "not-a-boolean"))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getBooleanListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    @Test
    fun getBooleanListArg_containsNull_throwsValidationException() {
        val args = mapOf(ARG_LIST to listOf(true, null))
        val exception =
            assertThrows(A2uiException.A2uiValidationException::class.java) {
                A2uiFunctionArgParser.getBooleanListArg(args, ARG_LIST)
            }
        assertThat(exception.context[KEY_PATH]).isEqualTo("/$ARG_LIST/1")
    }

    private companion object {
        private const val ARG_VAL = "val"
        private const val ARG_LIST = "list"
        private const val KEY_PATH = "path"
    }
}
