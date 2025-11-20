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

import android.app.Activity
import android.hardware.HardwareBuffer
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.TrackingState as JXRCoreTrackingState
import androidx.xr.runtime.math.Matrix4
import com.google.ar.core.Camera
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Pose as ArCorePose
import com.google.ar.core.TrackingState as ARCoreTrackingState
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class CameraStateExtenderTest {

    private lateinit var runtime: ArCoreRuntime
    private lateinit var perceptionManager: ArCorePerceptionManager
    private lateinit var frame: Frame
    private lateinit var camera: Camera
    private lateinit var timeSource: TestTimeSource
    private lateinit var underTest: CameraStateExtender

    @Before
    fun setUp() {
        timeSource = TestTimeSource()
        var arCoreTimeSource = ArCoreTimeSource()
        var lifecycleManager =
            ArCoreManager(Activity(), ArCorePerceptionManager(arCoreTimeSource), arCoreTimeSource)
        perceptionManager = ArCorePerceptionManager(ArCoreTimeSource())
        runtime = ArCoreRuntime(lifecycleManager, perceptionManager)
        frame = mock<Frame>()
        camera = mock<Camera>()
        whenever(frame.camera).thenReturn(camera)
        perceptionManager._latestFrame = frame
        underTest = CameraStateExtender()
    }

    @After
    fun tearDown() {
        underTest.close()
    }

    @Test
    fun extend_notInitialized_throwsIllegalStateException(): Unit = runBlocking {
        val coreState = CoreState(timeSource.markNow())

        assertFailsWith<IllegalStateException> { underTest.extend(coreState) }
    }

    @Test
    fun extend_withNotTrackingState_returnsNulls(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(runtime))
        whenever(camera.trackingState).thenReturn(ARCoreTrackingState.PAUSED)

        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)

        // act
        underTest.extend(coreState)

        // assert
        assertThat(coreState.cameraState).isNotNull()
        assertThat(coreState.cameraState!!.trackingState).isEqualTo(JXRCoreTrackingState.PAUSED)
        assertThat(coreState.cameraState!!.cameraPose).isNull()
        assertThat(coreState.cameraState!!.displayOrientedPose).isNull()
        assertThat(coreState.cameraState!!.projectionMatrix).isNull()
        assertThat(coreState.cameraState!!.viewMatrix).isNull()
        assertThat(coreState.cameraState!!.hardwareBuffer).isNull()
        assertThat(coreState.cameraState!!.transformCoordinates2D).isNull()
    }

    @Test
    @Config(maxSdk = 26)
    fun extend_ApiLevelBelow27_withIsTrackingState_returnsCorrectValues(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(runtime))
        perceptionManager.setDisplayRotation(Surface.ROTATION_0, 100, 100)
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        // number of components * number of vertices * size of float
        val BUFFER_SIZE: Int = 2 * 4 * 4
        val inputVertices: FloatBuffer =
            ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer()
        inputVertices.put(
            floatArrayOf(/*0:*/ -1f, -1f, /*1:*/ +1f, -1f, /*2:*/ -1f, +1f, /*3:*/ +1f, +1f)
        )
        whenever(camera.trackingState).thenReturn(ARCoreTrackingState.TRACKING)

        val arCoreCameraPose = ArCorePose(floatArrayOf(1f, 2f, 3f), floatArrayOf(0f, 0f, 0f, 1f))
        val arCoreDisplayOrientedPose =
            ArCorePose(floatArrayOf(4f, 5f, 6f), floatArrayOf(0f, 0f, 0f, 1f))
        val projectionMatrixData = FloatArray(16) { Random.nextFloat() }
        val viewMatrixData = FloatArray(16) { Random.nextFloat() }

        whenever(camera.pose).thenReturn(arCoreCameraPose)
        whenever(camera.displayOrientedPose).thenReturn(arCoreDisplayOrientedPose)
        whenever(camera.getProjectionMatrix(any(), any(), any(), any())).thenAnswer { invocation ->
            val projectionMatrix = invocation.getArgument<FloatArray>(0)
            projectionMatrixData.copyInto(projectionMatrix)
        }
        whenever(camera.getViewMatrix(any(), any())).thenAnswer { invocation ->
            val viewMatrix = invocation.getArgument<FloatArray>(0)
            viewMatrixData.copyInto(viewMatrix)
        }
        whenever(
                frame.transformCoordinates2d(
                    any<Coordinates2d>(),
                    any<FloatBuffer>(),
                    any<Coordinates2d>(),
                    any<FloatBuffer>(),
                )
            )
            .thenAnswer { invocation ->
                val inVertices = invocation.getArgument<FloatBuffer>(1)
                val outVertices = invocation.getArgument<FloatBuffer>(3)
                inVertices.position(0)
                outVertices.put(inVertices)
            }

        // act
        underTest.extend(coreState)

        // assert
        assertThat(coreState.cameraState).isNotNull()
        assertThat(coreState.cameraState!!.trackingState).isEqualTo(JXRCoreTrackingState.TRACKING)
        assertThat(coreState.cameraState!!.cameraPose).isEqualTo(arCoreCameraPose.toRuntimePose())
        assertThat(coreState.cameraState!!.displayOrientedPose)
            .isEqualTo(arCoreDisplayOrientedPose.toRuntimePose())
        assertThat(coreState.cameraState!!.projectionMatrix)
            .isEqualTo(Matrix4(projectionMatrixData))
        assertThat(coreState.cameraState!!.viewMatrix).isEqualTo(Matrix4(viewMatrixData))
        assertThat(coreState.cameraState!!.hardwareBuffer).isNull()
        assertThat(coreState.cameraState!!.transformCoordinates2D).isNotNull()

        // act
        val outputVertices = coreState.cameraState!!.transformCoordinates2D!!.invoke(inputVertices)

        // assert
        assertThat(outputVertices).isEqualTo(inputVertices)
    }

    @Test
    @Config(minSdk = 27)
    fun extend_ApiLevel27AndUp_withIsTrackingState_returnsCorrectValues(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(runtime))
        perceptionManager.setDisplayRotation(Surface.ROTATION_0, 100, 100)
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        // number of components * number of vertices * size of float
        val BUFFER_SIZE: Int = 2 * 4 * 4
        val inputVertices: FloatBuffer =
            ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer()
        inputVertices.put(
            floatArrayOf(/*0:*/ -1f, -1f, /*1:*/ +1f, -1f, /*2:*/ -1f, +1f, /*3:*/ +1f, +1f)
        )
        whenever(camera.trackingState).thenReturn(ARCoreTrackingState.TRACKING)

        val arCoreCameraPose = ArCorePose(floatArrayOf(1f, 2f, 3f), floatArrayOf(0f, 0f, 0f, 1f))
        val arCoreDisplayOrientedPose =
            ArCorePose(floatArrayOf(4f, 5f, 6f), floatArrayOf(0f, 0f, 0f, 1f))
        val projectionMatrixData = FloatArray(16) { Random.nextFloat() }
        val viewMatrixData = FloatArray(16) { Random.nextFloat() }
        HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
            whenever(camera.pose).thenReturn(arCoreCameraPose)
            whenever(camera.displayOrientedPose).thenReturn(arCoreDisplayOrientedPose)
            whenever(camera.getProjectionMatrix(any(), any(), any(), any())).thenAnswer { invocation
                ->
                val projectionMatrix = invocation.getArgument<FloatArray>(0)
                projectionMatrixData.copyInto(projectionMatrix)
            }
            whenever(camera.getViewMatrix(any(), any())).thenAnswer { invocation ->
                val viewMatrix = invocation.getArgument<FloatArray>(0)
                viewMatrixData.copyInto(viewMatrix)
            }
            whenever(frame.hardwareBuffer).thenReturn(hardwareBuffer)
            whenever(
                    frame.transformCoordinates2d(
                        any<Coordinates2d>(),
                        any<FloatBuffer>(),
                        any<Coordinates2d>(),
                        any<FloatBuffer>(),
                    )
                )
                .thenAnswer { invocation ->
                    val inVertices = invocation.getArgument<FloatBuffer>(1)
                    val outVertices = invocation.getArgument<FloatBuffer>(3)
                    inVertices.position(0)
                    outVertices.put(inVertices)
                }

            // act
            underTest.extend(coreState)

            // assert
            assertThat(coreState.cameraState).isNotNull()
            assertThat(coreState.cameraState!!.trackingState)
                .isEqualTo(JXRCoreTrackingState.TRACKING)
            assertThat(coreState.cameraState!!.cameraPose)
                .isEqualTo(arCoreCameraPose.toRuntimePose())
            assertThat(coreState.cameraState!!.displayOrientedPose)
                .isEqualTo(arCoreDisplayOrientedPose.toRuntimePose())
            assertThat(coreState.cameraState!!.projectionMatrix)
                .isEqualTo(Matrix4(projectionMatrixData))
            assertThat(coreState.cameraState!!.viewMatrix).isEqualTo(Matrix4(viewMatrixData))
            assertThat(coreState.cameraState!!.hardwareBuffer).isEqualTo(hardwareBuffer)
            assertThat(coreState.cameraState!!.transformCoordinates2D).isNotNull()

            // act
            val outputVertices =
                coreState.cameraState!!.transformCoordinates2D!!.invoke(inputVertices)

            // assert
            assertThat(outputVertices).isEqualTo(inputVertices)
        }
    }

    @Test
    @Config(maxSdk = 26)
    fun extend_ApiLevelBelow27_withDisplayUnchanged_returnsNullTransformCoordinates2D(): Unit =
        runBlocking {
            // arrange
            underTest.initialize(listOf(runtime))
            val timeMark = timeSource.markNow()
            val coreState = CoreState(timeMark)
            whenever(camera.trackingState).thenReturn(ARCoreTrackingState.TRACKING)

            val arCoreCameraPose =
                ArCorePose(floatArrayOf(1f, 2f, 3f), floatArrayOf(0f, 0f, 0f, 1f))
            val arCoreDisplayOrientedPose =
                ArCorePose(floatArrayOf(4f, 5f, 6f), floatArrayOf(0f, 0f, 0f, 1f))
            val projectionMatrixData = FloatArray(16) { Random.nextFloat() }
            val viewMatrixData = FloatArray(16) { Random.nextFloat() }

            whenever(camera.pose).thenReturn(arCoreCameraPose)
            whenever(camera.displayOrientedPose).thenReturn(arCoreDisplayOrientedPose)
            whenever(camera.getProjectionMatrix(any(), any(), any(), any())).thenAnswer { invocation
                ->
                val projectionMatrix = invocation.getArgument<FloatArray>(0)
                projectionMatrixData.copyInto(projectionMatrix)
            }
            whenever(camera.getViewMatrix(any(), any())).thenAnswer { invocation ->
                val viewMatrix = invocation.getArgument<FloatArray>(0)
                viewMatrixData.copyInto(viewMatrix)
            }

            // act
            underTest.extend(coreState)

            // assert
            assertThat(coreState.cameraState).isNotNull()
            assertThat(coreState.cameraState!!.trackingState)
                .isEqualTo(JXRCoreTrackingState.TRACKING)
            assertThat(coreState.cameraState!!.cameraPose)
                .isEqualTo(arCoreCameraPose.toRuntimePose())
            assertThat(coreState.cameraState!!.displayOrientedPose)
                .isEqualTo(arCoreDisplayOrientedPose.toRuntimePose())
            assertThat(coreState.cameraState!!.projectionMatrix)
                .isEqualTo(Matrix4(projectionMatrixData))
            assertThat(coreState.cameraState!!.viewMatrix).isEqualTo(Matrix4(viewMatrixData))
            assertThat(coreState.cameraState!!.hardwareBuffer).isNull()
            assertThat(coreState.cameraState!!.transformCoordinates2D).isNull()
        }

    @Test
    @Config(minSdk = 27)
    fun extend_ApiLevel27AndUp_withDisplayUnchanged_returnsNullTransformCoordinates2D(): Unit =
        runBlocking {
            // arrange
            underTest.initialize(listOf(runtime))
            val timeMark = timeSource.markNow()
            val coreState = CoreState(timeMark)
            whenever(camera.trackingState).thenReturn(ARCoreTrackingState.TRACKING)

            val arCoreCameraPose =
                ArCorePose(floatArrayOf(1f, 2f, 3f), floatArrayOf(0f, 0f, 0f, 1f))
            val arCoreDisplayOrientedPose =
                ArCorePose(floatArrayOf(4f, 5f, 6f), floatArrayOf(0f, 0f, 0f, 1f))
            val projectionMatrixData = FloatArray(16) { Random.nextFloat() }
            val viewMatrixData = FloatArray(16) { Random.nextFloat() }
            HardwareBuffer.create(1, 1, HardwareBuffer.RGBA_8888, 1, 1).use { hardwareBuffer ->
                whenever(camera.pose).thenReturn(arCoreCameraPose)
                whenever(camera.displayOrientedPose).thenReturn(arCoreDisplayOrientedPose)
                whenever(camera.getProjectionMatrix(any(), any(), any(), any())).thenAnswer {
                    invocation ->
                    val projectionMatrix = invocation.getArgument<FloatArray>(0)
                    projectionMatrixData.copyInto(projectionMatrix)
                }
                whenever(camera.getViewMatrix(any(), any())).thenAnswer { invocation ->
                    val viewMatrix = invocation.getArgument<FloatArray>(0)
                    viewMatrixData.copyInto(viewMatrix)
                }
                whenever(frame.hardwareBuffer).thenReturn(hardwareBuffer)

                // act
                underTest.extend(coreState)

                // assert
                assertThat(coreState.cameraState).isNotNull()
                assertThat(coreState.cameraState!!.trackingState)
                    .isEqualTo(JXRCoreTrackingState.TRACKING)
                assertThat(coreState.cameraState!!.cameraPose)
                    .isEqualTo(arCoreCameraPose.toRuntimePose())
                assertThat(coreState.cameraState!!.displayOrientedPose)
                    .isEqualTo(arCoreDisplayOrientedPose.toRuntimePose())
                assertThat(coreState.cameraState!!.projectionMatrix)
                    .isEqualTo(Matrix4(projectionMatrixData))
                assertThat(coreState.cameraState!!.viewMatrix).isEqualTo(Matrix4(viewMatrixData))
                assertThat(coreState.cameraState!!.hardwareBuffer).isEqualTo(hardwareBuffer)
                assertThat(coreState.cameraState!!.transformCoordinates2D).isNull()
            }
        }

    @Test
    fun extend_cameraStateMapSizeExceedsMax(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(runtime))
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        whenever(camera.trackingState).thenReturn(ARCoreTrackingState.PAUSED)

        // act
        underTest.extend(coreState)
        // make sure the camera state is added to the map at the beginning
        check(coreState.cameraState != null)

        for (i in 1..CameraStateExtender.MAX_CAMERA_STATE_EXTENSION_SIZE) {
            timeSource += 10.milliseconds
            underTest.extend(CoreState(timeSource.markNow()))
        }

        // assert
        assertThat(coreState.cameraState).isNull()
    }

    @Test
    fun close_cleanUpData(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(runtime))
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        whenever(camera.trackingState).thenReturn(ARCoreTrackingState.PAUSED)
        underTest.extend(coreState)
        // make sure the camera state is added to the map at the beginning
        check(coreState.cameraState != null)

        // act
        underTest.close()

        // assert
        assertThat(coreState.cameraState).isNull()
    }
}
