/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.camera.core.impl

import android.os.Build
import androidx.camera.core.CameraUseCaseAdapterProvider
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.SessionConfig
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.FakeStreamSpecsCalculator
import androidx.camera.testing.impl.FrameRateUtil.FPS_120_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_240_240
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_240
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val CAMERA_ID = "2"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraInfoInternalTest {

    @Test
    fun selector_findsMatchingCamera() {
        val cameraInfo = FakeCameraInfoInternal(CAMERA_ID)
        val cameras = createCamerasWithIds(arrayOf(1, CAMERA_ID.toInt(), 3, 4))
        val filteredCameras = cameraInfo.cameraSelector.filter(LinkedHashSet(cameras))

        assertThat(filteredCameras).hasSize(1)
        assertThat(filteredCameras.first().cameraInfoInternal.cameraId).isEqualTo(CAMERA_ID)
    }

    @Test(expected = IllegalStateException::class)
    fun selector_doesNotFindMatchingCamera() {
        val cameraInfo = FakeCameraInfoInternal(CAMERA_ID)
        val cameras = createCamerasWithIds(arrayOf(1, 3, 4))
        cameraInfo.cameraSelector.filter(LinkedHashSet(cameras))
    }

    @OptIn(ExperimentalSessionConfig::class)
    @Test
    fun getSupportedFrameRateRanges_regularSession_returnAllFrameRateRanges() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setCameraUseCaseAdapterProvider(createFakeCameraUseCaseAdapterProvider())
            }
        val supportedFrameRateRanges = cameraInfo.getSupportedFrameRateRanges(SessionConfig())
        assertThat(supportedFrameRateRanges)
            .containsExactlyElementsIn(cameraInfo.supportedFrameRateRanges)
    }

    @OptIn(ExperimentalSessionConfig::class)
    @Test
    fun getSupportedFrameRateRanges_highSpeedSession_filterFixedFrameRateRanges() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setCameraUseCaseAdapterProvider(createFakeCameraUseCaseAdapterProvider())
                setSupportedHighSpeedResolutions(FPS_30_120, listOf(RESOLUTION_720P))
                setSupportedHighSpeedResolutions(FPS_120_120, listOf(RESOLUTION_720P))
                setSupportedHighSpeedResolutions(FPS_30_240, listOf(RESOLUTION_720P))
                setSupportedHighSpeedResolutions(FPS_240_240, listOf(RESOLUTION_720P))
            }
        val supportedFrameRateRanges =
            cameraInfo.getSupportedFrameRateRanges(
                object : SessionConfig() {
                    override val sessionType: Int = SESSION_TYPE_HIGH_SPEED
                }
            )
        assertThat(supportedFrameRateRanges).containsExactly(FPS_120_120, FPS_240_240)
    }

    private fun createCamerasWithIds(ids: Array<Int>): List<CameraInternal> {
        return ids.map { FakeCamera(it.toString()) }
    }

    private fun createFakeCameraUseCaseAdapterProvider(): CameraUseCaseAdapterProvider {
        val cameraUseCaseAdapter =
            CameraUseCaseAdapter(
                FakeCamera(),
                FakeCameraCoordinator(),
                FakeStreamSpecsCalculator(),
                FakeUseCaseConfigFactory(),
            )
        return object : CameraUseCaseAdapterProvider {
            @Throws(IllegalArgumentException::class)
            override fun provide(cameraId: String): CameraUseCaseAdapter {
                return cameraUseCaseAdapter
            }

            override fun provide(
                camera: CameraInternal,
                secondaryCamera: CameraInternal?,
                adapterCameraInfo: AdapterCameraInfo,
                secondaryAdapterCameraInfo: AdapterCameraInfo?,
                compositionSettings: CompositionSettings,
                secondaryCompositionSettings: CompositionSettings,
            ): CameraUseCaseAdapter {
                return cameraUseCaseAdapter
            }
        }
    }
}
