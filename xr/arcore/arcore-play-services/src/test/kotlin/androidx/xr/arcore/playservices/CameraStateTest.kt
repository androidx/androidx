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

import android.hardware.HardwareBuffer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraStateTest {

    private val timeSource = TestTimeSource()

    @Test
    fun equals_sameObject_returnsTrue() {
        HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
            val cameraState1 =
                CameraState(
                    timeMark = timeSource.markNow(),
                    trackingState = TrackingState.TRACKING,
                    cameraPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f)),
                    displayOrientedPose = Pose(Vector3(4f, 5f, 6f), Quaternion(0f, 0f, 0f, 1f)),
                    projectionMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    viewMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    hardwareBuffer = hardwareBuffer,
                    transformCoordinates2D = { inputVertices: FloatBuffer ->
                        val outputVertices =
                            ByteBuffer.allocateDirect(inputVertices.capacity() * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer()
                        inputVertices.put(outputVertices)
                        outputVertices
                    },
                )

            assertThat(cameraState1).isEqualTo(cameraState1)
        }
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
            val cameraState1 =
                CameraState(
                    timeMark = timeSource.markNow(),
                    trackingState = TrackingState.TRACKING,
                    cameraPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f)),
                    displayOrientedPose = Pose(Vector3(4f, 5f, 6f), Quaternion(0f, 0f, 0f, 1f)),
                    projectionMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    viewMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    hardwareBuffer = hardwareBuffer,
                    transformCoordinates2D = { inputVertices: FloatBuffer ->
                        val outputVertices =
                            ByteBuffer.allocateDirect(inputVertices.capacity() * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer()
                        inputVertices.put(outputVertices)
                        outputVertices
                    },
                )
            val cameraState1Copy =
                CameraState(
                    timeMark = cameraState1.timeMark,
                    trackingState = cameraState1.trackingState,
                    cameraPose = cameraState1.cameraPose,
                    displayOrientedPose = cameraState1.displayOrientedPose,
                    projectionMatrix = cameraState1.projectionMatrix,
                    viewMatrix = cameraState1.viewMatrix,
                    hardwareBuffer = cameraState1.hardwareBuffer,
                    transformCoordinates2D = cameraState1.transformCoordinates2D,
                )

            assertThat(cameraState1).isEqualTo(cameraState1Copy)
        }
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
            val cameraState1 =
                CameraState(
                    timeMark = timeSource.markNow(),
                    trackingState = TrackingState.TRACKING,
                    cameraPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f)),
                    displayOrientedPose = Pose(Vector3(4f, 5f, 6f), Quaternion(0f, 0f, 0f, 1f)),
                    projectionMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    viewMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    hardwareBuffer = hardwareBuffer,
                    transformCoordinates2D = { inputVertices: FloatBuffer ->
                        val outputVertices =
                            ByteBuffer.allocateDirect(inputVertices.capacity() * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer()
                        inputVertices.put(outputVertices)
                        outputVertices
                    },
                )
            val cameraState2 =
                CameraState(
                    timeMark = cameraState1.timeMark + 1.seconds,
                    trackingState = TrackingState.PAUSED,
                    cameraPose = cameraState1.cameraPose?.translate(Vector3(1f, 1f, 1f)),
                    displayOrientedPose =
                        cameraState1.displayOrientedPose?.rotate(
                            Quaternion.fromEulerAngles(90f, 0f, 0f)
                        ),
                    projectionMatrix =
                        cameraState1.projectionMatrix?.times(cameraState1.viewMatrix!!),
                    viewMatrix = cameraState1.viewMatrix?.times(cameraState1.projectionMatrix!!),
                    hardwareBuffer = null,
                    transformCoordinates2D = null,
                )

            assertThat(cameraState1).isNotEqualTo(cameraState2)
        }
    }

    @Test
    fun equals_differentObjectType_returnsFalse() {
        HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
            val cameraState1 =
                CameraState(
                    timeMark = timeSource.markNow(),
                    trackingState = TrackingState.TRACKING,
                    cameraPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f)),
                    displayOrientedPose = Pose(Vector3(4f, 5f, 6f), Quaternion(0f, 0f, 0f, 1f)),
                    projectionMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    viewMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    hardwareBuffer = hardwareBuffer,
                    transformCoordinates2D = { inputVertices: FloatBuffer ->
                        val outputVertices =
                            ByteBuffer.allocateDirect(inputVertices.capacity() * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer()
                        inputVertices.put(outputVertices)
                        outputVertices
                    },
                )
            val other = Object()

            assertThat(cameraState1).isNotEqualTo(other)
        }
    }

    @Test
    fun hashCode_sameValues_returnsSameHashCode() {
        HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
            val cameraState1 =
                CameraState(
                    timeMark = timeSource.markNow(),
                    trackingState = TrackingState.TRACKING,
                    cameraPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f)),
                    displayOrientedPose = Pose(Vector3(4f, 5f, 6f), Quaternion(0f, 0f, 0f, 1f)),
                    projectionMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    viewMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    hardwareBuffer = hardwareBuffer,
                    transformCoordinates2D = { inputVertices: FloatBuffer ->
                        val outputVertices =
                            ByteBuffer.allocateDirect(inputVertices.capacity() * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer()
                        inputVertices.put(outputVertices)
                        outputVertices
                    },
                )
            val cameraState1Copy =
                CameraState(
                    timeMark = cameraState1.timeMark,
                    trackingState = cameraState1.trackingState,
                    cameraPose = cameraState1.cameraPose,
                    displayOrientedPose = cameraState1.displayOrientedPose,
                    projectionMatrix = cameraState1.projectionMatrix,
                    viewMatrix = cameraState1.viewMatrix,
                    hardwareBuffer = cameraState1.hardwareBuffer,
                    transformCoordinates2D = cameraState1.transformCoordinates2D,
                )

            assertThat(cameraState1.hashCode()).isEqualTo(cameraState1Copy.hashCode())
        }
    }

    @Test
    fun hashCode_differentValues_returnsDifferentHashCode() {
        HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
            val cameraState1 =
                CameraState(
                    timeMark = timeSource.markNow(),
                    trackingState = TrackingState.TRACKING,
                    cameraPose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1f)),
                    displayOrientedPose = Pose(Vector3(4f, 5f, 6f), Quaternion(0f, 0f, 0f, 1f)),
                    projectionMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    viewMatrix =
                        Matrix4(
                            floatArrayOf(
                                1f,
                                2f,
                                3f,
                                4f,
                                5f,
                                6f,
                                7f,
                                8f,
                                9f,
                                10f,
                                11f,
                                12f,
                                13f,
                                14f,
                                15f,
                                16f,
                            )
                        ),
                    hardwareBuffer = hardwareBuffer,
                    transformCoordinates2D = { inputVertices: FloatBuffer ->
                        val outputVertices =
                            ByteBuffer.allocateDirect(inputVertices.capacity() * 4)
                                .order(ByteOrder.nativeOrder())
                                .asFloatBuffer()
                        inputVertices.put(outputVertices)
                        outputVertices
                    },
                )
            val cameraState2 =
                CameraState(
                    timeMark = cameraState1.timeMark + 1.seconds,
                    trackingState = TrackingState.PAUSED,
                    cameraPose = cameraState1.cameraPose?.translate(Vector3(1f, 1f, 1f)),
                    displayOrientedPose =
                        cameraState1.displayOrientedPose?.rotate(
                            Quaternion.fromEulerAngles(90f, 0f, 0f)
                        ),
                    projectionMatrix =
                        cameraState1.projectionMatrix?.times(cameraState1.viewMatrix!!),
                    viewMatrix = cameraState1.viewMatrix?.times(cameraState1.projectionMatrix!!),
                    hardwareBuffer = null,
                    transformCoordinates2D = null,
                )

            assertThat(cameraState1.hashCode()).isNotEqualTo(cameraState2.hashCode())
        }
    }
}
