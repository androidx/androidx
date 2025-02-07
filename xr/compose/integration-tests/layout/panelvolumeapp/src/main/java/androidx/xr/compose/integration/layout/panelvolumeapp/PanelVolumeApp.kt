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
package androidx.xr.compose.integration.layout.panelvolumeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.Volume
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.scale
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import kotlinx.coroutines.guava.await

class PanelVolumeApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    private fun SpatialContent() {
        val session =
            checkNotNull(LocalSession.current) {
                "LocalSession.current was null. Session must be available."
            }
        var arrows by remember { mutableStateOf<GltfModel?>(null) }
        val gltfEntity = arrows?.let { remember { GltfModelEntity.create(session, it) } }

        LaunchedEffect(Unit) { arrows = GltfModel.create(session, "models/xyzArrows.glb").await() }

        val infiniteTransition = rememberInfiniteTransition()
        val panelYOffset by
            infiniteTransition.animateValue(
                initialValue = 0.dp,
                targetValue = 200.dp,
                typeConverter = Dp.VectorConverter,
                animationSpec = infiniteRepeatable(animation = tween(2000)),
            )

        SpatialPanel(
            modifier =
                SubspaceModifier.width(200.dp).height(200.dp).offset(y = panelYOffset).movable(),
            name = "RootPanel",
        ) {
            Box(
                modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Row {
                    Text(text = "Panel", textAlign = TextAlign.Center, fontSize = 20.sp)
                    if (gltfEntity != null) {
                        Subspace {
                            Volume(
                                modifier =
                                    SubspaceModifier.scale(.3f)
                                        .offset(x = 1.meters.toDp(), z = -0.5.meters.toDp())
                            ) {
                                gltfEntity.setParent(it)
                            }
                        }
                    }
                }
            }
        }
    }
}
