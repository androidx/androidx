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

import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
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

/** Manage the UI for the Panel Entity. */
class PanelEntityManager(private val session: Session) {
    private val mSession: Session
    var panelEntity: PanelEntity? by androidx.compose.runtime.mutableStateOf(null)
        private set

    private var mTextView: TextView? by mutableStateOf(null)
    private var mMovableComponent: MovableComponent? = null
    private var mResizableComponent: ResizableComponent? = null

    init {
        mSession = session
    }

    private fun destroyPanelEntity() {
        panelEntity?.dispose()
        panelEntity = null
    }

    @Composable
    fun PanelEntitySettings() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = (panelEntity == null),
                onClick = {
                    // Create PanelEntity and Components if they don't exist.
                    if (panelEntity == null) {
                        val mTextView =
                            TextView(session.activity).apply {
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
                                pose = Pose(Vector3(0f, 0f, 0f)),
                            )

                        mMovableComponent = MovableComponent.createSystemMovable(mSession)
                        mResizableComponent = ResizableComponent.create(mSession)
                        val simpleResizeListener =
                            object : ResizeListener {
                                override fun onResizeStart(entity: Entity, newSize: FloatSize3d) {}

                                override fun onResizeUpdate(entity: Entity, newSize: FloatSize3d) {}

                                override fun onResizeEnd(entity: Entity, newSize: FloatSize3d) {
                                    panelEntity?.size = newSize.to2d()
                                    mTextView.text = "This Panel's dimensions are $newSize"
                                }
                            }
                        mResizableComponent?.addResizeListener(simpleResizeListener)
                        val unused = panelEntity!!.addComponent(mMovableComponent!!)
                        val unused2 = panelEntity!!.addComponent(mResizableComponent!!)
                    }
                },
            ) {
                Text(text = "Create Panel Entity", fontSize = 20.sp)
            }
            Button(enabled = (panelEntity != null), onClick = { destroyPanelEntity() }) {
                Text(text = "Destroy Panel Entity", fontSize = 20.sp)
            }
        }
    }
}
