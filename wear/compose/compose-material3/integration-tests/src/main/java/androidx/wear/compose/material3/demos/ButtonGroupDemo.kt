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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.ButtonGroupScope
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.IconToggleButtonShapes
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.compose.material3.TextToggleButtonDefaults
import androidx.wear.compose.material3.TextToggleButtonShapes
import androidx.wear.compose.material3.samples.icons.WifiOffIcon
import androidx.wear.compose.material3.samples.icons.WifiOnIcon

@Composable
fun ButtonGroupDemo() {
    val interactionSource1 = remember { MutableInteractionSource() }
    val interactionSource2 = remember { MutableInteractionSource() }
    val interactionSource3 = remember { MutableInteractionSource() }
    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        ButtonGroup(Modifier.fillMaxWidth()) {
            Button(
                onClick = {},
                Modifier.animateWidth(interactionSource1),
                interactionSource = interactionSource1
            ) {
                Text("<", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            FilledIconButton(
                onClick = {},
                Modifier.animateWidth(interactionSource2),
                interactionSource = interactionSource2
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Button(
                onClick = {},
                Modifier.animateWidth(interactionSource3),
                interactionSource = interactionSource3
            ) {
                Text(">", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ButtonGroupToggleButtonsDemo() {
    val iconSize = 32.dp
    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Column {
            ButtonGroup(Modifier.fillMaxWidth()) {
                MyIconToggleButton(IconToggleButtonDefaults.shapes(), Modifier.weight(1.2f)) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(iconSize)
                    )
                }
                MyIconToggleButton(IconToggleButtonDefaults.animatedShapes()) { checked ->
                    if (checked) {
                        WifiOnIcon(Modifier.size(iconSize))
                    } else {
                        WifiOffIcon(Modifier.size(iconSize))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ButtonGroup(Modifier.fillMaxWidth()) {
                MyTextToggleButton(TextToggleButtonDefaults.shapes()) { checked ->
                    Text(
                        text = if (checked) "On" else "Off",
                        style = TextToggleButtonDefaults.defaultButtonTextStyle
                    )
                }
                MyTextToggleButton(
                    TextToggleButtonDefaults.animatedShapes(),
                    Modifier.weight(1.2f)
                ) { checked ->
                    Text(
                        text = if (checked) "On" else "Off",
                        style = TextToggleButtonDefaults.defaultButtonTextStyle
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonGroupScope.MyIconToggleButton(
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    content: @Composable (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var checked by remember { mutableStateOf(false) }
    IconToggleButton(
        checked = checked,
        modifier =
            modifier
                .height(IconToggleButtonDefaults.SmallButtonSize)
                .fillMaxWidth()
                .animateWidth(interactionSource),
        onCheckedChange = { checked = !checked },
        shapes = shapes,
        interactionSource = interactionSource
    ) {
        content(checked)
    }
}

@Composable
private fun ButtonGroupScope.MyTextToggleButton(
    shapes: TextToggleButtonShapes,
    modifier: Modifier = Modifier,
    content: @Composable (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var checked by remember { mutableStateOf(false) }
    TextToggleButton(
        checked = checked,
        modifier =
            modifier
                .height(TextToggleButtonDefaults.DefaultButtonSize)
                .fillMaxWidth()
                .animateWidth(interactionSource),
        onCheckedChange = { checked = !checked },
        shapes = shapes,
        interactionSource = interactionSource
    ) {
        content(checked)
    }
}
