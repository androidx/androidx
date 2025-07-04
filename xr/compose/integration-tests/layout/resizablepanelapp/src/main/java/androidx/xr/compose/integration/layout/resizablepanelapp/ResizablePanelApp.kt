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

package androidx.xr.compose.integration.layout.resizablepanelapp

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.integration.common.AnotherActivity
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.DpVolumeSize

class ResizablePanelApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        val transition =
            rememberInfiniteTransition("dynamicValue")
                .animateFloat(
                    initialValue = 100f,
                    targetValue = 350f,
                    animationSpec =
                        infiniteRepeatable(tween(30000), repeatMode = RepeatMode.Reverse),
                    label = "transition",
                )

        val panelWidth = (transition.value).dp
        val panelHeight = (transition.value).dp
        // Variables need to be remembered to persist callback changes within onSizeChange during
        // recomposition.
        var onSizeChangeWidth by remember { mutableStateOf(200.dp) }
        var onSizeChangeHeight by remember { mutableStateOf(200.dp) }
        SpatialColumn(modifier = SubspaceModifier.testTag("PanelGridColumn")) {
            SpatialRow(
                modifier = SubspaceModifier.fillMaxWidth(),
                alignment = SpatialAlignment.BottomCenter,
            ) {
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("LeftColumn")
                ) {
                    if (
                        transition.value >= 150f
                    ) { // After approximately 5 seconds the delayed panel will appear
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                        ) {
                            PanelContent(
                                "[NOT RESIZABLE] Left Column Rendering Delayed Panel: ${transition.value}"
                            )
                        }
                        SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    }
                    SpatialPanel(
                        modifier = SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                    ) {
                        PanelContent("[NOT RESIZABLE] Left Column Panel: ${transition.value}")
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(200.dp)
                                .resizable(
                                    maintainAspectRatio = true,
                                    minimumSize = DpVolumeSize(100.dp, 100.dp, 100.dp),
                                    maximumSize = DpVolumeSize(500.dp, 500.dp, 500.dp),
                                )
                    ) {
                        PanelContent("[RESIZABLE] Left Column Panel: ${transition.value}")
                    }
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(600.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("MiddleColumn")
                ) {
                    val density = LocalDensity.current
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(onSizeChangeWidth)
                                .height(onSizeChangeHeight)
                                .fillMaxWidth()
                                .resizable { newSize ->
                                    with(density) {
                                        onSizeChangeHeight = newSize.height.toDp()
                                        onSizeChangeWidth = newSize.width.toDp()
                                    }
                                    true // Return true to indicate that the resize event was
                                    // handled by the app.
                                }
                    ) {
                        PanelContent(
                            "[RESIZABLE WITH ON_SIZE_CHANGE LISTENER] Middle Column Panel: ${transition.value}"
                        )
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialMainPanel(
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(panelHeight)
                                .resizable(true)
                    )
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("RightColumn")
                ) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                    ) {
                        PanelContent("[NOT RESIZABLE] Right Column Panel: ${transition.value}")
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialActivityPanel(
                        intent = Intent(this@ResizablePanelApp, AnotherActivity::class.java),
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(200.dp)
                                .resizable(true)
                                .testTag("ActivityPanel"),
                    )
                }
            }
        }
    }

    @UiComposable
    @Composable
    fun PanelContent(vararg text: String) {
        Box(
            modifier = Modifier.background(Color.LightGray).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                for (item in text) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
        }
    }
}
