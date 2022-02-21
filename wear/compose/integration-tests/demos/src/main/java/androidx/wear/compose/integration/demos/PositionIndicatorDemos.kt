/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState

@Composable
fun HideWhenFullDemo() {
    var smallList by remember { mutableStateOf(true) }
    val listState = rememberScrollState()

    Scaffold(
        positionIndicator = { PositionIndicator(scrollState = listState) }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .verticalScroll(listState)
        ) {
            repeat(if (smallList) 3 else 10) {
                Chip(
                    onClick = { smallList = !smallList },
                    label = { Text("Item #$it") }
                )
            }
        }
    }
}

@Composable
fun HideWhenFullSLCDemo() {
    var smallList by remember { mutableStateOf(true) }
    val listState = rememberScalingLazyListState()

    Scaffold(
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = listState,
                modifier = Modifier
            )
        }
    ) {
        ScalingLazyColumn(
            state = listState,
            autoCentering = true
        ) {
            items(
                count = if (smallList) 3 else 10
            ) {
                Chip(
                    onClick = { smallList = !smallList },
                    label = { Text("Item #$it") }
                )
            }
        }
    }
}
