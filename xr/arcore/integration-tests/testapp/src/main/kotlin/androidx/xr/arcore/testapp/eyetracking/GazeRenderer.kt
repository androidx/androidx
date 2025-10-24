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

import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.Eye
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

class GazeRenderer(val session: Session, val lifecycleScope: CoroutineScope, var config: Config) :
    DefaultLifecycleObserver {

    data class EyeData(val left: Eye.State?, val right: Eye.State?)

    class EyeWidget(
        private val session: Session,
        private val entity: GroupEntity,
        private val view: TextView,
        private val left: Boolean,
    ) {
        private val arDevice = ArDevice.getInstance(session)

        companion object {
            fun create(session: Session, name: String, isLeft: Boolean): EyeWidget {
                val entity = GroupEntity.create(session, name)
                val view = TextView(session.activity.applicationContext)
                view.setBackgroundColor(INVALID)
                val panel =
                    PanelEntity.create(
                        session,
                        view,
                        FloatSize2d(PANEL_SIZE, PANEL_SIZE),
                        name,
                        Pose(Vector3(0f, 0f, -1f), Quaternion.Identity),
                    )
                entity.addChild(panel)
                return EyeWidget(session, entity, view, isLeft)
            }

            const val PANEL_SIZE = 0.05f
        }

        var enabled: Boolean
            get() = entity.isEnabled()
            set(value) = entity.setEnabled(value)

        fun dispose() {
            entity.dispose()
        }

        fun update(config: Config, eye: Eye.State?) {
            val gazePose = getEyePose(config, eye)

            entity.setEnabled(gazePose != null)
            gazePose?.let {
                val headScenePose =
                    session.scene.perceptionSpace.getScenePoseFromPerceptionPose(
                        arDevice.state.value.devicePose
                    )
                val pose = headScenePose.transformPoseTo(gazePose, session.scene.activitySpace)
                entity.setPose(pose)
            }

            eye?.let {
                val isOpen = getEyeIsOpen(config, it)
                val trackingState = getEyeTrackingState(config, it)
                view.setBackgroundColor(getColor(isOpen!!, trackingState!!))
            }
        }

        fun getColor(isOpen: Boolean, trackingState: TrackingState): Int {
            if (trackingState == TrackingState.PAUSED) return INVALID

            return when (isOpen) {
                true -> if (left) GAZE_LEFT else GAZE_RIGHT
                false -> if (left) SHUT_LEFT else SHUT_RIGHT
            }
        }
    }

    private var leftEyeWidget: EyeWidget = EyeWidget.create(session, "leftEye", true)
    private var rightEyeWidget: EyeWidget = EyeWidget.create(session, "rightEye", false)
    private lateinit var updateJob: CompletableJob

    override fun onResume(owner: LifecycleOwner) {
        leftEyeWidget.enabled = true
        rightEyeWidget.enabled = true
        updateJob = SupervisorJob(lifecycleScope.launch { eyes().collect { renderEyeData(it) } })
    }

    override fun onPause(owner: LifecycleOwner) {
        updateJob.complete()
        leftEyeWidget.enabled = false
        rightEyeWidget.enabled = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        leftEyeWidget.dispose()
        rightEyeWidget.dispose()
    }

    private fun eyes(): Flow<EyeData> {
        val leftEye = Eye.left(session)
        val rightEye = Eye.right(session)

        return when {
            leftEye != null && rightEye != null ->
                leftEye.state.zip(rightEye.state) { l, r -> EyeData(l, r) }
            leftEye != null && rightEye == null ->
                leftEye.state.transform { emit(EyeData(it, null)) }
            leftEye == null && rightEye != null ->
                rightEye.state.transform { emit(EyeData(null, it)) }
            else -> throw Exception("both eyes are null")
        }
    }

    private fun renderEyeData(eyeData: EyeData) {
        leftEyeWidget.update(config, eyeData.left)
        rightEyeWidget.update(config, eyeData.right)
    }
}
