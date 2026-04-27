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
@file:JvmName("CameraStateExt")

package androidx.xr.arcore.playservices

import android.os.Build
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.StateExtender
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.math.Matrix4
import com.google.ar.core.Coordinates2d
import com.google.ar.core.TrackingState as ARCoreTrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.time.ComparableTimeMark

/** [StateExtender] in charge of extending [CoreState] with [CameraState]. */
// TODO(b/500400207): Dynamically load if play-services runtime is loaded and CAMERA feature
// detected.
@Suppress("NotCloseable")
internal class CameraStateExtender : StateExtender {

    internal companion object {
        internal const val MAX_CAMERA_STATE_EXTENSION_SIZE = 100

        internal val cameraStateMap = mutableMapOf<ComparableTimeMark, CameraState>()

        private val timeMarkQueue = ArrayDeque<ComparableTimeMark>()
    }

    internal lateinit var perceptionManager: ArCorePerceptionManager

    private var isInitialized = false
    private var isPlayServicesEnvironment = false
    private var outputVerticesBuffer: FloatBuffer? = null
    private var hasProvidedTransform = false

    override fun initialize(runtimes: List<JxrRuntime>) {
        isInitialized = true

        val manager =
            runtimes.filterIsInstance<PerceptionRuntime>().firstOrNull()?.perceptionManager
        if (manager is ArCorePerceptionManager) {
            perceptionManager = manager
            isPlayServicesEnvironment = true
        }
    }

    override suspend fun extend(coreState: CoreState) {
        check(isInitialized) { "CameraStateExtender is not initialized." }
        if (!isPlayServicesEnvironment) return
        synchronized(perceptionManager.frameLock) { updateCameraStateMap(coreState) }
    }

    override fun close() {
        cameraStateMap.clear()
        timeMarkQueue.clear()
        outputVerticesBuffer = null
        hasProvidedTransform = false
    }

    private fun getTransformCoordinates2DFunction(): ((FloatBuffer) -> FloatBuffer)? {
        if (!perceptionManager.isSessionInitialized) {
            return null
        }

        // TODO(b/505484455): Monitor the CameraConfig and/or ImageStabilizationMode of the ARCore
        // session to force coordinate transformation recalculation when the hardware or software
        // configuration changes.

        if (!perceptionManager.displayChanged && hasProvidedTransform) {
            return null
        }

        return { inputVertices: FloatBuffer ->
            val originalPosition = inputVertices.position()
            inputVertices.rewind()
            try {
                val requiredCapacity = inputVertices.limit()
                synchronized(perceptionManager.frameLock) {
                    var outputVertices = outputVerticesBuffer
                    if (outputVertices == null || outputVertices.capacity() < requiredCapacity) {
                        outputVertices =
                            ByteBuffer.allocateDirect(requiredCapacity * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer()
                        outputVerticesBuffer = outputVertices
                    }
                    outputVertices.clear()
                    outputVertices.limit(requiredCapacity)

                    perceptionManager._latestFrame.transformCoordinates2d(
                        Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                        inputVertices,
                        Coordinates2d.TEXTURE_NORMALIZED,
                        outputVertices,
                    )
                    perceptionManager.displayChanged = false
                    hasProvidedTransform = true
                    outputVertices.rewind()
                    outputVertices
                }
            } finally {
                inputVertices.position(originalPosition)
            }
        }
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

/**
 * Provides the latest [CameraState], which contains the latest information about the device camera,
 * such as pose, projection, and timestamp for the current frame.
 */
@ExperimentalCameraApi
@Suppress("ExperimentalPropertyAnnotation")
public val CoreState.cameraState: CameraState?
    get() = CameraStateExtender.cameraStateMap[this.timeMark]
