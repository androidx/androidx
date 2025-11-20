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

package androidx.xr.compose.testapp.resizablepanel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.common.AnotherActivity
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.compose.unit.DpVolumeSize

class ResizablePanel : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainPanelContent()
            Subspace { PanelRow() }
        }
    }

    @Composable
    @SubspaceComposable
    private fun PanelRow() {
        val panelSize = SubspaceModifier.size(DpVolumeSize(250.dp, 250.dp, 0.dp)).padding(20.dp)
        var onSizeChangeWidth by remember { mutableStateOf(250.dp) }
        var onSizeChangeHeight by remember { mutableStateOf(250.dp) }
        val transition =
            rememberInfiniteTransition("dynamicValue")
                .animateFloat(
                    initialValue = 250f,
                    targetValue = 350f,
                    animationSpec =
                        infiniteRepeatable(tween(10000), repeatMode = RepeatMode.Reverse),
                    label = "transition",
                )
        SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(20.dp)) {
            SpatialRow(
                modifier = SubspaceModifier.fillMaxWidth(),
                alignment = SpatialAlignment.Center,
                horizontalArrangement = SpatialArrangement.SpaceEvenly,
            ) {
                SpatialColumn {
                    // Non-resizable panel with delayed rendering
                    if (
                        transition.value >= 150f
                    ) { // After approximately 5 seconds the delayed panel will appear
                        SpatialPanel(modifier = panelSize) {
                            PanelContent("NON-RESIZABLE", "delayed rendering")
                        }
                    }

                    // Fixed Non-resizable panel
                    SpatialPanel(modifier = panelSize) { PanelContent("FIXED", "non-resizable") }

                    // Resizable panel
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.height(250.dp)
                                .padding(20.dp)
                                .width((transition.value).dp),
                        resizePolicy =
                            ResizePolicy(
                                shouldMaintainAspectRatio = true,
                                minimumSize = DpVolumeSize(100.dp, 100.dp, 100.dp),
                                maximumSize = DpVolumeSize(500.dp, 500.dp, 500.dp),
                            ),
                    ) {
                        PanelContent("RESIZABLE", "Max: 500dp x 500dp", "Min: 100dp x 100dp")
                    }
                }

                SpatialColumn {
                    // Resizable with onResize change
                    val density = LocalDensity.current
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.padding(20.dp)
                                .width(onSizeChangeWidth)
                                .height(onSizeChangeHeight)
                                .fillMaxWidth(),
                        resizePolicy =
                            ResizePolicy(
                                onSizeChange = { newSize ->
                                    with(density) {
                                        onSizeChangeHeight = newSize.height.toDp()
                                        onSizeChangeWidth = newSize.width.toDp()
                                    }
                                    true // Return true to indicate that the resize event was
                                    // handled by the app.
                                }
                            ),
                    ) {
                        PanelContent("RESIZABLE", "with", "onResizeChange listener")
                    }
                }

                SpatialColumn {
                    // Non-resizable panel
                    SpatialPanel(modifier = panelSize) { PanelContent("NON-RESIZABLE") }

                    // Activity Panel
                    val intent = Intent(this@ResizablePanel, AnotherActivity::class.java)
                    intent.putExtra("TITLE", "Activity Panel")
                    intent.putExtra("INSIDE_TEXT", "Resizable Activity Panel")
                    SpatialActivityPanel(
                        intent = intent,
                        modifier =
                            panelSize
                                .offset(x = 120.dp)
                                .width(300.dp)
                                .height(300.dp)
                                .testTag("ActivityPanel"),
                        dragPolicy = MovePolicy(),
                        resizePolicy = ResizePolicy(true),
                    )
                }
            }

            // MainPanel
            SpatialRow(modifier = SubspaceModifier.fillMaxWidth()) {
                SpatialMainPanel(
                    modifier = SubspaceModifier.width(640.dp).height(480.dp),
                    resizePolicy = ResizePolicy(true),
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainPanelContent() {
        CommonTestScaffold(
            title = "Resizable Panel Test case",
            bottomBarText = "Bottom Bar",
            onClickBackArrow = { this@ResizablePanel.finish() },
            showBottomBar = false,
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().background(PurpleGrey80).padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Row {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Resizable MainPanel",
                            fontSize = 32.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    @UiComposable
    @Composable
    fun PanelContent(vararg text: String) {
        Box(
            modifier = Modifier.background(PurpleGrey80).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                for (item in text) {
                    Text(text = item, fontSize = 20.sp, color = Color.White)
                }
            }
        }
    }
}
