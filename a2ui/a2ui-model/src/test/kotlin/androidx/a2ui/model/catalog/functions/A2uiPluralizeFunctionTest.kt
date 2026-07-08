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

import android.content.Context
import androidx.a2ui.model.protocol.A2uiException
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class A2uiPluralizeFunctionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = Mockito.mock(Context::class.java)
    }

    @Test
    fun execute_zeroCategory_evaluatesCorrectly() {
        // Welsh (cy) treats 0 as 'zero'
        val function =
            A2uiPluralizeFunction(context, localeProvider = { Locale.forLanguageTag("cy") })
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
        assertThat(function.execute(args)).isEqualTo(VAL_ZERO)
    }

    @Test
    fun execute_oneCategory_evaluatesCorrectly() {
        // English (en) treats 1 as 'one'
        val function = A2uiPluralizeFunction(context, localeProvider = { Locale.ENGLISH })
        val args =
            mapOf(
                ARG_VALUE_KEY to 1.0,
                ARG_ZERO_KEY to VAL_ZERO,
                ARG_ONE_KEY to VAL_ONE,
                ARG_TWO_KEY to VAL_TWO,
                ARG_FEW_KEY to VAL_FEW,
                ARG_MANY_KEY to VAL_MANY,
                ARG_OTHER_KEY to VAL_OTHER,
            )
        assertThat(function.execute(args)).isEqualTo(VAL_ONE)
    }

    @Test
    fun execute_twoCategory_evaluatesCorrectly() {
        // Welsh (cy) treats 2 as 'two'
        val function =
            A2uiPluralizeFunction(context, localeProvider = { Locale.forLanguageTag("cy") })
        val args =
            mapOf(
                ARG_VALUE_KEY to 2.0,
                ARG_ZERO_KEY to VAL_ZERO,
                ARG_ONE_KEY to VAL_ONE,
                ARG_TWO_KEY to VAL_TWO,
                ARG_FEW_KEY to VAL_FEW,
                ARG_MANY_KEY to VAL_MANY,
                ARG_OTHER_KEY to VAL_OTHER,
            )
        assertThat(function.execute(args)).isEqualTo(VAL_TWO)
    }

    @Test
    fun execute_fewCategory_evaluatesCorrectly() {
        // Welsh (cy) treats 3 as 'few'
        val function =
            A2uiPluralizeFunction(context, localeProvider = { Locale.forLanguageTag("cy") })
        val args =
            mapOf(
                ARG_VALUE_KEY to 3.0,
                ARG_ZERO_KEY to VAL_ZERO,
                ARG_ONE_KEY to VAL_ONE,
                ARG_TWO_KEY to VAL_TWO,
                ARG_FEW_KEY to VAL_FEW,
                ARG_MANY_KEY to VAL_MANY,
                ARG_OTHER_KEY to VAL_OTHER,
            )
        assertThat(function.execute(args)).isEqualTo(VAL_FEW)
    }

    @Test
    fun execute_manyCategory_evaluatesCorrectly() {
        // Welsh (cy) treats 6 as 'many'
        val function =
            A2uiPluralizeFunction(context, localeProvider = { Locale.forLanguageTag("cy") })
        val args =
            mapOf(
                ARG_VALUE_KEY to 6.0,
                ARG_ZERO_KEY to VAL_ZERO,
                ARG_ONE_KEY to VAL_ONE,
                ARG_TWO_KEY to VAL_TWO,
                ARG_FEW_KEY to VAL_FEW,
                ARG_MANY_KEY to VAL_MANY,
                ARG_OTHER_KEY to VAL_OTHER,
            )
        assertThat(function.execute(args)).isEqualTo(VAL_MANY)
    }

    @Test
    fun execute_otherCategory_evaluatesCorrectly() {
        // English (en) treats 5 as 'other'
        val function = A2uiPluralizeFunction(context, localeProvider = { Locale.ENGLISH })
        val args =
            mapOf(
                ARG_VALUE_KEY to 5.0,
                ARG_ZERO_KEY to VAL_ZERO,
                ARG_ONE_KEY to VAL_ONE,
                ARG_TWO_KEY to VAL_TWO,
                ARG_FEW_KEY to VAL_FEW,
                ARG_MANY_KEY to VAL_MANY,
                ARG_OTHER_KEY to VAL_OTHER,
            )
        assertThat(function.execute(args)).isEqualTo(VAL_OTHER)
    }

    @Test
    fun execute_missingCategory_fallsBackToOther() {
        // Value 1.0 would resolve to 'one', but since 'one' is not provided, it falls back to
        // 'other'
        val function = A2uiPluralizeFunction(context, localeProvider = { Locale.ENGLISH })
        val args = mapOf(ARG_VALUE_KEY to 1.0, ARG_OTHER_KEY to VAL_FALLBACK_OTHER)
        assertThat(function.execute(args)).isEqualTo(VAL_FALLBACK_OTHER)
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        val function = A2uiPluralizeFunction(context)
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            function.execute(emptyMap())
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
