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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ElevatedToggleButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    ToggleButton(checked = checked, onCheckedChange = { checked = it }) { Text("Button") }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun RoundToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    val shapes =
        ButtonShapes(
            shape = ToggleButtonDefaults.roundShape,
            pressedShape = ToggleButtonDefaults.pressedShape,
            checkedShape = ToggleButtonDefaults.squareShape
        )
    ToggleButton(checked = checked, onCheckedChange = { checked = it }, shapes = shapes) {
        Text("Button")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ElevatedToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    ElevatedToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        Text("Elevated Button")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun TonalToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    TonalToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        Text("Tonal Button")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun OutlinedToggleButtonSample() {
    var checked by remember { mutableStateOf(false) }
    OutlinedToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        Text("Outlined Button")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ToggleButtonWithIconSample() {
    var checked by remember { mutableStateOf(false) }
    ElevatedToggleButton(checked = checked, onCheckedChange = { checked = it }) {
        Icon(
            if (checked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Like")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun XSmallToggleButtonWithIconSample() {
    var checked by remember { mutableStateOf(false) }
    ToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        modifier = Modifier.heightIn(ButtonDefaults.XSmallContainerHeight),
        shapes =
            ToggleButtonDefaults.shapes(
                shape = ToggleButtonDefaults.XSmallSquareShape,
                pressedShape = ToggleButtonDefaults.XSmallPressedShape,
                checkedShape = ToggleButtonDefaults.checkedShape
            ),
        contentPadding = ButtonDefaults.XSmallContentPadding
    ) {
        Icon(
            if (checked) Icons.Filled.Edit else Icons.Outlined.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.XSmallIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.XSmallIconSpacing))
        Text("Label")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun MediumToggleButtonWithIconSample() {
    var checked by remember { mutableStateOf(false) }
    ToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
        shapes =
            ToggleButtonDefaults.shapes(
                shape = ToggleButtonDefaults.MediumSquareShape,
                pressedShape = ToggleButtonDefaults.MediumPressedShape,
                checkedShape = ToggleButtonDefaults.checkedShape
            ),
        contentPadding = ButtonDefaults.MediumContentPadding
    ) {
        Icon(
            if (checked) Icons.Filled.Edit else Icons.Outlined.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.MediumIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.MediumIconSpacing))
        Text(text = "Label", style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun LargeToggleButtonWithIconSample() {
    var checked by remember { mutableStateOf(false) }
    ToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        modifier = Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
        shapes =
            ToggleButtonDefaults.shapes(
                shape = ToggleButtonDefaults.LargeSquareShape,
                pressedShape = ToggleButtonDefaults.LargePressedShape,
                checkedShape = ToggleButtonDefaults.checkedShape
            ),
        contentPadding = ButtonDefaults.LargeContentPadding
    ) {
        Icon(
            if (checked) Icons.Filled.Edit else Icons.Outlined.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.LargeIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.LargeIconSpacing))
        Text(text = "Label", style = MaterialTheme.typography.headlineSmall)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun XLargeToggleButtonWithIconSample() {
    var checked by remember { mutableStateOf(false) }
    ToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        modifier = Modifier.heightIn(ButtonDefaults.XLargeContainerHeight),
        shapes =
            ToggleButtonDefaults.shapes(
                shape = ToggleButtonDefaults.XLargeSquareShape,
                pressedShape = ToggleButtonDefaults.XLargePressedShape,
                checkedShape = ToggleButtonDefaults.checkedShape
            ),
        contentPadding = ButtonDefaults.XLargeContentPadding
    ) {
        Icon(
            if (checked) Icons.Filled.Edit else Icons.Outlined.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.XLargeIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.XLargeIconSpacing))
        Text(text = "Label", style = MaterialTheme.typography.headlineLarge)
    }
}
