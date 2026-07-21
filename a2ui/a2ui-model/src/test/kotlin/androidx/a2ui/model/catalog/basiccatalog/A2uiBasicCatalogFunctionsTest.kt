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

package androidx.a2ui.model.catalog.basiccatalog

import androidx.a2ui.model.catalog.functions.A2uiFormatCurrencyFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatDateFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatNumberFunction
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test

class A2uiBasicCatalogFunctionsTest {

    private val fakeUrlOpener = A2uiUrlOpener { _ -> }
    private val fakeMessageFormatter = A2uiMessageFormatter { _, _, _ -> "" }

    @Test
    fun createBasicCatalogFunctions_returnsAllExpectedFunctions() {
        val functions = createBasicCatalogFunctions(fakeUrlOpener, fakeMessageFormatter)
        val functionNames = functions.map { it.definition.name }

        assertThat(functionNames)
            .containsExactly(
                "and",
                "email",
                "formatCurrency",
                "formatDate",
                "formatNumber",
                "formatString",
                "length",
                "not",
                "numeric",
                "openUrl",
                "or",
                "pluralize",
                "regex",
                "required",
            )
    }

    @Test
    fun createBasicCatalogFunctions_withNullLocaleProvider_usesDefaultInstances() {
        val functions =
            createBasicCatalogFunctions(
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = null,
            )

        // Verify we fall back to standard instances for non-stateful function objects
        val currencyFunc = functions.first { it is A2uiFormatCurrencyFunction }
        val dateFunc = functions.first { it is A2uiFormatDateFunction }
        val numberFunc = functions.first { it is A2uiFormatNumberFunction }

        assertThat(currencyFunc).isSameInstanceAs(A2uiFormatCurrencyFunction.INSTANCE)
        assertThat(dateFunc).isSameInstanceAs(A2uiFormatDateFunction.INSTANCE)
        assertThat(numberFunc).isSameInstanceAs(A2uiFormatNumberFunction.INSTANCE)
    }

    @Test
    fun createBasicCatalogFunctions_withCustomLocaleProvider_usesInjectedProvider() {
        val customLocale = Locale.FRANCE
        val customLocaleProvider = A2uiLocaleProvider { customLocale }

        val functions =
            createBasicCatalogFunctions(
                urlOpener = fakeUrlOpener,
                messageFormatter = fakeMessageFormatter,
                localeProvider = customLocaleProvider,
            )

        val currencyFunc = functions.first { it is A2uiFormatCurrencyFunction }
        val dateFunc = functions.first { it is A2uiFormatDateFunction }
        val numberFunc = functions.first { it is A2uiFormatNumberFunction }

        assertThat(currencyFunc).isNotSameInstanceAs(A2uiFormatCurrencyFunction.INSTANCE)
        assertThat(dateFunc).isNotSameInstanceAs(A2uiFormatDateFunction.INSTANCE)
        assertThat(numberFunc).isNotSameInstanceAs(A2uiFormatNumberFunction.INSTANCE)
    }
}
