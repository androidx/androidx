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

class A2uiFormatCurrencyFunctionTest {

    @Test
    fun execute_validArguments_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        assertThat(function.execute(mapOf(ARG_VALUE to 12.34, ARG_CURRENCY to CURRENCY_USD)))
            .isEqualTo("$12.34")
    }

    @Test
    fun execute_zeroDecimals_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        assertThat(
                function.execute(
                    mapOf(ARG_VALUE to 12.34, ARG_CURRENCY to CURRENCY_USD, ARG_DECIMALS to 0.0)
                )
            )
            .isEqualTo("$12")
    }

    @Test
    fun execute_fourDecimals_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        assertThat(
                function.execute(
                    mapOf(ARG_VALUE to 12.3456, ARG_CURRENCY to CURRENCY_USD, ARG_DECIMALS to 4.0)
                )
            )
            .isEqualTo("$12.3456")
    }

    @Test
    fun execute_withoutGrouping_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        assertThat(
                function.execute(
                    mapOf(ARG_VALUE to 1234.56, ARG_CURRENCY to CURRENCY_USD, ARG_GROUPING to false)
                )
            )
            .isEqualTo("$1234.56")
    }

    @Test
    fun execute_withGrouping_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        assertThat(
                function.execute(
                    mapOf(ARG_VALUE to 1234.56, ARG_CURRENCY to CURRENCY_USD, ARG_GROUPING to true)
                )
            )
            .isEqualTo("$1,234.56")
    }

    // Locale Tests: Always use USD as currency, vary the locale.
    @Test
    fun execute_germanLocale_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.GERMANY })
        val result =
            function.execute(mapOf(ARG_VALUE to 1234.56, ARG_CURRENCY to CURRENCY_USD)) as String
        // German format uses dot grouping and comma decimals (e.g. 1.234,56 $)
        assertThat(result).contains("1.234,56")
    }

    @Test
    fun execute_britishLocale_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.UK })
        val result =
            function.execute(mapOf(ARG_VALUE to 1234.56, ARG_CURRENCY to CURRENCY_USD)) as String
        // UK format uses comma grouping and dot decimals (e.g. $1,234.56)
        assertThat(result).contains("1,234.56")
    }

    @Test
    fun execute_frenchLocale_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.FRANCE })
        val result =
            function.execute(mapOf(ARG_VALUE to 1234.56, ARG_CURRENCY to CURRENCY_USD)) as String

        // French format uses space grouping and comma decimals (e.g. 1 234,56 $)
        val normalizedResult = result.replace('\u00A0', ' ').replace('\u202F', ' ')
        assertThat(normalizedResult).contains("1 234,56")
    }

    @Test
    fun execute_gbpCurrency_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to 1234.56, ARG_CURRENCY to CURRENCY_GBP)) as String

        assertThat(result).contains("£")
    }

    @Test
    fun execute_eurCurrency_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to 1234.56, ARG_CURRENCY to CURRENCY_EUR)) as String

        assertThat(result).contains("€")
    }

    @Test
    fun execute_jpyCurrency_evaluatesCorrectly() {
        val function = A2uiFormatCurrencyFunction(localeProvider = { Locale.US })
        val result =
            function.execute(mapOf(ARG_VALUE to 1234.0, ARG_CURRENCY to CURRENCY_JPY)) as String

        assertThat(result).contains("¥")
    }

    @Test
    fun execute_missingCurrency_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatCurrencyFunction.INSTANCE.execute(mapOf(ARG_VALUE to 12.34))
        }
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatCurrencyFunction.INSTANCE.execute(mapOf(ARG_CURRENCY to CURRENCY_USD))
        }
    }

    @Test
    fun execute_invalidValueType_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatCurrencyFunction.INSTANCE.execute(
                mapOf(ARG_VALUE to "not-a-number", ARG_CURRENCY to CURRENCY_USD)
            )
        }
    }

    @Test
    fun execute_emptyCurrency_throwsRuntimeException() {
        assertThrows(A2uiException.A2uiRuntimeException::class.java) {
            A2uiFormatCurrencyFunction.INSTANCE.execute(
                mapOf(ARG_VALUE to 12.34, ARG_CURRENCY to "")
            )
        }
    }

    @Test
    fun execute_invalidDecimalsType_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatCurrencyFunction.INSTANCE.execute(
                mapOf(
                    ARG_VALUE to 12.34,
                    ARG_CURRENCY to CURRENCY_USD,
                    ARG_DECIMALS to "not-an-int",
                )
            )
        }
    }

    @Test
    fun execute_invalidGroupingType_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatCurrencyFunction.INSTANCE.execute(
                mapOf(
                    ARG_VALUE to 12.34,
                    ARG_CURRENCY to CURRENCY_USD,
                    ARG_GROUPING to "not-a-boolean",
                )
            )
        }
    }

    @Test
    fun execute_invalidCurrencyCode_throwsValidationException() {
        assertThrows(A2uiException.A2uiRuntimeException::class.java) {
            A2uiFormatCurrencyFunction.INSTANCE.execute(
                mapOf(ARG_VALUE to 12.34, ARG_CURRENCY to CURRENCY_INVALID)
            )
        }
    }

    private companion object {
        private const val ARG_VALUE = "value"
        private const val ARG_CURRENCY = "currency"
        private const val ARG_DECIMALS = "decimals"
        private const val ARG_GROUPING = "grouping"

        private const val CURRENCY_USD = "USD"
        private const val CURRENCY_GBP = "GBP"
        private const val CURRENCY_EUR = "EUR"
        private const val CURRENCY_JPY = "JPY"
        private const val CURRENCY_INVALID = "INVALID"
    }
}
