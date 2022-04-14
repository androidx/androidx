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

package androidx.camera.camera2.internal

import android.graphics.ImageFormat.PRIVATE
import android.graphics.ImageFormat.YUV_420_888
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.util.Size
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.core.impl.SessionConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

private val RESOLUTION = Size(640, 480)

/**
 * Unit tests for [ZslControlImpl].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.M)
public class ZslControlImplTest {

    private lateinit var zslControl: ZslControlImpl
    private lateinit var sessionConfigBuilder: SessionConfig.Builder

    @Before
    public fun setUp() {
        sessionConfigBuilder = SessionConfig.Builder().also { sessionConfigBuilder ->
            sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        }
    }

    @Test
    public fun isYuvReprocessingSupported_addZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = true,
            isPrivateReprocessingSupported = false
        ))

        zslControl.addZslConfig(RESOLUTION, sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNotNull()
        assertThat(zslControl.mReprocessingImageWriter).isNull()
        assertThat(zslControl.mReprocessingImageReader.imageFormat).isEqualTo(YUV_420_888)
    }

    @Test
    public fun isPrivateReprocessingSupported_addZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = true
        ))

        zslControl.addZslConfig(RESOLUTION, sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNotNull()
        assertThat(zslControl.mReprocessingImageWriter).isNull()
        assertThat(zslControl.mReprocessingImageReader.imageFormat).isEqualTo(PRIVATE)
    }

    @Test
    public fun isReprocessingNotSupported_notAddZslConfig() {
        zslControl = ZslControlImpl(createCameraCharacteristicsCompat(
            hasCapabilities = true,
            isYuvReprocessingSupported = false,
            isPrivateReprocessingSupported = false
        ))

        zslControl.addZslConfig(RESOLUTION, sessionConfigBuilder)

        assertThat(zslControl.mReprocessingImageReader).isNull()
        assertThat(zslControl.mReprocessingImageWriter).isNull()
    }

    private fun createCameraCharacteristicsCompat(
        hasCapabilities: Boolean,
        isYuvReprocessingSupported: Boolean,
        isPrivateReprocessingSupported: Boolean
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
        }

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics)
    }
}