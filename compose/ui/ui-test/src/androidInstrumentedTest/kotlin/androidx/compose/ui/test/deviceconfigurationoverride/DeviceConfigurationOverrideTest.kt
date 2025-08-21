/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test.deviceconfigurationoverride

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.FontWeightAdjustment
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.Keyboard
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.Navigation
import androidx.compose.ui.test.RoundScreen
import androidx.compose.ui.test.Touchscreen
import androidx.compose.ui.test.UiMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.then
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeviceConfigurationOverrideTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun smallSizeOverride_onSmallerElements_isDisplayed() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(100.dp, 100.dp))
            ) {
                Row {
                    Spacer(Modifier.requiredSize(40.dp, 40.dp))
                    Spacer(Modifier.testTag("node").requiredSize(40.dp, 40.dp))
                }
            }
        }

        rule.onNodeWithTag("node").assertIsDisplayed()
    }

    @Test
    fun smallSizeOverride_onLargerElements_isNotDisplayed() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(100.dp, 100.dp))
            ) {
                Row {
                    Spacer(Modifier.requiredSize(120.dp, 120.dp))
                    Spacer(Modifier.testTag("node").requiredSize(120.dp, 120.dp))
                }
            }
        }

        rule.onNodeWithTag("node").assertIsNotDisplayed()
    }

    @Test
    fun largeSizeOverride_onSmallerElements_isDisplayed() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(3000.dp, 3000.dp))
            ) {
                Row {
                    Spacer(Modifier.requiredSize(1400.dp, 1400.dp))
                    Spacer(Modifier.testTag("node").requiredSize(1400.dp, 1400.dp))
                }
            }
        }

        rule.onNodeWithTag("node").assertIsDisplayed()
    }

    @Test
    fun largeSizeOverride_onLargerElements_isNotDisplayed() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(3000.dp, 3000.dp))
            ) {
                Row {
                    Spacer(Modifier.requiredSize(3200.dp, 3200.dp))
                    Spacer(Modifier.testTag("node").requiredSize(3200.dp, 3200.dp))
                }
            }
        }

        rule.onNodeWithTag("node").assertIsNotDisplayed()
    }

    @Test
    fun sizeOverride_allowsForCorrectSpace_smallPortraitAspectRatio() {
        lateinit var actualDensity: Density
        var actualConstraints: Constraints? = null

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(30.dp, 40.dp))
            ) {
                Spacer(
                    modifier =
                        Modifier.layout { measurable, constraints ->
                            actualConstraints = constraints
                            actualDensity = this

                            val placeable = measurable.measure(constraints)

                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                )
            }
        }

        // The constraint should be within 0.5 pixels of the specified size
        // Due to rounding, we can't expect to have the Spacer take exactly the requested size which
        // is true in normal Compose code as well
        assertEquals(
            with(actualDensity) { 30.dp.toPx() },
            actualConstraints!!.maxWidth.toFloat(),
            0.5f,
        )
        assertEquals(
            with(actualDensity) { 40.dp.toPx() },
            actualConstraints!!.maxHeight.toFloat(),
            0.5f,
        )
    }

    @Test
    fun sizeOverride_allowsForCorrectSpace_smallLandscapeAspectRatio() {
        lateinit var actualDensity: Density
        var actualConstraints: Constraints? = null

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(40.dp, 30.dp))
            ) {
                Spacer(
                    modifier =
                        Modifier.layout { measurable, constraints ->
                            actualConstraints = constraints
                            actualDensity = this

                            val placeable = measurable.measure(constraints)

                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                )
            }
        }

        // The constraint should be within 0.5 pixels of the specified size
        // Due to rounding, we can't expect to have the Spacer take exactly the requested size which
        // is true in normal Compose code as well
        assertEquals(
            with(actualDensity) { 40.dp.toPx() },
            actualConstraints!!.maxWidth.toFloat(),
            0.5f,
        )
        assertEquals(
            with(actualDensity) { 30.dp.toPx() },
            actualConstraints!!.maxHeight.toFloat(),
            0.5f,
        )
    }

    @Test
    fun sizeOverride_allowsForCorrectSpace_largePortraitAspectRatio() {
        lateinit var actualDensity: Density
        var actualConstraints: Constraints? = null

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(3000.dp, 4000.dp))
            ) {
                Spacer(
                    modifier =
                        Modifier.layout { measurable, constraints ->
                            actualConstraints = constraints
                            actualDensity = this

                            val placeable = measurable.measure(constraints)

                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                )
            }
        }

        // The constraint should be within 0.5 pixels of the specified size
        // Due to rounding, we can't expect to have the Spacer take exactly the requested size which
        // is true in normal Compose code as well
        assertEquals(
            with(actualDensity) { 3000.dp.toPx() },
            actualConstraints!!.maxWidth.toFloat(),
            0.5f,
        )
        assertEquals(
            with(actualDensity) { 4000.dp.toPx() },
            actualConstraints!!.maxHeight.toFloat(),
            0.5f,
        )
    }

    @Test
    fun sizeOverride_allowsForCorrectSpace_largeLandscapeAspectRatio() {
        lateinit var actualDensity: Density
        var actualConstraints: Constraints? = null

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(4000.dp, 3000.dp))
            ) {
                Spacer(
                    modifier =
                        Modifier.layout { measurable, constraints ->
                            actualConstraints = constraints
                            actualDensity = this

                            val placeable = measurable.measure(constraints)

                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                )
            }
        }

        // The constraint should be within 0.5 pixels of the specified size
        // Due to rounding, we can't expect to have the Spacer take exactly the requested size which
        // is true in normal Compose code as well
        assertEquals(
            with(actualDensity) { 4000.dp.toPx() },
            actualConstraints!!.maxWidth.toFloat(),
            0.5f,
        )
        assertEquals(
            with(actualDensity) { 3000.dp.toPx() },
            actualConstraints!!.maxHeight.toFloat(),
            0.5f,
        )
    }

    @Test
    fun sizeOverride_largeRequestedSize_overridesConfigurationDensity() {
        lateinit var originalDensity: Density
        lateinit var overriddenDensity: Density
        lateinit var overriddenConfiguration: Configuration

        rule.setContent {
            originalDensity = LocalDensity.current
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(3000.dp, 4000.dp))
            ) {
                overriddenDensity = LocalDensity.current
                overriddenConfiguration = LocalConfiguration.current
            }
        }

        // A 3000dp by 4000dp device is so big, that we can assume that the density needs to be
        // overridden.
        // If this test runs on a device with that size screen, where overriding density is not
        // necessary, this test might fail. If that is happening, hopefully the future is a nice
        // place.
        assertTrue(originalDensity.density > overriddenDensity.density)

        // Convert the Configuration's density in DPI to the raw float multiplier
        val overriddenConfigurationDensityMultiplier =
            overriddenConfiguration.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

        assertEquals(
            overriddenDensity.density,
            overriddenConfigurationDensityMultiplier,
            // Compare within half a step of density DPI changes
            1f / DisplayMetrics.DENSITY_DEFAULT / 2f,
        )
    }

    @Test
    fun sizeOverride_notNeededForPortrait_doesNotOverrideConfigurationDensity() {
        lateinit var originalDensity: Density
        lateinit var overriddenDensity: Density
        lateinit var overriddenConfiguration: Configuration

        rule.setContent {
            originalDensity = LocalDensity.current
            Box(Modifier.size(35.dp, 45.dp)) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(30.dp, 40.dp))
                ) {
                    overriddenDensity = LocalDensity.current
                    overriddenConfiguration = LocalConfiguration.current
                }
            }
        }

        // This is a strict equality for floating point values which is normally problematic, but
        // these should be precisely equal
        assertEquals(originalDensity.density, overriddenDensity.density)

        // Convert the Configuration's density in DPI to the raw float multiplier
        val overriddenConfigurationDensityMultiplier =
            overriddenConfiguration.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

        // This is a strict equality for floating point values which is normally problematic, but
        // these should be precisely equal
        assertEquals(
            overriddenDensity.density,
            overriddenConfigurationDensityMultiplier,
            // Compare within half a step of density DPI changes
            1f / DisplayMetrics.DENSITY_DEFAULT / 2f,
        )
    }

    @Test
    fun sizeOverride_notNeededForLandscape_doesNotOverrideConfigurationDensity() {
        lateinit var originalDensity: Density
        lateinit var overriddenDensity: Density
        lateinit var overriddenConfiguration: Configuration

        rule.setContent {
            originalDensity = LocalDensity.current
            Box(Modifier.size(45.dp, 35.dp)) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(40.dp, 30.dp))
                ) {
                    overriddenDensity = LocalDensity.current
                    overriddenConfiguration = LocalConfiguration.current
                }
            }
        }

        // This is a strict equality for floating point values which is normally problematic, but
        // these should be precisely equal
        assertEquals(originalDensity.density, overriddenDensity.density)

        // Convert the Configuration's density in DPI to the raw float multiplier
        val overriddenConfigurationDensityMultiplier =
            overriddenConfiguration.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

        // This is a strict equality for floating point values which is normally problematic, but
        // these should be precisely equal
        assertEquals(
            overriddenDensity.density,
            overriddenConfigurationDensityMultiplier,
            // Compare within half a step of density DPI changes
            1f / DisplayMetrics.DENSITY_DEFAULT / 2f,
        )
    }

    @Test
    fun layoutDirectionOverride_toRtl_overridesLayoutDirection() {
        lateinit var layoutDirection: LayoutDirection
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
                layoutDirection = LocalLayoutDirection.current
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(LayoutDirection.Rtl, layoutDirection)
        assertEquals(View.LAYOUT_DIRECTION_RTL, configuration.layoutDirection)
    }

    @Test
    fun layoutDirectionOverride_toLtr_overridesLayoutDirection() {
        lateinit var layoutDirection: LayoutDirection
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Ltr)
            ) {
                layoutDirection = LocalLayoutDirection.current
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(LayoutDirection.Ltr, layoutDirection)
        assertEquals(View.LAYOUT_DIRECTION_LTR, configuration.layoutDirection)
    }

    @Test
    fun fontScaleOverride_overridesFontScale() {
        lateinit var density: Density
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                density = LocalDensity.current
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(1.5f, density.fontScale)
        assertEquals(1.5f, configuration.fontScale)
    }

    @Test
    fun localesOverride_overridesLocales() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Locales(LocaleList(Locale("es-ES")))
            ) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(
            LocaleListCompat.forLanguageTags("es-ES"),
            ConfigurationCompat.getLocales(configuration),
        )
    }

    @Test
    fun localesOverride_overridesLayoutDirection() {
        lateinit var layoutDirection: LayoutDirection
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Locales(LocaleList(Locale("ar")))
            ) {
                layoutDirection = LocalLayoutDirection.current
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(
            LocaleListCompat.forLanguageTags("ar"),
            ConfigurationCompat.getLocales(configuration),
        )
        assertEquals(LayoutDirection.Rtl, layoutDirection)
        assertEquals(View.LAYOUT_DIRECTION_RTL, configuration.layoutDirection)
    }

    @Test
    fun darkModeOverride_toDark_overridesIsSystemInDarkMode() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.DarkMode(true)) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(
            Configuration.UI_MODE_NIGHT_YES,
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
        )
    }

    @Test
    fun darkModeOverride_toLight_overridesIsSystemInDarkMode() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.DarkMode(false)) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(
            Configuration.UI_MODE_NIGHT_NO,
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
        )
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun fontWeightAdjustmentOverride_overridesFontWeightAdjustment() {
        lateinit var configuration: Configuration
        lateinit var typefaceNormal: android.graphics.Typeface
        lateinit var typefaceBold: android.graphics.Typeface

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontWeightAdjustment(500)) {
                typefaceNormal =
                    LocalFontFamilyResolver.current
                        .resolveAsTypeface(FontFamily.SansSerif, FontWeight.Normal)
                        .value
                typefaceBold =
                    LocalFontFamilyResolver.current
                        .resolveAsTypeface(FontFamily.SansSerif, FontWeight.Bold)
                        .value
                configuration = LocalConfiguration.current
            }
        }

        // (Normal + 500).coerceIn(0, 1000) = (400 + 500).coerceIn(0, 1000) = 900
        assertEquals(900, typefaceNormal.weight)
        // (Bold + 500).coerceIn(0, 1000) = (700 + 500).coerceIn(0, 1000) = 1000
        assertEquals(1000, typefaceBold.weight)
        assertEquals(500, configuration.fontWeightAdjustment)
    }

    @Test
    fun roundScreenOverride_isRound_overridesIsScreenRound() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                configuration = LocalConfiguration.current
            }
        }

        assertTrue(configuration.isScreenRound)
    }

    @Test
    fun roundScreenOverride_isNotRound_overridesIsScreenRound() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(false)) {
                configuration = LocalConfiguration.current
            }
        }

        assertFalse(configuration.isScreenRound)
    }

    @Test
    fun keyboardOverride_qwerty_overridesKeyboardConfigValue() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Keyboard(Configuration.KEYBOARD_QWERTY)
            ) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(configuration.keyboard, Configuration.KEYBOARD_QWERTY)
        assertEquals(configuration.keyboardHidden, Configuration.KEYBOARDHIDDEN_NO)
        assertEquals(configuration.hardKeyboardHidden, Configuration.HARDKEYBOARDHIDDEN_NO)
    }

    @Test
    fun keyboardOverride_hardKeyboardHidden_overridesKeyboardConfigValue() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Keyboard(
                    keyboardType = Configuration.KEYBOARD_QWERTY,
                    isHardKeyboardHidden = true,
                )
            ) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(configuration.keyboard, Configuration.KEYBOARD_QWERTY)
        assertEquals(configuration.keyboardHidden, Configuration.KEYBOARDHIDDEN_NO)
        assertEquals(configuration.hardKeyboardHidden, Configuration.HARDKEYBOARDHIDDEN_YES)
    }

    @Test
    fun keyboardOverride_hidden_overridesKeyboardConfigValue() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Keyboard(
                    keyboardType = Configuration.KEYBOARD_QWERTY,
                    isHardKeyboardHidden = true,
                    isHidden = true,
                )
            ) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(configuration.keyboard, Configuration.KEYBOARD_QWERTY)
        assertEquals(configuration.keyboardHidden, Configuration.KEYBOARDHIDDEN_YES)
        assertEquals(configuration.hardKeyboardHidden, Configuration.HARDKEYBOARDHIDDEN_YES)
    }

    @Test
    fun navigationOverride_dpad_overridesNavigationConfigValue() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Navigation(Configuration.NAVIGATION_DPAD)
            ) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(configuration.navigation, Configuration.NAVIGATION_DPAD)
        assertEquals(configuration.navigationHidden, Configuration.NAVIGATIONHIDDEN_NO)
    }

    @Test
    fun navigationOverride_hidden_overridesNavigationConfigValue() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.Navigation(Configuration.NAVIGATION_DPAD, true)
            ) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(configuration.navigation, Configuration.NAVIGATION_DPAD)
        assertEquals(configuration.navigationHidden, Configuration.NAVIGATIONHIDDEN_YES)
    }

    @Test
    fun touchscreen_false_overridesTouchscreenConfigValue() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.Touchscreen(false)) {
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(configuration.touchscreen, Configuration.TOUCHSCREEN_NOTOUCH)
    }

    @Test
    fun uiModeOverride_car_overridesUiModeConfigValue() {
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.UiMode(Configuration.UI_MODE_TYPE_CAR)
            ) {
                configuration = LocalConfiguration.current
            }
        }

        val uiMode = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        assertEquals(uiMode, Configuration.UI_MODE_TYPE_CAR)
    }

    @Test
    fun combiningDeviceConfigurationOverride_respectsOrder() {
        lateinit var layoutDirection: LayoutDirection
        lateinit var configuration: Configuration

        rule.setContent {
            DeviceConfigurationOverride(
                // Apply Arabic first, which will override to RTL
                DeviceConfigurationOverride.Locales(LocaleList(Locale("ar"))) then
                    // Then override back to LTR
                    DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Ltr)
            ) {
                layoutDirection = LocalLayoutDirection.current
                configuration = LocalConfiguration.current
            }
        }

        assertEquals(
            LocaleListCompat.forLanguageTags("ar"),
            ConfigurationCompat.getLocales(configuration),
        )
        assertEquals(LayoutDirection.Ltr, layoutDirection)
        assertEquals(View.LAYOUT_DIRECTION_LTR, configuration.layoutDirection)
    }
}
