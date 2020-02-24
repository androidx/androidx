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
import androidx.compose.remember
import androidx.ui.foundation.Canvas
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.dp
import androidx.ui.unit.minDimension

class SimpleRadioButton3TestCase : BaseSimpleRadioButtonTestCase() {

    @Composable
    override fun emitContent() {
        val innerSize = getInnerSize()
        val borderPaint = remember { Paint().apply { style = PaintingStyle.stroke } }
        val fillPaint = remember { Paint() }
        Canvas(LayoutSize(48.dp)) {
            val center = Offset(size.width.value / 2f, size.height.value / 2f)
            drawCircle(center, size.minDimension.value, borderPaint)
            val innerRadius = innerSize.value.value / 2f
            drawCircle(center, innerRadius, fillPaint)
        }
    }
}
