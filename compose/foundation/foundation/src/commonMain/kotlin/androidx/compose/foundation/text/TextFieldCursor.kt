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

package androidx.compose.foundation.text

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.text.input.internal.CursorAnimationState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

internal fun Modifier.cursor(
    state: LegacyTextFieldState,
    value: TextFieldValue,
    offsetMapping: OffsetMapping,
    cursorBrush: Brush,
    enabled: Boolean
) = if (enabled) composed {
    val cursorAnimation = remember { CursorAnimationState() }
    // Don't bother animating the cursor if it wouldn't draw any pixels.
    val isBrushSpecified = !(cursorBrush is SolidColor && cursorBrush.value.isUnspecified)
    // Only animate the cursor when its window is actually focused. This also disables the cursor
    // animation when the screen is off.
    // TODO confirm screen-off behavior.
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    if (isWindowFocused && state.hasFocus && value.selection.collapsed && isBrushSpecified) {
        LaunchedEffect(value.annotatedString, value.selection) {
            cursorAnimation.snapToVisibleAndAnimate()
        }
        drawWithContent {
            this.drawContent()
            val cursorAlphaValue = cursorAnimation.cursorAlpha
            if (cursorAlphaValue != 0f) {
                val transformedOffset = offsetMapping
                    .originalToTransformed(value.selection.start)
                val cursorRect = state.layoutResult?.value?.getCursorRect(transformedOffset)
                    ?: Rect(0f, 0f, 0f, 0f)
                val cursorWidth = DefaultCursorThickness.toPx()
                val cursorX = (cursorRect.left + cursorWidth / 2)
                    // Do not use coerceIn because it is not guaranteed that the minimum value is
                    // smaller than the maximum value.
                    .coerceAtMost(size.width - cursorWidth / 2)
                    .coerceAtLeast(cursorWidth / 2)

                drawLine(
                    cursorBrush,
                    Offset(cursorX, cursorRect.top),
                    Offset(cursorX, cursorRect.bottom),
                    alpha = cursorAlphaValue,
                    strokeWidth = cursorWidth
                )
            }
        }
    } else {
        Modifier
    }
} else this

private val cursorAnimationSpec: AnimationSpec<Float> = infiniteRepeatable(
    animation = keyframes {
        durationMillis = 1000
        1f at 0
        1f at 499
        0f at 500
        0f at 999
    }
)

internal val DefaultCursorThickness = 2.dp

private object FixedMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = 1f
}
