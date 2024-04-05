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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretProperties [CaretProperties] for the caret of the tooltip, if a default
 * caret is desired with a specific dimension. Please see [TooltipDefaults.caretProperties] to
 * see the default dimensions. Pass in null for this parameter if no caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@ExperimentalMaterial3Api
@Composable
actual fun CaretScope.PlainTooltip(
    modifier: Modifier,
    caretProperties: CaretProperties?,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
) = PlainTooltipImpl(
    modifier,
    caretProperties,
    shape, contentColor,
    containerColor,
    tonalElevation,
    shadowElevation,
    content
)

@Composable
internal actual fun getCurrentConfiguration() = Configuration(LocalWindowInfo.current)

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Configuration.getScreenWidthPx(density: Density): Int {
    val windowInfo = delegate as WindowInfo
    return windowInfo.containerSize.width
}
