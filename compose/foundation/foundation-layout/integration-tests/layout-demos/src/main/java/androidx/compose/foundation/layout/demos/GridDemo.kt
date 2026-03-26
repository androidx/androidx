/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalGridApi::class)

package androidx.compose.foundation.layout.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridFlow
import androidx.compose.foundation.layout.GridScope
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GridDemo() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        MixedSizingDemo()
        Spacer(Modifier.height(32.dp))
        FlexibleSizingDemo()
        Spacer(Modifier.height(32.dp))
        ContentBasedSizingDemo()
        Spacer(Modifier.height(32.dp))
        NegativeIndicesDemo()
        Spacer(Modifier.height(32.dp))
        GapsAndContentDemo()
        Spacer(Modifier.height(32.dp))
        AlignmentDemo()
        Spacer(Modifier.height(32.dp))
        AutoPlacementDemo()
        Spacer(Modifier.height(32.dp))
        InfiniteConstraintsDemo()
        Spacer(Modifier.height(32.dp))
        MinContentSafetyDemo()
        Spacer(Modifier.height(32.dp))
        AutoSizingDemo()
        Spacer(Modifier.height(32.dp))
        ResponsiveConstraintsDemo()
        Spacer(Modifier.height(32.dp))
        AspectRatioDemo()
        Spacer(Modifier.height(32.dp))
        LazyListInGridDemo()
    }
}

@Composable
private fun AutoPlacementDemo() {
    DemoHeader("Auto Placement & Flow")

    Text(
        "1. Flow = Row (Fixed Cols, Implicit Rows)",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Grid(
        config = {
            repeat(3) { column(GridTrackSize.Fixed(80.dp)) } // 3 Explicit Columns
            gap(4.dp)
            flow = GridFlow.Row // Default
        },
        modifier = Modifier.demoContainer(borderColor = Color.Blue),
    ) {
        // 1. Simple auto items
        repeat(3) { index ->
            GridDemoItem(text = "${index + 1}", color = Color.Cyan, measureSize = false)
        }

        // 2. Auto item with Span (Takes 2 spots)
        GridDemoItem(text = "Span 2", columnSpan = 2, color = Color.Magenta, measureSize = false)

        // 3. More auto items (Wrapping to next row)
        repeat(2) { index ->
            GridDemoItem(text = "${index + 5}", color = Color.Cyan, measureSize = false)
        }
    }

    Spacer(Modifier.height(24.dp))

    Text(
        "2. Flow = Column (Fixed Rows, Implicit Cols)",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    // Scrollable container to allow implicit columns to grow horizontally
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Grid(
            config = {
                flow = GridFlow.Column
                repeat(3) { row(50.dp) } // 3 Explicit Rows
                gap(4.dp)
            },
            modifier = Modifier.border(1.dp, Color.Magenta.copy(alpha = 0.5f)).padding(8.dp),
        ) {
            repeat(10) { index ->
                GridDemoItem(
                    text = "${index + 1}",
                    color = if (index % 2 == 0) Color.Yellow else Color.Green,
                    measureSize = false,
                    // Explicit size helps visualization in 'Auto' implicit tracks
                    modifier = Modifier.size(60.dp, 40.dp),
                )
            }
        }
    }
}

@Composable
private fun MixedSizingDemo() {
    DemoHeader("Mixed Sizing: Fixed vs Fraction vs Flex")
    Grid(
        config = {
            columns(
                GridTrackSize.Fixed(100.dp),
                GridTrackSize.Percentage(0.3f),
                GridTrackSize.Flex(1.fr),
            )
            row(100.dp)
        },
        modifier = Modifier.demoContainer(),
    ) {
        GridDemoItem(text = "Fixed\n100dp", color = Color.Red, row = 1, column = 1)
        GridDemoItem(text = "Fraction\n30% of Total", color = Color.Blue, row = 1, column = 2)
        GridDemoItem(text = "Flex\nRest of Space", color = Color.Green, row = 1, column = 3)
    }
}

@Composable
private fun FlexibleSizingDemo() {
    DemoHeader("Flexible (Fr) Sizing")
    Grid(
        config = {
            column(80.dp)
            column(1.fr)
            column(2.fr)
            row(1.fr)
            row(60.dp)
        },
        modifier = Modifier.height(200.dp).demoContainer(borderColor = Color.Magenta),
    ) {
        val rowLabels = listOf("Flex 1.fr", "60dp")
        val colLabels = listOf("Fixed 80dp", "Flex 1.fr", "Flex 2.fr")

        rowLabels.forEachIndexed { rowIndex, rowLabel ->
            colLabels.forEachIndexed { colIndex, colLabel ->
                GridDemoItem(
                    text = "H: $rowLabel\nW: $colLabel",
                    row = rowIndex + 1,
                    column = colIndex + 1,
                )
            }
        }
    }
}

@Composable
fun NegativeIndicesDemo() {
    DemoHeader("Negative Indices")
    Grid(
        config = {
            repeat(3) { column(60.dp) }
            repeat(3) { row(60.dp) }
            gap(4.dp)
        },
        modifier = Modifier.border(1.dp, Color.Gray),
    ) {
        GridDemoItem(text = "TL", row = 1, column = 1, color = Color.Red)
        GridDemoItem("TR", row = 1, column = -1, color = Color.Blue)
        GridDemoItem("BL", row = -1, column = 1, color = Color.Green)
        GridDemoItem("BR", row = -1, column = -1, color = Color.Yellow)
        GridDemoItem("Center", row = 2, column = 2, color = Color.Gray)
    }
}

@Composable
private fun ContentBasedSizingDemo() {
    DemoHeader("Intrinsic Sizing (Min vs Max Content)")
    Grid(
        config = {
            column(GridTrackSize.MinContent)
            column(GridTrackSize.MaxContent)
            column(GridTrackSize.Flex(1.fr))
            column(GridTrackSize.Auto)
            row(GridTrackSize.Auto)
            gap(8.dp)
        },
        modifier = Modifier.demoContainer(borderColor = Color.Black),
    ) {
        GridDemoItem(text = "Min Content\nWraps", row = 1, column = 1, color = Color.Red)
        GridDemoItem(text = "Max Content Expands", row = 1, column = 2, color = Color.Blue)
        GridDemoItem(text = "Flex Fills\nRemainder", row = 1, column = 3, color = Color.Green)
        GridDemoItem(text = "Auto\nContent", row = 1, column = 4, color = Color.Yellow)
    }
}

@Composable
private fun GapsAndContentDemo() {
    DemoHeader("Gaps & Content Sizing")
    Grid(
        config = {
            column(GridTrackSize.Fixed(100.dp))
            column(GridTrackSize.Flex(1.fr))
            column(GridTrackSize.Fixed(200.dp))
            row(GridTrackSize.Fixed(60.dp))
            row(GridTrackSize.Fixed(100.dp))
            gap(row = 12.dp, column = 6.dp)
        },
        modifier = Modifier.demoContainer(borderColor = Color.Cyan),
    ) {
        repeat(6) {
            val row = (it / 3) + 1
            val col = (it % 3) + 1
            GridDemoItem(text = "Item ${it + 1}", measureSize = false, row = row, column = col)
        }
    }
}

@Composable
private fun AlignmentDemo() {
    DemoHeader("Cell Content Alignment")
    Grid(
        config = {
            repeat(3) { column(GridTrackSize.Fixed(100.dp)) }
            repeat(3) { row(GridTrackSize.Fixed(100.dp)) }
            gap(4.dp)
        },
        modifier = Modifier.demoContainer(borderColor = Color.Red),
    ) {
        val alignments =
            listOf(
                Alignment.TopStart to "TopStart",
                Alignment.TopCenter to "TopCenter",
                Alignment.TopEnd to "TopEnd",
                Alignment.CenterStart to "CenterStart",
                Alignment.Center to "Center",
                Alignment.CenterEnd to "CenterEnd",
                Alignment.BottomStart to "BottomStart",
                Alignment.BottomCenter to "BottomCenter",
                Alignment.BottomEnd to "BottomEnd",
            )

        alignments.forEachIndexed { index, (alignment, name) ->
            val row = (index / 3) + 1
            val col = (index % 3) + 1

            Box(
                Modifier.gridItem(row, col)
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .border(1.dp, Color.DarkGray.copy(alpha = 0.1f))
            )

            Box(
                Modifier.gridItem(row, col, alignment = alignment)
                    .size(60.dp, 40.dp)
                    .background(Color.Yellow.copy(alpha = 0.7f))
                    .border(1.dp, Color.Red),
                contentAlignment = Alignment.Center,
            ) {
                Text(name, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun InfiniteConstraintsDemo() {
    DemoHeader("Infinite Constraints")
    Text(
        "Percentage tracks fall back to Auto sizing when placed in an infinite container.",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 4.dp),
    )

    Row(
        Modifier.fillMaxWidth()
            .border(1.dp, Color.Gray)
            .padding(8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Grid(
            config = {
                column(GridTrackSize.Percentage(0.5f))
                row(GridTrackSize.Auto)
                gap(4.dp)
            },
            modifier = Modifier.border(1.dp, Color.Blue),
        ) {
            GridDemoItem(
                text = "I am 150dp wide\n(Percentage -> Auto)",
                modifier = Modifier.width(150.dp),
                color = Color.Cyan,
            )
        }
    }
}

@Composable
private fun MinContentSafetyDemo() {
    DemoHeader("Min-Content Flex")
    Text(
        "Flex tracks implement minmax(min-content, 1fr).",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Box(Modifier.width(50.dp).border(2.dp, Color.Red).padding(2.dp)) {
        Grid(
            config = {
                column(GridTrackSize.Flex(1.fr))
                row(GridTrackSize.Auto)
            },
            modifier = Modifier.border(1.dp, Color.Green),
        ) {
            GridDemoItem(
                text = "Min 120dp",
                modifier = Modifier.width(120.dp),
                color = Color.Magenta,
            )
        }
    }
}

@Composable
private fun AutoSizingDemo() {
    DemoHeader("Auto Track Sizing")
    Text(
        "Auto tracks behave as minmax(min-content, max-content).\n" +
            "They wrap content when constrained, but expand when space is available.",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    // 1. Ample Space: Auto expands to fit the text in one line (MaxContent)
    Text("1. Ample Space (200dp) -> MaxContent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Box(Modifier.width(200.dp).border(1.dp, Color.Gray).padding(4.dp)) {
        Grid(config = { column(GridTrackSize.Auto) }) {
            GridDemoItem(text = "I am Long Text that behaves like MaxContent", color = Color.Cyan)
        }
    }

    Spacer(Modifier.height(8.dp))

    // 2. Constrained Space: Auto shrinks and wraps text (approaching MinContent)
    Text("2. Tight Space (100dp) -> Wraps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Box(Modifier.width(100.dp).border(1.dp, Color.Red).padding(4.dp)) {
        Grid(config = { column(GridTrackSize.Auto) }) {
            GridDemoItem(text = "I am Long Text that behaves like MinContent", color = Color.Yellow)
        }
    }

    Spacer(Modifier.height(8.dp))

    // 3. Comparison: MinContent vs Auto
    Text("3. MinContent vs Auto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Grid(
        config = {
            column(GridTrackSize.MinContent) // Will crush text to longest word
            column(GridTrackSize.Auto) // Will wrap comfortably
            gap(8.dp)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Blue).padding(4.dp),
    ) {
        GridDemoItem(text = "MinContent crushes me", color = Color.Red, row = 1, column = 1)
        GridDemoItem(text = "Auto wraps me nicely", color = Color.Green, row = 1, column = 2)
    }
}

@Composable
private fun ResponsiveConstraintsDemo() {
    DemoHeader("Responsive Layout (Constraints)")

    Text(
        "Resize the slider to change the container width.\n" +
            "The Grid config reads 'constraints.maxWidth' to determine column count.",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    var containerWidth by remember { mutableStateOf(300.dp) }

    Column(Modifier.fillMaxWidth().border(1.dp, Color.LightGray).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Width: ${containerWidth.value.toInt()}dp",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = containerWidth.value,
                onValueChange = { containerWidth = it.dp },
                valueRange = 200f..500f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
        }

        // 2. Responsive Container
        Box(
            modifier =
                Modifier.width(containerWidth)
                    .border(2.dp, Color.Blue.copy(alpha = 0.5f))
                    .padding(4.dp)
        ) {
            Grid(
                config = {
                    // Accessing 'constraints' from GridConfigurationScope.
                    val maxWidthDp = constraints.maxWidth.toDp()

                    val columnCount =
                        when {
                            maxWidthDp < 300.dp -> 2 // Compact
                            maxWidthDp < 400.dp -> 3 // Medium
                            else -> 4 // Expanded
                        }

                    // Define columns based on calculation
                    repeat(columnCount) { column(1.fr) }
                    gap(4.dp)
                }
            ) {
                // Populate plenty of items to show the flow
                repeat(8) {
                    val color =
                        when {
                            containerWidth < 300.dp -> Color.Red // Compact Theme
                            containerWidth < 400.dp -> Color.Yellow // Medium Theme
                            else -> Color.Green // Expanded Theme
                        }
                    GridDemoItem(text = "${it + 1}", color = color, measureSize = false)
                }
            }
        }
    }
}

@Composable
private fun AspectRatioDemo() {
    DemoHeader("Aspect Ratio in Flex Tracks")
    Text(
        "Aspect ratio modifiers should be respected within flexible tracks without exploding grid bounds.",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    Grid(
        config = {
            column(160.dp)
            column(1.fr)
            row(90.dp)
            row(1.fr)
            gap(8.dp)
        },
        modifier = Modifier.height(300.dp).fillMaxWidth().border(1.dp, Color.Gray).padding(8.dp),
    ) {
        val modifier = Modifier.aspectRatio(16f / 9f).fillMaxSize()
        GridDemoItem("Fixed / Fixed", modifier = modifier, row = 1, column = 1, color = Color.Red)
        GridDemoItem("Flex / Fixed", modifier = modifier, row = 1, column = 2, color = Color.Blue)
        GridDemoItem("Fixed / Flex", modifier = modifier, row = 2, column = 1, color = Color.Green)
        GridDemoItem("Flex / Flex", modifier = modifier, row = 2, column = 2, color = Color.Yellow)
    }
}

@Composable
private fun LazyListInGridDemo() {
    DemoHeader("Lazy List in Flex Track")
    Text(
        "Flex tracks (1.fr) allowing SubcomposeLayouts like LazyColumn to be safely placed inside them.",
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    Grid(
        config = {
            column(minmax(0.dp, 1.fr))
            row(GridTrackSize.Auto)
            row(minmax(0.dp, 1.fr))
            gap(8.dp)
        },
        // We provide a fixed height so the 1.fr row has a finite boundary
        // within the vertically scrolling parent Column.
        modifier = Modifier.height(300.dp).demoContainer(borderColor = Color.Green),
    ) {
        // Row 1: Fixed/Auto header
        GridDemoItem(text = "Header (Auto Row)", color = Color.Yellow)

        // Row 2: LazyColumn in a minmax track
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.3f))
                    .border(1.dp, Color.Gray)
        ) {
            items(50) { index ->
                Text(
                    text = "Lazy Item #${index + 1}",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun DemoHeader(text: String) =
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp),
    )

@Composable
private fun GridScope.GridDemoItem(
    text: String,
    modifier: Modifier = Modifier,
    row: Int? = null,
    column: Int? = null,
    rowSpan: Int = 1,
    columnSpan: Int = 1,
    color: Color = Color.Green,
    measureSize: Boolean = true,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    var finalModifier = modifier.fillMaxSize()

    if (row != null && column != null) {
        finalModifier = finalModifier.gridItem(row, column, rowSpan, columnSpan)
    } else if (rowSpan > 1 || columnSpan > 1) {
        finalModifier = finalModifier.gridItem(rowSpan = rowSpan, columnSpan = columnSpan)
    }

    Box(
        finalModifier
            .onSizeChanged { size = it }
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.5f))
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, fontSize = 10.sp, textAlign = TextAlign.Center)
            if (measureSize) {
                val w = with(density) { size.width.toDp().value.toInt() }
                val h = with(density) { size.height.toDp().value.toInt() }
                Text(text = "$w x $h", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun Modifier.demoContainer(borderColor: Color = Color.Black) =
    this.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.5f)).padding(8.dp)
