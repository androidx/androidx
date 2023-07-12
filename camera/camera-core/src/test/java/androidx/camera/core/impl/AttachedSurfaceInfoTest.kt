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
package androidx.camera.core.impl

import android.graphics.ImageFormat
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.testing.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AttachedSurfaceInfoTest {
    private var attachedSurfaceInfo: AttachedSurfaceInfo? = null
    private val surfaceConfig = SurfaceConfig.create(
        SurfaceConfig.ConfigType.JPEG,
        SurfaceConfig.ConfigSize.PREVIEW
    )
    private val imageFormat = ImageFormat.JPEG
    private val size = Size(1920, 1080)
    private val dynamicRange = DynamicRange.SDR
    private val captureTypes = listOf(CaptureType.PREVIEW)
    private val inputFormat = ImageFormat.PRIVATE
    private val targetFramerate = Range(10, 20)
    private val config = FakeUseCaseConfig.Builder(
        CaptureType.PREVIEW,
        inputFormat
    ).useCaseConfig.config

    @Before
    fun setup() {
        attachedSurfaceInfo = AttachedSurfaceInfo.create(
            surfaceConfig,
            imageFormat,
            size,
            dynamicRange,
            captureTypes,
            config,
            targetFramerate
        )
    }

    @Test
    fun canGetSurfaceConfig() {
        Truth.assertThat(attachedSurfaceInfo!!.surfaceConfig).isEqualTo(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.PREVIEW
            )
        )
    }

    @Test
    fun canGetImageFormat() {
        Truth.assertThat(attachedSurfaceInfo!!.imageFormat).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun canGetSize() {
        Truth.assertThat(attachedSurfaceInfo!!.size).isEqualTo(size)
    }

    @Test
    fun canGetDynamicRange() {
        Truth.assertThat(attachedSurfaceInfo!!.dynamicRange).isEqualTo(dynamicRange)
    }

    @Test
    fun canGetCaptureTypes() {
        Truth.assertThat(attachedSurfaceInfo!!.captureTypes.size).isEqualTo(captureTypes.size)
        for ((index, value) in captureTypes.withIndex()) {
            Truth.assertThat(attachedSurfaceInfo!!.captureTypes[index]).isEqualTo(value)
        }
    }

    @Test
    fun canGetImplementationOption() {
        Truth.assertThat(
            attachedSurfaceInfo!!.implementationOptions!!
                .containsOption(ImageInputConfig.OPTION_INPUT_FORMAT)
        )
            .isTrue()
        Truth.assertThat(
            attachedSurfaceInfo!!.implementationOptions!!
                .retrieveOption(ImageInputConfig.OPTION_INPUT_FORMAT)
        )
            .isEqualTo(inputFormat)
    }

    @Test
    fun canGetTargetFrameRate() {
        Truth.assertThat(attachedSurfaceInfo!!.targetFrameRate).isEqualTo(targetFramerate)
    }

    @Test
    fun nullGetTargetFrameRateReturnsNull() {
        val attachedSurfaceInfo2 = AttachedSurfaceInfo.create(
            surfaceConfig,
            imageFormat,
            size,
            dynamicRange,
            listOf(CaptureType.PREVIEW),
            config,
            null
        )
        Truth.assertThat(attachedSurfaceInfo2.targetFrameRate).isNull()
    }
}