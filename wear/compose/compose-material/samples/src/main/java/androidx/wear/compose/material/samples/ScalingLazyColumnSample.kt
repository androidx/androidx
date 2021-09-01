/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text

@Sampled
@Composable
fun SimpleScalingLazyColumn() {
    ScalingLazyColumn {
        items(20) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
fun SimpleScalingLazyColumnWithContentPadding() {
    ScalingLazyColumn(contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp)) {
        items(20) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
fun ScalingLazyColumnWithHeaders() {
    ScalingLazyColumn {
        item { Spacer(modifier = Modifier.size(20.dp)) }
        item { ListHeader { Text("Header1") } }
        items(5) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item { ListHeader { Text("Header2") } }
        items(5) {
            Chip(
                onClick = { },
                label = { Text("List item ${it + 5}") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
fun ScalingLazyColumnWithHeadersReversed() {
    ScalingLazyColumn(reverseLayout = true) {
        item { Spacer(modifier = Modifier.size(20.dp)) }
        item { ListHeader { Text("Header1") } }
        items(5) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item { ListHeader { Text("Header2") } }
        items(5) {
            Chip(
                onClick = { },
                label = { Text("List item ${it + 5}") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}