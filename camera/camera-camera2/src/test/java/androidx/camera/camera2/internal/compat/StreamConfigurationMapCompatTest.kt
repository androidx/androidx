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

package androidx.camera.camera2.internal.compat

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import androidx.camera.camera2.internal.compat.workaround.OutputSizesCorrector
import androidx.camera.core.impl.ImageFormatConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder

/** Unit tests for [StreamConfigurationMapCompat]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamConfigurationMapCompatTest {

    companion object {
        private val SIZE_480P = Size(640, 480)
        private val SIZE_720P = Size(1080, 720)
        private val SIZE_1080P = Size(1920, 1080)
        private const val FORMAT_PRIVATE =
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
    }

    private lateinit var streamConfigurationMapCompat: StreamConfigurationMapCompat
    private val privateFormatOutputSizes = listOf(SIZE_1080P, SIZE_720P, SIZE_480P)

    @Before
    fun setUp() {
        val builder =
            StreamConfigurationMapBuilder.newBuilder().apply {
                privateFormatOutputSizes.forEach { size -> addOutputSize(FORMAT_PRIVATE, size) }
            }
        val cameraId = "0"

        // **** Camera 0 characteristics ****//
        streamConfigurationMapCompat =
            StreamConfigurationMapCompat.toStreamConfigurationMapCompat(
                builder.build(),
                OutputSizesCorrector(cameraId)
            )
    }

    @Test
    fun getOutputSizes_withFormat_callGetOutputSizes() {
        assertThat(streamConfigurationMapCompat.getOutputSizes(FORMAT_PRIVATE)!!.toList())
            .containsExactlyElementsIn(privateFormatOutputSizes)
    }

    @Test
    fun getOutputSizes_withClass_callGetOutputSizes() {
        assertThat(
                streamConfigurationMapCompat.getOutputSizes(SurfaceTexture::class.java)!!.toList()
            )
            .containsExactlyElementsIn(privateFormatOutputSizes)
    }

    @Test
    fun getOutputSizesByFormatTwice_whenReturnedArrayIsNull() {
        assumeTrue(streamConfigurationMapCompat.getOutputSizes(ImageFormat.RGB_565) == null)
        assertThat(streamConfigurationMapCompat.getOutputSizes(ImageFormat.RGB_565)).isNull()
    }

    @Test
    fun getOutputSizesByClassTwice_whenReturnedArrayIsNull() {
        assumeTrue(streamConfigurationMapCompat.getOutputSizes(ImageFormat::class.java) == null)
        assertThat(streamConfigurationMapCompat.getOutputSizes(ImageFormat::class.java)).isNull()
    }

    @Test
    @Config(minSdk = 23)
    fun getHighResolutionOutputSizesTwice_whenReturnedArrayIsNull() {
        assumeTrue(
            streamConfigurationMapCompat.getHighResolutionOutputSizes(ImageFormat.JPEG) == null
        )
        assertThat(streamConfigurationMapCompat.getHighResolutionOutputSizes(ImageFormat.JPEG))
            .isNull()
    }

    @Test
    fun getOutputFormats_notThrowingNullPointerException() {
        val cameraId = "0"
        val compat =
            StreamConfigurationMapCompat.toStreamConfigurationMapCompat(
                StreamConfigurationMapBuilder.newBuilder().build(),
                OutputSizesCorrector(cameraId)
            )

        // b/361590210: check the workaround for NullPointerException issue (on API 23+) of
        // StreamConfigurationMap provided by Robolectric is applied.
        if (Build.VERSION.SDK_INT >= 23) {
            assertThat(compat.getOutputFormats()).isNull()
        } else {
            assertThat(compat.getOutputFormats()).isNotNull()
        }
    }
}
