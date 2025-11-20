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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraNoResponseWhenEnablingFlashQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureWashedOutImageQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.UltraWideFlashCaptureUnderexposureQuirk
import androidx.camera.camera2.pipe.integration.internal.IntrinsicZoomCalculator
import androidx.camera.camera2.pipe.integration.testing.FakeUseTorchAsFlash.createUseTorchAsFlash
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class UseTorchAsFlashTest {

    @Test
    fun shouldUseTorchAsFlash_default_isFalse() = runBlocking {
        val useTorchAsFlash = createUseTorchAsFlash()

        assertThat(useTorchAsFlash.shouldUseTorchAsFlash { null }).isFalse()
    }

    @Test
    fun shouldUseTorchAsFlash_withCameraNoResponseWhenEnablingFlashQuirk_isTrue() = runBlocking {
        CameraNoResponseWhenEnablingFlashQuirk.AFFECTED_MODELS.forEach { model ->
            ShadowBuild.setModel(model)
            val useTorchAsFlash = createUseTorchAsFlash()

            assertThat(useTorchAsFlash.shouldUseTorchAsFlash { null }).isTrue()
        }
    }

    @Test
    fun shouldUseTorchAsFlash_withImageCaptureWashedOutImageQuirk_isTrue() = runBlocking {
        ImageCaptureWashedOutImageQuirk.BUILD_MODELS.forEach { model ->
            ShadowBuild.setModel(model)
            val useTorchAsFlash = createUseTorchAsFlash()

            assertThat(useTorchAsFlash.shouldUseTorchAsFlash { null }).isTrue()
        }
    }

    @Config(minSdk = 29)
    @Test
    fun shouldUseTorchAsFlash_withUwFlashCaptureUnderexposureQuirkAndUltraWideCamera_isTrue() =
        runBlocking {
            UltraWideFlashCaptureUnderexposureQuirk.BUILD_MODEL_PREFIXES.forEach { model ->
                // Arrange
                ShadowBuild.setModel(model)
                val useTorchAsFlash =
                    createUseTorchAsFlash(
                        intrinsicZoomCalculator =
                            object : IntrinsicZoomCalculator {
                                override fun calculateIntrinsicZoomRatio(
                                    cameraMetadata: CameraMetadata
                                ): Float {
                                    // Ultra-wide camera is recognized by checking if intrinsic zoom
                                    // ratio is
                                    // less than 1.0F
                                    return 0.5F
                                }
                            }
                    )

                // Act & Assert: Should return true for ultra-wide camera
                assertThat(
                        useTorchAsFlash.shouldUseTorchAsFlash {
                            FakeFrameMetadata(
                                mapOf(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID to "2")
                            )
                        }
                    )
                    .isTrue()
            }
        }

    @Config(minSdk = 29)
    @Test
    fun shouldUseTorchAsFlash_withUwFlashCaptureUnderexposureQuirkAndActiveCameraIdNull_isTrue() =
        runBlocking {
            UltraWideFlashCaptureUnderexposureQuirk.BUILD_MODEL_PREFIXES.forEach { model ->
                // Arrange
                ShadowBuild.setModel(model)
                val useTorchAsFlash =
                    createUseTorchAsFlash(
                        intrinsicZoomCalculator =
                            object : IntrinsicZoomCalculator {
                                override fun calculateIntrinsicZoomRatio(
                                    cameraMetadata: CameraMetadata
                                ): Float {
                                    // Ultra-wide camera is recognized by checking if intrinsic zoom
                                    // ratio is
                                    // less than 1.0F
                                    return 0.5F
                                }
                            }
                    )

                // Act & Assert: Should default to true for safety when active physical camera ID
                // is unavailable
                assertThat(
                        useTorchAsFlash.shouldUseTorchAsFlash {
                            FakeFrameMetadata(
                                mapOf(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID to null)
                            )
                        }
                    )
                    .isTrue()
            }
        }

    @Config(minSdk = 29)
    @Test
    fun shouldUseTorchAsFlash_withUwFlashCaptureUnderexposureQuirkButNotUltraWideCamera_isFalse() =
        runBlocking {
            ShadowLog.stream = System.out
            UltraWideFlashCaptureUnderexposureQuirk.BUILD_MODEL_PREFIXES.forEach { model ->
                // Arrange
                ShadowBuild.setModel(model)
                val useTorchAsFlash =
                    createUseTorchAsFlash(
                        intrinsicZoomCalculator =
                            object : IntrinsicZoomCalculator {
                                override fun calculateIntrinsicZoomRatio(
                                    cameraMetadata: CameraMetadata
                                ): Float {
                                    // Ultra-wide camera is recognized by checking if intrinsic zoom
                                    // ratio is
                                    // less than 1.0F, so 1.0F should lead to non-ultra-wide camera
                                    // flow.
                                    return 1.0F
                                }
                            }
                    )

                // Act & Assert: Should return false when not ultra-wide camera
                assertThat(
                        useTorchAsFlash.shouldUseTorchAsFlash {
                            FakeFrameMetadata(
                                mapOf(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID to "0")
                            )
                        }
                    )
                    .isFalse()
            }
        }

    @Test
    fun shouldUseTorchAsFlash_withUwFlashCaptureUnderexposureQuirkAndNullMetadata_isTrue() =
        runBlocking {
            UltraWideFlashCaptureUnderexposureQuirk.BUILD_MODEL_PREFIXES.forEach { model ->
                // Arrange
                ShadowBuild.setModel(model)
                val useTorchAsFlash = createUseTorchAsFlash()

                // Act & Assert: Should default to true for safety when metadata is unavailable
                assertThat(useTorchAsFlash.shouldUseTorchAsFlash { null }).isTrue()
            }
        }

    @Config(maxSdk = 28)
    @Test
    fun shouldUseTorchAsFlash_withUwFlashCaptureUnderexposureQuirkAndMaxSdk28_isTrue() =
        runBlocking {
            UltraWideFlashCaptureUnderexposureQuirk.BUILD_MODEL_PREFIXES.forEach { model ->
                // Arrange
                ShadowBuild.setModel(model)
                val useTorchAsFlash = createUseTorchAsFlash()

                // Act & Assert: Should default to true for safety when SDK level is 28 or below
                assertThat(useTorchAsFlash.shouldUseTorchAsFlash { FakeFrameMetadata() }).isTrue()
            }
        }

    @Test
    fun shouldDisableAePrecapture_withUwFlashCaptureUnderexposureQuirk_isFalse() {
        // Arrange
        ShadowBuild.setModel(UltraWideFlashCaptureUnderexposureQuirk.BUILD_MODEL_PREFIXES[0])
        val useTorchAsFlash = createUseTorchAsFlash()

        // Act & Assert
        assertThat(useTorchAsFlash.shouldDisableAePrecapture()).isFalse()
    }

    @Test
    fun shouldDisableAePrecapture_withLegacyQuirk_isTrue() {
        // Arrange
        ShadowBuild.setModel(ImageCaptureWashedOutImageQuirk.BUILD_MODELS[0]) // A legacy quirk
        val useTorchAsFlash = createUseTorchAsFlash()

        // Act & Assert
        assertThat(useTorchAsFlash.shouldDisableAePrecapture()).isTrue()
    }

    @Test
    fun shouldUseTorchAsFlash_lensFacingFront_isFalse() = runBlocking {
        CameraNoResponseWhenEnablingFlashQuirk.AFFECTED_MODELS.forEach { model ->
            ShadowBuild.setModel(model)
            val useTorchAsFlash =
                createUseTorchAsFlash(lensFacing = CameraCharacteristics.LENS_FACING_FRONT)

            assertThat(useTorchAsFlash.shouldUseTorchAsFlash { null }).isFalse()
        }
    }
}
