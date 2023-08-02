/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LightMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(lightColorScheme()) {
        content()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DarkMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(darkColorScheme()) {
        content()
    }
}

/**
 * Wraps Compose content in a [MaterialTheme].
 *
 * @param colorScheme a [ColorScheme] to provide to the theme. Usually a [lightColorScheme],
 * [darkColorScheme], or a dynamic one
 */
@OptIn(ExperimentalTvMaterial3Api::class)
fun ComposeContentTestRule.setMaterialContent(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    setContent {
        MaterialTheme(colorScheme = colorScheme) {
            content()
        }
    }
}
