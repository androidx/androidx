/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.Handle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.uikit.LocalNativeTextInputContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available

internal data class HandleStyle(
    val dotDiameter: Dp,
    val stemWidth: Dp,
    val shadowRadius: Dp,
    val shadowAlpha: Float,
)

internal val iosHandleStyle: HandleStyle by lazy {
    if (available(OS.Ios to OSVersion(major = 17))) {
        HandleStyle(
            dotDiameter = 16.7.dp,
            stemWidth = 2.dp,
            shadowRadius = 13.dp,
            shadowAlpha = 0.3f,
        )
    } else {
        HandleStyle(
            dotDiameter = 11.dp,
            stemWidth = 2.dp,
            shadowRadius = 0.dp,
            shadowAlpha = 0f,
        )
    }
}

// Clickable padding around the visible handle.
private val PADDING = 5.dp

@OptIn(InternalComposeUiApi::class)
@Composable
internal actual fun SelectionHandle(
    offsetProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    minTouchTargetSize: DpSize,
    lineHeight: Float,
    modifier: Modifier
) {
    val nativeInputProvider = LocalNativeTextInputContext.current
    if (nativeInputProvider.usingNativeTextInput()) {
        return // iOS draws selection handles itself.
    }
    val style = iosHandleStyle
    val isLeft = isLeftSelectionHandle(isStartHandle, direction, handlesCrossed)
    val handleReferencePoint = if (isLeft) Alignment.BottomCenter else Alignment.TopCenter

    HandlePopup(
        positionProvider = {
            var offset = offsetProvider.provide()
            if (offset.isSpecified && !isLeft) {
                offset += Offset(0f, -lineHeight)
            }
            offset
        },
        handleReferencePoint = handleReferencePoint
    ) {
        IosSelectionHandleIcon(
            modifier = modifier.semantics {
                val position = offsetProvider.provide()
                this[SelectionHandleInfoKey] = SelectionHandleInfo(
                    handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd,
                    position = position,
                    anchor = if (isLeft) SelectionHandleAnchor.Left else SelectionHandleAnchor.Right,
                    visible = position.isSpecified,
                )
            },
            iconVisible = { offsetProvider.provide().isSpecified },
            lineHeight = lineHeight,
            isLeft = isLeft,
            style = style,
        )
    }
}

@Composable
private fun IosSelectionHandleIcon(
    modifier: Modifier,
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean,
    style: HandleStyle,
) {
    val density = LocalDensity.current
    val handleColor = LocalTextSelectionColors.current.handleColor
    val lineHeightDp = with(density) { lineHeight.toDp() }
    val dotRadius = style.dotDiameter / 2
    Spacer(
        modifier
            .size(
                width = (PADDING + dotRadius) * 2,
                height = dotRadius * 2 + PADDING + lineHeightDp
            )
            .drawIosSelectionHandle(iconVisible, lineHeight, isLeft, handleColor, density, style)
    )
}

private fun Modifier.drawIosSelectionHandle(
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean,
    handleColor: Color,
    density: Density,
    style: HandleStyle,
): Modifier = drawWithCache {
    val paddingPx = with(density) { PADDING.toPx() }
    val dotRadiusPx = with(density) { (style.dotDiameter / 2).toPx() }
    val stemWidthPx = with(density) { style.stemWidth.toPx() }
    val shadowRadiusPx = with(density) { style.shadowRadius.toPx() }
    val shadowPaint = if (style.shadowRadius > 0.dp && style.shadowAlpha > 0f) {
        Paint().apply {
            color = Color.Black.copy(alpha = style.shadowAlpha)
            isAntiAlias = true
            // Skia gaussian-blur sigma matching androidx.compose.ui.graphics.BlurEffect.
            val sigma = 0.57735f * shadowRadiusPx + 0.5f
            skiaPaint.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
        }
    } else null

    onDrawWithContent {
        drawContent()
        if (!iconVisible()) return@onDrawWithContent

        val stemWidthInt = stemWidthPx.roundToInt().toFloat()
        val stemLeftX = floor((size.width - stemWidthInt) / 2f)
        val stemCenterX = stemLeftX + stemWidthInt / 2f
        val dotCenterY = if (isLeft) paddingPx + dotRadiusPx else lineHeight + dotRadiusPx

        val stemTopY: Float
        val stemBottomY: Float
        if (isLeft) {
            stemTopY = dotCenterY
            stemBottomY = size.height
        } else {
            stemTopY = 0f
            stemBottomY = dotCenterY
        }

        if (shadowPaint != null) {
            drawContext.canvas.drawCircle(
                center = Offset(stemCenterX, dotCenterY),
                radius = dotRadiusPx,
                paint = shadowPaint,
            )
        }

        // Vertical stem
        drawRect(
            color = handleColor,
            topLeft = Offset(stemLeftX, stemTopY),
            size = Size(stemWidthInt, stemBottomY - stemTopY)
        )
        // Dot
        drawCircle(
            color = handleColor,
            radius = dotRadiusPx,
            center = Offset(stemCenterX, dotCenterY)
        )
    }
}
