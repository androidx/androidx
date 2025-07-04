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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SurfaceEntity

/** Manage the UI for the Surface Entity. */
class SurfaceEntityManager(private val session: Session) {
    private val mSession: Session
    var surfaceEntity: SurfaceEntity? by mutableStateOf(null)
        private set

    private var mMovableComponent: MovableComponent? = null // movable component for surfaceEntity

    init {
        mSession = session
    }

    private fun destroySurfaceEntity() {
        surfaceEntity?.dispose()
        surfaceEntity = null
    }

    @Composable
    fun SurfaceEntitySettings() {
        val canvasRadioOptions =
            listOf<SurfaceEntity.CanvasShape>(
                SurfaceEntity.CanvasShape.Quad(1f, 1f),
                SurfaceEntity.CanvasShape.Vr180Hemisphere(1f),
                SurfaceEntity.CanvasShape.Vr360Sphere(1f),
            )
        val (selectedCanvasShapeOption, onCanvasShapeOptionSelected) =
            remember { mutableStateOf<SurfaceEntity.CanvasShape>(canvasRadioOptions[0]) }

        // Radio buttons for canvas shape and stereo mode
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (canvas in canvasRadioOptions) {
                RadioButton(
                    selected = (canvas.javaClass == selectedCanvasShapeOption.javaClass),
                    onClick = {
                        onCanvasShapeOptionSelected(canvas)
                        surfaceEntity?.canvasShape = canvas
                    },
                )
                Text(text = canvas.javaClass.simpleName, modifier = Modifier.padding(end = 30.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = (surfaceEntity == null),
                onClick = {
                    // Create SurfaceEntity and MovableComponent if they don't exist.
                    if (surfaceEntity == null) {
                        surfaceEntity =
                            SurfaceEntity.create(
                                mSession,
                                SurfaceEntity.StereoMode.MONO,
                                Pose.Identity,
                                selectedCanvasShapeOption,
                            )
                        // Make the video player movable (to make it easier to look at it from
                        // different
                        // angles and distances)
                        mMovableComponent = MovableComponent.createSystemMovable(mSession)
                        // The quad has a radius of 1.0 meters
                        mMovableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)
                        val unused = surfaceEntity!!.addComponent(mMovableComponent!!)
                    }
                },
            ) {
                Text(text = "Create Surface Entity", fontSize = 20.sp)
            }
            Button(
                enabled = (surfaceEntity != null),
                onClick = {
                    // Destroy SurfaceEntity and MovableComponent if they exist.
                    destroySurfaceEntity()
                },
            ) {
                Text(text = "Destroy Surface Entity", fontSize = 20.sp)
            }
        }
    }
}
