/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.integration.test.core

import androidx.compose.Composable
import androidx.ui.core.Draw
import androidx.ui.core.ambientDensity
import androidx.ui.foundation.Border
import androidx.ui.foundation.DrawBorder
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.geometry.Offset
import androidx.ui.geometry.shift
import androidx.ui.graphics.Color
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Path
import androidx.ui.graphics.Shape
import androidx.ui.layout.Container
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.withDensity

class SimpleRadioButton2TestCase : BaseSimpleRadioButtonTestCase() {
    @Composable
    override fun emitContent() {
        Container(width = 48.dp, height = 48.dp) {
            // code below was replaced by DrawBorder but we still need it
            // in order to have honest benchmark here
            val borderModifier = DrawBorder(Border(1.dp, Color.Cyan), CircleShape)
            val density = ambientDensity()
            Draw { canvas, size ->
                borderModifier.draw(density, {}, canvas, size)
            }
            val padding = (48.dp - getInnerSize().value) / 2
            DrawShape(PaddingShape(padding, CircleShape), Color.Cyan)
        }
    }
}

private data class PaddingShape(val padding: Dp, val shape: Shape) : Shape {
    override fun createOutline(size: PxSize, density: Density): Outline {
        val twoPaddings = withDensity(density) { (padding * 2).toPx() }
        val sizeMinusPaddings = PxSize(size.width - twoPaddings, size.height - twoPaddings)
        val rawResult = shape.createOutline(sizeMinusPaddings, density)
        return rawResult.offset(twoPaddings / 2)
    }
}

private fun Outline.offset(size: Px): Outline {
    val offset = Offset(size.value, size.value)
    return when (this) {
        is Outline.Rectangle -> Outline.Rectangle(rect.shift(offset))
        is Outline.Rounded -> Outline.Rounded(rrect.shift(offset))
        is Outline.Generic -> Outline.Generic(Path().apply {
            addPath(path)
            shift(offset)
        })
    }
}
