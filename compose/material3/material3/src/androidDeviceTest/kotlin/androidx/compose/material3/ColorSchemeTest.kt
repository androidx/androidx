/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ColorSchemeTest {

    @get:Rule val rule = createComposeRule()

    /**
     * Test for switching between provided [ColorScheme]s, ensuring that the existing colors objects
     * are preserved. (b/182635582)
     */
    @Test
    fun switchingBetweenColors() {
        val lightColors = lightColorScheme()
        val darkColors = darkColorScheme()
        val colorSchemeState = mutableStateOf(lightColors)
        var currentColorScheme: ColorScheme? = null
        rule.setContent {
            MaterialTheme(colorSchemeState.value) {
                Button(onReadColorScheme = { currentColorScheme = it })
            }
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColorScheme())).isTrue()
            assertThat(darkColors.contentEquals(darkColorScheme())).isTrue()
            // Current colors should be light
            assertThat(currentColorScheme!!.contentEquals(lightColors)).isTrue()
            // Change current colors to dark
            colorSchemeState.value = darkColors
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColorScheme())).isTrue()
            assertThat(darkColors.contentEquals(darkColorScheme())).isTrue()
            // Current colors should be dark
            assertThat(currentColorScheme!!.contentEquals(darkColors)).isTrue()
            // Change current colors back to light
            colorSchemeState.value = lightColors
        }

        rule.runOnIdle {
            // Initial colors should never be touched
            assertThat(lightColors.contentEquals(lightColorScheme())).isTrue()
            assertThat(darkColors.contentEquals(darkColorScheme())).isTrue()
            // Current colors should be light
            assertThat(currentColorScheme!!.contentEquals(lightColors)).isTrue()
        }
    }

    @Test
    fun baselineContentContrast() {
        val expectedContrastValue = 3 // Minimum 3:1 contrast ratio
        val baselineSchemes =
            listOf(lightColorScheme(), darkColorScheme(), expressiveLightColorScheme())

        for (colorScheme in baselineSchemes) {
            assertThat(calculateContrastRatio(colorScheme.onPrimary, colorScheme.primary))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onSecondary, colorScheme.secondary))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onTertiary, colorScheme.tertiary))
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onPrimaryContainer,
                        colorScheme.primaryContainer,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onSecondaryContainer,
                        colorScheme.secondaryContainer,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onTertiaryContainer,
                        colorScheme.tertiaryContainer,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onError, colorScheme.error))
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(colorScheme.onErrorContainer, colorScheme.errorContainer)
                )
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onSurface, colorScheme.surface))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onSurface, colorScheme.surfaceContainer))
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(colorScheme.onSurface, colorScheme.surfaceContainerHigh)
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onSurface,
                        colorScheme.surfaceContainerHighest,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(colorScheme.onSurface, colorScheme.surfaceContainerLow)
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onSurface,
                        colorScheme.surfaceContainerLowest,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onSurface, colorScheme.surfaceDim))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onSurface, colorScheme.surfaceBright))
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(colorScheme.inverseOnSurface, colorScheme.inverseSurface)
                )
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(colorScheme.onPrimaryFixed, colorScheme.primaryFixed))
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onPrimaryFixedVariant,
                        colorScheme.primaryFixed,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(colorScheme.onPrimaryFixed, colorScheme.primaryFixedDim)
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onPrimaryFixedVariant,
                        colorScheme.primaryFixedDim,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(colorScheme.onSecondaryFixed, colorScheme.secondaryFixed)
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onSecondaryFixedVariant,
                        colorScheme.secondaryFixed,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onSecondaryFixed,
                        colorScheme.secondaryFixedDim,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onSecondaryFixedVariant,
                        colorScheme.secondaryFixedDim,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(colorScheme.onTertiaryFixed, colorScheme.tertiaryFixed)
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onTertiaryFixedVariant,
                        colorScheme.tertiaryFixed,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onTertiaryFixed,
                        colorScheme.tertiaryFixedDim,
                    )
                )
                .isAtLeast(expectedContrastValue)
            assertThat(
                    calculateContrastRatio(
                        colorScheme.onTertiaryFixedVariant,
                        colorScheme.tertiaryFixedDim,
                    )
                )
                .isAtLeast(expectedContrastValue)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun dynamicColorSchemes_resolveCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val lightDynamicScheme = dynamicLightColorScheme(context)
        val darkDynamicScheme = dynamicDarkColorScheme(context)

        assertThat(lightDynamicScheme).isNotNull()
        assertThat(darkDynamicScheme).isNotNull()

        // Verify they load colors (not fallback black color objects)
        assertThat(lightDynamicScheme.primary).isNotEqualTo(Color.Black)
        assertThat(darkDynamicScheme.primary).isNotEqualTo(Color.Black)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun dynamicColorSchemes_unthemedContext_resolveCorrectly() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val unthemedContext =
            object : android.content.ContextWrapper(targetContext) {
                override fun getTheme(): android.content.res.Resources.Theme? = null
            }
        val lightDynamicScheme = dynamicLightColorScheme(unthemedContext)
        val darkDynamicScheme = dynamicDarkColorScheme(unthemedContext)
        val tonalPalette = dynamicTonalPalette(unthemedContext)

        val expectedLight = dynamicLightColorScheme(targetContext)
        val expectedDark = dynamicDarkColorScheme(targetContext)
        val expectedPalette = dynamicTonalPalette(targetContext)

        assertThat(lightDynamicScheme).isNotNull()
        assertThat(darkDynamicScheme).isNotNull()
        assertThat(tonalPalette).isNotNull()

        // Verify they load identical colors as a themed context
        assertThat(lightDynamicScheme.contentEquals(expectedLight)).isTrue()
        assertThat(darkDynamicScheme.contentEquals(expectedDark)).isTrue()
        assertThat(tonalPalette.contentEquals(expectedPalette)).isTrue()
    }

    @Composable
    private fun Button(onReadColorScheme: (ColorScheme) -> Unit) {
        val colorScheme = MaterialTheme.colorScheme
        onReadColorScheme(colorScheme)
    }
}

/**
 * [ColorScheme] is @Stable, so by contract it doesn't have equals implemented. And since it creates
 * a new Colors object to mutate internally, we can't compare references. Instead we compare the
 * properties to make sure that the properties are equal.
 *
 * @return true if all the properties inside [this] are equal to those in [other], false otherwise.
 */
private fun ColorScheme.contentEquals(other: ColorScheme): Boolean {
    if (primary != other.primary) return false
    if (onPrimary != other.onPrimary) return false
    if (primaryContainer != other.primaryContainer) return false
    if (onPrimaryContainer != other.onPrimaryContainer) return false
    if (inversePrimary != other.inversePrimary) return false
    if (secondary != other.secondary) return false
    if (onSecondary != other.onSecondary) return false
    if (secondaryContainer != other.secondaryContainer) return false
    if (onSecondaryContainer != other.onSecondaryContainer) return false
    if (tertiary != other.tertiary) return false
    if (onTertiary != other.onTertiary) return false
    if (tertiaryContainer != other.tertiaryContainer) return false
    if (onTertiaryContainer != other.onTertiaryContainer) return false
    if (background != other.background) return false
    if (onBackground != other.onBackground) return false
    if (surface != other.surface) return false
    if (onSurface != other.onSurface) return false
    if (surfaceVariant != other.surfaceVariant) return false
    if (onSurfaceVariant != other.onSurfaceVariant) return false
    if (inverseSurface != other.inverseSurface) return false
    if (inverseOnSurface != other.inverseOnSurface) return false
    if (error != other.error) return false
    if (onError != other.onError) return false
    if (errorContainer != other.errorContainer) return false
    if (onErrorContainer != other.onErrorContainer) return false
    if (outline != other.outline) return false
    if (outlineVariant != other.outlineVariant) return false
    if (scrim != other.scrim) return false
    if (surfaceBright != other.surfaceBright) return false
    if (surfaceContainer != other.surfaceContainer) return false
    if (surfaceContainerHigh != other.surfaceContainerHigh) return false
    if (surfaceContainerHighest != other.surfaceContainerHighest) return false
    if (surfaceContainerLow != other.surfaceContainerLow) return false
    if (surfaceContainerLowest != other.surfaceContainerLowest) return false
    if (surfaceDim != other.surfaceDim) return false
    if (primaryFixed != other.primaryFixed) return false
    if (primaryFixedDim != other.primaryFixedDim) return false
    if (onPrimaryFixed != other.onPrimaryFixed) return false
    if (onPrimaryFixedVariant != other.onPrimaryFixedVariant) return false
    if (secondaryFixed != other.secondaryFixed) return false
    if (secondaryFixedDim != other.secondaryFixedDim) return false
    if (onSecondaryFixed != other.onSecondaryFixed) return false
    if (onSecondaryFixedVariant != other.onSecondaryFixedVariant) return false
    if (tertiaryFixed != other.tertiaryFixed) return false
    if (tertiaryFixedDim != other.tertiaryFixedDim) return false
    if (onTertiaryFixed != other.onTertiaryFixed) return false
    if (onTertiaryFixedVariant != other.onTertiaryFixedVariant) return false
    return true
}

private fun TonalPalette.contentEquals(other: TonalPalette): Boolean {
    return primary100 == other.primary100 &&
        primary99 == other.primary99 &&
        primary95 == other.primary95 &&
        primary90 == other.primary90 &&
        primary80 == other.primary80 &&
        primary70 == other.primary70 &&
        primary60 == other.primary60 &&
        primary50 == other.primary50 &&
        primary40 == other.primary40 &&
        primary30 == other.primary30 &&
        primary20 == other.primary20 &&
        primary10 == other.primary10 &&
        primary0 == other.primary0 &&
        secondary100 == other.secondary100 &&
        secondary99 == other.secondary99 &&
        secondary95 == other.secondary95 &&
        secondary90 == other.secondary90 &&
        secondary80 == other.secondary80 &&
        secondary70 == other.secondary70 &&
        secondary60 == other.secondary60 &&
        secondary50 == other.secondary50 &&
        secondary40 == other.secondary40 &&
        secondary30 == other.secondary30 &&
        secondary20 == other.secondary20 &&
        secondary10 == other.secondary10 &&
        secondary0 == other.secondary0 &&
        tertiary100 == other.tertiary100 &&
        tertiary99 == other.tertiary99 &&
        tertiary95 == other.tertiary95 &&
        tertiary90 == other.tertiary90 &&
        tertiary80 == other.tertiary80 &&
        tertiary70 == other.tertiary70 &&
        tertiary60 == other.tertiary60 &&
        tertiary50 == other.tertiary50 &&
        tertiary40 == other.tertiary40 &&
        tertiary30 == other.tertiary30 &&
        tertiary20 == other.tertiary20 &&
        tertiary10 == other.tertiary10 &&
        tertiary0 == other.tertiary0 &&
        neutral100 == other.neutral100 &&
        neutral99 == other.neutral99 &&
        neutral98 == other.neutral98 &&
        neutral96 == other.neutral96 &&
        neutral95 == other.neutral95 &&
        neutral94 == other.neutral94 &&
        neutral92 == other.neutral92 &&
        neutral90 == other.neutral90 &&
        neutral87 == other.neutral87 &&
        neutral80 == other.neutral80 &&
        neutral70 == other.neutral70 &&
        neutral60 == other.neutral60 &&
        neutral50 == other.neutral50 &&
        neutral40 == other.neutral40 &&
        neutral30 == other.neutral30 &&
        neutral24 == other.neutral24 &&
        neutral22 == other.neutral22 &&
        neutral20 == other.neutral20 &&
        neutral17 == other.neutral17 &&
        neutral12 == other.neutral12 &&
        neutral10 == other.neutral10 &&
        neutral6 == other.neutral6 &&
        neutral4 == other.neutral4 &&
        neutral0 == other.neutral0 &&
        neutralVariant100 == other.neutralVariant100 &&
        neutralVariant99 == other.neutralVariant99 &&
        neutralVariant98 == other.neutralVariant98 &&
        neutralVariant96 == other.neutralVariant96 &&
        neutralVariant95 == other.neutralVariant95 &&
        neutralVariant94 == other.neutralVariant94 &&
        neutralVariant92 == other.neutralVariant92 &&
        neutralVariant90 == other.neutralVariant90 &&
        neutralVariant87 == other.neutralVariant87 &&
        neutralVariant80 == other.neutralVariant80 &&
        neutralVariant70 == other.neutralVariant70 &&
        neutralVariant60 == other.neutralVariant60 &&
        neutralVariant50 == other.neutralVariant50 &&
        neutralVariant40 == other.neutralVariant40 &&
        neutralVariant30 == other.neutralVariant30 &&
        neutralVariant24 == other.neutralVariant24 &&
        neutralVariant22 == other.neutralVariant22 &&
        neutralVariant20 == other.neutralVariant20 &&
        neutralVariant17 == other.neutralVariant17 &&
        neutralVariant12 == other.neutralVariant12 &&
        neutralVariant10 == other.neutralVariant10 &&
        neutralVariant6 == other.neutralVariant6 &&
        neutralVariant4 == other.neutralVariant4 &&
        neutralVariant0 == other.neutralVariant0
}
