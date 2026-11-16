/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.compose

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.view.Surface
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.ForwardingCameraInfo
import androidx.camera.core.impl.utils.TransformUtils
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.TransformationMode
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@SuppressLint("RestrictedApiAndroidX")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument // Needed for Robolectric to correctly instrument classes in the same module
class DisplayRotationTransformationTest {

    // getSensorRotationDegrees(): ROTATION_0 -> 90, ROTATION_90 -> 0, ROTATION_180 -> 270,
    // ROTATION_270 -> 180.
    private val backCamera = FakeCameraInfoInternal(90, CameraSelector.LENS_FACING_BACK)

    private val rotations =
        listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)

    @Test
    fun preApplied_subtractsBakedRotationFromSensorRotation_andReversesWhenMirrored() {
        // (baked rotation, mirrored, display rotation) to expected source rotation.
        val expectations =
            mapOf(
                // Sensor rotation at ROTATION_90 is 0; 0 - 90 = 270 degrees.
                Triple(90, false, Surface.ROTATION_90) to 270,
                // -(0 - 90) = 90 degrees.
                Triple(90, true, Surface.ROTATION_90) to 90,
                // A 180-degree display rotation adds a half turn, with or without mirroring.
                Triple(90, false, Surface.ROTATION_270) to 90,
                Triple(90, true, Surface.ROTATION_270) to 270,
                // Sensor rotation at ROTATION_0 is 90; 90 - 0 = 90 degrees.
                Triple(0, false, Surface.ROTATION_0) to 90,
                // Sensor rotation at ROTATION_180 is 270; 270 - 180 = 90 degrees.
                Triple(180, false, Surface.ROTATION_180) to 90,
            )
        for ((input, expected) in expectations) {
            val (bakedRotation, isMirrored, displayRotation) = input
            val sensorToBuffer = sensorToBufferMatrix(bakedRotation, isMirrored)
            val result =
                cameraXInfo(hasCameraTransform = false, sensorToBufferTransform = sensorToBuffer)
                    .toViewfinderTransformationInfo(backCamera, displayRotation, sensorToBuffer)

            assertWithMessage("$input").that(result.sourceRotation).isEqualTo(expected)
        }
    }

    @Test
    fun preApplied_nonUniformSensorScaling_extractsExactBakedRotation() {
        for (bakedRotation in listOf(0, 90, 180, 270)) {
            for (isMirrored in listOf(false, true)) {
                val matrix =
                    Matrix().apply {
                        setScale(0.5f, 0.75f)
                        postConcat(
                            TransformUtils.getRectToRect(
                                RectF(0f, 0f, 1920f, 1080f),
                                if (bakedRotation % 180 == 0) {
                                    RectF(0f, 0f, 1280f, 720f)
                                } else {
                                    RectF(0f, 0f, 720f, 1280f)
                                },
                                bakedRotation,
                                isMirrored,
                            )
                        )
                    }
                for (display in rotations) {
                    val expected =
                        TransformUtils.within360(
                            (backCamera.getSensorRotationDegrees(display) - bakedRotation).let {
                                if (isMirrored) -it else it
                            }
                        )
                    assertThat(preAppliedSourceRotation(matrix, backCamera, display))
                        .isEqualTo(expected)
                }
            }
        }
    }

    @Test
    fun preApplied_preservesOtherFields() {
        val info =
            cameraXInfo(
                hasCameraTransform = false,
                sensorToBufferTransform =
                    sensorToBufferMatrix(bakedRotationDegrees = 180, isMirrored = false),
                cropRect = Rect(1, 2, 3, 4),
                isMirroring = true,
            )

        val result = info.toViewfinderTransformationInfo(backCamera, Surface.ROTATION_90)

        assertThat(result)
            .isEqualTo(
                TransformationInfo(
                    sourceRotation = 180,
                    isSourceMirroredHorizontally = true,
                    isSourceMirroredVertically = false,
                    cropRectLeft = 1f,
                    cropRectTop = 2f,
                    cropRectRight = 3f,
                    cropRectBottom = 4f,
                    transformationMode = TransformationMode.PRE_APPLIED,
                )
            )
    }

    @Test
    fun deferred_usesSensorRotationRelativeToCurrentDisplay() {
        val info = cameraXInfo(hasCameraTransform = true, sensorToBufferTransform = Matrix())

        for (rotation in rotations) {
            val result = info.toViewfinderTransformationInfo(backCamera, rotation)

            assertThat(result.sourceRotation)
                .isEqualTo(backCamera.getSensorRotationDegrees(rotation))
        }
    }

    private fun sensorToBufferMatrix(bakedRotationDegrees: Int, isMirrored: Boolean): Matrix =
        TransformUtils.getRectToRect(
            RectF(0f, 0f, 1920f, 1080f),
            if (bakedRotationDegrees % 180 == 0) {
                RectF(0f, 0f, 1920f, 1080f)
            } else {
                RectF(0f, 0f, 1080f, 1920f)
            },
            bakedRotationDegrees,
            isMirrored,
        )

    private fun cameraXInfo(
        hasCameraTransform: Boolean,
        sensorToBufferTransform: Matrix,
        cropRect: Rect = Rect(0, 0, 1080, 1920),
        isMirroring: Boolean = false,
    ) =
        SurfaceRequest.TransformationInfo.of(
            cropRect,
            /* rotationDegrees= */ 0,
            Surface.ROTATION_0,
            hasCameraTransform,
            sensorToBufferTransform,
            isMirroring,
        )
}

/**
 * Returns the [AdapterCameraInfo] of physical camera "physical-2", with a sensor rotation of 270
 * degrees, of logical back camera "0", with a sensor rotation of 90 degrees.
 */
internal fun createPhysicalAdapterCameraInfo(): AdapterCameraInfo {
    val physicalCamera =
        object :
            ForwardingCameraInfo(
                FakeCameraInfoInternal("physical-2", 270, CameraSelector.LENS_FACING_BACK)
            ) {
            override fun getCameraSelector(): CameraSelector =
                CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .setPhysicalCameraId("physical-2")
                    .build()
        }
    val logicalCamera =
        object :
            ForwardingCameraInfo(FakeCameraInfoInternal("0", 90, CameraSelector.LENS_FACING_BACK)) {
            override fun getPhysicalCameraInfos(): Set<CameraInfo> = setOf(physicalCamera)
        }
    return AdapterCameraInfo(logicalCamera, "physical-2", CameraConfigs.defaultConfig())
}
