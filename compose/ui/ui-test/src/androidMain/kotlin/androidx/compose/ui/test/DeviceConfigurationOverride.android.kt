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

package androidx.compose.ui.test

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import kotlin.math.floor

actual fun DeviceConfigurationOverride.Companion.ForcedSize(
    size: DpSize
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    // First override the density. Doing this first allows using the resulting density in the
    // overridden configuration.
    DensityForcedSize(size) {
        // Second, override the configuration, with the current configuration modified by the
        // resulting density
        OverriddenConfiguration(
            configuration =
                Configuration().apply {
                    // Initialize from the current configuration
                    updateFrom(LocalConfiguration.current)

                    // Override densityDpi
                    densityDpi =
                        floor(LocalDensity.current.density * DisplayMetrics.DENSITY_DEFAULT).toInt()
                },
            content = contentUnderTest
        )
    }
}

actual fun DeviceConfigurationOverride.Companion.FontScale(
    fontScale: Float
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override font scale
                this.fontScale = fontScale
            },
        content = contentUnderTest
    )
}

actual fun DeviceConfigurationOverride.Companion.LayoutDirection(
    layoutDirection: LayoutDirection
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override screen layout for layout direction
                screenLayout =
                    screenLayout and
                        Configuration.SCREENLAYOUT_LAYOUTDIR_MASK.inv() or
                        when (layoutDirection) {
                            LayoutDirection.Ltr -> Configuration.SCREENLAYOUT_LAYOUTDIR_LTR
                            LayoutDirection.Rtl -> Configuration.SCREENLAYOUT_LAYOUTDIR_RTL
                        }
            },
        content = contentUnderTest
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the locales for the contained content.
 *
 * This will change resource resolution for the content under test, and also override the layout
 * direction as specified by the locales.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideLocalesSample
 */
fun DeviceConfigurationOverride.Companion.Locales(
    locales: LocaleList,
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Update the locale list
                ConfigurationCompat.setLocales(
                    this,
                    LocaleListCompat.forLanguageTags(
                        locales.localeList.fastJoinToString(",", transform = Locale::toLanguageTag)
                    )
                )
            },
        content = contentUnderTest
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the dark mode or light mode theme for the
 * contained content. Inside the content under test, `isSystemInDarkTheme()` will return
 * [isDarkMode].
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideDarkModeSample
 */
fun DeviceConfigurationOverride.Companion.DarkMode(
    isDarkMode: Boolean,
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override dark mode
                uiMode =
                    uiMode and
                        Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (isDarkMode) {
                            Configuration.UI_MODE_NIGHT_YES
                        } else {
                            Configuration.UI_MODE_NIGHT_NO
                        }
            },
        content = contentUnderTest
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the font weight adjustment for the contained
 * content.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideFontWeightAdjustmentSample
 */
@RequiresApi(31)
fun DeviceConfigurationOverride.Companion.FontWeightAdjustment(
    fontWeightAdjustment: Int,
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override fontWeightAdjustment
                this.fontWeightAdjustment = fontWeightAdjustment
            },
        content = contentUnderTest
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides whether the screen is round for the contained
 * content.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideRoundScreenSample
 */
@RequiresApi(23)
fun DeviceConfigurationOverride.Companion.RoundScreen(
    isScreenRound: Boolean,
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override isRound in screenLayout
                screenLayout =
                    when (isScreenRound) {
                        true ->
                            (screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK.inv()) or
                                Configuration.SCREENLAYOUT_ROUND_YES
                        false ->
                            (screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK.inv()) or
                                Configuration.SCREENLAYOUT_ROUND_NO
                    }
            },
        content = contentUnderTest
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the window insets for the contained content.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideWindowInsetsSample
 */
fun DeviceConfigurationOverride.Companion.WindowInsets(
    windowInsets: WindowInsetsCompat,
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    val currentContentUnderTest by rememberUpdatedState(contentUnderTest)
    val currentWindowInsets by rememberUpdatedState(windowInsets)
    AndroidView(
        factory = { context ->
            object : AbstractComposeView(context) {
                @Composable
                override fun Content() {
                    currentContentUnderTest()
                }

                override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                    children.forEach {
                        it.dispatchApplyWindowInsets(
                            WindowInsets(currentWindowInsets.toWindowInsets())
                        )
                    }
                    return WindowInsetsCompat.CONSUMED.toWindowInsets()!!
                }

                /**
                 * Deprecated, but intercept the `requestApplyInsets` call via the deprecated
                 * method.
                 */
                @Deprecated("Deprecated in Java")
                override fun requestFitSystemWindows() {
                    dispatchApplyWindowInsets(WindowInsets(currentWindowInsets.toWindowInsets()!!))
                }
            }
        },
        update = { with(currentWindowInsets) { it.requestApplyInsets() } }
    )
}

/**
 * Overrides the compositions locals related to the given [configuration].
 *
 * There currently isn't a single source of truth for these values, so we update them all according
 * to the given [configuration].
 */
@Composable
private fun OverriddenConfiguration(configuration: Configuration, content: @Composable () -> Unit) {
    // We don't override the theme, but we do want to override the configuration and this seems
    // convenient to do so
    val newContext =
        ContextThemeWrapper(LocalContext.current, 0).apply {
            applyOverrideConfiguration(configuration)
        }

    CompositionLocalProvider(
        LocalContext provides newContext,
        LocalConfiguration provides configuration,
        LocalLayoutDirection provides
            if (configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                LayoutDirection.Ltr
            } else {
                LayoutDirection.Rtl
            },
        LocalDensity provides
            Density(
                configuration.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT,
                configuration.fontScale
            ),
        LocalFontFamilyResolver provides createFontFamilyResolver(newContext),
        content = content
    )
}
