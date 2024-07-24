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

package androidx.camera.camera2.pipe.integration.adapter

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.adapter.ZslControlImpl.Companion.MAX_IMAGES
import androidx.camera.camera2.pipe.integration.adapter.ZslControlImpl.Companion.RING_BUFFER_CAPACITY
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.SessionConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.M)
@DoNotInstrument
class ZslControlImplTest {
    private lateinit var zslControlImpl: ZslControlImpl
    private lateinit var sessionConfigBuilder: SessionConfig.Builder

    @Before
    fun setUp() {
        sessionConfigBuilder =
            SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
            }
    }

    @Test
    fun isPrivateReprocessingSupported_addZslConfig() {
        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = false,
                    isPrivateReprocessingSupported = true,
                    isJpegValidOutputFormat = true
                )
            )

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNotNull()
        assertThat(zslControlImpl.reprocessingImageReader!!.imageFormat)
            .isEqualTo(ImageFormat.PRIVATE)
        assertThat(zslControlImpl.reprocessingImageReader!!.maxImages).isEqualTo(MAX_IMAGES)
        assertThat(zslControlImpl.reprocessingImageReader!!.width)
            .isEqualTo(PRIVATE_REPROCESSING_MAXIMUM_SIZE.width)
        assertThat(zslControlImpl.reprocessingImageReader!!.height)
            .isEqualTo(PRIVATE_REPROCESSING_MAXIMUM_SIZE.height)
        assertThat(zslControlImpl.zslRingBuffer.maxCapacity).isEqualTo(RING_BUFFER_CAPACITY)
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_ZERO_SHUTTER_LAG)
    }

    @Test
    fun isYuvReprocessingSupported_notAddZslConfig() {
        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = true,
                    isPrivateReprocessingSupported = false,
                    isJpegValidOutputFormat = true
                )
            )

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNull()
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_PREVIEW)
    }

    @Test
    fun isJpegNotValidOutputFormat_notAddZslConfig() {
        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = true,
                    isPrivateReprocessingSupported = false,
                    isJpegValidOutputFormat = false
                )
            )

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNull()
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_PREVIEW)
    }

    @Test
    fun isReprocessingNotSupported_notAddZslConfig() {
        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = false,
                    isPrivateReprocessingSupported = false,
                    isJpegValidOutputFormat = false
                )
            )

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNull()
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_PREVIEW)
    }

    @Test
    fun isZslDisabledByUserCaseConfig_notAddZslConfig() {
        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = false,
                    isPrivateReprocessingSupported = true,
                    isJpegValidOutputFormat = true
                )
            )
        zslControlImpl.setZslDisabledByUserCaseConfig(true)

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNull()
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_PREVIEW)
    }

    @Test
    fun isZslDisabledByFlashMode_addZslConfig() {
        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = false,
                    isPrivateReprocessingSupported = true,
                    isJpegValidOutputFormat = true
                )
            )
        zslControlImpl.setZslDisabledByFlashMode(true)

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNotNull()
        assertThat(zslControlImpl.reprocessingImageReader!!.imageFormat)
            .isEqualTo(ImageFormat.PRIVATE)
        assertThat(zslControlImpl.reprocessingImageReader!!.maxImages).isEqualTo(MAX_IMAGES)
        assertThat(zslControlImpl.reprocessingImageReader!!.width)
            .isEqualTo(PRIVATE_REPROCESSING_MAXIMUM_SIZE.width)
        assertThat(zslControlImpl.reprocessingImageReader!!.height)
            .isEqualTo(PRIVATE_REPROCESSING_MAXIMUM_SIZE.height)
        assertThat(zslControlImpl.zslRingBuffer.maxCapacity).isEqualTo(RING_BUFFER_CAPACITY)
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_ZERO_SHUTTER_LAG)
    }

    @Test
    fun isZslDisabled_clearZslConfig() {
        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = false,
                    isPrivateReprocessingSupported = true,
                    isJpegValidOutputFormat = true
                )
            )

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        zslControlImpl.setZslDisabledByUserCaseConfig(true)
        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNull()
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_PREVIEW)
    }

    @Test
    fun hasZslDisablerQuirk_notAddZslConfig() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-F936B")

        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = false,
                    isPrivateReprocessingSupported = true,
                    isJpegValidOutputFormat = true
                )
            )

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNull()
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_PREVIEW)
    }

    @Test
    fun hasNoZslDisablerQuirk_addZslConfig() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-G973")

        zslControlImpl =
            ZslControlImpl(
                createCameraProperties(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = false,
                    isPrivateReprocessingSupported = true,
                    isJpegValidOutputFormat = true
                )
            )

        zslControlImpl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControlImpl.reprocessingImageReader).isNotNull()
        assertThat(zslControlImpl.reprocessingImageReader!!.imageFormat)
            .isEqualTo(ImageFormat.PRIVATE)
        assertThat(zslControlImpl.reprocessingImageReader!!.maxImages).isEqualTo(MAX_IMAGES)
        assertThat(zslControlImpl.reprocessingImageReader!!.width)
            .isEqualTo(PRIVATE_REPROCESSING_MAXIMUM_SIZE.width)
        assertThat(zslControlImpl.reprocessingImageReader!!.height)
            .isEqualTo(PRIVATE_REPROCESSING_MAXIMUM_SIZE.height)
        assertThat(zslControlImpl.zslRingBuffer.maxCapacity).isEqualTo(RING_BUFFER_CAPACITY)
        assertThat(sessionConfigBuilder.build().templateType).isEqualTo(TEMPLATE_ZERO_SHUTTER_LAG)
    }

    private fun createCameraProperties(
        hasCapabilities: Boolean,
        isYuvReprocessingSupported: Boolean,
        isPrivateReprocessingSupported: Boolean,
        isJpegValidOutputFormat: Boolean
    ): CameraProperties {
        val characteristicsMap = mutableMapOf<CameraCharacteristics.Key<*>, Any?>()
        val capabilities = arrayListOf<Int>()
        if (isYuvReprocessingSupported) {
            capabilities.add(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING)
        }
        if (isPrivateReprocessingSupported) {
            capabilities.add(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING
            )
        }

        if (hasCapabilities) {
            characteristicsMap[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] =
                capabilities.toIntArray()

            // Input formats
            val streamConfigurationMap: StreamConfigurationMap = mock()

            if (isYuvReprocessingSupported && isPrivateReprocessingSupported) {
                whenever(streamConfigurationMap.inputFormats)
                    .thenReturn(arrayOf(ImageFormat.YUV_420_888, ImageFormat.PRIVATE).toIntArray())
                whenever(streamConfigurationMap.getInputSizes(ImageFormat.YUV_420_888))
                    .thenReturn(arrayOf(YUV_REPROCESSING_MAXIMUM_SIZE))
                whenever(streamConfigurationMap.getInputSizes(ImageFormat.PRIVATE))
                    .thenReturn(arrayOf(PRIVATE_REPROCESSING_MAXIMUM_SIZE))
            } else if (isYuvReprocessingSupported) {
                whenever(streamConfigurationMap.inputFormats)
                    .thenReturn(arrayOf(ImageFormat.YUV_420_888).toIntArray())
                whenever(streamConfigurationMap.getInputSizes(ImageFormat.YUV_420_888))
                    .thenReturn(arrayOf(YUV_REPROCESSING_MAXIMUM_SIZE))
            } else if (isPrivateReprocessingSupported) {
                whenever(streamConfigurationMap.inputFormats)
                    .thenReturn(arrayOf(ImageFormat.PRIVATE).toIntArray())
                whenever(streamConfigurationMap.getInputSizes(ImageFormat.PRIVATE))
                    .thenReturn(arrayOf(PRIVATE_REPROCESSING_MAXIMUM_SIZE))
            }

            // Output formats for input
            if (isJpegValidOutputFormat) {
                whenever(streamConfigurationMap.getValidOutputFormatsForInput(ImageFormat.PRIVATE))
                    .thenReturn(arrayOf(ImageFormat.JPEG).toIntArray())
                whenever(
                        streamConfigurationMap.getValidOutputFormatsForInput(
                            ImageFormat.YUV_420_888
                        )
                    )
                    .thenReturn(arrayOf(ImageFormat.JPEG).toIntArray())
            }

            characteristicsMap[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP] =
                streamConfigurationMap
        }
        val cameraMetadata = FakeCameraMetadata(characteristics = characteristicsMap)

        return FakeCameraProperties(
            cameraMetadata,
            CameraId("0"),
        )
    }

    companion object {
        val YUV_REPROCESSING_MAXIMUM_SIZE = Size(4000, 3000)
        val PRIVATE_REPROCESSING_MAXIMUM_SIZE = Size(3000, 2000)
    }
}
