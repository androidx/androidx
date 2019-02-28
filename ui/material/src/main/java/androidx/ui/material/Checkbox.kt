/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.material

import androidx.ui.baseui.selection.Toggleable
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.Dp
import androidx.ui.core.adapter.DensityConsumer
import androidx.ui.core.adapter.Draw
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.dp
import androidx.ui.core.min
import androidx.ui.core.toPx
import androidx.ui.core.toRoundedPixels
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.shrink
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.Path
import com.google.r4a.Component
import com.google.r4a.composer

// TODO(clara): This should not be a class once R4A bug is fixed
class Checkbox : Component() {

    // TODO(clara): remove this default
    var color: Color? = null
    var value: ToggleableState = ToggleableState.CHECKED
    var testTag: String? = null

    override fun compose() {
        <Toggleable testTag value>
            <MeasureBox> constraints ->
                collect {
                    <Colors.Consumer> colors ->
                        <DrawCheckbox
                            color=(color ?: colors.secondary)
                            value
                            strokeWidth=strokeWidth />
                    </Colors.Consumer>
                }
                val calculatedWidth = min(
                    min(constraints.maxHeight, constraints.maxWidth),
                    checkboxSize.toPx()
                ).toRoundedPixels()
                layout(calculatedWidth, calculatedWidth) {
                    // No mChildren to place
                }
            </MeasureBox>
        </Toggleable>
    }
}

internal fun DrawCheckbox(value: ToggleableState, color: Color, strokeWidth: Dp) {
    <DensityConsumer> density ->
        val radius = Radius.circular(radiusSize.toPx(density).value)
        val strokeWidthPx = strokeWidth.toPx(density).value
        <Draw> canvas, parentSize ->
            val outer = RRect(0f, 0f, parentSize.width.value, parentSize.height.value, radius)
            if (value == ToggleableState.CHECKED) {
                drawChecked(canvas, outer, color, strokeWidthPx)
            } else if (value == ToggleableState.UNCHECKED) {
                // TODO(clara): Where does this color come from?
                drawUnchecked(canvas, outer, color, strokeWidthPx)
            } else { // Indeterminate
                drawIndeterminate(canvas, outer, color, strokeWidthPx)
            }
        </Draw>
    </DensityConsumer>
}

internal fun drawUnchecked(canvas: Canvas, outer: RRect, color: Color, strokeWidth: Float) {
    val paint = Paint()
    paint.strokeWidth = strokeWidth
    paint.color = Color(0x42000000.toInt()) // grey for now
    val inner = outer.shrink(paint.strokeWidth)
    canvas.drawDRRect(outer, inner, paint)
}

internal fun drawChecked(canvas: Canvas, outer: RRect, color: Color, strokeWidth: Float) {
    val paint = Paint()
    paint.strokeWidth = strokeWidth
    paint.color = color
    // Background
    canvas.drawRRect(outer, paint)

    // Check
    paint.color = Color(0xFFFFFFFF.toInt())
    paint.style = PaintingStyle.stroke
    val path = Path()
    val width = outer.width
    val start = Offset(width * 0.15f, width * 0.45f)
    val mid = Offset(width * 0.4f, width * 0.7f)
    val end = Offset(width * 0.85f, width * 0.25f)
    path.moveTo(start.dx, start.dy)
    path.lineTo(mid.dx, mid.dy)
    path.lineTo(end.dx, end.dy)
    canvas.drawPath(path, paint)
}

internal fun drawIndeterminate(canvas: Canvas, outer: RRect, color: Color, strokeWidth: Float) {
    val paint = Paint()
    paint.strokeWidth = strokeWidth
    paint.color = color
    // Background
    canvas.drawRRect(outer, paint)

    // Dash
    paint.color = Color(0xFFFFFFFF.toInt())
    val width = outer.width
    val start = Offset(width * 0.2f, width * 0.5f)
    val end = Offset(width * 0.8f, width * 0.5f)
    canvas.drawLine(start, end, paint)
}

internal val checkboxSize = 24.dp
// TODO(clara): I made these values up, not in spec
internal val strokeWidth = 2.5.dp
internal val radiusSize = 4.dp