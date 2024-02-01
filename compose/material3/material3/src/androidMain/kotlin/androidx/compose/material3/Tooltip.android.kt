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
import androidx.compose.ui.unit.dp

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
@Composable
@ExperimentalMaterial3Api
actual fun CaretScope.PlainTooltip(
    modifier: Modifier,
    caretProperties: CaretProperties?,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
) {
    val drawCaretModifier =
        if (caretProperties != null) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            Modifier.drawCaret { anchorLayoutCoordinates ->
                drawCaretWithPath(
                    CaretType.Plain,
                    density,
                    configuration,
                    containerColor,
                    caretProperties,
                    anchorLayoutCoordinates
                )
            }.then(modifier)
        } else modifier
    Surface(
        modifier = drawCaretModifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box(modifier = Modifier
            .sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = PlainTooltipMaxWidth,
                minHeight = TooltipMinHeight
            )
            .padding(PlainTooltipContentPadding)
        ) {
            val textStyle =
                MaterialTheme.typography.fromToken(PlainTooltipTokens.SupportingTextFont)

            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content
            )
        }
    }
}

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action.
 * Tooltips are used to provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param caretProperties [CaretProperties] for the caret of the tooltip, if a default
 * caret is desired with a specific dimension. Pass in null for this parameter if no
 * caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Composable
@ExperimentalMaterial3Api
actual fun CaretScope.RichTooltip(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    action: (@Composable () -> Unit)?,
    caretProperties: CaretProperties?,
    shape: Shape,
    colors: RichTooltipColors,
    tonalElevation: Dp,
    shadowElevation: Dp,
    text: @Composable () -> Unit
) {
    val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation
    val elevatedColor =
        MaterialTheme.colorScheme.applyTonalElevation(
            colors.containerColor,
            absoluteElevation
        )
    val drawCaretModifier =
        if (caretProperties != null) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            Modifier.drawCaret { anchorLayoutCoordinates ->
                drawCaretWithPath(
                    CaretType.Rich,
                    density,
                    configuration,
                    elevatedColor,
                    caretProperties,
                    anchorLayoutCoordinates
                )
            }.then(modifier)
        } else modifier
    Surface(
        modifier = drawCaretModifier
            .sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = RichTooltipMaxWidth,
                minHeight = TooltipMinHeight
            ),
        shape = shape,
        color = colors.containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        val actionLabelTextStyle =
            MaterialTheme.typography.fromToken(RichTooltipTokens.ActionLabelTextFont)
        val subheadTextStyle =
            MaterialTheme.typography.fromToken(RichTooltipTokens.SubheadFont)
        val supportingTextStyle =
            MaterialTheme.typography.fromToken(RichTooltipTokens.SupportingTextFont)

        Column(
            modifier = Modifier.padding(horizontal = RichTooltipHorizontalPadding)
        ) {
            title?.let {
                Box(
                    modifier = Modifier.paddingFromBaseline(top = HeightToSubheadFirstLine)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.titleContentColor,
                        LocalTextStyle provides subheadTextStyle,
                        content = it
                    )
                }
            }
            Box(
                modifier = Modifier.textVerticalPadding(
                    title != null,
                    action != null
                )
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor,
                    LocalTextStyle provides supportingTextStyle,
                    content = text
                )
            }
            action?.let {
                Box(
                    modifier = Modifier
                        .requiredHeightIn(min = ActionLabelMinHeight)
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
    caretType: CaretType,
    density: Density,
    configuration: Configuration,
    containerColor: Color,
    caretProperties: CaretProperties,
    anchorLayoutCoordinates: LayoutCoordinates?
): DrawResult {
    val path = Path()

    if (anchorLayoutCoordinates != null) {
        val caretHeightPx: Int
        val caretWidthPx: Int
        val screenWidthPx: Int
        val tooltipAnchorSpacing: Int
        with(density) {
            caretHeightPx = caretProperties.caretHeight.roundToPx()
            caretWidthPx = caretProperties.caretWidth.roundToPx()
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
        val caretY = if (isCaretTop) { 0f } else { tooltipHeight }

        val position: Offset
        if (caretType == CaretType.Plain) {
            position =
                if (anchorMid + tooltipWidth / 2 > screenWidthPx) {
                    // Caret needs to be near the right
                    val anchorMidFromRightScreenEdge =
                        screenWidthPx - anchorMid
                    val caretX = tooltipWidth - anchorMidFromRightScreenEdge
                    Offset(caretX, caretY)
                } else {
                    // Caret needs to be near the left
                    val tooltipLeft =
                        anchorLeft - (this.size.width / 2 - anchorWidth / 2)
                    val caretX = anchorMid - maxOf(tooltipLeft, 0f)
                    Offset(caretX, caretY)
                }
        } else {
            // Default the caret to the left
            var preferredPosition = Offset(anchorMid - anchorLeft, caretY)
            if (anchorLeft + tooltipWidth > screenWidthPx) {
                // Need to move the caret to the right
                preferredPosition = Offset(anchorMid - (anchorRight - tooltipWidth), caretY)
                if (anchorRight - tooltipWidth < 0) {
                    // Need to center the caret
                    // Caret might need to be offset depending on where
                    // the tooltip is placed relative to the anchor
                    if (anchorLeft - tooltipWidth / 2 + anchorWidth / 2 <= 0) {
                        preferredPosition = Offset(anchorMid, caretY)
                    } else if (anchorRight + tooltipWidth / 2 - anchorWidth / 2 >= screenWidthPx) {
                        val anchorMidFromRightScreenEdge =
                            screenWidthPx - anchorMid
                        val caretX = tooltipWidth - anchorMidFromRightScreenEdge
                        preferredPosition = Offset(caretX, caretY)
                    } else {
                        preferredPosition = Offset(tooltipWidth / 2, caretY)
                    }
                }
            }
            position = preferredPosition
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
            drawPath(
                path = path,
                color = containerColor
            )
        }
    }
}

@ExperimentalMaterial3Api
private enum class CaretType {
    Plain, Rich
}
