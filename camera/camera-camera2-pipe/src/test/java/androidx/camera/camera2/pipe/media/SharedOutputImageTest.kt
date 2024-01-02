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

import android.os.Build
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [OutputImage] and [SharedOutputImage] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SharedOutputImageTest {
    private val streamId = StreamId(42)
    private val outputId = OutputId(64)
    private val mockImage: ImageWrapper = mock { whenever(it.timestamp).thenReturn(1234) }
    private val outputImage = OutputImage.from(streamId, outputId, mockImage)
    private val finalizer: Finalizer<OutputImage> = mock()

    @Test
    fun sharedImagesIsNotClosedByDefault() {
        val sharedImage = SharedOutputImage.from(outputImage)
        verify(mockImage, never()).close()
        sharedImage.close()
        verify(mockImage, times(1)).close()
    }

    @Test
    fun closingSharedImageClosesWrappedImage() {
        val sharedImage = SharedOutputImage.from(outputImage)
        sharedImage.close()
        verify(mockImage, times(1)).close()
    }

    @Test
    fun closingMultipleTimesOnlyClosesImageOnce() {
        val sharedImage = SharedOutputImage.from(outputImage)
        sharedImage.close()
        sharedImage.close()
        sharedImage.close()
        verify(mockImage, times(1)).close()
    }

    @Test
    fun sharedImagesCanBeAcquired() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        val sharedImage2 = sharedImage1.acquire()
        val sharedImage3 = sharedImage2.acquire()
        sharedImage1.close()
        sharedImage2.close()
        verify(mockImage, never()).close()

        sharedImage3.close()
        verify(mockImage, times(1)).close()
    }

    @Test
    fun sharedImageCannotBeAcquiredAfterClose() {
        val sharedImage1 = SharedOutputImage.from(outputImage)
        sharedImage1.close()

        val sharedImage2 = sharedImage1.acquireOrNull()
        assertThat(sharedImage2).isNull()
        verify(mockImage, times(1)).close()
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
        verify(mockImage, never()).close()
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
}
