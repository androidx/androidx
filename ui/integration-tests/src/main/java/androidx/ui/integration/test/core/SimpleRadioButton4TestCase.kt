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
import androidx.ui.core.Modifier
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.foundation.drawBorder
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp

class SimpleRadioButton4TestCase : BaseSimpleRadioButtonTestCase() {

    @Composable
    override fun emitContent() {
        val innerSize = getInnerSize()
        Box(
            Modifier.preferredSize(48.dp)
                .drawBackground(Color.Cyan, CircleShape)
                .padding(innerSize.value)
                .drawBorder(
                    border = Border(color = Color.Cyan, size = 1.dp),
                    shape = CircleShape
                )
        ) {}
    }
}