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

import androidx.annotation.Sampled
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.FontWeightAdjustment
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.RoundScreen
import androidx.compose.ui.test.then
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.FontScale(1.5f)
    ) {
        MyScreen() // will be rendered with a larger than default font scale
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideForcedSizeSample() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))
    ) {
        MyScreen() // will be rendered in the space for 1280dp by 800dp without clipping
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideLayoutDirectionSample() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
    ) {
        MyComponent() // will be rendered with a right-to-left layout direction
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideLocalesSample() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.Locales(LocaleList("es-ES"))
    ) {
        MyScreen() // will be rendered with overridden locale
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideDarkModeSample() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.DarkMode(true)
    ) {
        isSystemInDarkTheme() // will be true
    }
}

@Sampled
@Composable
fun DeviceConfigurationOverrideFontWeightAdjustmentSample() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.FontWeightAdjustment(200)
    ) {
        MyComponent() // will be rendered with adjusted font weight
    }
}

@Suppress("ClassVerificationFailure") // Only used in sample
@Sampled
@Composable
fun DeviceConfigurationOverrideRoundScreenSample() {
    DeviceConfigurationOverride(
        DeviceConfigurationOverride.RoundScreen(true)
    ) {
        LocalConfiguration.current.isScreenRound // will be true
    }
}

@Composable
private fun MyScreen() = Unit

@Composable
private fun MyComponent() = Unit
