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

@file:RequiresApi(21)

package androidx.camera.camera2.internal

import android.graphics.ImageFormat.JPEG
import android.graphics.ImageFormat.PRIVATE
import android.graphics.ImageFormat.YUV_420_888
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.ZslControlImpl.MAX_IMAGES
import androidx.camera.camera2.internal.ZslControlImpl.RING_BUFFER_CAPACITY
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.core.impl.SessionConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.util.ReflectionHelpers

val YUV_REPROCESSING_MAXIMUM_SIZE = Size(4000, 3000)
val PRIVATE_REPROCESSING_MAXIMUM_SIZE = Size(3000, 2000)
private const val CAMERA_ID_0 = "0"

/**
 * Unit tests for [ZslControlImpl].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.M)
class ZslControlImplTest {

    private lateinit var zslControl: ZslControlImpl
    private lateinit var sessionConfigBuilder: SessionConfig.Builder

    @Before
    fun setUp() {
        sessionConfigBuilder = SessionConfig.Builder().also { sessionConfigBuilder ->
            sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        }
    }

    @Test
    fun isPrivateReprocessingSupported_addZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = true,
            isJpegValidOutputFormat = true
        ))

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNotNull()
        assertThat(zslControl.mReprocessingImageReader.imageFormat).isEqualTo(PRIVATE)
        assertThat(zslControl.mReprocessingImageReader.maxImages).isEqualTo(
            MAX_IMAGES)
        assertThat(zslControl.mReprocessingImageReader.width).isEqualTo(
            PRIVATE_REPROCESSING_MAXIMUM_SIZE.width)
        assertThat(zslControl.mReprocessingImageReader.height).isEqualTo(
            PRIVATE_REPROCESSING_MAXIMUM_SIZE.height)
        assertThat(zslControl.mImageRingBuffer.maxCapacity).isEqualTo(
            RING_BUFFER_CAPACITY)
    }

    @Test
    fun isYuvReprocessingSupported_notAddZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = true,
            isPrivateReprocessingSupported = false,
            isJpegValidOutputFormat = true
        ))

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNull()
    }

    @Test
    fun isJpegNotValidOutputFormat_notAddZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = true,
            isPrivateReprocessingSupported = false,
            isJpegValidOutputFormat = false
        ))

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNull()
    }

    @Test
    fun isReprocessingNotSupported_notAddZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = false,
            isJpegValidOutputFormat = false
        ))

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNull()
    }

    @Test
    fun isZslDisabledByUserCaseConfig_notAddZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = true,
            isJpegValidOutputFormat = true
        ))
        zslControl.isZslDisabledByUserCaseConfig = true

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNull()
    }

    @Test
    fun isZslDisabledByFlashMode_addZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = true,
            isJpegValidOutputFormat = true
        ))
        zslControl.isZslDisabledByFlashMode = true

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNotNull()
        assertThat(zslControl.mReprocessingImageReader.imageFormat).isEqualTo(PRIVATE)
        assertThat(zslControl.mReprocessingImageReader.maxImages).isEqualTo(
            MAX_IMAGES)
        assertThat(zslControl.mReprocessingImageReader.width).isEqualTo(
            PRIVATE_REPROCESSING_MAXIMUM_SIZE.width)
        assertThat(zslControl.mReprocessingImageReader.height).isEqualTo(
            PRIVATE_REPROCESSING_MAXIMUM_SIZE.height)
        assertThat(zslControl.mImageRingBuffer.maxCapacity).isEqualTo(
            RING_BUFFER_CAPACITY)
    }

    @Test
    fun isZslDisabled_clearZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = true,
            isJpegValidOutputFormat = true
        ))

        zslControl.addZslConfig(sessionConfigBuilder)

        zslControl.isZslDisabledByUserCaseConfig = true
        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNull()
    }

    @Test
    fun hasZslDisablerQuirk_notAddZslConfig() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-F936B")

        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = true,
            isJpegValidOutputFormat = true
        ))

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNull()
    }

    @Test
    fun hasNoZslDisablerQuirk_addZslConfig() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-G973")

        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = true,
            isJpegValidOutputFormat = true
        ))

        zslControl.addZslConfig(sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNotNull()
        assertThat(zslControl.mReprocessingImageReader.imageFormat).isEqualTo(PRIVATE)
        assertThat(zslControl.mReprocessingImageReader.maxImages).isEqualTo(
            MAX_IMAGES)
        assertThat(zslControl.mReprocessingImageReader.width).isEqualTo(
            PRIVATE_REPROCESSING_MAXIMUM_SIZE.width)
        assertThat(zslControl.mReprocessingImageReader.height).isEqualTo(
            PRIVATE_REPROCESSING_MAXIMUM_SIZE.height)
        assertThat(zslControl.mImageRingBuffer.maxCapacity).isEqualTo(
            RING_BUFFER_CAPACITY)
    }

    private fun createCameraCharacteristicsCompat(
        hasCapabilities: Boolean,
        isYuvReprocessingSupported: Boolean,
        isPrivateReprocessingSupported: Boolean,
        isJpegValidOutputFormat: Boolean
    ): CameraCharacteristicsCompat {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)

        val capabilities = arrayListOf<Int>()
        if (isYuvReprocessingSupported) {
            capabilities.add(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING)
        }
        if (isPrivateReprocessingSupported) {
            capabilities.add(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING)
        }

        if (hasCapabilities) {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                capabilities.toIntArray()
            )

            // Input formats
            val streamConfigurationMap: StreamConfigurationMap = mock(
                StreamConfigurationMap::class.java)

            if (isYuvReprocessingSupported && isPrivateReprocessingSupported) {
                Mockito.`when`(streamConfigurationMap.inputFormats).thenReturn(
                    arrayOf(YUV_420_888, PRIVATE).toIntArray()
                )
                Mockito.`when`(streamConfigurationMap.getInputSizes(YUV_420_888)).thenReturn(
                    arrayOf(YUV_REPROCESSING_MAXIMUM_SIZE)
                )
                Mockito.`when`(streamConfigurationMap.getInputSizes(PRIVATE)).thenReturn(
                    arrayOf(PRIVATE_REPROCESSING_MAXIMUM_SIZE)
                )
            } else if (isYuvReprocessingSupported) {
                Mockito.`when`(streamConfigurationMap.inputFormats).thenReturn(
                    arrayOf(YUV_420_888).toIntArray()
                )
                Mockito.`when`(streamConfigurationMap.getInputSizes(YUV_420_888)).thenReturn(
                    arrayOf(YUV_REPROCESSING_MAXIMUM_SIZE)
                )
            } else if (isPrivateReprocessingSupported) {
                Mockito.`when`(streamConfigurationMap.inputFormats).thenReturn(
                    arrayOf(PRIVATE).toIntArray()
                )
                Mockito.`when`(streamConfigurationMap.getInputSizes(PRIVATE)).thenReturn(
                    arrayOf(PRIVATE_REPROCESSING_MAXIMUM_SIZE)
                )
            }

            // Output formats for input
            if (isJpegValidOutputFormat) {
                Mockito.`when`(streamConfigurationMap.getValidOutputFormatsForInput(PRIVATE))
                    .thenReturn(arrayOf(JPEG).toIntArray())
                Mockito.`when`(streamConfigurationMap.getValidOutputFormatsForInput(YUV_420_888))
                    .thenReturn(arrayOf(JPEG).toIntArray())
            }

            shadowCharacteristics.set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, streamConfigurationMap)
        }

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
            characteristics,
            CAMERA_ID_0
        )
    }
}
