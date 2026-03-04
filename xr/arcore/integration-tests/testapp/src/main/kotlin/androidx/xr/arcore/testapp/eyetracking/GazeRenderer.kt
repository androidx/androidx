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

package androidx.xr.arcore.testapp.eyetracking

import android.util.Log
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.Eye
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GazeRenderer {

    class EyeWidget(
        private val session: Session,
        private val entity: Entity,
        private val model: GltfModelEntity,
        private val left: Boolean,
    ) {
        private val arDevice = ArDevice.getInstance(session)

        companion object {
            suspend fun create(session: Session, name: String, isLeft: Boolean): EyeWidget {
                val rootEntity = Entity.create(session, "$name Root")

                val offsetPose = Pose(Vector3(0f, 0f, -0.2f), Quaternion.Identity)
                val offsetEntity = Entity.create(session, "$name Offset", offsetPose)
                rootEntity.addChild(offsetEntity)

                val assetName =
                    when (isLeft) {
                        true -> "BoundingBoxGreen.glb"
                        false -> "BoundingBoxBlue.glb"
                    }
                val model = GltfModel.create(session, Paths.get("models", assetName))
                val modelEntity = GltfModelEntity.create(session, model)
                modelEntity.setScale(PANEL_SIZE)
                offsetEntity.addChild(modelEntity)
                return EyeWidget(session, rootEntity, modelEntity, isLeft)
            }

            const val ALPHA_OPEN = 1.0f
            const val ALPHA_SHUT = 0.5f
            const val ALPHA_STOPPED = 0.1f
            const val PANEL_SIZE = 0.01f
        }

        fun dispose() {
            entity.dispose()
        }

        fun update(eyeState: Eye.State) {
            entity.setEnabled(eyeState.trackingState != TrackingState.STOPPED)

            val newPose =
                eyeState.pose.let {
                    val headScenePose =
                        session.scene.perceptionSpace.getScenePoseFromPerceptionPose(
                            arDevice.state.value.devicePose
                        )
                    headScenePose.transformPoseTo(it, session.scene.activitySpace)
                }
            entity.setPose(newPose)

            model.setAlpha(getAlpha(eyeState.isOpen, eyeState.trackingState))
        }

        fun getAlpha(isOpen: Boolean, trackingState: TrackingState): Float {
            if (trackingState == TrackingState.PAUSED) return ALPHA_STOPPED

            return when (isOpen) {
                true -> ALPHA_OPEN
                false -> ALPHA_SHUT
            }
        }
    }

    private lateinit var _coroutineScope: CoroutineScope
    private lateinit var _supervisorJob: Job

    fun startRendering(session: Session, coroutineScope: CoroutineScope) {
        _supervisorJob = SupervisorJob()
        _coroutineScope = CoroutineScope(coroutineScope.coroutineContext + _supervisorJob)

        Log.d("GazeRenderer", "startRendering()")

        _coroutineScope.launch {
            Eye.left(session)?.let {
                val widget = EyeWidget.create(session, "leftEye", true)
                try {
                    it.state.collect { state -> widget.update(state) }
                } finally {
                    widget.dispose()
                }
            }
        }

        _coroutineScope.launch {
            Eye.right(session)?.let {
                val widget = EyeWidget.create(session, "rightEye", false)
                try {
                    it.state.collect { state -> widget.update(state) }
                } finally {
                    widget.dispose()
                }
            }
        }
    }

    fun stopRendering() {
        Log.d("GazeRenderer", "stopRendering()")
        check(::_supervisorJob.isInitialized) { "_supervisorJob is not initialized" }

        _supervisorJob.cancel()
    }
}
