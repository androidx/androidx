/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val GridModes =
    listOf(
        "Fixed(1)" to GridCells.Fixed(1),
        "Fixed(2)" to GridCells.Fixed(2),
        "Fixed(3)" to GridCells.Fixed(3),
        "Adaptive(100.dp)" to GridCells.Adaptive(minSize = 100.dp),
    )

@Composable
internal fun LazyGridCrossAxisCellCountDemo() {
    var currentModeIndex by remember { mutableIntStateOf(0) }
    val (label, gridCells) = GridModes[currentModeIndex]

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { currentModeIndex = (currentModeIndex + 1) % GridModes.size }) {
            Text("Mode: $label (tap to switch)")
        }

        LazyVerticalGrid(
            columns = gridCells,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(30) { index ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    if (crossAxisCellCount == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Item $index", style = MaterialTheme.typography.subtitle1)
                            Text("List row (1 col)", style = MaterialTheme.typography.caption)
                        }
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Item $index", style = MaterialTheme.typography.subtitle2)
                            Text(
                                "$crossAxisCellCount cols",
                                style = MaterialTheme.typography.caption,
                            )
                        }
                    }
                }
            }
        }
    }
}
