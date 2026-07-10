/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidImageTest {

    private val imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2)
    private val imageWriter = ImageWriter.newInstance(imageReader.surface, 2)
    private var realImage: Image? = null

    @After
    fun tearDown() {
        realImage?.close()
        imageWriter.close()
        imageReader.close()
    }

    @Test
    fun wrapImage_propertiesAreSynced() {
        val image = imageWriter.dequeueInputImage()
        image.timestamp = 12345L

        val androidImage = AndroidImage(image)

        assertThat(androidImage.width).isEqualTo(640)
        assertThat(androidImage.height).isEqualTo(480)
        assertThat(androidImage.format).isEqualTo(ImageFormat.YUV_420_888)
        assertThat(androidImage.timestamp).isEqualTo(12345L)

        // Modify via wrapper and check underlying Image
        androidImage.timestamp = 54321L
        assertThat(image.timestamp).isEqualTo(54321L)

        androidImage.close()
    }

    @Test
    fun getTimestamp_afterClose_doesNotThrow() {
        val image = imageWriter.dequeueInputImage()
        image.timestamp = 12345L

        val androidImage = AndroidImage(image)

        // Cache the timestamp before closing
        val t = androidImage.timestamp
        assertThat(t).isEqualTo(12345L)

        androidImage.close()

        // Accessing the timestamp after close should not throw an exception
        assertThat(androidImage.timestamp).isEqualTo(12345L)
    }

    private fun getRealImage(): Image {
        val inputImage = imageWriter.dequeueInputImage()
        imageWriter.queueInputImage(inputImage)
        realImage = imageReader.acquireLatestImage()
        return realImage!!
    }

    @Test
    fun unwrapAs_androidImage() {
        val image = getRealImage()
        val androidImage = AndroidImage(image)

        assertThat(androidImage.unwrapAs(AndroidImage::class.java)).isSameInstanceAs(androidImage)
    }

    @Test
    fun unwrapAs_imageWrapper() {
        val image = getRealImage()
        val androidImage = AndroidImage(image)

        assertThat(androidImage.unwrapAs(ImageWrapper::class.java)).isSameInstanceAs(androidImage)
    }

    @Test
    fun unwrapAs_mutableImageWrapper() {
        val image = getRealImage()
        val androidImage = AndroidImage(image)

        assertThat(androidImage.unwrapAs(MutableImageWrapper::class.java))
            .isSameInstanceAs(androidImage)
    }

    @Test
    fun unwrapAs_image() {
        val image = getRealImage()
        val androidImage = AndroidImage(image)

        assertThat(androidImage.unwrapAs(Image::class.java)).isSameInstanceAs(image)
    }

    @Test
    fun plane_unwrapAs_androidImagePlane() {
        val image = getRealImage()
        val plane = AndroidImage.Plane(image.planes[0])

        assertThat(plane.unwrapAs(AndroidImage.Plane::class.java)).isSameInstanceAs(plane)
    }

    @Test
    fun plane_unwrapAs_imagePlane() {
        val image = getRealImage()
        val plane = AndroidImage.Plane(image.planes[0])

        assertThat(plane.unwrapAs(ImagePlane::class.java)).isSameInstanceAs(plane)
    }

    @Test
    fun plane_unwrapAs_image_plane() {
        val image = getRealImage()
        val imagePlane = image.planes[0]
        val plane = AndroidImage.Plane(imagePlane)

        assertThat(plane.unwrapAs(Image.Plane::class.java)).isSameInstanceAs(imagePlane)
    }
}
