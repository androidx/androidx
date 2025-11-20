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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.testing.FakeCamera2MetadataProvider
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.M)
class Camera2QuirksTest {
    private val fakeCameraId: CameraId = RobolectricCameras.create()

    private val fakeCameraMetadata =
        FakeCameraMetadata(
            characteristics =
                mapOf(
                    Pair(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                        INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                    )
                )
        )

    private val metadataProvider =
        FakeCamera2MetadataProvider(mapOf(fakeCameraId to fakeCameraMetadata))

    private val graphConfigFlags = CameraGraph.Flags()

    private val graphConfig = CameraGraph.Config(camera = fakeCameraId, streams = listOf())

    @Test
    fun shouldWaitForRepeatingRequestStartOnDisconnect_strict_mode_off() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(metadataProvider = metadataProvider, cameraPipeFlags = CameraPipe.Flags())

        // verify function return true
        assertThat(camera2Quirks.shouldWaitForRepeatingRequestStartOnDisconnect(graphConfig))
            .isTrue()
    }

    @Test
    fun shouldWaitForRepeatingRequestStartOnDisconnect_strict_mode_on() {
        // strict mode on
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(strictModeEnabled = true),
            )

        // verify
        assertThat(camera2Quirks.shouldWaitForRepeatingRequestStartOnDisconnect(graphConfig))
            .isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.P])
    @Test
    fun shouldCreateEmptyCaptureSessionBeforeClosing_strict_mode_off_within_sdk_range() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(metadataProvider = metadataProvider, cameraPipeFlags = CameraPipe.Flags())

        // verify
        assertThat(camera2Quirks.shouldCreateEmptyCaptureSessionBeforeClosing(fakeCameraId))
            .isTrue()
    }

    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.Q])
    @Test
    fun shouldCreateEmptyCaptureSessionBeforeClosing_strict_mode_off_outside_sdk_range() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(strictModeEnabled = true),
            )

        // verify
        assertThat(camera2Quirks.shouldCreateEmptyCaptureSessionBeforeClosing(fakeCameraId))
            .isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.P])
    @Test
    fun shouldCreateEmptyCaptureSessionBeforeClosing_strict_mode_on_within_sdk_range() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(strictModeEnabled = true),
            )

        // verify
        assertThat(camera2Quirks.shouldCreateEmptyCaptureSessionBeforeClosing(fakeCameraId))
            .isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.Q])
    @Test
    fun shouldCreateEmptyCaptureSessionBeforeClosing_strict_mode_on_outside_sdk_range() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(strictModeEnabled = true),
            )

        // verify
        assertThat(camera2Quirks.shouldCreateEmptyCaptureSessionBeforeClosing(fakeCameraId))
            .isFalse()
    }

    @Test
    fun shouldWaitForCameraDeviceOnClosed_strict_mode_off() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(metadataProvider = metadataProvider, cameraPipeFlags = CameraPipe.Flags())

        assertThat(camera2Quirks.shouldWaitForCameraDeviceOnClosed(fakeCameraId)).isTrue()
    }

    @Test
    fun shouldWaitForCameraDeviceOnClosed_strict_mode_on() {
        // strict mode on
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(strictModeEnabled = true),
            )

        assertThat(camera2Quirks.shouldWaitForCameraDeviceOnClosed(fakeCameraId)).isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.S_V2])
    @Test
    fun shouldCloseCameraBeforeCreatingCaptureSession_strict_mode_off_within_sdk_range() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(metadataProvider = metadataProvider, cameraPipeFlags = CameraPipe.Flags())

        // verify
        assertThat(camera2Quirks.shouldCloseCameraBeforeCreatingCaptureSession(fakeCameraId))
            .isTrue()
    }

    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    @Test
    fun shouldCloseCameraBeforeCreatingCaptureSession_strict_mode_off_outside_sdk_range() {
        // strict mode off by default
        val camera2Quirks =
            Camera2Quirks(metadataProvider = metadataProvider, cameraPipeFlags = CameraPipe.Flags())

        // verify
        assertThat(camera2Quirks.shouldCloseCameraBeforeCreatingCaptureSession(fakeCameraId))
            .isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.S_V2])
    @Test
    fun shouldCloseCameraBeforeCreatingCaptureSession_strict_mode_on_within_sdk_range() {
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(strictModeEnabled = true),
            )

        // verify
        assertThat(camera2Quirks.shouldCloseCameraBeforeCreatingCaptureSession(fakeCameraId))
            .isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.TIRAMISU])
    @Test
    fun shouldCloseCameraBeforeCreatingCaptureSession_strict_mode_on_outside_sdk_range() {
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(strictModeEnabled = true),
            )

        // verify
        assertThat(camera2Quirks.shouldCloseCameraBeforeCreatingCaptureSession(fakeCameraId))
            .isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.TIRAMISU])
    @Test
    fun getRepeatingRequestFrameCountForCapture_strict_mode_off() {
        val camera2Quirks =
            Camera2Quirks(metadataProvider = metadataProvider, cameraPipeFlags = CameraPipe.Flags())

        val repeat = 3u
        val flags =
            CameraGraph.Flags(
                awaitRepeatingRequestBeforeCapture =
                    CameraGraph.RepeatingRequestRequirementsBeforeCapture(
                        repeatingFramesToComplete = repeat
                    )
            )
        assertThat(camera2Quirks.getRepeatingRequestFrameCountForCapture(flags))
            .isEqualTo(repeat.toInt())
    }

    @Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.TIRAMISU])
    @Test
    fun getRepeatingRequestFrameCountForCapture_strict_mode_on() {
        val camera2Quirks =
            Camera2Quirks(
                metadataProvider = metadataProvider,
                cameraPipeFlags = CameraPipe.Flags(true),
            )

        val repeat = 3u
        val flags =
            CameraGraph.Flags(
                awaitRepeatingRequestBeforeCapture =
                    CameraGraph.RepeatingRequestRequirementsBeforeCapture(
                        repeatingFramesToComplete = repeat
                    )
            )
        assertThat(camera2Quirks.getRepeatingRequestFrameCountForCapture(flags)).isEqualTo(0)
    }
}
