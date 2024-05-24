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

package androidx.camera.core.internal.compat.workaround

import android.media.MediaCodec
import android.os.Build
import android.view.Surface
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.SessionConfig
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceSorterTest {

    private val deferrableSurfaces: MutableList<DeferrableSurface> = mutableListOf()

    @After
    fun tearDown() {
        deferrableSurfaces.forEach { it.close() }
    }

    @Test
    fun sort_previewSurfaceIsInTheFirstAndMediaCodecSurfaceIsInTheLast() {
        // Arrange.
        val videoOutput =
            SessionConfig.OutputConfig.builder(
                    createSurface(containerClass = MediaCodec::class.java)
                )
                .build()
        val previewOutput =
            SessionConfig.OutputConfig.builder(createSurface(containerClass = Preview::class.java))
                .build()
        val imageOutput =
            SessionConfig.OutputConfig.builder(
                    createSurface(containerClass = ImageCapture::class.java)
                )
                .build()
        val surfaceSorter = SurfaceSorter()

        // All combinations
        val outputConfigs1 = mutableListOf(previewOutput, videoOutput, imageOutput)
        val outputConfigs2 = mutableListOf(previewOutput, imageOutput, videoOutput)
        val outputConfigs3 = mutableListOf(videoOutput, previewOutput, imageOutput)
        val outputConfigs4 = mutableListOf(videoOutput, imageOutput, previewOutput)
        val outputConfigs5 = mutableListOf(imageOutput, videoOutput, previewOutput)
        val outputConfigs6 = mutableListOf(imageOutput, previewOutput, videoOutput)

        // Act.
        surfaceSorter.sort(outputConfigs1)
        surfaceSorter.sort(outputConfigs2)
        surfaceSorter.sort(outputConfigs3)
        surfaceSorter.sort(outputConfigs4)
        surfaceSorter.sort(outputConfigs5)
        surfaceSorter.sort(outputConfigs6)

        // Assert.
        assertThat(outputConfigs1.first()).isEqualTo(previewOutput)
        assertThat(outputConfigs2.first()).isEqualTo(previewOutput)
        assertThat(outputConfigs3.first()).isEqualTo(previewOutput)
        assertThat(outputConfigs4.first()).isEqualTo(previewOutput)
        assertThat(outputConfigs5.first()).isEqualTo(previewOutput)
        assertThat(outputConfigs6.first()).isEqualTo(previewOutput)

        assertThat(outputConfigs1.last()).isEqualTo(videoOutput)
        assertThat(outputConfigs2.last()).isEqualTo(videoOutput)
        assertThat(outputConfigs3.last()).isEqualTo(videoOutput)
        assertThat(outputConfigs4.last()).isEqualTo(videoOutput)
        assertThat(outputConfigs5.last()).isEqualTo(videoOutput)
        assertThat(outputConfigs6.last()).isEqualTo(videoOutput)
    }

    private fun createSurface(containerClass: Class<*>): DeferrableSurface {
        val deferrableSurface = ImmediateSurface(mock(Surface::class.java))
        deferrableSurface.setContainerClass(containerClass)
        deferrableSurfaces.add(deferrableSurface)
        return deferrableSurface
    }
}
