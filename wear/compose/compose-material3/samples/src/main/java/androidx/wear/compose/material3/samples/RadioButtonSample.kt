/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitRadioButton
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun RadioButton() {
    Column(modifier = Modifier.selectableGroup()) {
        var selectedButton by remember { mutableStateOf(0) }
        // RadioButton uses the Radio selection control by default.
        RadioButton(
            label = {
                Text("Radio button", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                Text("With secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            selected = selectedButton == 0,
            onSelect = { selectedButton = 0 },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Favorite icon"
                )
            },
            enabled = true,
        )
        Spacer(modifier = Modifier.height(4.dp))
        RadioButton(
            label = {
                Text("Radio button", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                Text("With secondary label", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            selected = selectedButton == 1,
            onSelect = { selectedButton = 1 },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Favorite icon"
                )
            },
            enabled = true,
        )
    }
}

@Sampled
@Composable
fun SplitRadioButton() {
    Column(modifier = Modifier.selectableGroup()) {
        var selectedButton by remember { mutableStateOf(0) }
        // SplitRadioButton uses the Radio selection control by default.
        SplitRadioButton(
            label = {
                Text("First Radio Button", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            selected = selectedButton == 0,
            onSelect = { selectedButton = 0 },
            onClick = {
                /* Do something */
            },
            enabled = true,
        )
        Spacer(modifier = Modifier.height(4.dp))
        SplitRadioButton(
            label = {
                Text("Second Radio Button", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            selected = selectedButton == 1,
            onSelect = { selectedButton = 1 },
            onClick = {
                /* Do something */
            },
            enabled = true,
        )
    }
}
