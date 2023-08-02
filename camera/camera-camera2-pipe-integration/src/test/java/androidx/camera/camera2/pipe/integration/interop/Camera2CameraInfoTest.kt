/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.interop

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator.createCameraInfoAdapter
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraState
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExposureState
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.Timebase
import androidx.lifecycle.LiveData
import com.google.common.truth.Truth
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCamera2Interop::class)
class Camera2CameraInfoTest {

    @Test
    fun canGetId_fromCamera2CameraInfo() {
        val cameraId = "42"
        val camera2CameraInfo =
            Camera2CameraInfo.from(createCameraInfoAdapter(cameraId = CameraId(cameraId)))
        val extractedId: String = camera2CameraInfo.getCameraId()

        // Assert.
        Truth.assertThat(extractedId).isEqualTo(cameraId)
    }

    @Test
    fun canExtractCharacteristics_fromCamera2CameraInfo() {
        val cameraHardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        val camera2CameraInfo = Camera2CameraInfo.from(
            createCameraInfoAdapter(
                cameraProperties = FakeCameraProperties(
                    FakeCameraMetadata(
                        characteristics = mapOf(
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                                cameraHardwareLevel,
                            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to
                                Rect(0, 0, 640, 480)
                        )
                    )
                )
            )
        )

        val hardwareLevel: Int? = camera2CameraInfo.getCameraCharacteristic<Int>(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        )

        // Assert.
        Truth.assertThat(hardwareLevel).isEqualTo(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    @Test
    fun canGetCamera2CameraInfo() {
        val cameraInfoAdapter = createCameraInfoAdapter()

        val camera2CameraInfo: Camera2CameraInfo = cameraInfoAdapter.camera2CameraInfo
        val resultCamera2CameraInfo: Camera2CameraInfo = Camera2CameraInfo.from(cameraInfoAdapter)

        // Assert.
        Truth.assertThat(resultCamera2CameraInfo).isEqualTo(camera2CameraInfo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromCameraInfoThrows_whenNotCamera2Impl() {
        val wrongCameraInfo = object : CameraInfoInternal {
            override fun getSensorRotationDegrees(): Int {
                throw NotImplementedError("Not used in testing")
            }

            override fun getSensorRotationDegrees(relativeRotation: Int): Int {
                throw NotImplementedError("Not used in testing")
            }

            override fun hasFlashUnit(): Boolean {
                throw NotImplementedError("Not used in testing")
            }

            override fun getTorchState(): LiveData<Int> {
                throw NotImplementedError("Not used in testing")
            }

            override fun getZoomState(): LiveData<ZoomState> {
                throw NotImplementedError("Not used in testing")
            }

            override fun getExposureState(): ExposureState {
                throw NotImplementedError("Not used in testing")
            }

            override fun getCameraState(): LiveData<CameraState> {
                throw NotImplementedError("Not used in testing")
            }

            override fun getImplementationType(): String {
                throw NotImplementedError("Not used in testing")
            }

            override fun getLensFacing(): Int {
                throw NotImplementedError("Not used in testing")
            }

            override fun getCameraId(): String {
                throw NotImplementedError("Not used in testing")
            }

            override fun addSessionCaptureCallback(
                executor: Executor,
                callback: CameraCaptureCallback
            ) {
                throw NotImplementedError("Not used in testing")
            }

            override fun removeSessionCaptureCallback(callback: CameraCaptureCallback) {
                throw NotImplementedError("Not used in testing")
            }

            override fun getCameraQuirks(): Quirks {
                throw NotImplementedError("Not used in testing")
            }

            override fun getSupportedFrameRateRanges(): Set<Range<Int>> {
                throw NotImplementedError("Not used in testing")
            }

            override fun getEncoderProfilesProvider(): EncoderProfilesProvider {
                throw NotImplementedError("Not used in testing")
            }

            override fun getTimebase(): Timebase {
                throw NotImplementedError("Not used in testing")
            }

            override fun getSupportedResolutions(format: Int): MutableList<Size> {
                throw NotImplementedError("Not used in testing")
            }

            override fun getSupportedHighResolutions(format: Int): MutableList<Size> {
                throw NotImplementedError("Not used in testing")
            }

            override fun getSupportedDynamicRanges(): MutableSet<DynamicRange> {
                throw NotImplementedError("Not used in testing")
            }
        }
        Camera2CameraInfo.from(wrongCameraInfo)
    }

    // TODO: Port https://android-review.googlesource.com/c/platform/frameworks/support/+/1757509
    //  for extension features.
}
