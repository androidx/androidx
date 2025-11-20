/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
@DoNotInstrument
class ZoomCompatTest {
    @Test
    @Config(minSdk = 30)
    fun canProvideZoomCompat_whenGettingControlZoomRatioThrowsError() {
        assertThat(ZoomCompat.Bindings.provideZoomCompat(throwingCameraProperties))
            .isInstanceOf(CropRegionZoomCompat::class.java)
    }

    @Test
    fun canProvideZoomCompat_whenGettingActiveArraySizeReturnsNull() {
        assertThat(ZoomCompat.Bindings.provideZoomCompat(FakeCameraProperties()))
            .isInstanceOf(NoOpZoomCompat::class.java)
    }

    @Test
    @Config(maxSdk = 29)
    fun reset_CropRegionZoomCompat_removeParameters() {
        val fakeRequestControl = FakeUseCaseCameraRequestControl()
        val fakeCameraMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 10, 10))
            )
        val zoomCompat = CropRegionZoomCompat(FakeCameraProperties(fakeCameraMetadata))
        zoomCompat.resetAsync(fakeRequestControl)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertWithMessage("CONTROL_ZOOM_RATIO not reset by default zoom state")
                .that(fakeRequestControl.removeParameterCalls)
                .contains(CaptureRequest.CONTROL_ZOOM_RATIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                assertWithMessage("CONTROL_SETTINGS_OVERRIDE not reset by default zoom state")
                    .that(fakeRequestControl.removeParameterCalls)
                    .contains(CaptureRequest.CONTROL_SETTINGS_OVERRIDE)
            }
        } else {
            assertWithMessage("SCALER_CROP_REGION not reset by default zoom state")
                .that(fakeRequestControl.removeParameterCalls)
                .contains(CaptureRequest.SCALER_CROP_REGION)
        }
    }

    @Test
    @Config(minSdk = 30)
    fun reset_AndroidRZoomCompat_removeParameters() {
        val fakeRequestControl = FakeUseCaseCameraRequestControl()
        val zoomCompat = AndroidRZoomCompat(FakeCameraProperties(), Range(1.0f, 5.0f))
        zoomCompat.resetAsync(fakeRequestControl)

        assertWithMessage("CONTROL_ZOOM_RATIO not reset by default zoom state")
            .that(fakeRequestControl.removeParameterCalls)
            .contains(CaptureRequest.CONTROL_ZOOM_RATIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            assertWithMessage("CONTROL_SETTINGS_OVERRIDE not reset by default zoom state")
                .that(fakeRequestControl.removeParameterCalls)
                .contains(CaptureRequest.CONTROL_SETTINGS_OVERRIDE)
        }
    }

    private val throwingCameraProperties =
        object : CameraProperties {
            override val cameraId: CameraId
                get() = TODO("Not yet implemented")

            override val metadata: CameraMetadata
                get() = throwingCameraMetadata
        }

    private val throwingCameraMetadata =
        object : CameraMetadata {
            override val camera: CameraId
                get() = TODO("Not yet implemented")

            override val isRedacted: Boolean
                get() = TODO("Not yet implemented")

            override val keys: Set<CameraCharacteristics.Key<*>>
                get() = TODO("Not yet implemented")

            override val physicalCameraIds: Set<CameraId>
                get() = TODO("Not yet implemented")

            override val physicalRequestKeys: Set<CaptureRequest.Key<*>>
                get() = TODO("Not yet implemented")

            override val supportedExtensions: Set<Int>
                get() = TODO("Not yet implemented")

            override val requestKeys: Set<CaptureRequest.Key<*>>
                get() = TODO("Not yet implemented")

            override val resultKeys: Set<CaptureResult.Key<*>>
                get() = TODO("Not yet implemented")

            override val sessionKeys: Set<CaptureRequest.Key<*>>
                get() = TODO("Not yet implemented")

            override val sessionCharacteristicsKeys: Set<CameraCharacteristics.Key<*>>
                get() = TODO("Not yet implemented")

            override fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata {
                TODO("Not yet implemented")
            }

            override suspend fun getExtensionMetadata(extension: Int): CameraExtensionMetadata {
                TODO("Not yet implemented")
            }

            override fun awaitExtensionMetadata(extension: Int): CameraExtensionMetadata {
                TODO("Not yet implemented")
            }

            override fun <T> get(key: CameraCharacteristics.Key<T>): T? {
                println("throwingCameraMetadata get: key = $key")
                if (
                    Build.VERSION.SDK_INT >= 30 &&
                        key == CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE
                ) {
                    throw AssertionError()
                }
                if (key == CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) {
                    @Suppress("UNCHECKED_CAST") // T is guaranteed to be Rect
                    return Rect(0, 0, 4000, 3000) as T?
                }
                TODO("Not yet implemented")
            }

            override fun <T> get(key: Metadata.Key<T>): T? {
                TODO("Not yet implemented")
            }

            override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
                TODO("Not yet implemented")
            }

            override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T {
                TODO("Not yet implemented")
            }

            override suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata {
                TODO("Not yet implemented")
            }

            override fun <T : Any> unwrapAs(type: KClass<T>): T? {
                TODO("Not yet implemented")
            }
        }
}
