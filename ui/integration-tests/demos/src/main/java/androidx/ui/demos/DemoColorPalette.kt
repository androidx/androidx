/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.demos

import android.content.SharedPreferences
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.ui.foundation.isSystemInDarkTheme
import androidx.ui.material.ColorPalette
import androidx.ui.material.darkColorPalette
import androidx.ui.material.lightColorPalette

/**
 * Wrapper class that contains a light and dark [ColorPalette], to allow saving and
 * restoring the entire light / dark theme to and from [SharedPreferences].
 */
@Stable
class DemoColorPalette {
    var lightColors: ColorPalette by mutableStateOf(lightColorPalette())
    var darkColors: ColorPalette by mutableStateOf(darkColorPalette())

    @Composable
    val colors
        get() = if (isSystemInDarkTheme()) darkColors else lightColors
}
