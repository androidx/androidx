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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.SelectableChip
import androidx.wear.compose.material.SplitSelectableChip
import androidx.wear.compose.material.Text

@Sampled
@Composable
fun SelectableChipWithRadioButton() {
    var selectedRadioIndex by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SelectableChip(
            modifier = Modifier.fillMaxWidth(),
            selected = selectedRadioIndex == 0,
            onClick = { selectedRadioIndex = 0 },
            label = {
                // The primary label should have a maximum 3 lines of text
                Text("Primary label", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                // and the secondary label should have max 2 lines of text.
                Text("Secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            appIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                    contentDescription = "airplane",
                    modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                )
            },
            enabled = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SelectableChip(
            modifier = Modifier.fillMaxWidth(),
            selected = selectedRadioIndex == 1,
            onClick = { selectedRadioIndex = 1 },
            label = {
                // The primary label should have a maximum 3 lines of text
                Text("Alternative label", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                // and the secondary label should have max 2 lines of text.
                Text("Alternative secondary", maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            appIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                    contentDescription = "airplane",
                    modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                )
            },
            enabled = true,
        )
    }
}

@Sampled
@Composable
fun SplitSelectableChipWithRadioButton() {
    var selectedRadioIndex by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SplitSelectableChip(
            modifier = Modifier.fillMaxWidth(),
            selected = selectedRadioIndex == 0,
            onSelectionClick = { selectedRadioIndex = 0 },
            label = {
                // The primary label should have a maximum 3 lines of text
                Text("Primary label", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                // and the secondary label should have max 2 lines of text.
                Text("Secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            onContainerClick = {
                /* Do something */
            },
            enabled = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SplitSelectableChip(
            modifier = Modifier.fillMaxWidth(),
            selected = selectedRadioIndex == 1,
            onSelectionClick = { selectedRadioIndex = 1 },
            label = {
                // The primary label should have a maximum 3 lines of text
                Text("Alternative label", maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            secondaryLabel = {
                // and the secondary label should have max 2 lines of text.
                Text("Alternative secondary", maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            onContainerClick = {
                /* Do something */
            },
            enabled = true,
        )
    }
}
