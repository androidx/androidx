/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.testapp.spatialalignmentusage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SpatialSpacer
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAbsoluteAlignment
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple80
import androidx.xr.compose.unit.DpVolumeSize

class SpatialAlignmentUsageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContent { IntegrationTestsAppTheme { SpatialAlignmentApp() } }
    }

    private val layoutSize = 160.dp
    private val cellPadding = 10.dp
    private val childWidth = 60.dp
    private val childHeight = 40.dp

    @Composable
    private fun SpatialAlignmentApp() {
        Subspace { SpatialContent() }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        SpatialColumn {
            CommonTestPanel(
                size = DpVolumeSize(480.dp, 320.dp, 0.dp),
                title = getString(R.string.spatial_alignment_usage_test_case),
                showBottomBar = true,
                onClickBackArrow = { this@SpatialAlignmentUsageActivity.finish() },
                onClickRecreate = { this@SpatialAlignmentUsageActivity.recreate() },
            ) { padding ->
                @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/481422057
                Column(
                    modifier =
                        Modifier.background(color = Purple80)
                            .fillMaxSize()
                            .padding(padding)
                            .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            "This testcase show cases how SpatialAlignment APIs work in different LayoutDirection.",
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        style = TextStyle(fontSize = 20.sp),
                    )
                }
            }

            SpatialSpacer(modifier = SubspaceModifier.height(20.dp))
            SpatialCurvedRow(curveRadius = 2000.dp) {
                SpatialBiasAlignmentApis(LayoutDirection.Ltr)
                SpatialBiasAlignmentApis(LayoutDirection.Rtl)
            }

            SpatialSpacer(modifier = SubspaceModifier.height(20.dp))
            SpatialCurvedRow(curveRadius = 2000.dp) {
                SpatialBiasAbsoluteAlignmentApis(LayoutDirection.Ltr)
                SpatialBiasAbsoluteAlignmentApis(LayoutDirection.Rtl)
            }
        }
    }

    /**
     * Renders a 2D grid of cell border boxes behind the aligned elements.
     *
     * Why this exists instead of separate border boxes inside [AlignedBox]: Previously, each
     * [AlignedBox] allocated its own dedicated [SpatialPanel] just to draw a black border around
     * each cell. This created over 60 separate [SpatialPanel] instances, which led to excessive
     * panel allocation overhead.
     *
     * Consolidating all border boxes into this single background [SpatialPanel] per section
     * (positioned slightly behind at z = -1.dp) drastically cuts down panel allocations. It uses
     * the shared [layoutSize] and [cellPadding] so each foreground [AlignedBox] lines up precisely
     * within its corresponding border grid cell.
     */
    @Composable
    @SubspaceComposable
    private fun AlignmentBorderGrid(rows: Int, columns: Int) {
        SpatialPanel(
            modifier = SubspaceModifier.offset(z = -1.dp),
            shape = SpatialRoundedCornerShape(CornerSize(1.dp)),
        ) {
            Column {
                repeat(rows) {
                    Row {
                        repeat(columns) {
                            Box(
                                modifier =
                                    Modifier.size(layoutSize)
                                        .padding(cellPadding)
                                        .border(2.dp, Color.Black)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialBiasAlignmentApis(layoutDirection: LayoutDirection) {
        SpatialColumn(modifier = SubspaceModifier.width(600.dp)) {
            SpatialPanel {
                Column(
                    modifier = Modifier.width(500.dp).height(60.dp).background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "Spatial Alignment APIs", style = TextStyle(fontSize = 24.sp))
                    Text(
                        text = "Layout Direction - $layoutDirection",
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
            }
            SpatialSpacer(modifier = SubspaceModifier.height(10.dp))
            SpatialBox {
                AlignmentBorderGrid(rows = 3, columns = 3)
                SpatialColumn {
                    SpatialRow {
                        // TopStart
                        AlignedBox(SpatialAlignment.TopStart, "TopStart", layoutDirection)
                        // TopCenter
                        AlignedBox(SpatialAlignment.TopCenter, "TopCenter", layoutDirection)
                        // TopEnd
                        AlignedBox(SpatialAlignment.TopEnd, "TopEnd", layoutDirection)
                    }
                    SpatialRow {
                        // CenterStart
                        AlignedBox(SpatialAlignment.CenterStart, "CenterStart", layoutDirection)
                        // Center
                        AlignedBox(SpatialAlignment.Center, "Center", layoutDirection)
                        // CenterEnd
                        AlignedBox(SpatialAlignment.CenterEnd, "CenterEnd", layoutDirection)
                    }
                    SpatialRow {
                        // BottomStart
                        AlignedBox(SpatialAlignment.BottomStart, "BottomStart", layoutDirection)
                        // BottomCenter
                        AlignedBox(SpatialAlignment.BottomCenter, "BottomCenter", layoutDirection)
                        // BottomEnd
                        AlignedBox(SpatialAlignment.BottomEnd, "BottomEnd", layoutDirection)
                    }
                }
            }
        }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialBiasAbsoluteAlignmentApis(layoutDirection: LayoutDirection) {
        SpatialColumn(modifier = SubspaceModifier.width(600.dp)) {
            SpatialPanel {
                Column(
                    modifier = Modifier.width(500.dp).height(60.dp).background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Spatial Absolute Alignment APIs",
                        style = TextStyle(fontSize = 24.sp),
                    )
                    Text(
                        text = "Layout Direction - $layoutDirection",
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
            }
            SpatialSpacer(modifier = SubspaceModifier.height(10.dp))
            SpatialBox {
                AlignmentBorderGrid(rows = 3, columns = 2)
                SpatialColumn {
                    SpatialRow {
                        // TopLeft
                        AlignedBox(SpatialAbsoluteAlignment.TopLeft, "TopLeft", layoutDirection)
                        // TopRight
                        AlignedBox(SpatialAbsoluteAlignment.TopRight, "TopRight", layoutDirection)
                    }
                    SpatialRow {
                        // CenterLeft
                        AlignedBox(
                            SpatialAbsoluteAlignment.CenterLeft,
                            "CenterLeft",
                            layoutDirection,
                        )
                        // CenterRight
                        AlignedBox(
                            SpatialAbsoluteAlignment.CenterRight,
                            "CenterRight",
                            layoutDirection,
                        )
                    }
                    SpatialRow {
                        // BottomLeft
                        AlignedBox(
                            SpatialAbsoluteAlignment.BottomLeft,
                            "BottomLeft",
                            layoutDirection,
                        )
                        // BottomRight
                        AlignedBox(
                            SpatialAbsoluteAlignment.BottomRight,
                            "BottomRight",
                            layoutDirection,
                        )
                    }
                }
            }
        }
    }

    @Composable
    @SubspaceComposable
    private fun AlignedBox(
        alignment: SpatialAlignment,
        text: String,
        layoutDirection: LayoutDirection,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            SpatialBox(
                modifier = SubspaceModifier.size(layoutSize).padding(cellPadding),
                alignment = alignment,
            ) {
                SpatialPanel(modifier = SubspaceModifier.padding(3.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.width(childWidth).height(childHeight).background(Color.Gray),
                    ) {
                        Text(text, color = Color.White, style = TextStyle(fontSize = 8.sp))
                    }
                }
            }
        }
    }
}
