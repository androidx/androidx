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

package androidx.xr.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.compose.unit.Meter
import java.nio.file.Paths

@Sampled
public fun SpatialGltfModelSample() {
    @Composable
    fun AppContent() {
        val modelState =
            rememberSpatialGltfModelState(
                source = SpatialGltfModelSource.fromPath(Paths.get("models", "Dragon_Evolved.gltf"))
            )
        Subspace() {
            SpatialGltfModel(state = modelState) {
                val headNode = modelState.nodes.find { it.name == "head" }
                if (headNode != null) {
                    val offsetX = Meter(headNode.modelPose.translation.x).toDp()
                    val offsetY = Meter(headNode.modelPose.translation.y).toDp()
                    val offsetZ = Meter(headNode.modelPose.translation.z).toDp()
                    SpatialPanel(
                        shape = SpatialRoundedCornerShape(CornerSize(25)),
                        modifier =
                            SubspaceModifier.width(700.dp)
                                .height(700.dp)
                                .offset(offsetX, offsetY, offsetZ),
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(Color.Blue.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "Head", color = Color.White, fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }
}
