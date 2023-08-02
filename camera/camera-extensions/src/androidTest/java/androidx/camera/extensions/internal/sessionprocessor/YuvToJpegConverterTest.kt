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
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxys
import androidx.camera.core.ImmutableImageInfo
import androidx.camera.core.impl.ImageReaderProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.TestImageUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class YuvToJpegConverterTest {
    companion object {
        const val WIDTH = 640
        const val HEIGHT = 480
        const val MAX_IMAGES = 2
    }

    private lateinit var jpegImageReaderProxy: ImageReaderProxy
    private lateinit var yuvToJpegConverter: YuvToJpegConverter

    @Before
    fun setUp() {
        jpegImageReaderProxy = ImageReaderProxys.createIsolatedReader(
            WIDTH, HEIGHT, ImageFormat.JPEG, MAX_IMAGES
        )
        yuvToJpegConverter = YuvToJpegConverter(
            100, jpegImageReaderProxy.surface!!
        )
    }

    @After
    fun tearDown() {
        jpegImageReaderProxy.close()
    }

    private fun generateYuvImage(): ImageProxy {
        return TestImageUtil.createYuvFakeImageProxy(
            ImmutableImageInfo.create(
                TagBundle.emptyBundle(), 0, 0, Matrix()
            ), WIDTH, HEIGHT
        )
    }

    @Test
    fun canOutputJpeg() = runBlocking {
        val deferredImage = CompletableDeferred<ImageProxy>()
        jpegImageReaderProxy.setOnImageAvailableListener({ imageReader ->
            imageReader.acquireNextImage()?.let { deferredImage.complete(it) }
        }, CameraXExecutors.ioExecutor())

        val imageYuv = generateYuvImage()
        yuvToJpegConverter.writeYuvImage(imageYuv)

        withTimeout(1000) {
            deferredImage.await().use {
                assertThat(it.format).isEqualTo(ImageFormat.JPEG)
                assertExifWidthAndHeight(it, WIDTH, HEIGHT)
            }
        }
    }

    private fun assertExifWidthAndHeight(imageProxy: ImageProxy, width: Int, height: Int) {
        val exif = Exif.createFromImageProxy(imageProxy)
        assertThat(exif.width).isEqualTo(width)
        assertThat(exif.height).isEqualTo(height)
    }

    @Test
    fun canSetRotation() = runBlocking {
        val rotationDegrees = 270
        val deferredImage = CompletableDeferred<ImageProxy>()
        jpegImageReaderProxy.setOnImageAvailableListener({ imageReader ->
            imageReader.acquireNextImage()?.let { deferredImage.complete(it) }
        }, CameraXExecutors.ioExecutor())

        val imageYuv = generateYuvImage()
        yuvToJpegConverter.setRotationDegrees(rotationDegrees)
        yuvToJpegConverter.writeYuvImage(imageYuv)

        withTimeout(1000) {
            deferredImage.await().use {
                assertThat(it.format).isEqualTo(ImageFormat.JPEG)
                val exif = Exif.createFromImageProxy(it)
                assertThat(exif.rotation).isEqualTo(rotationDegrees)
            }
        }
    }
}
