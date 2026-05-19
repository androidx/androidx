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

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import java.util.function.Consumer

/** Manage the UI for the Panel Entity. */
class PanelEntityManager(
    private val session: Session,
    activity: ComponentActivity,
    private val maxEntities: Int = 1,
    private val entitiesPerClick: Int = 1,
) {

    val panelEntity: PanelEntity?
        get() = panelEntities.firstOrNull()

    private val panelEntities = mutableListOf<PanelEntity>()

    private val createPanelEntityButton =
        activity.findViewById<Button>(R.id.button_create_panel_entity)
    private val destroyPanelEntityButton =
        activity.findViewById<Button>(R.id.button_destroy_panel_entity)

    init {
        updateButtonEnabledState()

        createPanelEntityButton.setOnClickListener {
            createPanelEntity(activity)
            updateButtonEnabledState()
        }

        destroyPanelEntityButton.setOnClickListener {
            destroyPanelEntity()
            updateButtonEnabledState()
        }
    }

    private fun createPanelEntity(context: Context) {
        for (i in 1..entitiesPerClick) {
            createOnePanelEntity(context)
        }
    }

    private fun createOnePanelEntity(context: Context) {
        if (panelEntities.size < maxEntities) {
            val panelNumber = panelEntities.size + 1
            val mTextView =
                TextView(context).apply {
                    text = "Hello, XR World! Panel $panelNumber"
                    textSize = 24f
                    setTextColor(Color.BLACK)
                    setBackgroundColor(Color.LTGRAY)
                    gravity = Gravity.CENTER
                }
            val newPanel =
                PanelEntity.create(
                    session = session,
                    view = mTextView,
                    pixelDimensions = IntSize2d(800, 360),
                    name = "samplePanelEntity$panelNumber",
                    // Offset each new panel slightly
                    pose =
                        Pose(
                            Vector3(
                                -0.6f + (panelNumber * 0.1f),
                                -0.4f,
                                0.2f + (panelNumber * 0.01f),
                            )
                        ),
                    parent = session.scene.activitySpace,
                )

            val movableComponent = MovableComponent.createSystemMovable(session)
            val simpleResizeListener =
                Consumer<ResizeEvent> { resizeEvent: ResizeEvent ->
                    if (resizeEvent.resizeState == ResizeEvent.ResizeState.END) {
                        newPanel.size = resizeEvent.newSize.to2d()
                        val panelWidthInActivitySpace: Float =
                            newPanel.size.width * resizeEvent.entity.getScale(Space.ACTIVITY)
                        val panelHeightInActivitySpace: Float =
                            newPanel.size.height * resizeEvent.entity.getScale(Space.ACTIVITY)
                        mTextView.text =
                            "Panel#$panelNumber's size is W:$panelWidthInActivitySpace x H:$panelHeightInActivitySpace in ActivitySpace units"
                    }
                }
            val resizableComponent =
                ResizableComponent.create(session, resizeEventListener = simpleResizeListener)
            newPanel.addComponent(movableComponent)
            newPanel.addComponent(resizableComponent)
            panelEntities.add(newPanel)
        }
    }

    private fun destroyPanelEntity() {
        for (i in 1..entitiesPerClick) {
            if (panelEntities.isNotEmpty()) {
                val lastPanel = panelEntities.removeAt(panelEntities.lastIndex)
                lastPanel.removeAllComponents()
                lastPanel.parent = null
            }
        }
    }

    private fun updateButtonEnabledState() {
        createPanelEntityButton.isEnabled = panelEntities.size < maxEntities
        destroyPanelEntityButton.isEnabled = panelEntities.isNotEmpty()
        if (maxEntities > 1) {
            val currentCount = panelEntities.size
            createPanelEntityButton.text =
                if (currentCount == maxEntities) "Create panel Entity"
                else
                    "Create Panel Entity #${currentCount + 1}-#${
                        minOf(
                            currentCount + entitiesPerClick,
                            maxEntities,
                        )
                    }"
            destroyPanelEntityButton.text =
                if (currentCount == 0) "Destroy Panel Entity"
                else
                    "Destroy Panel Entity #${currentCount}-#${
                        maxOf(
                            currentCount - entitiesPerClick,
                            1,
                        )
                    }"
        }
    }
}
