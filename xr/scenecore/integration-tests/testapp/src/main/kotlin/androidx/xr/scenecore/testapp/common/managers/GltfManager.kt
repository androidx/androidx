/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.testapp.common.managers

import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.testapp.R
import java.nio.file.Paths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Manages the UI for the GLTF entity. */
class GltfManager(private val session: Session, activity: AppCompatActivity) {
    private val mSession = session
    private val _mGltfModelFlow = MutableStateFlow<GltfModel?>(null)
    var mGltfModel: GltfModel?
        get() = _mGltfModelFlow.value
        set(value) {
            _mGltfModelFlow.value = value
        }

    private val onEntityChangedCallbacks = mutableListOf<(GltfModelEntity?) -> Unit>()
    private val _mGltfModelEntityFlow = MutableStateFlow<GltfModelEntity?>(null)
    var mGltfModelEntity: GltfModelEntity?
        get() = _mGltfModelEntityFlow.value
        set(value) {
            _mGltfModelEntityFlow.value = value
            for (callback in onEntityChangedCallbacks) callback(_mGltfModelEntityFlow.value)
        }

    private val _modelIsEnabledFlow = MutableStateFlow<Boolean>(true)
    private var modelIsEnabled: Boolean
        get() = _modelIsEnabledFlow.value
        set(value) {
            _modelIsEnabledFlow.value = value
        }

    private val hideModelButton = activity.findViewById<Button>(R.id.button_hide_model)
    private val loadGltfEntityModelButton =
        activity.findViewById<Button>(R.id.button_load_gltf_entity_model)
    private val createGltfEntityButton =
        activity.findViewById<Button>(R.id.button_create_gltf_entity)
    private val destroyGltfEntityButton =
        activity.findViewById<Button>(R.id.button_destroy_gltf_entity)

    init {

        updateButtonStates()

        hideModelButton.setOnClickListener {
            if (mGltfModelEntity != null) {
                modelIsEnabled = mGltfModelEntity!!.isEnabled(true)
                mGltfModelEntity?.setEnabled(!modelIsEnabled)
                modelIsEnabled = !modelIsEnabled
            }
            updateButtonStates()
        }

        loadGltfEntityModelButton.setOnClickListener {
            activity.lifecycleScope.launch {
                mGltfModel = GltfModel.create(mSession, Paths.get("models", "Dragon_Evolved.gltf"))
                updateButtonStates()
            }
        }

        createGltfEntityButton.setOnClickListener {
            mGltfModelEntity =
                GltfModelEntity.create(
                    mSession,
                    mGltfModel!!,
                    Pose(Vector3.Forward * 3f + Vector3.Right),
                )
            updateButtonStates()
        }

        destroyGltfEntityButton.setOnClickListener {
            mGltfModelEntity?.dispose()
            mGltfModelEntity = null
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        hideModelButton.isEnabled = (mGltfModelEntity != null)
        hideModelButton.text = (if (modelIsEnabled) "Hide Model" else "Show Model")
        loadGltfEntityModelButton.isEnabled = (mGltfModel == null)
        createGltfEntityButton.isEnabled = (mGltfModelEntity == null && mGltfModel != null)
        destroyGltfEntityButton.isEnabled = (mGltfModelEntity != null)
    }

    fun AddOnEntityChangedListener(callback: (GltfModelEntity?) -> Unit) =
        onEntityChangedCallbacks.add(callback)

    fun ClearListeners() = onEntityChangedCallbacks.clear()
}
