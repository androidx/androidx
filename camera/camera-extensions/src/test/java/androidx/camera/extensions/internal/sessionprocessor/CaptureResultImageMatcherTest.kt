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

package androidx.camera.extensions.internal.sessionprocessor

import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CaptureResultImageMatcherTest {
    private val matcher = CaptureResultImageMatcher()

    private val imagesRetrieved = mutableListOf<ImageReference>()
    private val stageIdsRetrieved = mutableListOf<Int>()

    @Before
    fun setUp() {
        matcher.setImageReferenceListener { imageRef, _, stageId ->
            imagesRetrieved.add(imageRef)
            stageIdsRetrieved.add(stageId)
        }
    }

    private fun createCaptureResult(timestamp: Long): TotalCaptureResult {
        val captureResult = Mockito.mock(TotalCaptureResult::class.java)
        `when`(captureResult.get(CaptureResult.SENSOR_TIMESTAMP)).thenReturn(timestamp)
        return captureResult
    }

    private fun createImageRef(timestamp: Long): FakeImageReference {
        val image = Mockito.mock(Image::class.java)
        `when`(image.timestamp).thenReturn(timestamp)
        return FakeImageReference(image)
    }

    @Test
    fun canMatchImage_captureResultFirst() {
        val captureResult0 = createCaptureResult(timestamp = 1000L)
        matcher.captureResultIncoming(captureResult0, 0)

        val imageRef0 = createImageRef(timestamp = 1000L)
        matcher.imageIncoming(imageRef0)

        assertThat(imagesRetrieved.size).isEqualTo(1)
        assertThat(imageRef0).isSameInstanceAs(imagesRetrieved[0])
        assertThat(imageRef0.refCount).isEqualTo(1)
    }

    @Test
    fun canMatchImage_imageFirst() {
        val imageRef0 = createImageRef(timestamp = 1000L)
        matcher.imageIncoming(imageRef0)

        val captureResult0 = createCaptureResult(timestamp = 1000L)
        matcher.captureResultIncoming(captureResult0, 0)

        assertThat(imagesRetrieved.size).isEqualTo(1)
        assertThat(imageRef0).isSameInstanceAs(imagesRetrieved[0])
        assertThat(imageRef0.refCount).isEqualTo(1)
    }

    @Test
    fun matchImageFailed() {
        val imageRef0 = createImageRef(timestamp = 1000L)
        matcher.imageIncoming(imageRef0)

        val captureResult0 = createCaptureResult(timestamp = 2000L)
        matcher.captureResultIncoming(captureResult0, 0)
        assertThat(imagesRetrieved.size).isEqualTo(0)
    }

    @Test
    fun canMatchMultipleImages() {
        val imageRef0 = createImageRef(timestamp = 1000L)
        val imageRef1 = createImageRef(timestamp = 2000L)
        val imageRef2 = createImageRef(timestamp = 3000L)
        val captureResult0 = createCaptureResult(timestamp = 1000L)
        val captureResult1 = createCaptureResult(timestamp = 2000L)
        val captureResult2 = createCaptureResult(timestamp = 3000L)

        matcher.imageIncoming(imageRef0)
        matcher.captureResultIncoming(captureResult0, 0)
        matcher.imageIncoming(imageRef1)
        matcher.imageIncoming(imageRef2)
        matcher.captureResultIncoming(captureResult1, 1)
        matcher.captureResultIncoming(captureResult2, 2)

        assertThat(imagesRetrieved).containsExactly(imageRef0, imageRef1, imageRef2)
        assertThat(stageIdsRetrieved).containsExactly(0, 1, 2)
    }

    @Test
    fun canMatchImage_duplicateTimestamp() {
        val imageRef0 = createImageRef(timestamp = 1000L)
        val imageRef1 = createImageRef(timestamp = 1000L)
        val imageRef2 = createImageRef(timestamp = 2000L)
        val imageRef3 = createImageRef(timestamp = 2000L)
        val imageRef4 = createImageRef(timestamp = 3000L)

        val captureResult0 = createCaptureResult(timestamp = 1000L)
        val captureResult1 = createCaptureResult(timestamp = 1000L)
        val captureResult2 = createCaptureResult(timestamp = 2000L)
        val captureResult3 = createCaptureResult(timestamp = 2000L)
        val captureResult4 = createCaptureResult(timestamp = 3000L)

        matcher.imageIncoming(imageRef0)
        matcher.captureResultIncoming(captureResult0, 0)
        matcher.imageIncoming(imageRef1)
        matcher.imageIncoming(imageRef2)
        matcher.captureResultIncoming(captureResult1, 1)
        matcher.captureResultIncoming(captureResult2, 2)
        matcher.captureResultIncoming(captureResult3, 3)
        matcher.imageIncoming(imageRef3)
        matcher.captureResultIncoming(captureResult4, 4)
        matcher.imageIncoming(imageRef4)

        assertThat(imagesRetrieved)
            .containsExactly(imageRef0, imageRef1, imageRef2, imageRef3, imageRef4)
        assertThat(stageIdsRetrieved).containsExactly(0, 1, 2, 3, 4)
    }

    @Test
    fun canClear() {
        val imageRef0 = createImageRef(timestamp = 1000L)
        val imageRef1 = createImageRef(timestamp = 1000L)
        val imageRef2 = createImageRef(timestamp = 3000L)
        val captureResult0 = createCaptureResult(timestamp = 1000L)
        matcher.imageIncoming(imageRef0)
        matcher.captureResultIncoming(captureResult0, 0)
        matcher.imageIncoming(imageRef1)
        matcher.imageIncoming(imageRef2)

        matcher.clear()

        assertThat(imagesRetrieved).containsExactly(imageRef0)
        assertThat(stageIdsRetrieved).containsExactly(0)
        assertThat(imageRef0.refCount).isEqualTo(1)
        // unmatched images are decremented.
        assertThat(imageRef1.refCount).isEqualTo(0)
        assertThat(imageRef2.refCount).isEqualTo(0)

        // After clear(), the pending ImageReference are cleared so it won't match.
        val captureResult1 = createCaptureResult(timestamp = 1000L)
        val captureResult2 = createCaptureResult(timestamp = 3000L)
        matcher.captureResultIncoming(captureResult1)
        matcher.captureResultIncoming(captureResult2)

        assertThat(imagesRetrieved).containsNoneOf(imageRef1, imageRef2)
    }

    @Test
    fun canClearImageReferenceListener() {
        matcher.clearImageReferenceListener()

        val imageRef0 = createImageRef(timestamp = 1000L)
        val captureResult0 = createCaptureResult(timestamp = 1000L)

        matcher.imageIncoming(imageRef0)
        matcher.captureResultIncoming(captureResult0, 0)

        assertThat(imagesRetrieved).isEmpty()
    }

    @Test
    fun canRemoveStaleImages() {
        val imageRef0 = createImageRef(timestamp = 1000L)
        val imageRef1 = createImageRef(timestamp = 1000L)
        val imageRef2 = createImageRef(timestamp = 2000L)
        val captureResult3 = createCaptureResult(timestamp = 3000L)
        matcher.imageIncoming(imageRef0)
        matcher.imageIncoming(imageRef1)
        matcher.imageIncoming(imageRef2)
        matcher.captureResultIncoming(captureResult3, 3)

        // Images that are older than captureResult3 are decremented.
        assertThat(imageRef0.refCount).isEqualTo(0)
        assertThat(imageRef1.refCount).isEqualTo(0)
        assertThat(imageRef2.refCount).isEqualTo(0)

        // The pending images are clear so they shouldn't match anymore.
        val captureResult0 = createCaptureResult(timestamp = 1000L)
        val captureResult1 = createCaptureResult(timestamp = 1000L)
        val captureResult2 = createCaptureResult(timestamp = 2000L)
        matcher.captureResultIncoming(captureResult0, 0)
        matcher.captureResultIncoming(captureResult1, 1)
        matcher.captureResultIncoming(captureResult2, 2)
        assertThat(imagesRetrieved).isEmpty()
    }

    @Test
    fun canRemoveStaleCaptureResults() {
        val captureResult0 = createCaptureResult(timestamp = 1000L)
        val captureResult1 = createCaptureResult(timestamp = 1000L)
        val captureResult2 = createCaptureResult(timestamp = 2000L)
        val imageRef3 = createImageRef(timestamp = 3000L)

        matcher.captureResultIncoming(captureResult0, 0)
        matcher.captureResultIncoming(captureResult1, 1)
        matcher.captureResultIncoming(captureResult2, 2)
        matcher.imageIncoming(imageRef3)

        // Capture results older than the image are clear so they won't match anymore.
        val imageRef0 = createImageRef(timestamp = 1000L)
        val imageRef1 = createImageRef(timestamp = 1000L)
        val imageRef2 = createImageRef(timestamp = 2000L)
        matcher.imageIncoming(imageRef0)
        matcher.imageIncoming(imageRef1)
        matcher.imageIncoming(imageRef2)

        // The pending images are clear so they shouldn't match anymore.
        assertThat(imagesRetrieved).isEmpty()
    }

    class FakeImageReference(val image: Image) : ImageReference {
        var refCount = 1

        override fun increment(): Boolean {
            if (refCount <= 0) {
                return false
            }
            refCount++
            return true
        }

        override fun decrement(): Boolean {
            if (refCount <= 0) {
                return false
            }
            refCount--
            return true
        }

        override fun get(): Image = image
    }
}
