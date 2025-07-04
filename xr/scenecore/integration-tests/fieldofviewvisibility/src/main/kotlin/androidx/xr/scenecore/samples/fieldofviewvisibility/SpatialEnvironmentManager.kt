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

package androidx.xr.scenecore.samples.fieldofviewvisibility

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.xr.runtime.Session
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import java.nio.file.Paths

/** Manage the UI for the Spatial Environment. */
class SpatialEnvironmentManager(private val session: Session) {
    private val mSession: Session
    private var mSpatialEnvironmentPreference: SpatialEnvironment.SpatialEnvironmentPreference? =
        null

    init {
        mSession = session
    }

    private fun setGeoAndSkybox(skybox: ExrImage?, geometry: GltfModel?) {
        mSpatialEnvironmentPreference = SpatialEnvironmentPreference(skybox, geometry)
        mSession.scene.spatialEnvironment.preferredSpatialEnvironment =
            mSpatialEnvironmentPreference
    }

    @Composable
    fun SpatialEnvironmentSettings() {
        var groundGeo by remember { mutableStateOf<GltfModel?>(null) }
        var blueSkybox by remember { mutableStateOf<ExrImage?>(null) }

        LaunchedEffect(Unit) {
            groundGeo = GltfModel.create(session, Paths.get("models", "GroundGeometry.glb"))
        }
        LaunchedEffect(Unit) {
            blueSkybox = ExrImage.createFromZip(session, Paths.get("skyboxes", "BlueSkybox.zip"))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { setGeoAndSkybox(blueSkybox, groundGeo) }) {
                Text(text = "Set both Geometry and Skybox", fontSize = 15.sp)
            }
            Button(
                onClick = {
                    mSpatialEnvironmentPreference = null
                    mSession.scene.spatialEnvironment.preferredSpatialEnvironment = null
                }
            ) {
                Text(text = "Revert to System Default Environment", fontSize = 15.sp)
            }
        }
    }
}
