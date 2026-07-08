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

class A2uiFormatNumberFunctionTest {

    @Test
    fun execute_germanLocale_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.GERMANY })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56)) as String
        assertThat(result).isEqualTo("1.234,56")
    }

    @Test
    fun execute_frenchLocale_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.FRANCE })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56)) as String
        // French locale uses non-breaking space as grouping separator. Normalize to standard space.
        val normalizedResult = result.replace('\u00A0', ' ').replace('\u202F', ' ')
        assertThat(normalizedResult).isEqualTo("1 234,56")
    }

    @Test
    fun execute_usLocale_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56)) as String
        assertThat(result).isEqualTo("1,234.56")
    }

    @Test
    fun execute_withGrouping_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56, ARG_GROUPING to true)) as String
        assertThat(result).isEqualTo("1,234.56")
    }

    @Test
    fun execute_withoutGrouping_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56, ARG_GROUPING to false)) as String
        assertThat(result).isEqualTo("1234.56")
    }

    @Test
    fun execute_zeroDecimals_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56, ARG_DECIMALS to 0.0)) as String
        assertThat(result).isEqualTo("1,235")
    }

    @Test
    fun execute_fourDecimals_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56, ARG_DECIMALS to 4.0)) as String
        assertThat(result).isEqualTo("1,234.5600")
    }

    @Test
    fun execute_valueTypeDouble_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234.56)) as String
        assertThat(result).isEqualTo("1,234.56")
    }

    @Test
    fun execute_valueTypeInt_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234)) as String
        assertThat(result).isEqualTo("1,234")
    }

    @Test
    fun execute_valueTypeLong_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to 1234L)) as String
        assertThat(result).isEqualTo("1,234")
    }

    @Test
    fun execute_valueTypeString_evaluatesCorrectly() {
        val function = A2uiFormatNumberFunction(localeProvider = { Locale.US })
        val result = function.execute(mapOf(ARG_VALUE to "1234.56")) as String
        assertThat(result).isEqualTo("1,234.56")
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatNumberFunction.INSTANCE.execute(emptyMap())
        }
    }

    @Test
    fun execute_invalidValueType_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiFormatNumberFunction.INSTANCE.execute(mapOf(ARG_VALUE to "not-a-number"))
        }
    }

    private companion object {
        private const val ARG_VALUE = "value"
        private const val ARG_GROUPING = "grouping"
        private const val ARG_DECIMALS = "decimals"
    }
}
