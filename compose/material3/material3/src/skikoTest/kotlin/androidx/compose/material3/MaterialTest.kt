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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi

/**
 * Wraps Compose content in a [MaterialTheme] and a [Surface].
 *
 * @param colorScheme a [ColorScheme] to provide to the theme. Usually a [lightColorScheme],
 *   [darkColorScheme], or a dynamic one
 * @param modifier a [Modifier] to be applied at the [Surface] wrapper
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setMaterialContent(
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit,
) {
    setContent {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = modifier) {
                val windowInfo = LocalWindowInfo.current
                CompositionLocalProvider(
                    LocalWindowInfo provides FocusedWindowInfo(windowInfo),
                    composable,
                )
            }
        }
    }
}

private class FocusedWindowInfo(private val delegate: WindowInfo) : WindowInfo {
    override val isWindowFocused = true
    override val keyboardModifiers
        get() = delegate.keyboardModifiers

    override val containerSize
        get() = delegate.containerSize

    override val containerDpSize
        get() = delegate.containerDpSize
}
