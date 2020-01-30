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

package androidx.ui.integration.test.core

import androidx.compose.Composable
import androidx.ui.foundation.Border
import androidx.ui.foundation.DrawBorder
import androidx.ui.foundation.background
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutPadding
import androidx.ui.unit.dp

class SimpleRadioButton4TestCase : BaseSimpleRadioButtonTestCase() {

    @Composable
    override fun emitContent() {
        val innerSize = getInnerSize()
        Container(
            width = 48.dp, height = 48.dp, modifier = background(CircleShape, Color.Cyan) +
                    LayoutPadding(innerSize.value) +
                    DrawBorder(
                        border = Border(color = Color.Cyan, size = 1.dp),
                        shape = CircleShape
                    )
        ) {}
    }
}