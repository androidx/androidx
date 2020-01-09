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
import androidx.ui.unit.dp
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.border.Border
import androidx.ui.foundation.shape.border.DrawBorder
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Container

class SimpleRadioButton1TestCase : BaseSimpleRadioButtonTestCase() {
    @Composable
    override fun emitContent() {
        Container(width = 48.dp, height = 48.dp) {
            DrawBorder(CircleShape, Border(Color.Cyan, 1.dp))
            val innerSize = getInnerSize().value
            Container(width = innerSize, height = innerSize) {
                DrawShape(CircleShape, Color.Cyan)
            }
        }
    }
}
