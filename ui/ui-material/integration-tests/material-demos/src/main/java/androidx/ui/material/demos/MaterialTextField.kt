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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.preferredHeightIn
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.material.FilledTextField
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Email
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

// TODO(soboleva): remove explicit currentTextStyle() from label when b/143464846 is fixed
@Composable
fun MaterialTextFieldsDemo() {
    val space = with(DensityAmbient.current) { 10.dp.toIntPx() }
    Column(Modifier.fillMaxHeight(), verticalArrangement = arrangeWithSpacer(space)) {
        var text by state { TextFieldValue() }
        FilledTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Label", style = currentTextStyle()) },
            modifier = Modifier.preferredWidth(150.dp)
        )

        FilledTextField(
            value = text,
            onValueChange = { text = it },
            label = { Icon(Icons.Filled.Email) },
            placeholder = { Text(text = "example@example.com", style = currentTextStyle()) },
            modifier = Modifier.preferredHeightIn(maxHeight = 150.dp)
        )

        FilledTextField(
            value = text,
            onValueChange = { text = it },
            label = {
                Box(Modifier.preferredSize(40.dp, 40.dp).drawBackground(Color.Red))
            },
            placeholder = { Text(text = "placeholder", style = currentTextStyle()) }
        )

        var initialText by state { "Initial text" }
        FilledTextField(
            value = initialText,
            onValueChange = { initialText = it },
            label = {
                Text("With initial text", style = currentTextStyle())
            }
        )

        FilledTextField(
            value = initialText,
            onValueChange = { initialText = it },
            label = {}
        )
    }
}

private fun arrangeWithSpacer(space: IntPx) = object : Arrangement.Vertical {
    override fun arrange(
        totalSize: IntPx,
        size: List<IntPx>,
        layoutDirection: LayoutDirection
    ): List<IntPx> {
        val positions = mutableListOf<IntPx>()
        var current = 0.ipx
        size.forEach {
            positions.add(current)
            current += (it + space)
        }
        return positions
    }
}