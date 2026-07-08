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
import java.util.TimeZone
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiFormatDateFunctionTest {

    @Test
    fun execute_isoFormat_evaluatesCorrectly() {
        assertThat(
                A2uiFormatDateFunction.INSTANCE.execute(
                    mapOf(ARG_VALUE to 1781481600L, ARG_FORMAT to "ISO")
                )
            )
            .isEqualTo("2026-06-15T00:00:00Z")
    }

    @Test
    fun execute_germanLocale_evaluatesCorrectly() {
        val function = A2uiFormatDateFunction(localeProvider = { Locale.GERMANY })
        val result =
            function.execute(mapOf(ARG_VALUE to 1781481600L, ARG_FORMAT to "MMMM")) as String
        assertThat(result.lowercase()).isEqualTo("juni")
    }

    @Test
    fun execute_frenchLocale_evaluatesCorrectly() {
        val function = A2uiFormatDateFunction(localeProvider = { Locale.FRANCE })
        val result =
            function.execute(mapOf(ARG_VALUE to 1781481600L, ARG_FORMAT to "MMMM")) as String
        assertThat(result.lowercase()).isEqualTo("juin")
    }

    // Value Type Tests: Always use same format (MMMM) and en-US locale, vary the value types.
    @Test
    fun execute_valueTypeLongSecond_evaluatesCorrectly() {
        val function = A2uiFormatDateFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to 1781481600L, ARG_FORMAT to "MMMM")) as String
        assertThat(result).isEqualTo("June")
    }

    @Test
    fun execute_valueTypeLongMilli_evaluatesCorrectly() {
        val function = A2uiFormatDateFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to 1781481600000L, ARG_FORMAT to "MMMM")) as String
        assertThat(result).isEqualTo("June")
    }

    @Test
    fun execute_valueTypeInt_evaluatesCorrectly() {
        val function = A2uiFormatDateFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to 1781481600, ARG_FORMAT to "MMMM")) as String
        assertThat(result).isEqualTo("June")
    }

    @Test
    fun execute_valueTypeDouble_evaluatesCorrectly() {
        val function = A2uiFormatDateFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to 1781481600.0, ARG_FORMAT to "MMMM")) as String
        assertThat(result).isEqualTo("June")
    }

    @Test
    fun execute_valueTypeString_evaluatesCorrectly() {
        val function = A2uiFormatDateFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to "1781481600", ARG_FORMAT to "MMMM")) as String
        assertThat(result).isEqualTo("June")
    }

    // Token Tests: Test all TR35 pattern tokens in a single test under en-US and programmatically
    // set timezone.
    @Test
    fun execute_allTokensInSinglePattern_evaluatesCorrectly() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val calendar =
                java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(2026, java.util.Calendar.JUNE, 15, 14, 30, 45)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
            val timestamp = calendar.timeInMillis / 1000L

            val function = A2uiFormatDateFunction(localeProvider = { Locale.US })
            val result =
                function.execute(
                    mapOf(
                        ARG_VALUE to timestamp,
                        ARG_FORMAT to "yyyy yy MMMM MMM MM M EEEE E dd d HH H hh h mm ss a",
                    )
                ) as String

            assertThat(result)
                .isEqualTo("2026 26 June Jun 06 6 Monday Mon 15 15 14 14 02 2 30 45 PM")
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun execute_missingFormat_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatDateFunction.INSTANCE.execute(mapOf(ARG_VALUE to 1781481600L))
        }
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatDateFunction.INSTANCE.execute(mapOf(ARG_FORMAT to "yyyy-MM-dd"))
        }
    }

    @Test
    fun execute_invalidTimestampType_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatDateFunction.INSTANCE.execute(
                mapOf(ARG_VALUE to "not-a-timestamp", ARG_FORMAT to "yyyy-MM-dd")
            )
        }
    }

    @Test
    fun execute_invalidPattern_throwsRuntimeException() {
        assertThrows(A2uiException.A2uiRuntimeException::class.java) {
            A2uiFormatDateFunction.INSTANCE.execute(
                mapOf(ARG_VALUE to 1781481600L, ARG_FORMAT to "invalid-pattern")
            )
        }
    }

    private companion object {
        private const val ARG_VALUE = "value"
        private const val ARG_FORMAT = "format"
    }
}
