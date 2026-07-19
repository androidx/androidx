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
import java.util.Locale
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiPluralizeFunctionTest {

    @Test
    fun execute_validInput_propagateCorrectMessageToMessageFormatter() {
        val formatter = TestMessageFormatter()
        val function =
            A2uiPluralizeFunction(formatter, localeProvider = { Locale.forLanguageTag("cy") })
        val args =
            mapOf(
                ARG_VALUE_KEY to 0.0,
                ARG_ZERO_KEY to VAL_ZERO,
                ARG_ONE_KEY to VAL_ONE,
                ARG_TWO_KEY to VAL_TWO,
                ARG_FEW_KEY to VAL_FEW,
                ARG_MANY_KEY to VAL_MANY,
                ARG_OTHER_KEY to VAL_OTHER,
            )

        assertThat(function.execute(args)).isEqualTo("formatted_string")
        assertThat(formatter.capturedPattern)
            .isEqualTo(
                "{count, plural, zero {zero} one {one} two {two} few {few} many {many} other {other} }"
            )
        assertThat(formatter.capturedLocale).isEqualTo(Locale.forLanguageTag("cy"))
        assertThat(formatter.capturedArguments).isEqualTo(mapOf("count" to 0.0))
    }

    @Test
    fun execute_onlyOtherCategory_propagateCorrectMessageToMessageFormatter() {
        val formatter = TestMessageFormatter()
        val function = A2uiPluralizeFunction(formatter, localeProvider = { Locale.ENGLISH })
        val args = mapOf(ARG_VALUE_KEY to 1.0, ARG_OTHER_KEY to VAL_FALLBACK_OTHER)

        assertThat(function.execute(args)).isEqualTo("formatted_string")
        assertThat(formatter.capturedPattern).isEqualTo("{count, plural, other {fallback_other} }")
        assertThat(formatter.capturedLocale).isEqualTo(Locale.ENGLISH)
        assertThat(formatter.capturedArguments).isEqualTo(mapOf("count" to 1.0))
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        val formatter = TestMessageFormatter()
        val function = A2uiPluralizeFunction(formatter)
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            function.execute(emptyMap())
        }
    }

    private class TestMessageFormatter : A2uiMessageFormatter {
        var capturedPattern: String? = null
        var capturedLocale: Locale? = null
        var capturedArguments: Map<String, Any>? = null

        override fun format(pattern: String, locale: Locale, arguments: Map<String, Any>): String {
            capturedPattern = pattern
            capturedLocale = locale
            capturedArguments = arguments
            return "formatted_string"
        }
    }

    private companion object {
        private const val ARG_VALUE_KEY = "value"
        private const val ARG_ZERO_KEY = "zero"
        private const val ARG_ONE_KEY = "one"
        private const val ARG_TWO_KEY = "two"
        private const val ARG_FEW_KEY = "few"
        private const val ARG_MANY_KEY = "many"
        private const val ARG_OTHER_KEY = "other"

        private const val VAL_ZERO = "zero"
        private const val VAL_ONE = "one"
        private const val VAL_TWO = "two"
        private const val VAL_FEW = "few"
        private const val VAL_MANY = "many"
        private const val VAL_OTHER = "other"
        private const val VAL_FALLBACK_OTHER = "fallback_other"
    }
}
