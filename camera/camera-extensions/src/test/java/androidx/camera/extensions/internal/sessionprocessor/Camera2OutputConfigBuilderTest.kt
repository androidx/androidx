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

package androidx.camera.extensions.internal.sessionprocessor

import android.graphics.ImageFormat
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.extensions.impl.advanced.ImageReaderOutputConfigImpl
import androidx.camera.extensions.impl.advanced.MultiResolutionImageReaderOutputConfigImpl
import androidx.camera.extensions.impl.advanced.SurfaceOutputConfigImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val PHYSICAL_CAMERA_ID = "3"
private const val SURFACE_GROUP_ID = 1
private const val IMAGE_FORMAT = ImageFormat.YUV_420_888
private const val MAX_IMAGES = 2
private val SIZE = Size(640, 480)

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Camera2OutputConfigBuilderTest {
    @Test
    fun canBuildFromImpl_imageReaderOutputConfigImpl() {
        // Arrange
        val outConfigImpl = mock(ImageReaderOutputConfigImpl::class.java)
        `when`(outConfigImpl.size).thenReturn(SIZE)
        `when`(outConfigImpl.imageFormat).thenReturn(IMAGE_FORMAT)
        `when`(outConfigImpl.maxImages).thenReturn(MAX_IMAGES)

        // Act
        val builder = Camera2OutputConfigBuilder.fromImpl(outConfigImpl)
        builder.setPhysicalCameraId(PHYSICAL_CAMERA_ID)
        builder.setSurfaceGroupId(SURFACE_GROUP_ID)

        // Assert
        val camera2OutputConfig = builder.build() as ImageReaderOutputConfig
        assertThat(camera2OutputConfig.physicalCameraId).isEqualTo(PHYSICAL_CAMERA_ID)
        assertThat(camera2OutputConfig.surfaceGroupId).isEqualTo(SURFACE_GROUP_ID)
        assertThat(camera2OutputConfig.size).isEqualTo(SIZE)
        assertThat(camera2OutputConfig.imageFormat).isEqualTo(IMAGE_FORMAT)
        assertThat(camera2OutputConfig.maxImages).isEqualTo(MAX_IMAGES)
    }

    @Test
    fun canBuildFromImpl_surfaceOutputConfigImpl() {
        // Arrange
        val outConfigImpl = mock(SurfaceOutputConfigImpl::class.java)
        val surface = mock(Surface::class.java)
        `when`(outConfigImpl.surface).thenReturn(surface)

        // Act
        val builder = Camera2OutputConfigBuilder.fromImpl(outConfigImpl)
        builder.setPhysicalCameraId(PHYSICAL_CAMERA_ID)
        builder.setSurfaceGroupId(SURFACE_GROUP_ID)

        // Assert
        val camera2OutputConfig = builder.build() as SurfaceOutputConfig
        assertThat(camera2OutputConfig.physicalCameraId).isEqualTo(PHYSICAL_CAMERA_ID)
        assertThat(camera2OutputConfig.surfaceGroupId).isEqualTo(SURFACE_GROUP_ID)
        assertThat(camera2OutputConfig.surface).isEqualTo(surface)
    }

    @Test
    fun canBuildFromImpl_MultiResolutionImageReaderOutputConfigImpl() {
        // Arrange
        val outConfigImpl = mock(MultiResolutionImageReaderOutputConfigImpl::class.java)
        `when`(outConfigImpl.imageFormat).thenReturn(IMAGE_FORMAT)
        `when`(outConfigImpl.maxImages).thenReturn(MAX_IMAGES)

        // Act
        val builder = Camera2OutputConfigBuilder.fromImpl(outConfigImpl)
        builder.setPhysicalCameraId(PHYSICAL_CAMERA_ID)
        builder.setSurfaceGroupId(SURFACE_GROUP_ID)

        // Assert
        val camera2OutputConfig = builder.build() as MultiResolutionImageReaderOutputConfig
        assertThat(camera2OutputConfig.physicalCameraId).isEqualTo(PHYSICAL_CAMERA_ID)
        assertThat(camera2OutputConfig.surfaceGroupId).isEqualTo(SURFACE_GROUP_ID)
        assertThat(camera2OutputConfig.imageFormat).isEqualTo(IMAGE_FORMAT)
        assertThat(camera2OutputConfig.maxImages).isEqualTo(MAX_IMAGES)
    }

    @Test
    fun canBuildFromImageReaderOutputConfig() {
        // Act
        val builder = Camera2OutputConfigBuilder
            .newImageReaderConfig(SIZE, IMAGE_FORMAT, MAX_IMAGES)
        builder.setPhysicalCameraId(PHYSICAL_CAMERA_ID)
        builder.setSurfaceGroupId(SURFACE_GROUP_ID)

        // Assert
        val camera2OutputConfig = builder.build() as ImageReaderOutputConfig
        assertThat(camera2OutputConfig.physicalCameraId).isEqualTo(PHYSICAL_CAMERA_ID)
        assertThat(camera2OutputConfig.surfaceGroupId).isEqualTo(SURFACE_GROUP_ID)
        assertThat(camera2OutputConfig.size).isEqualTo(SIZE)
        assertThat(camera2OutputConfig.imageFormat).isEqualTo(IMAGE_FORMAT)
        assertThat(camera2OutputConfig.maxImages).isEqualTo(MAX_IMAGES)
    }

    @Test
    fun canBuildFromSurfaceOutputConfig() {
        // Arrange
        val surface = mock(Surface::class.java)

        // Act
        val builder = Camera2OutputConfigBuilder.newSurfaceConfig(surface)
        builder.setPhysicalCameraId(PHYSICAL_CAMERA_ID)
        builder.setSurfaceGroupId(SURFACE_GROUP_ID)

        // Assert
        val camera2OutputConfig = builder.build() as SurfaceOutputConfig
        assertThat(camera2OutputConfig.physicalCameraId).isEqualTo(PHYSICAL_CAMERA_ID)
        assertThat(camera2OutputConfig.surfaceGroupId).isEqualTo(SURFACE_GROUP_ID)
        assertThat(camera2OutputConfig.surface).isEqualTo(surface)
    }

    @Test
    fun canBuildFromMultiResolutionImageReaderOutputConfig() {
        // Act
        val builder = Camera2OutputConfigBuilder
            .newMultiResolutionImageReaderConfig(IMAGE_FORMAT, MAX_IMAGES)
        builder.setPhysicalCameraId(PHYSICAL_CAMERA_ID)
        builder.setSurfaceGroupId(SURFACE_GROUP_ID)

        // Assert
        val camera2OutputConfig = builder.build() as MultiResolutionImageReaderOutputConfig
        assertThat(camera2OutputConfig.physicalCameraId).isEqualTo(PHYSICAL_CAMERA_ID)
        assertThat(camera2OutputConfig.surfaceGroupId).isEqualTo(SURFACE_GROUP_ID)
        assertThat(camera2OutputConfig.imageFormat).isEqualTo(IMAGE_FORMAT)
        assertThat(camera2OutputConfig.maxImages).isEqualTo(MAX_IMAGES)
    }

    @Test
    fun canAddSurfaceSharing() {
        // Arrange
        val surface = mock(Surface::class.java)
        val builder = Camera2OutputConfigBuilder
            .newImageReaderConfig(SIZE, IMAGE_FORMAT, MAX_IMAGES)
        val outputConfig1 = Camera2OutputConfigBuilder.newSurfaceConfig(surface).build()
        val outputConfig2 = Camera2OutputConfigBuilder
            .newImageReaderConfig(SIZE, IMAGE_FORMAT, MAX_IMAGES).build()

        // Act
        builder.addSurfaceSharingOutputConfig(outputConfig1)
        builder.addSurfaceSharingOutputConfig(outputConfig2)
        val outputConfig = builder.build()

        // Assert
        assertThat(outputConfig.surfaceSharingOutputConfigs)
            .containsExactly(outputConfig1, outputConfig2)
    }
}