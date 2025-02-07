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

package androidx.xr.compose.integration.layout.rotationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.math.Vector3
import kotlin.math.floor
import kotlin.math.roundToInt

class RotationApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    private fun SpatialContent() {
        val infiniteTransition = rememberInfiniteTransition()
        val singleRotationDurationMs = 5000

        val rotationValue by
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 359f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(singleRotationDurationMs, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
            )

        val axisAngle by
            infiniteTransition.animateValue(
                initialValue = Vector3(0.1f, 0.0f, 0.0f),
                targetValue = Vector3(1.1f, 1.1f, 1.1f),
                typeConverter =
                    TwoWayConverter(
                        {
                            val axisSingleValue =
                                (it.x.roundToInt()) +
                                    (2 * it.y.roundToInt()) +
                                    (4 * it.z.roundToInt())
                            AnimationVector1D(axisSingleValue.toFloat() / 7)
                        },
                        {
                            val scaledAnimationValue = (it.value * 7) + 1.0f
                            val x = floor(scaledAnimationValue % 2)
                            val y = floor((scaledAnimationValue / 2) % 2)
                            val z = floor((scaledAnimationValue / 4) % 2)

                            Vector3(x, y, z)
                        },
                    ),
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(singleRotationDurationMs * 7, easing = LinearEasing)
                    ),
            )

        SpatialColumn(name = "OutermostContainer") {
            SpatialRow(name = "StatusRow") {
                AppPanel(
                    modifier = SubspaceModifier.height(80.dp),
                    name = "StatusPanel",
                    text = "Rotation Axis: ${axisAngle.x}, ${axisAngle.y}, ${axisAngle.z}",
                )
            }
            SpatialRow(name = "ContentRow") {
                SpatialColumn(
                    modifier = SubspaceModifier.rotate(axisAngle, rotationValue),
                    name = "RotatingColumn",
                ) {
                    val rotatedColumnPanelModifier = SubspaceModifier.size(200.dp).padding(20.dp)
                    AppPanel(
                        modifier = rotatedColumnPanelModifier,
                        name = "TopPanelRotatingColumn",
                        text = "Top panel: ${rotationValue.roundToInt()}",
                    )
                    AppPanel(
                        modifier = rotatedColumnPanelModifier,
                        name = "CenterPanelRotatingColumn",
                        text = "Center panel: ${rotationValue.roundToInt()}",
                    )
                    AppPanel(
                        modifier = rotatedColumnPanelModifier,
                        name = "BottomPanelRotatingColumn",
                        text = "Bottom panel: ${rotationValue.roundToInt()}",
                    )
                }
                SpatialColumn(name = "RightmostColumn") {
                    val rotatedRowPanelModifier = SubspaceModifier.size(200.dp).padding(20.dp)
                    SpatialRow(
                        modifier = SubspaceModifier.rotate(axisAngle, rotationValue),
                        name = "RotatingRow",
                    ) {
                        AppPanel(
                            modifier = rotatedRowPanelModifier,
                            name = "RightPanelRotatingRow",
                            text = "Left panel: ${rotationValue.roundToInt()}",
                        )
                        AppPanel(
                            modifier = rotatedRowPanelModifier,
                            name = "CenterPanelRotatingRow",
                            text = "Center panel: ${rotationValue.roundToInt()}",
                        )
                        AppPanel(
                            modifier = rotatedRowPanelModifier,
                            name = "LeftPanelRotatingRow",
                            text = "Right panel: ${rotationValue.roundToInt()}",
                        )
                    }
                    SpatialRow(alignment = SpatialAlignment.Center, name = "RowWithRotatingPanel") {
                        AppPanel(
                            modifier =
                                SubspaceModifier.width(400.dp)
                                    .height(400.dp)
                                    .padding(20.dp)
                                    .rotate(axisAngle, rotationValue),
                            name = "RotatingPanel",
                            text = "Rotating Panel: ${rotationValue.roundToInt()}",
                        )
                    }
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    fun AppPanel(modifier: SubspaceModifier = SubspaceModifier, name: String, text: String = "") {
        SpatialPanel(modifier = modifier, name = name) { PanelContent(text) }
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
                    Text(text = item, textAlign = TextAlign.Center, fontSize = 20.sp)
                }
            }
        }
    }
}
