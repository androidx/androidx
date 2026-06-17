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

import android.view.View
import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.SceneCoreEntity
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene

@Sampled
@Composable
@SubspaceComposable
public fun SceneCoreEntitySample() {
    val session = checkNotNull(LocalSession.current)
    val context = LocalContext.current
    val view = remember(context) { View(context) }
    val density = LocalDensity.current
    val virtualPixelDensity = session.scene.virtualPixelDensity

    // A state variable to dynamically control the size of the panel in DP
    var panelSizeDp by remember { mutableStateOf(400.dp) }

    SceneCoreEntity(
        factory = {
            // Convert the DP size to meters using the virtual pixel density, PanelEntity also has
            // APIs that use pixels directly, but a lot of other SceneCore APIs do not have this.
            val sizeInPx = with(density) { panelSizeDp.toPx() }
            val sizeInMeters = virtualPixelDensity.convertPixelsToMeters(sizeInPx)

            PanelEntity.create(
                session = session,
                view = view,
                dimensions = FloatSize2d(sizeInMeters, sizeInMeters),
                name = "SamplePanel",
            )
        },
        update = { entity ->
            // Update the entity size when state changes
            val sizeInPx = with(density) { panelSizeDp.toPx() }
            val sizeInMeters = virtualPixelDensity.convertPixelsToMeters(sizeInPx)
            entity.size = FloatSize2d(sizeInMeters, sizeInMeters)
        },
    )
}
