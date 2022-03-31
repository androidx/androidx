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

package androidx.camera.core

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Handler
import androidx.camera.core.impl.ImageReaderProxy
import androidx.camera.core.impl.MutableTagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.spy

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23) // This test uses ImageWriter which is supported from api 23.
class ModifiableImageReaderProxyTest {
    private lateinit var imageReader: ImageReader
    private lateinit var imageReaderProxy: ModifiableImageReaderProxy
    private var imageWriter: ImageWriter? = null

    @Before
    fun setUp() {
        imageReader = spy(ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2))
        imageReaderProxy = ModifiableImageReaderProxy(imageReader)
    }

    @After
    fun tearDown() {
        imageReaderProxy.close()
        imageWriter?.close()
    }

    @Test
    fun canModifyImageTagBundle_acquireNext() {
        generateImage(imageReader)

        val tagBundle = MutableTagBundle.create()
        imageReaderProxy.setImageTagBundle(tagBundle)
        val imageProxy = imageReaderProxy.acquireNextImage()
        assertThat(imageProxy!!.imageInfo.tagBundle).isEqualTo(tagBundle)
    }

    @Test
    fun canModifyImageTagBundle_acquireLatest() {
        generateImage(imageReader)

        val tagBundle = MutableTagBundle.create()
        imageReaderProxy.setImageTagBundle(tagBundle)
        val imageProxy = imageReaderProxy.acquireLatestImage()
        assertThat(imageProxy!!.imageInfo.tagBundle).isEqualTo(tagBundle)
        imageProxy.close()
    }

    @Test
    fun canModifyImageTimestamp_acquireNext() {
        generateImage(imageReader)

        imageReaderProxy.setImageTimeStamp(1000)
        val imageProxy = imageReaderProxy.acquireNextImage()
        assertThat(imageProxy!!.imageInfo.timestamp).isEqualTo(1000)
        imageProxy.close()
    }

    @Test
    fun canModifyImageTimestamp_acquireLatest() {
        generateImage(imageReader)

        imageReaderProxy.setImageTimeStamp(1000)
        val imageProxy = imageReaderProxy.acquireLatestImage()
        assertThat(imageProxy!!.imageInfo.timestamp).isEqualTo(1000)
        imageProxy.close()
    }

    @Test
    fun canModifyImageRotationDegrees_acquireNext() {
        generateImage(imageReader)

        imageReaderProxy.setImageRotationDegrees(90)
        val imageProxy = imageReaderProxy.acquireNextImage()
        assertThat(imageProxy!!.imageInfo.rotationDegrees).isEqualTo(90)
        imageProxy.close()
    }

    @Test
    fun canModifyImageRotationDegress_acquireLatest() {
        generateImage(imageReader)

        imageReaderProxy.setImageRotationDegrees(90)
        val imageProxy = imageReaderProxy.acquireLatestImage()
        assertThat(imageProxy!!.imageInfo.rotationDegrees).isEqualTo(90)
        imageProxy.close()
    }

    @Test
    fun canModifyImageMatrix_acquireNext() {
        generateImage(imageReader)

        val matrix = Matrix()
        imageReaderProxy.setImageSensorToBufferTransformaMatrix(matrix)
        val imageProxy = imageReaderProxy.acquireNextImage()
        assertThat(imageProxy!!.imageInfo.sensorToBufferTransformMatrix).isSameInstanceAs(matrix)
        imageProxy.close()
    }

    @Test
    fun canModifyImageMatrix_acquireLatest() {
        generateImage(imageReader)

        val matrix = Matrix()
        imageReaderProxy.setImageSensorToBufferTransformaMatrix(matrix)
        val imageProxy = imageReaderProxy.acquireLatestImage()
        assertThat(imageProxy!!.imageInfo.sensorToBufferTransformMatrix).isSameInstanceAs(matrix)
        imageProxy.close()
    }

    private fun generateImage(imageReader: ImageReader) {
        imageWriter = ImageWriter.newInstance(imageReader.surface, 2)
        val image = imageWriter!!.dequeueInputImage()
        imageWriter!!.queueInputImage(image)
    }

    @Test
    fun parametersMatchesInnerImageReader() {
        assertThat(imageReaderProxy.width).isEqualTo(640)
        assertThat(imageReaderProxy.height).isEqualTo(480)
        assertThat(imageReaderProxy.imageFormat).isEqualTo(ImageFormat.YUV_420_888)
        assertThat(imageReaderProxy.maxImages).isEqualTo(2)
        assertThat(imageReaderProxy.surface).isEqualTo(imageReader.surface)
    }

    @Test
    fun setOnImageAvailableListener_innerReaderIsInvoked() {
        val listener = Mockito.mock(
            ImageReaderProxy.OnImageAvailableListener::class.java
        )

        imageReaderProxy.setOnImageAvailableListener(
            listener,
            CameraXExecutors.directExecutor()
        )

        val transformedListenerCaptor = ArgumentCaptor.forClass(
            ImageReader.OnImageAvailableListener::class.java
        )
        val handlerCaptor = ArgumentCaptor.forClass(
            Handler::class.java
        )
        Mockito.verify(imageReader, Mockito.times(1))
            .setOnImageAvailableListener(
                transformedListenerCaptor.capture(), handlerCaptor.capture()
            )

        transformedListenerCaptor.value.onImageAvailable(imageReader)
        Mockito.verify(listener, Mockito.times(1)).onImageAvailable(imageReaderProxy)
    }
}