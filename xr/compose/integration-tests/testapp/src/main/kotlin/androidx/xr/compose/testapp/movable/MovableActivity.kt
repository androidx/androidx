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

package androidx.xr.compose.testapp.movable

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.common.AnotherActivity
import androidx.xr.compose.testapp.ui.components.ColumnWithCenterText
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.math.Quaternion

class MovableActivity : ComponentActivity() {
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
                        infiniteRepeatable(
                            tween(30000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "transition",
                )
        rememberInfiniteTransition("movingValue")
            .animateFloat(
                initialValue = 0f,
                targetValue = 350f,
                animationSpec =
                    infiniteRepeatable(
                        tween(30000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "moving",
            )
        // TODO(b/367745125): Restore growing panel animation when we can fix glitches.
        val panelWidth = 200.dp
        // Variables need to be remembered to persist callback changes within onPoseChange during
        // recomposition.
        var xValueMovable by remember { mutableStateOf(0.dp) }
        var yValueMovable by remember { mutableStateOf(0.dp) }
        var zValueMovable by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        var rotateValueMovable by remember { mutableStateOf(Quaternion.Identity) }
        SpatialColumn(SubspaceModifier.testTag("PanelGridSpatialColumn")) {
            SpatialRow(
                modifier = SubspaceModifier.fillMaxWidth(),
                alignment = SpatialAlignment.BottomCenter,
            ) {
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("LeftColumn"),
                    verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                ) {
                    if (
                        transition.value >= 150f
                    ) { // After approximately 5 seconds the delayed panel will appear
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                        ) {
                            PanelContent("[NOT MOVABLE] Delayed Panel")
                        }
                    }
                    SpatialPanel(
                        modifier = SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                    ) {
                        PanelContent("[NOT MOVABLE]")
                    }
                    SpatialPanel(
                        modifier = SubspaceModifier.width(200.dp).height(200.dp),
                        dragPolicy = MovePolicy(),
                    ) {
                        PanelContent("[MOVABLE]")
                    }
                    SpatialPanel(
                        modifier = SubspaceModifier.width(200.dp).height(200.dp),
                        dragPolicy = MovePolicy(),
                    ) {
                        PanelContent("[MOVABLE]")
                    }
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(600.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("MiddleColumn"),
                    verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                ) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                    ) {
                        PanelContent("[NOT MOVABLE]")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.offset(xValueMovable, yValueMovable, zValueMovable)
                                .width(panelWidth)
                                .height(200.dp)
                                .rotate(rotateValueMovable),
                        dragPolicy =
                            MovePolicy(
                                onMove = { poseChangeEvent ->
                                    with(density) {
                                        xValueMovable = poseChangeEvent.pose.translation.x.toDp()
                                        yValueMovable = poseChangeEvent.pose.translation.y.toDp()
                                        zValueMovable = poseChangeEvent.pose.translation.z.toDp()
                                        rotateValueMovable = poseChangeEvent.pose.rotation
                                        // This true is to indicate that the callback will handle
                                        // the moving of the panel.
                                        true
                                    }
                                }
                            ),
                    ) {
                        PanelContent("[MOVABLE WITH CUSTOM LISTENER]")
                    }

                    CommonTestPanel(
                        size = DpVolumeSize(640.dp, 480.dp, 0.dp),
                        title = getString(R.string.movable_panels_test),
                        showBottomBar = true,
                        onClickBackArrow = { this@MovableActivity.finish() },
                        onClickRecreate = { this@MovableActivity.recreate() },
                    ) { padding ->
                        ColumnWithCenterText(padding, "Main Panel Content")
                    }
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("RightColumn"),
                    verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                ) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(panelWidth).height(200.dp).fillMaxWidth()
                    ) {
                        PanelContent("[NOT MOVABLE]")
                    }
                    SpatialActivityPanel(
                        intent = Intent(this@MovableActivity, AnotherActivity::class.java),
                        modifier =
                            SubspaceModifier.offset(x = 120.dp)
                                .width(250.dp)
                                .height(200.dp)
                                .testTag("ActivityPanel"),
                        dragPolicy = MovePolicy(true),
                    )
                }
            }
        }
    }

    @UiComposable
    @Composable
    fun PanelContent(vararg text: String) {
        ColumnWithCenterText(PaddingValues(0.dp, 0.dp, 0.dp), text[0])
    }
}
