/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import android.hardware.HardwareBuffer
import android.os.Build
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeImage
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [OutputImage] and [SharedOutputImage] */
@RunWith(RobolectricTestRunner::class)
class SharedOutputImageTest {
    private val streamId = StreamId(42)
    private val outputId = OutputId(64)

    private lateinit var fakeImage: FakeImage
    private lateinit var outputImage: OutputImage
    private val finalizer: Finalizer<OutputImage> = mock()

    @Before
    fun init() {
        fakeImage = FakeImage(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, IMAGE_TIMESTAMP)
        outputImage = OutputImage.from(streamId, outputId, fakeImage)
    }

    @After
    fun tearDown() {
        outputImage.close()
    }

    @Test
    fun sharedImagesIsNotClosedByDefault() {
        val sharedImage = SharedOutputImage.from(outputImage)
        assertThat(fakeImage.isClosed).isFalse()
        sharedImage.close()
        assertThat(fakeImage.isClosed).isTrue()
    }

    @Test
    fun closingSharedImageClosesWrappedImage() {
        val sharedImage = SharedOutputImage.from(outputImage)
        sharedImage.close()
        assertThat(fakeImage.isClosed).isTrue()
    }

    @Test
    fun closingMultipleTimesOnlyClosesImageOnce() {
        val sharedImage = SharedOutputImage.from(outputImage)
        sharedImage.close()
        sharedImage.close()
        sharedImage.close()
        assertThat(fakeImage.numberOfTimesClosed).isEqualTo(1)
    }

    @Test
    fun sharedImagesCanBeAcquired() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        val sharedImage2 = sharedImage1.acquire()
        val sharedImage3 = sharedImage2.acquire()
        sharedImage1.close()
        sharedImage2.close()
        assertThat(fakeImage.isClosed).isFalse()

        sharedImage3.close()
        assertThat(fakeImage.isClosed).isTrue()
    }

    @Test
    fun sharedImageCannotBeAcquiredAfterClose() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        sharedImage1.close()

        val sharedImage2 = sharedImage1.acquireOrNull()
        assertThat(sharedImage2).isNull()
        assertThat(fakeImage.isClosed).isTrue()
    }

    @Test
    fun sharedImageCannotBeAcquiredFromClosedReference() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        val sharedImage2 = sharedImage1.acquire()
        sharedImage1.close()

        val sharedImageFrom1 = sharedImage1.acquireOrNull()
        val sharedImageFrom2 = sharedImage2.acquireOrNull()

        assertThat(sharedImageFrom1).isNull()
        assertThat(sharedImageFrom2).isNotNull()
        assertThat(fakeImage.isClosed).isFalse()
    }

    @Config(minSdk = Build.VERSION_CODES.P)
    @Test
    fun unwrapAsHardwareBufferReturnsHardwareBufferFromParentClass() {
        val imageHardwareBuffer = mock<HardwareBuffer>()
        val fakeImageWithHardwareBuffer =
            FakeImage(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, IMAGE_TIMESTAMP, imageHardwareBuffer)
        val outputImage = OutputImage.from(streamId, outputId, fakeImageWithHardwareBuffer)
        val sharedImage = SharedOutputImage.from(outputImage)

        val hardwareBuffer = sharedImage.unwrapAs(HardwareBuffer::class)

        checkNotNull(hardwareBuffer)
        assertThat(imageHardwareBuffer).isSameInstanceAs(hardwareBuffer)
    }

    @Config(minSdk = Build.VERSION_CODES.P)
    @Test
    fun getHardwareBufferReturnsHardwareBufferFromParentClass() {
        val imageHardwareBuffer = mock<HardwareBuffer>()
        val fakeImageWithHardwareBuffer =
            FakeImage(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, IMAGE_TIMESTAMP, imageHardwareBuffer)
        val outputImage = OutputImage.from(streamId, outputId, fakeImageWithHardwareBuffer)
        val sharedImage = SharedOutputImage.from(outputImage)

        val hardwareBuffer = sharedImage.hardwareBuffer

        checkNotNull(hardwareBuffer)
        assertThat(imageHardwareBuffer).isSameInstanceAs(hardwareBuffer)
    }

    @Test
    fun finalizerIsNotInvokedWhenSet() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        sharedImage1.setFinalizer(finalizer)
        verifyNoMoreInteractions(finalizer)
    }

    @Test
    fun finalizerIsNotInvokedWhenReferenceIsAcquired() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        sharedImage1.setFinalizer(finalizer)

        sharedImage1.acquire().close()
        sharedImage1.acquire().close()
        verifyNoMoreInteractions(finalizer)
    }

    @Test
    fun finalizerIsInvokedWhenClosed() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        val sharedImage2 = sharedImage1.acquire()
        val sharedImage3 = sharedImage2.acquire()
        sharedImage1.setFinalizer(finalizer)

        sharedImage1.close()
        verifyNoMoreInteractions(finalizer)

        sharedImage3.close()
        verifyNoMoreInteractions(finalizer)

        sharedImage2.close()
        verify(finalizer, times(1)).finalize(eq(outputImage))
    }

    @Test
    fun finalizerIsInvokedWhenReplaced() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        sharedImage1.setFinalizer(finalizer)
        sharedImage1.setFinalizer(ClosingFinalizer)
        verify(finalizer, times(1)).finalize(null)
    }

    @Test
    fun finalizerIsInvokedWhenReplacedInAltRef() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        sharedImage1.setFinalizer(finalizer)

        val sharedImage2 = sharedImage1.acquire()
        val sharedImage3 = sharedImage2.acquire()
        sharedImage3.setFinalizer(ClosingFinalizer)
        verify(finalizer, times(1)).finalize(null)
    }

    @Test
    fun finalizerIsInvokedWithNullWhenImageIsAlreadyClosed() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        sharedImage1.close()
        sharedImage1.setFinalizer(finalizer)
        verify(finalizer, times(1)).finalize(null)
    }

    companion object {
        private val IMAGE_HEIGHT: Int = 100
        private val IMAGE_WIDTH: Int = 200
        private val IMAGE_FORMAT: Int = 3
        private val IMAGE_TIMESTAMP: Long = 1234
    }
}
