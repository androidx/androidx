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

package androidx.xr.compose.integration.layout.movableandresizablepanelapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
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
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import kotlin.concurrent.fixedRateTimer

class MovableAndResizablePanelApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        var counter by remember { mutableIntStateOf(0) }
        // DynamicValue goes from 1 to 101, then back to 1, then it repeats.
        var dynamicValue by remember { mutableIntStateOf(1) }
        // TODO(b/352419050): Update test app to use built in animation.
        remember {
            fixedRateTimer(period = 3000, daemon = true) {
                counter += 1
                dynamicValue =
                    if (counter / 100 % 2 == 0) {
                        1 + (counter % 100)
                    } else {
                        101 - (counter % 100)
                    }
            }
        }
        val panelWidth = (dynamicValue * 40).dp
        SpatialColumn(SubspaceModifier.testTag("PanelGridColumn")) {
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
                    if (counter >= 5) {
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                        ) {
                            PanelContent(
                                "[STANDARD] Left Column Rendering Delayed Panel: $dynamicValue"
                            )
                        }
                        SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    }
                    SpatialPanel(
                        modifier = SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                    ) {
                        PanelContent("[STANDARD] Left Column Panel: $dynamicValue")
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(200.dp)
                                .resizable(maintainAspectRatio = true)
                    ) {
                        PanelContent("[RESIZABLE] Left Column Panel: $dynamicValue")
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(200.dp)
                                .movable()
                    ) {
                        PanelContent("[MOVABLE] Left Column Panel: $dynamicValue")
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(200.dp)
                                .resizable(maintainAspectRatio = true)
                                .movable()
                    ) {
                        PanelContent("[MOVABLE & RESIZABLE] Left Column Panel: $dynamicValue")
                    }
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(600.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("MiddleColumn")
                ) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                    ) {
                        PanelContent("[STANDARD] Middle Column Panel: $dynamicValue")
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialMainPanel(
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(200.dp)
                                .resizable()
                                .movable()
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
                        PanelContent("[STANDARD] Right Column Panel: $dynamicValue")
                    }
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    SpatialActivityPanel(
                        intent =
                            Intent(this@MovableAndResizablePanelApp, AnotherActivity::class.java),
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(panelWidth)
                                .height(200.dp)
                                .resizable()
                                .movable()
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
