/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.res

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class StringResourcesTest {

    // Constants defined in strings.xml
    private val NotLocalizedText = "NotLocalizedText"
    private val DefaultLocalizedText = "DefaultLocaleText"
    private val SpanishLocalizedText = "SpanishText"

    // Constants defined in strings.xml with formatting with integer 100.
    private val NotLocalizedFormatText = "NotLocalizedFormatText:100"
    private val DefaultLocalizedFormatText = "DefaultLocaleFormatText:100"
    private val SpanishLocalizedFormatText = "SpanishFormatText:100"

    // Constant used for formatting string in test.
    private val FormatValue = 100

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun stringResource_not_localized_defaultLocale() {
        rule.setContent {
            assertThat(stringResource(R.string.not_localized)).isEqualTo(NotLocalizedText)
        }
    }

    @Test
    fun stringResource_not_localized() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Locales(LocaleList(Locale("es-ES")))
            ) {
                assertThat(stringResource(R.string.not_localized)).isEqualTo(NotLocalizedText)
            }
        }
    }

    @Test
    fun stringResource_localized_defaultLocale() {
        rule.setContent {
            assertThat(stringResource(R.string.localized))
                .isEqualTo(DefaultLocalizedText)
        }
    }

    @Test
    fun stringResource_localized() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Locales(LocaleList(Locale("es-ES")))
            ) {
                assertThat(stringResource(R.string.localized))
                    .isEqualTo(SpanishLocalizedText)
            }
        }
    }

    @Test
    fun stringResource_not_localized_format_defaultLocale() {
        rule.setContent {
            assertThat(stringResource(R.string.not_localized_format, FormatValue))
                .isEqualTo(NotLocalizedFormatText)
        }
    }

    @Test
    fun stringResource_not_localized_format() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Locales(LocaleList(Locale("es-ES")))
            ) {
                assertThat(stringResource(R.string.not_localized_format, FormatValue))
                    .isEqualTo(NotLocalizedFormatText)
            }
        }
    }

    @Test
    fun stringResource_localized_format_defaultLocale() {
        rule.setContent {
            assertThat(stringResource(R.string.localized_format, FormatValue))
                .isEqualTo(DefaultLocalizedFormatText)
        }
    }

    @Test
    fun stringResource_localized_format() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Locales(LocaleList(Locale("es-ES")))
            ) {
                assertThat(stringResource(R.string.localized_format, FormatValue))
                    .isEqualTo(SpanishLocalizedFormatText)
            }
        }
    }

    @Test
    fun stringArrayResource() {
        rule.setContent {
            assertThat(stringArrayResource(R.array.string_array))
                .isEqualTo(arrayOf("string1", "string2"))
        }
    }

    @Test
    fun pluralStringResource_withoutArguments() {
        rule.setContent {
            assertThat(pluralStringResource(R.plurals.plurals_without_arguments, 1))
                .isEqualTo("There is one Android here")
            assertThat(pluralStringResource(R.plurals.plurals_without_arguments, 42))
                .isEqualTo("There are a number of Androids here")
        }
    }

    @Test
    fun pluralStringResource_withArguments() {
        rule.setContent {
            assertThat(pluralStringResource(R.plurals.plurals_with_arguments, 1, 1))
                .isEqualTo("There is 1 Android here")
            assertThat(pluralStringResource(R.plurals.plurals_with_arguments, 42, 42))
                .isEqualTo("There are 42 Androids here")
        }
    }
}
