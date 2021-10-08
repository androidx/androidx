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
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImmediateSurface
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

private const val BRAND = "SAMSUNG"
private const val HARDWARE = "samsungexynos7570"

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
    fun sort_mediaCodecSurfaceIsInTheLast() {
        // Arrange.
        setupQuirkDevice()
        val videoSurface = createSurface(containerClass = MediaCodec::class.java)
        val previewSurface = createSurface(containerClass = Preview::class.java)
        val imageSurface = createSurface(containerClass = ImageCapture::class.java)
        val surfaceSorter = SurfaceSorter()

        // All combinations
        val surfaces1 = mutableListOf(previewSurface, videoSurface, imageSurface)
        val surfaces2 = mutableListOf(previewSurface, imageSurface, videoSurface)
        val surfaces3 = mutableListOf(videoSurface, previewSurface, imageSurface)
        val surfaces4 = mutableListOf(videoSurface, imageSurface, previewSurface)
        val surfaces5 = mutableListOf(imageSurface, videoSurface, previewSurface)
        val surfaces6 = mutableListOf(imageSurface, previewSurface, videoSurface)

        // Act.
        surfaceSorter.sort(surfaces1)
        surfaceSorter.sort(surfaces2)
        surfaceSorter.sort(surfaces3)
        surfaceSorter.sort(surfaces4)
        surfaceSorter.sort(surfaces5)
        surfaceSorter.sort(surfaces6)

        // Assert.
        assertThat(surfaces1.last()).isEqualTo(videoSurface)
        assertThat(surfaces2.last()).isEqualTo(videoSurface)
        assertThat(surfaces3.last()).isEqualTo(videoSurface)
        assertThat(surfaces4.last()).isEqualTo(videoSurface)
        assertThat(surfaces5.last()).isEqualTo(videoSurface)
        assertThat(surfaces6.last()).isEqualTo(videoSurface)
    }

    @Test
    fun sort_videoCaptureSurfaceIsInTheLast() {
        // Arrange.
        setupQuirkDevice()
        val videoSurface = createSurface(containerClass = VideoCapture::class.java)
        val previewSurface = createSurface(containerClass = Preview::class.java)
        val imageSurface = createSurface(containerClass = ImageCapture::class.java)
        val surfaceSorter = SurfaceSorter()

        // All combinations
        val surfaces1 = mutableListOf(previewSurface, videoSurface, imageSurface)
        val surfaces2 = mutableListOf(previewSurface, imageSurface, videoSurface)
        val surfaces3 = mutableListOf(videoSurface, previewSurface, imageSurface)
        val surfaces4 = mutableListOf(videoSurface, imageSurface, previewSurface)
        val surfaces5 = mutableListOf(imageSurface, videoSurface, previewSurface)
        val surfaces6 = mutableListOf(imageSurface, previewSurface, videoSurface)

        // Act.
        surfaceSorter.sort(surfaces1)
        surfaceSorter.sort(surfaces2)
        surfaceSorter.sort(surfaces3)
        surfaceSorter.sort(surfaces4)
        surfaceSorter.sort(surfaces5)
        surfaceSorter.sort(surfaces6)

        // Assert.
        assertThat(surfaces1.last()).isEqualTo(videoSurface)
        assertThat(surfaces2.last()).isEqualTo(videoSurface)
        assertThat(surfaces3.last()).isEqualTo(videoSurface)
        assertThat(surfaces4.last()).isEqualTo(videoSurface)
        assertThat(surfaces5.last()).isEqualTo(videoSurface)
        assertThat(surfaces6.last()).isEqualTo(videoSurface)
    }

    @Test
    fun notQuirkDevice_wontSort() {
        // Arrange.
        val videoSurface = createSurface(containerClass = VideoCapture::class.java)
        val previewSurface = createSurface(containerClass = Preview::class.java)
        val imageSurface = createSurface(containerClass = ImageCapture::class.java)
        val surfaceSorter = SurfaceSorter()
        val surfaces = mutableListOf(videoSurface, previewSurface, imageSurface)

        // Act.
        surfaceSorter.sort(surfaces)

        // Assert.
        assertThat(surfaces).isEqualTo(listOf(videoSurface, previewSurface, imageSurface))
    }

    private fun createSurface(
        containerClass: Class<*>
    ): DeferrableSurface {
        val deferrableSurface = ImmediateSurface(mock(Surface::class.java))
        deferrableSurface.setContainerClass(containerClass)
        deferrableSurfaces.add(deferrableSurface)
        return deferrableSurface
    }

    private fun setupQuirkDevice() {
        ShadowBuild.setBrand(BRAND)
        ShadowBuild.setHardware(HARDWARE)
    }
}
