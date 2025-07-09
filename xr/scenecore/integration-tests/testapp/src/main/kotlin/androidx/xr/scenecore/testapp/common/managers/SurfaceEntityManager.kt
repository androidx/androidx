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
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.testapp.R
import kotlinx.coroutines.flow.MutableStateFlow

/** Manage the UI for the Surface Entity. */
class SurfaceEntityManager(private val session: Session, activity: AppCompatActivity) {
    private val onEntityChangedCallbacks = mutableListOf<(SurfaceEntity?) -> Unit>()
    private val _surfaceEntityFlow = MutableStateFlow<SurfaceEntity?>(null)
    var surfaceEntity: SurfaceEntity?
        get() = _surfaceEntityFlow.value
        set(value) {
            _surfaceEntityFlow.value = value
            for (callback in onEntityChangedCallbacks) callback(_surfaceEntityFlow.value)
        }

    private var mMovableComponent: MovableComponent? = null // movable component for surfaceEntity
    private val surfaceEntityRadioGroup =
        activity.findViewById<RadioGroup>(R.id.radio_group_surface_entity)
    private val createSurfaceEntityButton =
        activity.findViewById<Button>(R.id.button_create_surface_entity)
    private val destroySurfaceEntityButton =
        activity.findViewById<Button>(R.id.button_destroy_surface_entity)
    val canvasRadioOptions =
        listOf(
            SurfaceEntity.CanvasShape.Quad(1f, 1f),
            SurfaceEntity.CanvasShape.Vr180Hemisphere(1f),
            SurfaceEntity.CanvasShape.Vr360Sphere(1f),
        )
    private val _selectedCanvasShapeOptionFlow = MutableStateFlow(canvasRadioOptions[0])
    private var selectedCanvasShapeOption: SurfaceEntity.CanvasShape
        get() = _selectedCanvasShapeOptionFlow.value
        set(value) {
            _selectedCanvasShapeOptionFlow.value = value
        }

    init {
        updateButtonStates()
        surfaceEntityRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            selectedCanvasShapeOption =
                when (checkedId) {
                    R.id.radiobutton_quad -> canvasRadioOptions[0]
                    R.id.radiobutton_vr180 -> canvasRadioOptions[1]
                    R.id.radiobutton_vr360 -> canvasRadioOptions[2]
                    else -> canvasRadioOptions[0] // Default
                }
            // If entity exists, update its shape immediately
            if (surfaceEntity != null) {
                surfaceEntity?.canvasShape = selectedCanvasShapeOption
            }
        }

        createSurfaceEntityButton.setOnClickListener {
            createSurfaceEntity()
            updateButtonStates()
        }

        destroySurfaceEntityButton.setOnClickListener {
            destroySurfaceEntity()
            updateButtonStates()
        }
    }

    private fun createSurfaceEntity() {
        // Create SurfaceEntity and MovableComponent if they don't exist.
        if (surfaceEntity == null) {
            surfaceEntity =
                SurfaceEntity.create(
                    session,
                    SurfaceEntity.StereoMode.MONO,
                    Pose.Identity,
                    selectedCanvasShapeOption,
                )
            // Make the video player movable (to make it easier to look at it from
            // different angles and distances)
            mMovableComponent = MovableComponent.createSystemMovable(session)
            // The quad has a radius of 1.0 meters
            mMovableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)
            surfaceEntity!!.addComponent(mMovableComponent!!)
        }
    }

    private fun destroySurfaceEntity() {
        surfaceEntity?.dispose()
        surfaceEntity = null
        updateButtonStates()
    }

    private fun updateButtonStates() {
        createSurfaceEntityButton.isEnabled = (surfaceEntity == null)
        destroySurfaceEntityButton.isEnabled = (surfaceEntity != null)
    }

    fun AddOnEntityChangedListener(callback: (SurfaceEntity?) -> Unit) =
        onEntityChangedCallbacks.add(callback)

    fun ClearListeners() = onEntityChangedCallbacks.clear()
}
