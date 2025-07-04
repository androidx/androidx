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

import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeListener
import androidx.xr.scenecore.testapp.R
import kotlinx.coroutines.flow.MutableStateFlow

/** Manage the UI for the Panel Entity. */
class PanelEntityManager(private val session: Session, activity: FieldOfViewVisibilityActivity) {

    private val mSession = session
    private val _panelEntityFlow = MutableStateFlow<PanelEntity?>(null)
    var panelEntity: PanelEntity?
        get() = _panelEntityFlow.value
        set(value) {
            _panelEntityFlow.value = value
        }

    private var mMovableComponent: MovableComponent? = null
    private var mResizableComponent: ResizableComponent? = null
    private val createPanelEntityButton =
        activity.findViewById<Button>(R.id.button_create_panel_entity)
    private val destroyPanelEntityButton =
        activity.findViewById<Button>(R.id.button_destroy_panel_entity)

    init {
        updateButtonEnabledState()

        createPanelEntityButton.setOnClickListener {
            createPanelEntity()
            updateButtonEnabledState()
        }

        destroyPanelEntityButton.setOnClickListener {
            destroyPanelEntity()
            updateButtonEnabledState()
        }
    }

    private fun createPanelEntity() {
        // Create PanelEntity and Components if they don't exist.
        if (panelEntity == null) {
            val mTextView =
                TextView(mSession.activity).apply {
                    text = "Hello, XR World!"
                    textSize = 24f
                    setTextColor(Color.BLACK)
                    setBackgroundColor(Color.LTGRAY)
                    gravity = Gravity.CENTER
                }
            panelEntity =
                PanelEntity.create(
                    session = mSession,
                    view = mTextView,
                    pixelDimensions = IntSize2d(800, 360),
                    name = "samplePanelEntity",
                    pose = Pose(Vector3(-0.6f, 0f, 0.2f)),
                )

            mMovableComponent = MovableComponent.createSystemMovable(mSession)
            mResizableComponent = ResizableComponent.create(mSession)
            val simpleResizeListener =
                object : ResizeListener {
                    override fun onResizeStart(entity: Entity, originalSize: FloatSize3d) {}

                    override fun onResizeUpdate(entity: Entity, newSize: FloatSize3d) {}

                    override fun onResizeEnd(entity: Entity, finalSize: FloatSize3d) {
                        panelEntity?.size = finalSize.to2d()
                        mTextView.text = "This Panel's dimensions are $finalSize"
                    }
                }
            mResizableComponent?.addResizeListener(simpleResizeListener)
            panelEntity!!.addComponent(mMovableComponent!!)
            panelEntity!!.addComponent(mResizableComponent!!)
        }
    }

    private fun destroyPanelEntity() {
        panelEntity?.dispose()
        panelEntity = null
    }

    private fun updateButtonEnabledState() {
        createPanelEntityButton.isEnabled = (panelEntity == null)
        destroyPanelEntityButton.isEnabled = (panelEntity != null)
    }
}
