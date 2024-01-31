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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.tokens.PlainTooltipTokens
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
import androidx.compose.ui.platform.LocalDensity
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
@Composable
@ExperimentalMaterial3Api
internal fun CaretScope.PlainTooltipImpl(
    modifier: Modifier,
    caretProperties: (CaretProperties)?,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
) {
    val customModifier =
        if (caretProperties != null) {
            val density = LocalDensity.current
            val configuration = getCurrentConfiguration()
            Modifier.drawCaret { anchorLayoutCoordinates ->
                    drawCaretWithPath(
                        density,
                        configuration,
                        containerColor,
                        caretProperties,
                        anchorLayoutCoordinates
                    )
                }.then(modifier)
        } else modifier

    Surface(
        modifier = customModifier,
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

@ExperimentalMaterial3Api
private fun CacheDrawScope.drawCaretWithPath(
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
            screenWidthPx = configuration.getScreenWidthPx(density)
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

        val position =
            if (anchorMid + tooltipWidth / 2 > screenWidthPx) {
                val anchorMidFromRightScreenEdge =
                    screenWidthPx - anchorMid
                val caretX = tooltipWidth - anchorMidFromRightScreenEdge
                Offset(caretX, caretY)
            } else {
                val tooltipLeft =
                    anchorLeft - (this.size.width / 2 - anchorWidth / 2)
                val caretX = anchorMid - maxOf(tooltipLeft, 0f)
                Offset(caretX, caretY)
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

@Composable
internal expect fun getCurrentConfiguration(): Configuration

internal expect fun Configuration.getScreenWidthPx(density: Density): Int

@kotlin.jvm.JvmInline
internal value class Configuration(val delegate: Any)