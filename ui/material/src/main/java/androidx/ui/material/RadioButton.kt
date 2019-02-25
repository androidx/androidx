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
import androidx.ui.core.*
import androidx.ui.core.adapter.DensityConsumer
import androidx.ui.core.adapter.Draw
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle

import com.google.r4a.Composable
import com.google.r4a.Component
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.unaryPlus

// TODO(malkov): maybe intro this wrappers to separate module if heavy-used. Delete later
class RadioButtonWrapper : Component() {
    var checked: Boolean = false
    var color: Color? = null
    override fun compose() {
        <RadioButton checked color />
    }
}

@Composable
fun RadioButton(checked: Boolean, color: Color? = null) {
    val value = if (checked) ToggleableState.CHECKED else ToggleableState.UNCHECKED
    <Toggleable value>
        <MeasureBox> constraints ->
            collect {
                val colors = +ambient(Colors)
                <DrawRadioButton value color=(color ?: colors.primary)/>
            }
            val size = (radioRadius * 2).toPx()
            val w = max(constraints.minWidth, min(constraints.maxWidth, size))
            val h = max(constraints.minHeight, min(constraints.maxHeight, size))
            layout(w, h) {
                // no children to place
            }
        </MeasureBox>
    </Toggleable>
}

@Composable
internal fun DrawRadioButton(value: ToggleableState, color: Color) {
    <DensityConsumer> density ->
        <Draw> canvas, parentSize ->
            drawRadio(canvas, parentSize, density, value, color)
        </Draw>
    </DensityConsumer>
}


internal fun drawRadio(
        canvas: Canvas,
        parentSize: PixelSize,
        density: Density,
        state: ToggleableState,
        color: Color
) {
    val p = Paint()
    p.isAntiAlias = true
    p.color = if (state == ToggleableState.CHECKED) color else uncheckedRadioColor
    p.strokeWidth = radioStrokeWidth.toPx(density)
    p.style = PaintingStyle.stroke

    // TODO(malkov): currently Radio gravity is always CENTER but we need to be flexible
    val centerW = parentSize.width / 2
    val centerH = parentSize.height / 2
    val center = Offset(centerW, centerH)

    canvas.drawCircle(center, (radioRadius - strokeWidth / 2).toPx(density), p)

    if (state == ToggleableState.CHECKED) {
        p.style = PaintingStyle.fill
        p.strokeWidth = 0f
        canvas.drawCircle(center, innerCircleSize.toPx(density), p)
    }
}

// TODO(malkov): see how it goes and maybe move it to styles or cross-widget defaults
internal val uncheckedRadioColor = Color(0xFF7D7D7D.toInt())

// TODO(malkov): random numbers for now to produce radio as in material comp.
internal val radioRadius = 10.dp
internal val innerCircleSize = 4.75.dp
internal val radioStrokeWidth = 2.dp
