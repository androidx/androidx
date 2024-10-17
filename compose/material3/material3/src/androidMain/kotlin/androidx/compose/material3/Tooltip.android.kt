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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.tokens.PlainTooltipTokens
import androidx.compose.material3.tokens.RichTooltipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

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
@Suppress("DEPRECATION")
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Maintained for binary compatibility. " + "Use overload with maxWidth parameter."
)
@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.PlainTooltip(
    modifier: Modifier,
    caretSize: DpSize,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
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
        content = content
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
@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.PlainTooltip(
    modifier: Modifier,
    caretSize: DpSize,
    maxWidth: Dp,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
) {
    val drawCaretModifier =
        if (caretSize.isSpecified) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            Modifier.drawCaret { anchorLayoutCoordinates ->
                    drawCaretWithPath(
                        density,
                        configuration,
                        containerColor,
                        caretSize,
                        anchorLayoutCoordinates
                    )
                }
                .then(modifier)
        } else modifier
    Surface(
        modifier = drawCaretModifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box(
            modifier =
                Modifier.sizeIn(
                        minWidth = TooltipMinWidth,
                        maxWidth = maxWidth,
                        minHeight = TooltipMinHeight
                    )
                    .padding(PlainTooltipContentPadding)
        ) {
            val textStyle = PlainTooltipTokens.SupportingTextFont.value

            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content
            )
        }
    }
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
@Suppress("DEPRECATION")
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Maintained for binary compatibility. " + "Use overload with maxWidth parameter."
)
@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.RichTooltip(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    action: (@Composable () -> Unit)?,
    caretSize: DpSize,
    shape: Shape,
    colors: RichTooltipColors,
    tonalElevation: Dp,
    shadowElevation: Dp,
    text: @Composable () -> Unit
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
        text = text
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
@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.RichTooltip(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    action: (@Composable () -> Unit)?,
    caretSize: DpSize,
    maxWidth: Dp,
    shape: Shape,
    colors: RichTooltipColors,
    tonalElevation: Dp,
    shadowElevation: Dp,
    text: @Composable () -> Unit
) {
    val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation
    val elevatedColor =
        MaterialTheme.colorScheme.applyTonalElevation(colors.containerColor, absoluteElevation)
    val drawCaretModifier =
        if (caretSize.isSpecified) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            Modifier.drawCaret { anchorLayoutCoordinates ->
                    drawCaretWithPath(
                        density,
                        configuration,
                        elevatedColor,
                        caretSize,
                        anchorLayoutCoordinates
                    )
                }
                .then(modifier)
        } else modifier
    Surface(
        modifier =
            drawCaretModifier.sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = maxWidth,
                minHeight = TooltipMinHeight
            ),
        shape = shape,
        color = colors.containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        val actionLabelTextStyle = RichTooltipTokens.ActionLabelTextFont.value
        val subheadTextStyle = RichTooltipTokens.SubheadFont.value
        val supportingTextStyle = RichTooltipTokens.SupportingTextFont.value

        Column(modifier = Modifier.padding(horizontal = RichTooltipHorizontalPadding)) {
            title?.let {
                Box(modifier = Modifier.paddingFromBaseline(top = HeightToSubheadFirstLine)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.titleContentColor,
                        LocalTextStyle provides subheadTextStyle,
                        content = it
                    )
                }
            }
            Box(modifier = Modifier.textVerticalPadding(title != null, action != null)) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor,
                    LocalTextStyle provides supportingTextStyle,
                    content = text
                )
            }
            action?.let {
                Box(
                    modifier =
                        Modifier.requiredHeightIn(min = ActionLabelMinHeight)
                            .padding(bottom = ActionLabelBottomPadding)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.actionContentColor,
                        LocalTextStyle provides actionLabelTextStyle,
                        content = it
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
private fun CacheDrawScope.drawCaretWithPath(
    density: Density,
    configuration: Configuration,
    containerColor: Color,
    caretSize: DpSize,
    anchorLayoutCoordinates: LayoutCoordinates?
): DrawResult {
    val path = Path()

    if (anchorLayoutCoordinates != null) {
        val caretHeightPx: Int
        val caretWidthPx: Int
        val screenWidthPx: Int
        val tooltipAnchorSpacing: Int
        with(density) {
            caretHeightPx = caretSize.height.roundToPx()
            caretWidthPx = caretSize.width.roundToPx()
            screenWidthPx = configuration.screenWidthDp.dp.roundToPx()
            tooltipAnchorSpacing = SpacingBetweenTooltipAndAnchor.roundToPx()
        }
        val anchorBounds = anchorLayoutCoordinates.boundsInWindow()
        val anchorLeft = anchorBounds.left
        val anchorRight = anchorBounds.right
        val anchorTop = anchorBounds.top
        val anchorMid = (anchorRight + anchorLeft) / 2
        val anchorWidth = anchorRight - anchorLeft
        val tooltipWidth = this.size.width
        val tooltipHeight = this.size.height
        val isCaretTop = anchorTop - tooltipHeight - tooltipAnchorSpacing < 0
        val caretY =
            if (isCaretTop) {
                0f
            } else {
                tooltipHeight
            }

        // Default the caret to be in the middle
        // caret might need to be offset depending on where
        // the tooltip is placed relative to the anchor
        var position: Offset =
            if (anchorLeft - tooltipWidth / 2 + anchorWidth / 2 <= 0) {
                Offset(anchorMid, caretY)
            } else if (anchorRight + tooltipWidth / 2 - anchorWidth / 2 >= screenWidthPx) {
                val anchorMidFromRightScreenEdge = screenWidthPx - anchorMid
                val caretX = tooltipWidth - anchorMidFromRightScreenEdge
                Offset(caretX, caretY)
            } else {
                Offset(tooltipWidth / 2, caretY)
            }
        if (anchorMid - tooltipWidth / 2 < 0) {
            // The tooltip needs to be start aligned if it would collide with the left side of
            // screen.
            position = Offset(anchorMid - anchorLeft, caretY)
        } else if (anchorMid + tooltipWidth / 2 > screenWidthPx) {
            // The tooltip needs to be end aligned if it would collide with the right side of the
            // screen.
            position = Offset(anchorMid - (anchorRight - tooltipWidth), caretY)
        }

        if (isCaretTop) {
            path.apply {
                moveTo(x = position.x, y = position.y)
                lineTo(x = position.x + caretWidthPx / 2, y = position.y)
                lineTo(x = position.x, y = position.y - caretHeightPx)
                lineTo(x = position.x - caretWidthPx / 2, y = position.y)
                close()
            }
        } else {
            path.apply {
                moveTo(x = position.x, y = position.y)
                lineTo(x = position.x + caretWidthPx / 2, y = position.y)
                lineTo(x = position.x, y = position.y + caretHeightPx.toFloat())
                lineTo(x = position.x - caretWidthPx / 2, y = position.y)
                close()
            }
        }
    }

    return onDrawWithContent {
        if (anchorLayoutCoordinates != null) {
            drawContent()
            drawPath(path = path, color = containerColor)
        }
    }
}
