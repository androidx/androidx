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

package androidx.camera.core.impl

import android.graphics.ImageFormat
import android.util.Size
import androidx.camera.core.impl.CameraMode.CONCURRENT_CAMERA
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [SurfaceConfig]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class SurfaceConfigTest {
    @Test
    fun create_validTypeAndSize_createsSurfaceConfigWithCorrectProperties() {
        // Arrange
        val type = ConfigType.YUV
        val size = ConfigSize.VGA

        // Act
        val surfaceConfig = SurfaceConfig.create(type, size)

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(type)
        assertThat(surfaceConfig.configSize).isEqualTo(size)
        assertThat(surfaceConfig.streamUseCase).isEqualTo(SurfaceConfig.DEFAULT_STREAM_USE_CASE)
    }

    @Test
    fun create_validTypeSizeAndStreamUseCase_createsSurfaceConfigWithCorrectProperties() {
        // Arrange
        val type = ConfigType.JPEG
        val size = ConfigSize.MAXIMUM
        val streamUseCase = StreamUseCase.PREVIEW_VIDEO_STILL

        // Act
        val surfaceConfig = SurfaceConfig.create(type, size, streamUseCase)

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(type)
        assertThat(surfaceConfig.configSize).isEqualTo(size)
        assertThat(surfaceConfig.streamUseCase).isEqualTo(streamUseCase)
    }

    @Test
    fun isSupported_smallerSizeAndSameType_returnsTrue() {
        // Arrange
        val config1 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM)
        val config2 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)

        // Act
        val isSupported = config1.isSupported(config2)

        // Assert
        assertThat(isSupported).isTrue()
    }

    @Test
    fun isSupported_sameSizeAndSameType_returnsTrue() {
        // Arrange
        val config1 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)
        val config2 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)

        // Act
        val isSupported = config1.isSupported(config2)

        // Assert
        assertThat(isSupported).isTrue()
    }

    @Test
    fun isSupported_largerSizeAndSameType_returnsFalse() {
        // Arrange
        val config1 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)
        val config2 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM)

        // Act
        val isSupported = config1.isSupported(config2)

        // Assert
        assertThat(isSupported).isFalse()
    }

    @Test
    fun isSupported_sameSizeAndDifferentType_returnsFalse() {
        // Arrange
        val config1 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)
        val config2 = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.VGA)

        // Act
        val isSupported = config1.isSupported(config2)

        // Assert
        assertThat(isSupported).isFalse()
    }

    @Test
    fun isSupported_whenBothConfigsAreTheSame_returnsTrue() {
        // Arrange
        val config1 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)
        val config2 = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)

        // Act
        val result = config1.isSupported(config2)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun getConfigType_yuvFormat_returnsYUV() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val configType = SurfaceConfig.getConfigType(imageFormat)

        // Assert
        assertThat(configType).isEqualTo(ConfigType.YUV)
    }

    @Test
    fun getConfigType_jpegFormat_returnsJPEG() {
        // Arrange
        val imageFormat = ImageFormat.JPEG

        // Act
        val configType = SurfaceConfig.getConfigType(imageFormat)

        // Assert
        assertThat(configType).isEqualTo(ConfigType.JPEG)
    }

    @Test
    fun getConfigType_jpegRFormat_returnsJPEG_R() {
        // Arrange
        val imageFormat = ImageFormat.JPEG_R

        // Act
        val configType = SurfaceConfig.getConfigType(imageFormat)

        // Assert
        assertThat(configType).isEqualTo(ConfigType.JPEG_R)
    }

    @Test
    fun getConfigType_rawSensorFormat_returnsRAW() {
        // Arrange
        val imageFormat = ImageFormat.RAW_SENSOR

        // Act
        val configType = SurfaceConfig.getConfigType(imageFormat)

        // Assert
        assertThat(configType).isEqualTo(ConfigType.RAW)
    }

    @Test
    fun getConfigType_unknownFormat_returnsPRIV() {
        // Arrange
        val imageFormat = 9999 // Unknown format

        // Act
        val configType = SurfaceConfig.getConfigType(imageFormat)

        // Assert
        assertThat(configType).isEqualTo(ConfigType.PRIV)
    }

    @Test
    fun transformSurfaceConfig_concurrentCameraMode_smallerThan720p_returns720p() {
        // Arrange
        val cameraMode = CONCURRENT_CAMERA
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = s240p_4_3,
                surfaceSizeDefinition = surfaceSizeDefinition,
                cameraMode = cameraMode,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.S720P_16_9)
    }

    @Test
    fun transformSurfaceConfig_concurrentCameraMode_inputSize720p_returns720p() {
        // Arrange
        val cameraMode = CONCURRENT_CAMERA
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = s720p_16_9,
                surfaceSizeDefinition = surfaceSizeDefinition,
                cameraMode = cameraMode,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.S720P_16_9)
    }

    @Test
    fun transformSurfaceConfig_concurrentCameraMode_largerThan720pSmallerThan1440p_returns1440p() {
        // Arrange
        val cameraMode = CONCURRENT_CAMERA
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = Size(1980, 1080),
                surfaceSizeDefinition = surfaceSizeDefinition,
                cameraMode = cameraMode,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.S1440P_4_3)
    }

    @Test
    fun transformSurfaceConfig_usingFeatureComboTable_exactFixedSizeMatch_returnsFixedSize() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888
        val configSource = SurfaceConfig.ConfigSource.FEATURE_COMBINATION_TABLE

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = s720p_16_9,
                surfaceSizeDefinition = surfaceSizeDefinition,
                configSource = configSource,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.S720P_16_9)
    }

    @Test
    fun transformSurfaceConfig_usingFeatureComboTable_noFixedSizeButMatchesMax_returnsMaximum() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888
        val configSource = SurfaceConfig.ConfigSource.FEATURE_COMBINATION_TABLE

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                // creating new size instance to ensure different size instance than max supported
                size = Size(maxSize.width, maxSize.height),
                surfaceSizeDefinition = surfaceSizeDefinition,
                configSource = configSource,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.MAXIMUM)
    }

    @Test
    fun transformSurfaceConfig_withFeatureComboTable_noSizeMatch_returnsUnsupported() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888
        val configSource = SurfaceConfig.ConfigSource.FEATURE_COMBINATION_TABLE

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = s240p_4_3,
                surfaceSizeDefinition = surfaceSizeDefinition,
                configSource = configSource,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.NOT_SUPPORT)
    }

    @Test
    fun transformSurfaceConfig_withCaptureSessionTables_smallerThanAnalysisSize_returnsVGA() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = s240p_4_3,
                surfaceSizeDefinition = surfaceSizeDefinition,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.VGA)
    }

    @Test
    fun transformSurfaceConfig_withCaptureSessionTables_smallerThanPreviewSize_returnsPreview() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = s720p_16_9,
                surfaceSizeDefinition = surfaceSizeDefinition,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.PREVIEW)
    }

    @Test
    fun transformSurfaceConfig_withCaptureSessionTables_smallerThanRecordSize_returnsRecord() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = s1440p_4_3,
                surfaceSizeDefinition = surfaceSizeDefinition,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.RECORD)
    }

    @Test
    fun transformSurfaceConfig_withCaptureSessionTables_smallerThanMaxSize_returnsMaximum() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = Size(3900, 2800),
                surfaceSizeDefinition = surfaceSizeDefinition,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.MAXIMUM)
    }

    @Test
    fun transformSurfaceConfig_largerThanMaxButNotUltraHighCamera_returnsUnsupported() {
        // Arrange
        val imageFormat = ImageFormat.YUV_420_888

        // Act
        val surfaceConfig =
            SurfaceConfig.transformSurfaceConfig(
                imageFormat = imageFormat,
                size = Size(6000, 3000),
                surfaceSizeDefinition = surfaceSizeDefinition,
            )

        // Assert
        assertThat(surfaceConfig.configType).isEqualTo(ConfigType.YUV)
        assertThat(surfaceConfig.configSize).isEqualTo(ConfigSize.ULTRA_MAXIMUM)
    }

    @Test
    fun configSize_getRelatedFixedSize_withFixedSize_returnsCorrectSize() {
        // Arrange
        val configSize = ConfigSize.S1080P_4_3

        // Act
        val relatedSize = configSize.relatedFixedSize

        // Assert
        assertThat(relatedSize).isEqualTo(Size(1440, 1080))
    }

    @Test
    fun configSize_getRelatedFixedSize_withoutFixedSize_returnsNull() {
        // Arrange
        val configSize = ConfigSize.MAXIMUM

        // Act
        val relatedSize = configSize.relatedFixedSize

        // Assert
        assertThat(relatedSize).isNull()
    }

    private companion object {
        private val s240p_4_3 = Size(320, 240)
        private val analysisSize = Size(640, 480)
        private val s720p_16_9 = Size(1280, 720)
        private val s1440p_4_3 = Size(1920, 1440)
        private val previewSize = Size(1920, 1080)
        private val recordSize = Size(3840, 2160)
        private val maxSize = Size(4000, 3000)
        private val maxSize16x9 = Size(4800, 2700)
        private val ultraMaxSize = Size(9000, 4500)
        private val surfaceSizeDefinition =
            SurfaceSizeDefinition.create(
                analysisSize,
                mapOf(
                    ImageFormat.YUV_420_888 to s720p_16_9,
                    ImageFormat.PRIVATE to s720p_16_9,
                    ImageFormat.JPEG to s720p_16_9,
                ),
                previewSize,
                mapOf(
                    ImageFormat.YUV_420_888 to s1440p_4_3,
                    ImageFormat.PRIVATE to s1440p_4_3,
                    ImageFormat.JPEG to s1440p_4_3,
                ),
                recordSize,
                mapOf(
                    ImageFormat.YUV_420_888 to maxSize,
                    ImageFormat.PRIVATE to maxSize,
                    ImageFormat.JPEG to maxSize,
                ),
                mapOf(
                    ImageFormat.YUV_420_888 to maxSize,
                    ImageFormat.PRIVATE to maxSize,
                    ImageFormat.JPEG to maxSize,
                ),
                mapOf(
                    ImageFormat.YUV_420_888 to maxSize16x9,
                    ImageFormat.PRIVATE to maxSize16x9,
                    ImageFormat.JPEG to maxSize16x9,
                ),
                mapOf(
                    ImageFormat.YUV_420_888 to ultraMaxSize,
                    ImageFormat.PRIVATE to ultraMaxSize,
                    ImageFormat.JPEG to ultraMaxSize,
                ),
            )
    }
}
