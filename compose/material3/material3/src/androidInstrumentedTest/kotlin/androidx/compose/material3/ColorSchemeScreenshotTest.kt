/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.material3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ColorSchemeScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule(order = 0) val rule = createComposeRule()
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun surfaceColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colorScheme = MaterialTheme.colorScheme
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorItem(name = "Surface", color = colorScheme.surface)
                ColorItem(name = "Surface Container", color = colorScheme.surfaceContainer)
                ColorItem(name = "Surface Container High", color = colorScheme.surfaceContainerHigh)
                ColorItem(
                    name = "Surface Container Highest",
                    color = colorScheme.surfaceContainerHighest,
                )
                ColorItem(name = "Surface Container Low", color = colorScheme.surfaceContainerLow)
                ColorItem(
                    name = "Surface Container Lowest",
                    color = colorScheme.surfaceContainerLowest,
                )
                ColorItem(name = "Surface Dim", color = colorScheme.surfaceDim)
                ColorItem(name = "Surface Bright", color = colorScheme.surfaceBright)
                ColorItem(name = "On Surface", color = colorScheme.onSurface)
                ColorItem(name = "Inverse Surface", color = colorScheme.inverseSurface)
                ColorItem(name = "Inverse On Surface", color = colorScheme.inverseOnSurface)
            }
        }
        assertAgainstGolden("colorScheme_${scheme.name}_surfaceColors")
    }

    @Test
    fun primaryColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colorScheme = MaterialTheme.colorScheme
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorItem(name = "Primary", color = colorScheme.primary)
                ColorItem(name = "On Primary", color = colorScheme.onPrimary)
                ColorItem(name = "Primary Container", color = colorScheme.primaryContainer)
                ColorItem(name = "On Primary Container", color = colorScheme.onPrimaryContainer)
                ColorItem(name = "Inverse Primary", color = colorScheme.inversePrimary)
            }
        }
        assertAgainstGolden("colorScheme_${scheme.name}_primaryColors")
    }

    @Test
    fun secondaryColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colorScheme = MaterialTheme.colorScheme
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorItem(name = "Secondary", color = colorScheme.secondary)
                ColorItem(name = "On Secondary", color = colorScheme.onSecondary)
                ColorItem(name = "Secondary Container", color = colorScheme.secondaryContainer)
                ColorItem(name = "On Secondary Container", color = colorScheme.onSecondaryContainer)
            }
        }
        assertAgainstGolden("colorScheme_${scheme.name}_secondaryColors")
    }

    @Test
    fun tertiaryColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colorScheme = MaterialTheme.colorScheme
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorItem(name = "Tertiary", color = colorScheme.tertiary)
                ColorItem(name = "On Tertiary", color = colorScheme.onTertiary)
                ColorItem(name = "Tertiary Container", color = colorScheme.tertiaryContainer)
                ColorItem(name = "On Tertiary Container", color = colorScheme.onTertiaryContainer)
            }
        }
        assertAgainstGolden("colorScheme_${scheme.name}_tertiaryColors")
    }

    @Test
    fun errorColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colorScheme = MaterialTheme.colorScheme
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorItem(name = "Error", color = colorScheme.error)
                ColorItem(name = "On Error", color = colorScheme.onError)
                ColorItem(name = "Error Container", color = colorScheme.errorContainer)
                ColorItem(name = "On Error Container", color = colorScheme.onErrorContainer)
            }
        }
        assertAgainstGolden("colorScheme_${scheme.name}_errorColors")
    }

    /**
     * Deprecated colors roles and colors not directly tied to Primary, Secondary, Tertiary, Surface
     * and Error roles.
     */
    @Test
    fun miscColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colorScheme = MaterialTheme.colorScheme
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorItem(name = "Scrim", color = colorScheme.scrim)
                ColorItem(name = "Outline", color = colorScheme.outline)
                ColorItem(name = "Outline Variant", color = colorScheme.outlineVariant)
                ColorItem(name = "Surface Variant", color = colorScheme.surfaceVariant)
                ColorItem(name = "On Surface Variant", color = colorScheme.onSurfaceVariant)
                ColorItem(name = "Background Variant", color = colorScheme.background)
                ColorItem(name = "On Background Variant", color = colorScheme.onBackground)
            }
        }
        assertAgainstGolden("colorScheme_${scheme.name}_miscColors")
    }

    /**
     * Deprecated colors roles and colors not directly tied to Primary, Secondary, Tertiary, Surface
     * and Error roles.
     */
    @Test
    fun fixedColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colorScheme = MaterialTheme.colorScheme
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                ColorItem(name = "Primary Fixed", color = colorScheme.primaryFixed)
                ColorItem(name = "Primary Fixed Dim", color = colorScheme.primaryFixedDim)
                ColorItem(name = "On Primary Fixed", color = colorScheme.onPrimaryFixed)
                ColorItem(
                    name = "On Primary Fixed Variant",
                    color = colorScheme.onPrimaryFixedVariant,
                )
                ColorItem(name = "Secondary Fixed", color = colorScheme.secondaryFixed)
                ColorItem(name = "Secondary Fixed Dim", color = colorScheme.secondaryFixedDim)
                ColorItem(name = "On Secondary Fixed", color = colorScheme.onSecondaryFixed)
                ColorItem(
                    name = "On Secondary Fixed Variant",
                    color = colorScheme.onSecondaryFixedVariant,
                )
                ColorItem(name = "Tertiary Fixed", color = colorScheme.tertiaryFixed)
                ColorItem(name = "Tertiary Fixed Dim", color = colorScheme.tertiaryFixedDim)
                ColorItem(name = "On Tertiary Fixed", color = colorScheme.onTertiaryFixed)
                ColorItem(
                    name = "On Tertiary Fixed Variant",
                    color = colorScheme.onTertiaryFixedVariant,
                )
            }
        }
        assertAgainstGolden("colorScheme_${scheme.name}_fixedColors")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(Tag).captureToImage().assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        private val LightCustomColorScheme =
            lightColorScheme(
                primary = Color(0xFF006E2C),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF43B55F),
                onPrimaryContainer = Color(0xFF004117),
                inversePrimary = Color(0xFF6DDD81),
                secondary = Color(0xFF3F6743),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFC2F0C2),
                onSecondaryContainer = Color(0xFF466F4A),
                tertiary = Color(0xFF005EB3),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFF5EA1FF),
                onTertiaryContainer = Color(0xFF00376C),
                background = Color(0xFFF5FBF0),
                onBackground = Color(0xFF171D17),
                surface = Color(0xFFF5FBF0),
                onSurface = Color(0xFF171D17),
                surfaceVariant = Color(0xFFD9E6D6),
                onSurfaceVariant = Color(0xFF3E4A3E),
                inverseSurface = Color(0xFF2C322B),
                inverseOnSurface = Color(0xFFECF3E8),
                error = Color(0xFFBA1A1A),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
                outline = Color(0xFF6C786A),
                outlineVariant = Color(0xFFBDCABA),
                scrim = Color(0xFF000000),
                surfaceTint = Color(0xFF006E2C),
                surfaceContainerHighest = Color(0xFFDEE4DA),
                surfaceContainerHigh = Color(0xFFE4EADF),
                surfaceContainer = Color(0xFFE9F0E5),
                surfaceContainerLow = Color(0xFFEFF6EB),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceBright = Color(0xFFF5FBF0),
                surfaceDim = Color(0xFFD5DCD1),
            )

        private val DarkCustomColorScheme =
            darkColorScheme(
                primary = Color(0xFF6DDD81),
                onPrimary = Color(0xFF003914),
                primaryContainer = Color(0xFF008738),
                onPrimaryContainer = Color(0xFFF7FFF2),
                inversePrimary = Color(0xFF006E2C),
                secondary = Color(0xFFA5D2A6),
                onSecondary = Color(0xFF0F3819),
                secondaryContainer = Color(0xFF1D4524),
                onSecondaryContainer = Color(0xFF87B389),
                tertiary = Color(0xFFA7C8FF),
                onTertiary = Color(0xFF003061),
                tertiaryContainer = Color(0xFF0774D9),
                onTertiaryContainer = Color(0xFFFDFCFF),
                background = Color(0xFF0F150F),
                onBackground = Color(0xFFDEE4DA),
                surface = Color(0xFF0F150F),
                onSurface = Color(0xFFDEE4DA),
                surfaceVariant = Color(0xFF3E4A3E),
                onSurfaceVariant = Color(0xFFBDCABA),
                inverseSurface = Color(0xFFDEE4DA),
                inverseOnSurface = Color(0xFF2C322B),
                error = Color(0xFFFFB4A9),
                onError = Color(0xFF680003),
                errorContainer = Color(0xFF930006),
                onErrorContainer = Color(0xFFFFDAD4),
                outline = Color(0xFF6C786A),
                outlineVariant = Color(0xFF3E4A3E),
                scrim = Color(0xFF000000),
                surfaceTint = Color(0xFF6DDD81),
                surfaceContainerHighest = Color(0xFF30362F),
                surfaceContainerHigh = Color(0xFF252C25),
                surfaceContainer = Color(0xFF1B211B),
                surfaceContainerLow = Color(0xFF171D17),
                surfaceContainerLowest = Color(0xFF0A100A),
                surfaceBright = Color(0xFF343B34),
                surfaceDim = Color(0xFF0F150F),
            )

        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("light", lightColorScheme()),
                ColorSchemeWrapper("light_dynamic", LightCustomColorScheme),
                ColorSchemeWrapper("expressive_light", expressiveLightColorScheme()),
                ColorSchemeWrapper("dark", darkColorScheme()),
                ColorSchemeWrapper("dark_dynamic", DarkCustomColorScheme),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    private val Tag = "ColorScheme"
}

@Composable
private fun ColorItem(name: String, color: Color, threshold: Float = .3f) {
    val whiteTextStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
    val blackTextStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
    Surface(color = color) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).height(48.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = if (color.luminance() < threshold) whiteTextStyle else blackTextStyle,
            )
        }
    }
}
