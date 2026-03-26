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

package androidx.compose.foundation.layout.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridFlow
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.GridTrackSize.Companion.Fixed
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.rows
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
@Composable
@OptIn(ExperimentalGridApi::class)
fun SimpleGrid() {
    Grid(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        config = {
            // Define 2 Columns:
            // Col 1: Fixed navigation sidebar width
            column(100.dp)
            // Col 2: Takes remaining space
            column(1.fr)

            // Define 3 Rows:
            // Row 1: Header (sized to content)
            row(GridTrackSize.Auto)
            // Row 2: Main Content (takes remaining height)
            row(1.fr)
            // Row 3: Footer (fixed height)
            row(60.dp)

            // Add 8dp space between all cells
            gap(8.dp)
        },
    ) {
        // 1. Header: Spans across both columns at the top
        Box(
            modifier =
                Modifier.gridItem(row = 1, column = 1, columnSpan = 2)
                    .background(Color.Blue)
                    .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Header", color = Color.White)
        }

        // 2. Sidebar: Left column, middle row
        Box(
            modifier = Modifier.gridItem(row = 2, column = 1).background(Color.Green).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Nav", color = Color.Black)
        }

        // 3. Main Content: Right column, middle row
        Box(
            modifier =
                Modifier.gridItem(row = 2, column = 2).background(Color.LightGray).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Main Content Area")
        }

        // 4. Footer: Spans both columns at the bottom
        Box(
            modifier =
                Modifier.gridItem(row = 3, columnSpan = 2) // Column defaults to 1 if unspecified
                    .background(Color.Magenta)
                    .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Footer", color = Color.White)
        }
    }
}

@Sampled
@Composable
@OptIn(ExperimentalGridApi::class)
fun GridWithSpanningItems() {
    Grid(
        config = {
            columns(Fixed(60.dp), Fixed(60.dp), Fixed(60.dp))
            rows(Fixed(40.dp), Fixed(40.dp))
            gap(4.dp)
        },
        modifier = Modifier.background(Color.LightGray),
    ) {
        Text("1x1", Modifier.gridItem(row = 1, column = 1).background(Color.White))
        Text(
            "1x2 span col",
            Modifier.gridItem(row = 1, column = 2, columnSpan = 2).background(Color.Cyan),
        )
        Text(
            "2x1 span row",
            Modifier.gridItem(row = 1, column = 1, rowSpan = 2).background(Color.Yellow),
        )
        Text("2x2 span all", Modifier.gridItem(rows = 1..2, columns = 2..3).background(Color.Green))
    }
}

@Sampled
@Composable
@OptIn(ExperimentalGridApi::class)
fun GridWithAutoPlacement() {
    Grid(
        config = {
            columns(Fixed(80.dp), Fixed(80.dp)) // Explicitly 2 columns
            // Rows are implicit
            flow = GridFlow.Row // Default
            gap(4.dp)
        }
    ) {
        // These items will fill row by row
        repeat(6) { index ->
            Text("Item $index", Modifier.background(Color(index * 40, 255 - index * 40, 128)))
        }
    }
}

@Sampled
@Composable
@OptIn(ExperimentalGridApi::class)
fun GridConfigurationDslSample() {
    Grid(
        config = {
            // This defines the first column
            column(100.dp)
            // This defines the second column
            column(1.fr)

            // This defines the first row
            row(50.dp)
            // The order is important. additional calls to row() or column() append tracks.

            gap(all = 8.dp) // Set both row and column gaps
            columnGap(16.dp) // Override column gap
        }
    ) {
        Box(
            modifier = Modifier.gridItem(row = 1, column = 1).background(Color.Blue).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Row: 1, Column: 1", color = Color.White)
        }

        Box(
            modifier = Modifier.gridItem(row = 1, column = 1).background(Color.Blue).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Row: 1, Column: 2", color = Color.White)
        }
    }
}

@Sampled
@Composable
@OptIn(ExperimentalGridApi::class)
fun GridWithConstraints() {
    Grid(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        config = {
            val maxWidthDp = constraints.maxWidth.toDp()
            if (maxWidthDp < 600.dp) {
                // Compact Layout: 2 Columns
                repeat(2) { column(1.fr) }
            } else {
                // Expanded Layout: 4 Columns
                repeat(4) { column(1.fr) }
            }

            // Rows are auto-generated based on content
            gap(8.dp)
        },
    ) {
        repeat(8) { index ->
            Box(
                modifier = Modifier.background(Color.Cyan).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Item $index")
            }
        }
    }
}

@Sampled
@Composable
@OptIn(ExperimentalGridApi::class)
fun GridWithLazyList() {
    Grid(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        config = {
            column(120.dp) // Sidebar width
            column(minmax(0.dp, 1.fr)) // Content width

            row(60.dp) // Header height
            // IMPORTANT:
            // Flex track '1.fr' queries child intrinsic sizes. Since SubcomposeLayouts
            // (like LazyColumn) crash on intrinsic queries, we MUST use 'GridTrackSize.MinMax' with
            // an explicit minimum size (0.dp) to bypass the measurement crash safely!
            row(minmax(0.dp, 1.fr))

            gap(16.dp)
        },
    ) {
        // Top Header spanning both columns
        Box(
            Modifier.gridItem(row = 1, column = 1, columnSpan = 2)
                .background(Color.DarkGray)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("App Header", color = Color.White)
        }

        // Left Sidebar
        Box(
            Modifier.gridItem(row = 2, column = 1).background(Color.LightGray).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Navigation")
        }

        // Scrollable LazyColumn safely constrained in the flex area
        LazyColumn(
            modifier = Modifier.gridItem(row = 2, column = 2).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(50) { index ->
                Box(Modifier.fillMaxWidth().background(Color(0xFFE0E0FF)).padding(16.dp)) {
                    Text("Scrollable Content #$index")
                }
            }
        }
    }
}
