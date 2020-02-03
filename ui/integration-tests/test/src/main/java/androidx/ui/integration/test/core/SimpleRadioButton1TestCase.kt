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
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.unit.dp

class SimpleRadioButton1TestCase : BaseSimpleRadioButtonTestCase() {
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
            val innerSize = getInnerSize().value
            Container(width = innerSize, height = innerSize) {
                DrawShape(CircleShape, Color.Cyan)
            }
        }
    }
}
