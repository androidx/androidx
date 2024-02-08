/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField2
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun TextPointerIconDemo() {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            """The texts below demonstrate how different pointer hover icons work on
                | different texts and text fields."""
                .trimMargin().replace("\n", "")
        )
        IconDemoColumn()
        Column(
            modifier = Modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .border(1.dp, Color.LightGray)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("This box is set to the hand icon.")
            IconDemoColumn()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconDemoColumn() {
    val borderMod = Modifier.border(1.dp, Color.LightGray)
    val iconMod = borderMod.pointerHoverIcon(PointerIcon.Crosshair)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Regular Text, icon not set", borderMod)

        Text("Regular Text, icon crosshair", iconMod)

        SelectionContainer {
            Text("Selectable Text, icon not set", borderMod)
        }

        SelectionContainer {
            Text("Selectable Text, icon crosshair", iconMod)
        }

        var nonMod by remember { mutableStateOf("TextField, icon not set") }
        TextField(nonMod, { nonMod = it }, borderMod)

        var mod by remember { mutableStateOf("TextField, icon crosshair") }
        TextField(mod, { mod = it }, iconMod)

        val nonModTfs = rememberTextFieldState("BTF2, icon not set")
        BasicTextField2(nonModTfs, borderMod)

        val modTfs = rememberTextFieldState("BTF2, icon crosshair")
        BasicTextField2(modTfs, iconMod)
    }
}
