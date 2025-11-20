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
import kotlin.text.clear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Manages the UI for the GLTF entity. */
class GltfManager(
    private val session: Session,
    activity: AppCompatActivity,
    private val maxEntities: Int = 1,
    private val entitiesPerClick: Int = 1,
) {
    private val mSession = session
    private val _mGltfModelFlow = MutableStateFlow<GltfModel?>(null)
    var mGltfModel: GltfModel?
        get() = _mGltfModelFlow.value
        set(value) {
            _mGltfModelFlow.value = value
        }

    private val onEntityChangedCallbacks = mutableListOf<(GltfModelEntity?) -> Unit>()
    val gltfModelEntities = mutableListOf<GltfModelEntity>()
    val mGltfModelEntity: GltfModelEntity?
        get() = gltfModelEntities.firstOrNull()

    private val _modelIsEnabledFlow = MutableStateFlow(true)
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
            if (gltfModelEntities.isNotEmpty()) {
                val shouldBeEnabled = !modelIsEnabled
                gltfModelEntities.forEach { it.setEnabled(shouldBeEnabled) }
                modelIsEnabled = shouldBeEnabled
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
            createGltfEntity()
            updateButtonStates()
        }

        destroyGltfEntityButton.setOnClickListener {
            destroyGltfEntity()
            updateButtonStates()
        }
    }

    private fun createGltfEntity() {
        for (i in 1..entitiesPerClick) {
            if (gltfModelEntities.size < maxEntities && mGltfModel != null) {
                val entityNumber = gltfModelEntities.size + 1
                val newEntity =
                    GltfModelEntity.create(
                        mSession,
                        mGltfModel!!,
                        // Offset each new entity
                        Pose(Vector3.Forward * 3f + Vector3.Right * entityNumber.toFloat() * 1.5f),
                    )
                for (callback in onEntityChangedCallbacks) callback(newEntity)
                gltfModelEntities.add(newEntity)
            }
        }
    }

    private fun destroyGltfEntity() {
        for (i in 1..entitiesPerClick) {
            if (gltfModelEntities.isNotEmpty()) {
                val lastEntity = gltfModelEntities.removeAt(gltfModelEntities.lastIndex)
                lastEntity.dispose()
            }
        }
    }

    private fun updateButtonStates() {
        hideModelButton.isEnabled = gltfModelEntities.isNotEmpty()
        hideModelButton.text = if (modelIsEnabled) "Hide Models" else "Show Models"
        loadGltfEntityModelButton.isEnabled = mGltfModel == null
        createGltfEntityButton.isEnabled =
            gltfModelEntities.size < maxEntities && mGltfModel != null
        destroyGltfEntityButton.isEnabled = gltfModelEntities.isNotEmpty()
        if (maxEntities > 1) {
            val currentCount = gltfModelEntities.size
            createGltfEntityButton.text =
                if (currentCount == maxEntities) "Create Gltf Entity"
                else
                    "Create Gltf Entity #${currentCount + 1}-#${minOf(currentCount + entitiesPerClick, maxEntities)}"
            destroyGltfEntityButton.text =
                if (currentCount == 0) "Destroy Gltf Entity"
                else
                    "Destroy Gltf Entity #${currentCount}-#${maxOf(currentCount - entitiesPerClick, 1)}"
        }
    }

    fun AddOnEntityChangedListener(callback: (GltfModelEntity?) -> Unit) =
        onEntityChangedCallbacks.add(callback)

    fun ClearListeners() = onEntityChangedCallbacks.clear()
}
