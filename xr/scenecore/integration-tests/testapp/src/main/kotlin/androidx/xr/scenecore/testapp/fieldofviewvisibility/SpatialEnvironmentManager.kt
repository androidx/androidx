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

package androidx.xr.scenecore.testapp.fieldofviewvisibility

import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import java.nio.file.Paths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Manage the UI for the Spatial Environment. */
class SpatialEnvironmentManager(
    private val session: Session,
    private val activity: FieldOfViewVisibilityActivity,
) {
    private var mSpatialEnvironmentPreference: SpatialEnvironmentPreference? = null
    private val _geometryFlow = MutableStateFlow<GltfModel?>(null)
    private var geometry: GltfModel?
        get() = _geometryFlow.value
        set(value) {
            _geometryFlow.value = value
        }

    private val _skyboxFlow = MutableStateFlow<ExrImage?>(null)
    private var skybox: ExrImage?
        get() = _skyboxFlow.value
        set(value) {
            _skyboxFlow.value = value
        }

    init {
        activity.lifecycleScope.launch { loadExrImagesAndModels() }

        activity.findViewById<Button>(R.id.button_set_both_geometry_and_skybox).also {
            it.setOnClickListener {
                mSpatialEnvironmentPreference = SpatialEnvironmentPreference(skybox, geometry)
                session.scene.spatialEnvironment.preferredSpatialEnvironment =
                    mSpatialEnvironmentPreference
            }
        }

        activity.findViewById<Button>(R.id.button_revert_to_system_default_env).also {
            it.setOnClickListener {
                mSpatialEnvironmentPreference = null
                session.scene.spatialEnvironment.preferredSpatialEnvironment = null
            }
        }
    }

    private suspend fun loadExrImagesAndModels() {
        geometry = GltfModel.create(session, Paths.get("models", "GroundGeometry.glb"))

        skybox = ExrImage.createFromZip(session, Paths.get("skyboxes", "BlueSkybox.zip"))
    }
}
