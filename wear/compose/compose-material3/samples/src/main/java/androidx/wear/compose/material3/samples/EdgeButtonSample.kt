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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.samples.icons.CheckIcon

@Sampled
@Composable
fun EdgeButtonSample() {
    val sizes =
        listOf(
            EdgeButtonSize.ExtraSmall,
            EdgeButtonSize.Small,
            EdgeButtonSize.Medium,
            EdgeButtonSize.Large
        )
    val sizeNames = listOf("XS", "S", "M", "L")
    var size by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxSize().padding(top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row { Spacer(modifier = Modifier.height(16.dp)) }
                Row {
                    Text("Sizes", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(sizeNames.size) {
                        TextButton(
                            onClick = { size = it },
                            modifier = Modifier.size(TextButtonDefaults.SmallButtonSize)
                        ) {
                            Text(sizeNames[it])
                        }
                    }
                }
            }
            EdgeButton(
                onClick = { /* Do something */ },
                buttonSize = sizes[size],
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Check icon",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
        }
    }
}

@Preview
@Sampled
@Composable
fun EdgeButtonListSample() {
    val state = rememberScalingLazyListState()
    val horizontalPadding = LocalConfiguration.current.screenWidthDp.dp * 0.052f
    val verticalPadding = LocalConfiguration.current.screenHeightDp.dp * 0.16f
    val colors =
        listOf(
            "Filled" to ButtonDefaults.buttonColors(),
            "Filled Variant" to ButtonDefaults.filledVariantButtonColors(),
            "Filled Tonal" to ButtonDefaults.filledTonalButtonColors(),
            "Outlined" to ButtonDefaults.outlinedButtonColors(),
            "Disabled" to ButtonDefaults.buttonColors()
        )
    var selectedColor by remember { mutableIntStateOf(0) }
    val types = listOf("Icon only" to 0, "Text only" to 1)
    var selectedType by remember { mutableIntStateOf(0) }

    ScreenScaffold(
        scrollState = state,
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
        edgeButton = {
            EdgeButton(
                onClick = {},
                buttonSize = EdgeButtonSize.Medium,
                colors = colors[selectedColor].second,
                border =
                    if (colors[selectedColor].first == "Outlined")
                        ButtonDefaults.outlinedButtonBorder(true)
                    else null,
                enabled = colors[selectedColor].first != "Disabled"
            ) {
                if (selectedType == 0) {
                    // Remove extra spacing around the icon so it integrates better into the scroll.
                    CheckIcon(Modifier.size(21.dp).wrapContentSize(unbounded = true).size(32.dp))
                } else {
                    Text("Ok")
                }
            }
        },
    ) { contentPadding ->
        ScalingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().selectableGroup(),
            autoCentering = null,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Text("Color") }
            items(colors.size) { ix ->
                RadioButton(
                    label = { Text(colors[ix].first) },
                    selected = selectedColor == ix,
                    onSelect = { selectedColor = ix },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Text("Type") }
            items(types.size) { ix ->
                RadioButton(
                    label = { Text(types[ix].first) },
                    selected = selectedType == ix,
                    onSelect = { selectedType = ix },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
