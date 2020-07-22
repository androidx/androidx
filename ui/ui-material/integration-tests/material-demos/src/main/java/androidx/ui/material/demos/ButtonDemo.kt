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
import androidx.compose.foundation.Border
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.OutlinedButton
import androidx.ui.material.TextButton
import androidx.ui.material.samples.ButtonSample
import androidx.ui.material.samples.ButtonWithIconSample
import androidx.ui.material.samples.FluidExtendedFab
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
    ScrollableColumn(contentPadding = InnerPadding(10.dp)) {
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
        ButtonWithIconSample()
    }

    Spacer(Modifier.preferredHeight(DefaultSpace))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = {}, enabled = false) {
            Text("Disabled")
        }
        OutlinedButton(onClick = {}, enabled = false) {
            Text("Disabled")
        }
        TextButton(onClick = {}, enabled = false) {
            Text("Disabled")
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
    Spacer(Modifier.preferredHeight(DefaultSpace))
    FluidExtendedFab()
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
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
}
