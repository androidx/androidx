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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
