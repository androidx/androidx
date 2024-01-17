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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection

actual fun DeviceConfigurationOverride.Companion.ForcedSize(
    size: DpSize
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    DensityForcedSize(
        size = size,
        content = contentUnderTest
    )
}

actual fun DeviceConfigurationOverride.Companion.FontScale(
    fontScale: Float
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density, fontScale),
        content = contentUnderTest,
    )
}

actual fun DeviceConfigurationOverride.Companion.LayoutDirection(
    layoutDirection: LayoutDirection
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        content = contentUnderTest,
    )
}
