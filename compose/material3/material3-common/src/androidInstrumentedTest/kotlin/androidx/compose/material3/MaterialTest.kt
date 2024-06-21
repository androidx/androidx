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

package androidx.compose.material3.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wraps Compose content in a [MaterialTheme] and a [Surface].
 *
 * @param colorScheme a [ColorScheme] to provide to the theme. Usually a [lightColorScheme],
 *   [darkColorScheme], or a dynamic one
 * @param modifier a [Modifier] to be applied at the [Surface] wrapper
 */
fun ComposeContentTestRule.setMaterialContent(
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit
) {
    setContent {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = modifier) {
                CompositionLocalProvider(LocalWindowInfo provides WindowInfoFocused, composable)
            }
        }
    }
}

private val WindowInfoFocused =
    object : WindowInfo {
        override val isWindowFocused = true
    }

/** Constant to emulate very big but finite constraints */
val BigTestMaxWidth = 5000.dp
val BigTestMaxHeight = 5000.dp

fun ComposeContentTestRule.setMaterialContentForSizeAssertions(
    parentMaxWidth: Dp = BigTestMaxWidth,
    parentMaxHeight: Dp = BigTestMaxHeight,
    // TODO : figure out better way to make it flexible
    content: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        MaterialTheme {
            Surface {
                Box {
                    Box(
                        Modifier.sizeIn(maxWidth = parentMaxWidth, maxHeight = parentMaxHeight)
                            .testTag("containerForSizeAssertion")
                    ) {
                        content()
                    }
                }
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion")
}
