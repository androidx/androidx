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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.wear.compose.material3.Typography
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteTypographyFontSettingsTest {

    @Test
    fun fromWearTypography_keepsFontAxesAndFontFeatureSettings() {
        // Wear Material3 styles use variable fonts with axes and also set
        // fontFeatureSettings = "pnum". See b/568283088.
        val typography = Typography()
        val remoteTypography = RemoteTypography(typography)

        val styles: List<Triple<String, TextStyle, RemoteTextStyle>> =
            listOf(
                Triple("displayLarge", typography.displayLarge, remoteTypography.displayLarge),
                Triple("displayMedium", typography.displayMedium, remoteTypography.displayMedium),
                Triple("displaySmall", typography.displaySmall, remoteTypography.displaySmall),
                Triple("titleLarge", typography.titleLarge, remoteTypography.titleLarge),
                Triple("titleMedium", typography.titleMedium, remoteTypography.titleMedium),
                Triple("titleSmall", typography.titleSmall, remoteTypography.titleSmall),
                Triple("labelLarge", typography.labelLarge, remoteTypography.labelLarge),
                Triple("labelMedium", typography.labelMedium, remoteTypography.labelMedium),
                Triple("labelSmall", typography.labelSmall, remoteTypography.labelSmall),
                Triple("bodyLarge", typography.bodyLarge, remoteTypography.bodyLarge),
                Triple("bodyMedium", typography.bodyMedium, remoteTypography.bodyMedium),
                Triple("bodySmall", typography.bodySmall, remoteTypography.bodySmall),
                Triple(
                    "bodyExtraSmall",
                    typography.bodyExtraSmall,
                    remoteTypography.bodyExtraSmall,
                ),
                Triple(
                    "numeralExtraLarge",
                    typography.numeralExtraLarge,
                    remoteTypography.numeralExtraLarge,
                ),
                Triple("numeralLarge", typography.numeralLarge, remoteTypography.numeralLarge),
                Triple("numeralMedium", typography.numeralMedium, remoteTypography.numeralMedium),
                Triple("numeralSmall", typography.numeralSmall, remoteTypography.numeralSmall),
                Triple(
                    "numeralExtraSmall",
                    typography.numeralExtraSmall,
                    remoteTypography.numeralExtraSmall,
                ),
            )

        for ((name, textStyle, remoteStyle) in styles) {
            val font = (textStyle.fontFamily as FontListFontFamily).fonts.single() as AndroidFont
            assertWithMessage("$name has font axes")
                .that(font.variationSettings.settings)
                .isNotEmpty()
            assertWithMessage("$name fontVariationSettings")
                .that(remoteStyle.fontVariationSettings)
                .isEqualTo(font.variationSettings)
            assertWithMessage("$name fontFeatureSettings")
                .that(remoteStyle.fontFeatureSettings)
                .isEqualTo(textStyle.fontFeatureSettings)
        }
        assertThat(typography.labelMedium.fontFeatureSettings).isEqualTo("pnum")
    }
}
