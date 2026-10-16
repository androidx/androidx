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

package androidx.text.vertical.testapp

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * The limits of the pan offset of [ZoomableVerticalText], in pixels.
 *
 * The measure policy writes these values and the gesture handler reads them.
 *
 * This is a plain holder and not snapshot state. Compose records each snapshot read that occurs
 * during measure, and a write to one of those values asks for a new measure pass. A measure policy
 * that writes the state it reads therefore measures its content two times for each change.
 */
private class PanLimits {
    var minX: Float = 0f
    var maxX: Float = 0f
    var minY: Float = 0f
    var maxY: Float = 0f
}

@Composable
fun ZoomableVerticalText(
    isVertical: Boolean = true,
    styleColorsEnabled: Boolean = false,
    fontFamily: FontFamily = FontFamily.Serif,
    content: @Composable (TextStyle) -> Unit,
) {
    val fontSize = 32f
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val panLimits = remember { PanLimits() }
    val style =
        remember(zoom, styleColorsEnabled, fontFamily) {
            TextStyle(
                fontSize = (fontSize * zoom).sp,
                fontFamily = fontFamily,
                color = if (styleColorsEnabled) Color(0xFF1A237E) else Color.Unspecified,
                background = if (styleColorsEnabled) Color(0xFFFFF59D) else Color.Unspecified,
                localeList =
                    LocaleList(
                        Locale(
                            java.util.Locale.Builder()
                                .setLocale(java.util.Locale.JAPANESE)
                                .setUnicodeLocaleKeyword("lb", "strict")
                                .build()
                        )
                    ),
            )
        }

    Layout(
        modifier =
            Modifier.fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoom = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, offsetChange, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.25f, 10f)
                        offsetX =
                            (offsetX + offsetChange.x).coerceIn(panLimits.minX, panLimits.maxX)
                        offsetY =
                            (offsetY + offsetChange.y).coerceIn(panLimits.minY, panLimits.maxY)
                    }
                },
        content = { content(style) },
    ) { measurables, constraints ->
        val childConstraints =
            if (isVertical) {
                constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
            } else {
                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
            }
        val placeable = measurables.first().measure(childConstraints)

        val overflowX = maxOf(0f, (placeable.width - constraints.maxWidth).toFloat())
        if (isVertical) {
            panLimits.minX = 0f
            panLimits.maxX = overflowX
        } else {
            panLimits.minX = -overflowX
            panLimits.maxX = 0f
        }
        panLimits.minY = -maxOf(0f, (placeable.height - constraints.maxHeight).toFloat())
        panLimits.maxY = 0f

        // The gesture handler keeps the offset inside the limits. Clamp again here, because a
        // change of the zoom can shrink the limits after the last gesture event.
        val clampedX = offsetX.coerceIn(panLimits.minX, panLimits.maxX)
        val clampedY = offsetY.coerceIn(panLimits.minY, panLimits.maxY)

        val x =
            if (isVertical) {
                constraints.maxWidth - placeable.width + clampedX.roundToInt()
            } else {
                clampedX.roundToInt()
            }
        val y = clampedY.roundToInt()

        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x, y) }
    }
}
