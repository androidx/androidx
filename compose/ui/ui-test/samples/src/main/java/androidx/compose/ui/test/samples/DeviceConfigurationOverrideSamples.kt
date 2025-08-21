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

package androidx.compose.ui.test.samples

import android.content.res.Configuration
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.then
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.core.view.WindowInsetsCompat

@Sampled
@Composable
fun DeviceConfigurationOverrideThenSample() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.FontScale(1.5f) then
            DeviceConfigurationOverride.FontWeightAdjustment(200)
    ) {
        Text(text = "text with increased scale and weight")
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideFontScaleSample() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
        MyScreen() // will be rendered with a larger than default font scale
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideForcedSizeSample() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
        MyScreen() // will be rendered in the space for 1280dp by 800dp without clipping
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideLayoutDirectionSample() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)) {
        MyComponent() // will be rendered with a right-to-left layout direction
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideLocalesSample() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList("es-ES"))) {
        MyScreen() // will be rendered with overridden locale
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideDarkModeSample() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.DarkMode(true)) {
        isSystemInDarkTheme() // will be true
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideFontWeightAdjustmentSample() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.FontWeightAdjustment(200)) {
        MyComponent() // will be rendered with adjusted font weight
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideRoundScreenSample() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
        LocalConfiguration.current.isScreenRound // will be true
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideKeyboard() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.Keyboard(Configuration.KEYBOARD_QWERTY)
    ) {
        LocalConfiguration.current.keyboard // will be Configuration.KEYBOARD_QWERTY
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideNavigation() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.Navigation(
            navigationType = Configuration.NAVIGATION_DPAD,
            isHidden = false,
        )
    ) {
        LocalConfiguration.current.navigation // will be Configuration.NAVIGATION_DPAD
        LocalConfiguration.current.navigationHidden // will be Configuration.NAVIGATIONHIDDEN_NO
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideTouchscreen() {
    DeviceConfigurationOverride(DeviceConfigurationOverride.Touchscreen(false)) {
        LocalConfiguration.current.touchscreen // will be Configuration.TOUCHSCREEN_NOTOUCH
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideUiMode() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.UiMode(Configuration.UI_MODE_TYPE_CAR)
    ) {
        // will be Configuration.UI_MODE_TYPE_CAR
        LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideWindowInsetsSample() {
    fun IntRect.toAndroidXInsets() = androidx.core.graphics.Insets.of(left, top, right, bottom)

    DeviceConfigurationOverride(
        DeviceConfigurationOverride.WindowInsets(
            WindowInsetsCompat.Builder()
                .setInsets(
                    WindowInsetsCompat.Type.captionBar(),
                    with(LocalDensity.current) { DpRect(0.dp, 64.dp, 0.dp, 0.dp).toRect() }
                        .roundToIntRect()
                        .toAndroidXInsets(),
                )
                .setInsets(
                    WindowInsetsCompat.Type.navigationBars(),
                    with(LocalDensity.current) { DpRect(24.dp, 0.dp, 48.dp, 24.dp).toRect() }
                        .roundToIntRect()
                        .toAndroidXInsets(),
                )
                .build()
        )
    ) {
        Box(
            Modifier.background(Color.Blue)
                // Will apply 64dp padding on the top, 24dp padding on the sides, and 48dp on the
                // bottom
                .safeDrawingPadding()
                .background(Color.Red)
        )
    }
}

@Composable private fun MyScreen() = Unit

@Composable private fun MyComponent() = Unit
