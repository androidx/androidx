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

package androidx.compose.material3.windowsizeclass

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.toSize

/**
 * Calculates the window's [WindowSizeClass].
 *
 * A new [WindowSizeClass] will be returned whenever a configuration change causes the width or
 * height of the window to cross a breakpoint, such as when the device is rotated or the window is
 * resized.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
@ExperimentalMaterial3WindowSizeClassApi
fun calculateWindowSizeClass(): WindowSizeClass {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val size = with(density) { windowInfo.containerSize.toSize().toDpSize() }
    return WindowSizeClass.calculateFromSize(size)
}