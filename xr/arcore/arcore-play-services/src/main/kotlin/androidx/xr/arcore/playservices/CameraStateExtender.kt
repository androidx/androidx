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

package androidx.xr.arcore.playservices

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.StateExtender
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.math.Matrix4
import com.google.ar.core.Coordinates2d
import com.google.ar.core.TrackingState as ARCoreTrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.time.ComparableTimeMark

/** [StateExtender] in charge of extending [CoreState] with [CameraState]. */
internal class CameraStateExtender : StateExtender {

    internal companion object {
        internal const val MAX_CAMERA_STATE_EXTENSION_SIZE = 100

        internal val cameraStateMap = mutableMapOf<ComparableTimeMark, CameraState>()

        private val timeMarkQueue = ArrayDeque<ComparableTimeMark>()
    }

    internal lateinit var perceptionManager: ArCorePerceptionManager

    override fun initialize(runtimes: List<JxrRuntime>) {
        perceptionManager =
            runtimes.filterIsInstance<PerceptionRuntime>().first().perceptionManager
                as ArCorePerceptionManager
    }

    override suspend fun extend(coreState: CoreState) {
        check(this::perceptionManager.isInitialized) { "CameraStateExtender is not initialized." }
        synchronized(perceptionManager.frameLock) { updateCameraStateMap(coreState) }
    }

    internal fun close() {
        cameraStateMap.clear()
        timeMarkQueue.clear()
    }

    private fun getTransformCoordinates2DFunction(): ((FloatBuffer) -> FloatBuffer)? =
        if (perceptionManager.displayChanged)
            { inputVertices: FloatBuffer ->
                val outputVertices =
                    ByteBuffer.allocateDirect(inputVertices.capacity() * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer()
                synchronized(perceptionManager.frameLock) {
                    perceptionManager._latestFrame.transformCoordinates2d(
                        Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                        inputVertices,
                        Coordinates2d.TEXTURE_NORMALIZED,
                        outputVertices,
                    )
                }
                perceptionManager.displayChanged = false
                outputVertices
            }
        else {
            null
        }

    private fun getCameraState(coreState: CoreState): CameraState {
        val camera = perceptionManager._latestFrame.camera

        /**
         * When using the front-facing camera in ARCore 1.x, the Camera's TrackingState will always
         * be PAUSED, so in that case we need to ignore trackingState and populate the rest of the
         * values anyway, since an AR feature like FaceMesh tracking is likely being done.
         */
        if (
            camera.trackingState == ARCoreTrackingState.TRACKING ||
                perceptionManager.usingFrontFacingCamera
        ) {
            val projectionMatrixData = FloatArray(16)
            camera.getProjectionMatrix(
                projectionMatrixData,
                /* offset= */ 0,
                /* near= */ 0.1f,
                /* far= */ 100.0f,
            )
            var viewMatrixData = FloatArray(16)
            camera.getViewMatrix(viewMatrixData, 0)
            return CameraState(
                coreState.timeMark,
                TrackingState.fromArCoreTrackingState(camera.trackingState),
                camera.pose.toRuntimePose(),
                camera.displayOrientedPose.toRuntimePose(),
                Matrix4(projectionMatrixData),
                Matrix4(viewMatrixData),
                if (Build.VERSION.SDK_INT >= 27) perceptionManager._latestFrame.hardwareBuffer
                else null,
                getTransformCoordinates2DFunction(),
            )
        } else {
            return CameraState(
                coreState.timeMark,
                TrackingState.fromArCoreTrackingState(camera.trackingState),
            )
        }
    }

    private fun updateCameraStateMap(coreState: CoreState) {
        val cameraState = getCameraState(coreState)

        cameraStateMap.put(coreState.timeMark, cameraState)
        timeMarkQueue.add(coreState.timeMark)

        if (timeMarkQueue.size > MAX_CAMERA_STATE_EXTENSION_SIZE) {
            val timeMark = timeMarkQueue.removeFirst()
            cameraStateMap.remove(timeMark)
        }
    }
}

/** The state of the ARCore 1.x Camera. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public val CoreState.cameraState: CameraState?
    get() = CameraStateExtender.cameraStateMap[this.timeMark]
