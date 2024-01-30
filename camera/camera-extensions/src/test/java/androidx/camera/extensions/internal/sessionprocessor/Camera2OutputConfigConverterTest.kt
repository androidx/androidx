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

package androidx.camera.extensions.internal.sessionprocessor

import android.graphics.ImageFormat
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl
import androidx.camera.extensions.impl.advanced.ImageReaderOutputConfigImpl
import androidx.camera.extensions.impl.advanced.MultiResolutionImageReaderOutputConfigImpl
import androidx.camera.extensions.impl.advanced.SurfaceOutputConfigImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val PHYSICAL_CAMERA_ID = "3"
private const val SURFACE_GROUP_ID = 1
private const val IMAGE_FORMAT = ImageFormat.YUV_420_888
private const val MAX_IMAGES = 2
private const val ID = 10

@RequiresApi(21)
private val SIZE = Size(640, 480)

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Camera2OutputConfigConverterTest {
    private val surface = mock(Surface::class.java)
    private val sharedConfigImpls = listOf(
        FakeImageReaderConfigImpl(
            ID,
            SURFACE_GROUP_ID,
            PHYSICAL_CAMERA_ID,
            emptyList(),
            SIZE,
            IMAGE_FORMAT,
            MAX_IMAGES
        ),
        FakeSurfaceConfigImpl(
            ID,
            SURFACE_GROUP_ID,
            PHYSICAL_CAMERA_ID,
            emptyList(),
            surface,
        )
    )

    @Test
    fun canConvertFromImpl_imageReaderOutputConfigImpl() {
        // Arrange
        val outputConfigImpl = FakeImageReaderConfigImpl(
            ID,
            SURFACE_GROUP_ID,
            PHYSICAL_CAMERA_ID,
            sharedConfigImpls,
            SIZE,
            IMAGE_FORMAT,
            MAX_IMAGES
        )

        // Act
        val outputConfig = Camera2OutputConfigConverter.fromImpl(outputConfigImpl)

        // Assert
        assertOutputConfigElements(outputConfig, outputConfigImpl)
    }

    @Test
    fun canConvertFromImpl_surfaceOutputConfigImpl() {
        // Arrange
        val outputConfigImpl = FakeSurfaceConfigImpl(
            ID,
            SURFACE_GROUP_ID,
            PHYSICAL_CAMERA_ID,
            sharedConfigImpls,
            surface,
        )

        // Act
        val outputConfig = Camera2OutputConfigConverter.fromImpl(outputConfigImpl)

        // Assert
        assertOutputConfigElements(outputConfig, outputConfigImpl)
    }

    @Test
    fun canConvertFromImpl_multiResImageOutputConfigImpl() {
        // Arrange
        val outputConfigImpl = FakeMultiResImageReaderConfigImpl(
            ID,
            SURFACE_GROUP_ID,
            PHYSICAL_CAMERA_ID,
            sharedConfigImpls,
            IMAGE_FORMAT,
            MAX_IMAGES
        )

        // Act
        val outputConfig = Camera2OutputConfigConverter.fromImpl(outputConfigImpl)

        // Assert
        assertOutputConfigElements(outputConfig, outputConfigImpl)
    }

    @Test
    fun canConvertFromImpl_surfaceSharingConfigsIsNull() {
        // Arrange
        val outputConfigImpl = FakeSurfaceConfigImpl(
            ID,
            SURFACE_GROUP_ID,
            PHYSICAL_CAMERA_ID,
            null,
            surface,
        )

        // Act
        val outputConfig = Camera2OutputConfigConverter.fromImpl(outputConfigImpl)

        // Assert
        assertOutputConfigElements(outputConfig, outputConfigImpl)
    }

    private fun assertOutputConfigElements(
        outputConfig: Camera2OutputConfig,
        outputConfigImpl: Camera2OutputConfigImpl
    ) {
        assertThat(outputConfig.id).isEqualTo(outputConfigImpl.id)
        assertThat(outputConfig.physicalCameraId).isEqualTo(outputConfigImpl.physicalCameraId)
        assertThat(outputConfig.surfaceGroupId).isEqualTo(outputConfigImpl.surfaceGroupId)

        assertThat(outputConfig.surfaceSharingOutputConfigs.size)
            .isEqualTo(outputConfigImpl.surfaceSharingOutputConfigs?.size ?: 0)
        outputConfig.surfaceSharingOutputConfigs.forEachIndexed { index, sharingConfig ->
            val sharedConfigImpl = outputConfigImpl.surfaceSharingOutputConfigs!![index]
            assertOutputConfigElements(sharingConfig, sharedConfigImpl)
        }

        when (outputConfig) {
            is SurfaceOutputConfig -> {
                val surfaceOutputConfigImpl = outputConfigImpl as SurfaceOutputConfigImpl
                assertThat(outputConfig.surface).isEqualTo(surfaceOutputConfigImpl.surface)
            }
            is ImageReaderOutputConfig -> {
                val imageReaderOutputConfigImpl = outputConfigImpl as ImageReaderOutputConfigImpl
                assertThat(outputConfig.size).isEqualTo(imageReaderOutputConfigImpl.size)
                assertThat(outputConfig.imageFormat)
                    .isEqualTo(imageReaderOutputConfigImpl.imageFormat)
                assertThat(outputConfig.maxImages).isEqualTo(imageReaderOutputConfigImpl.maxImages)
            }

            is MultiResolutionImageReaderOutputConfig -> {
                val multiResOutputConfigImpl = outputConfigImpl
                    as MultiResolutionImageReaderOutputConfigImpl
                assertThat(outputConfig.imageFormat).isEqualTo(multiResOutputConfigImpl.imageFormat)
                assertThat(outputConfig.maxImages).isEqualTo(multiResOutputConfigImpl.maxImages)
            }
        }
    }

    class FakeImageReaderConfigImpl(
        private val id: Int,
        private val surfaceGroupId: Int,
        private val physicalCameraId: String?,
        private val sharedOutputConfigImpl: List<Camera2OutputConfigImpl>?,
        private val size: Size,
        private val format: Int,
        private val maxImages: Int,
    ) : ImageReaderOutputConfigImpl {
        override fun getId() = id
        override fun getSurfaceGroupId() = surfaceGroupId
        override fun getPhysicalCameraId() = physicalCameraId
        override fun getSurfaceSharingOutputConfigs() = sharedOutputConfigImpl
        override fun getSize() = size
        override fun getImageFormat() = format
        override fun getMaxImages() = maxImages
    }

    class FakeSurfaceConfigImpl(
        private val id: Int,
        private val surfaceGroupId: Int,
        private val physicalCameraId: String?,
        private val sharedOutputConfigImpl: List<Camera2OutputConfigImpl>?,
        private val surface: Surface,
    ) : SurfaceOutputConfigImpl {
        override fun getId() = id
        override fun getSurfaceGroupId() = surfaceGroupId
        override fun getPhysicalCameraId() = physicalCameraId
        override fun getSurfaceSharingOutputConfigs() = sharedOutputConfigImpl
        override fun getSurface() = surface
    }

    class FakeMultiResImageReaderConfigImpl(
        private val id: Int,
        private val surfaceGroupId: Int,
        private val physicalCameraId: String?,
        private val sharedOutputConfigImpl: List<Camera2OutputConfigImpl>?,
        private val format: Int,
        private val maxImages: Int,
    ) : MultiResolutionImageReaderOutputConfigImpl {
        override fun getId() = id
        override fun getSurfaceGroupId() = surfaceGroupId
        override fun getPhysicalCameraId() = physicalCameraId
        override fun getSurfaceSharingOutputConfigs() = sharedOutputConfigImpl
        override fun getImageFormat() = format
        override fun getMaxImages() = maxImages
    }
}
