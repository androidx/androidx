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
import androidx.ui.core.Modifier
import androidx.compose.foundation.Border
import androidx.compose.foundation.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawOutline
import androidx.compose.foundation.layout.preferredSize
import androidx.ui.unit.dp

class SimpleRadioButton1TestCase : BaseSimpleRadioButtonTestCase() {
    @Composable
    override fun emitContent() {
        Box(
            modifier = Modifier.preferredSize(48.dp),
            shape = CircleShape,
            border = Border(1.dp, Color.Cyan),
            gravity = ContentGravity.Center
        ) {
            val innerSize = getInnerSize().value
            Canvas(Modifier.preferredSize(innerSize)) {
                drawOutline(
                    CircleShape.createOutline(size, this),
                    Color.Cyan
                )
            }
        }
    }
}
