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
import androidx.ui.core.Modifier
import androidx.ui.foundation.Border
import androidx.ui.foundation.Text
import androidx.ui.foundation.shape.GenericShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.OutlinedButton
import androidx.ui.material.samples.ButtonSample
import androidx.ui.material.samples.IconButtonSample
import androidx.ui.material.samples.IconToggleButtonSample
import androidx.ui.material.samples.OutlinedButtonSample
import androidx.ui.material.samples.SimpleExtendedFabNoIcon
import androidx.ui.material.samples.SimpleExtendedFabWithIcon
import androidx.ui.material.samples.SimpleFab
import androidx.ui.material.samples.TextButtonSample
import androidx.ui.unit.dp

private val DefaultSpace = 20.dp

@Composable
fun ButtonDemo() {
    Column(Modifier.padding(10.dp)) {
        Buttons()
        Spacer(Modifier.preferredHeight(DefaultSpace))
        Fabs()
        Spacer(Modifier.preferredHeight(DefaultSpace))
        IconButtons()
        Spacer(Modifier.preferredHeight(DefaultSpace))
        CustomShapeButton()
    }
}

@Composable
private fun Buttons() {
    Text("Buttons")
    Spacer(Modifier.preferredHeight(DefaultSpace))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ButtonSample()
        OutlinedButtonSample()
        TextButtonSample()
    }

    Spacer(Modifier.preferredHeight(DefaultSpace))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = {}, backgroundColor = MaterialTheme.colors.secondary) {
            Text("Secondary Color")
        }
        // TODO(Andrey): Disabled button has wrong bg and text color for now.
        // Need to figure out where will we store their styling. Not a part of
        // ColorPalette right now and specs are not clear about this.
        Button(onClick = {}, enabled = false) {
            Text("DISABLED. TODO")
        }
    }
}

@Composable
private fun Fabs() {
    Text("Floating action buttons")
    Spacer(Modifier.preferredHeight(DefaultSpace))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SimpleFab()
        SimpleExtendedFabNoIcon()
        SimpleExtendedFabWithIcon()
    }
}

@Composable
private fun IconButtons() {
    Text("Icon buttons")
    Spacer(Modifier.preferredHeight(DefaultSpace))

    Row {
        IconButtonSample()
        IconToggleButtonSample()
    }
}

@Composable
private fun CustomShapeButton() {
    Text("Custom shape button")
    Spacer(Modifier.preferredHeight(DefaultSpace))
    OutlinedButton(
        onClick = {},
        modifier = Modifier.preferredSize(100.dp),
        shape = TriangleShape,
        backgroundColor = Color.Yellow,
        border = Border(size = 2.dp, color = Color.Black)
    ) {
        Text("Ok")
    }
}

private val TriangleShape = GenericShape { size ->
    moveTo(size.width.value / 2f, 0f)
    lineTo(size.width.value, size.height.value)
    lineTo(0f, size.height.value)
}
