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

package androidx.compose.material3

import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.RichTooltipTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
internal actual fun windowContainerWidthInPx(): Int {
    return with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
}

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Maintained for binary compatibility. " + "Use overload with maxWidth parameter.",
)
@Composable
@ExperimentalMaterial3Api
@JvmName("PlainTooltip")
fun TooltipScope.PlainTooltipAndroid(
    modifier: Modifier = Modifier,
    caretSize: DpSize = DpSize.Unspecified,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) =
    PlainTooltip(
        modifier = modifier,
        caretSize = caretSize,
        maxWidth = TooltipDefaults.plainTooltipMaxWidth,
        shape = shape,
        contentColor = contentColor,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content,
    )

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param maxWidth the maximum width for the plain tooltip
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@Deprecated(level = DeprecationLevel.HIDDEN, message = "Maintained for binary compatibility.")
@Composable
@ExperimentalMaterial3Api
@JvmName("PlainTooltip")
fun TooltipScope.PlainTooltipAndroid(
    modifier: Modifier = Modifier,
    caretSize: DpSize = DpSize.Unspecified,
    maxWidth: Dp = TooltipDefaults.plainTooltipMaxWidth,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    PlainTooltip(
        modifier = modifier,
        caretSize = caretSize,
        maxWidth = maxWidth,
        shape = shape,
        contentColor = contentColor,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content,
    )
}

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action. Tooltips are used to
 * provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Maintained for binary compatibility. " + "Use overload with maxWidth parameter.",
)
@Composable
@ExperimentalMaterial3Api
@JvmName("RichTooltip")
fun TooltipScope.RichTooltipAndroid(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretSize: DpSize = DpSize.Unspecified,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = ElevationTokens.Level0,
    shadowElevation: Dp = RichTooltipTokens.ContainerElevation,
    text: @Composable () -> Unit,
) =
    RichTooltip(
        modifier = modifier,
        title = title,
        action = action,
        caretSize = caretSize,
        maxWidth = TooltipDefaults.richTooltipMaxWidth,
        shape = shape,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        text = text,
    )

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action. Tooltips are used to
 * provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param maxWidth the maximum width for the rich tooltip
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Deprecated(level = DeprecationLevel.HIDDEN, message = "Maintained for binary compatibility.")
@Composable
@ExperimentalMaterial3Api
@JvmName("RichTooltip")
fun TooltipScope.RichTooltipAndroid(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretSize: DpSize = DpSize.Unspecified,
    maxWidth: Dp = TooltipDefaults.richTooltipMaxWidth,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = ElevationTokens.Level0,
    shadowElevation: Dp = RichTooltipTokens.ContainerElevation,
    text: @Composable () -> Unit,
) {
    RichTooltip(
        modifier = modifier,
        title = title,
        action = action,
        caretSize = caretSize,
        maxWidth = maxWidth,
        shape = shape,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        text = text,
    )
}
