/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes

import android.graphics.ImageFormat
import android.graphics.Rect
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.Exif
import com.google.common.truth.Truth
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

private const val CAPTURE_TIMEOUT = 15_000.toLong() //  15 seconds

/** A fake implementation of the [ImageCapture.OnImageCapturedCallback] and used for test. */
public class FakeImageCaptureCallback(captureCount: Int = 1) :
    ImageCapture.OnImageCapturedCallback() {
    /** Data class of various image properties which are tested. */
    public data class ImageProperties(
        val size: Size? = null,
        val format: Int = -1,
        val rotationDegrees: Int = -1,
        val cropRect: Rect? = null,
        val exif: Exif? = null,
    )

    private val latch = CountdownDeferred(captureCount)
    public val results: MutableList<ImageProperties> = mutableListOf()
    public val errors: MutableList<ImageCaptureException> = mutableListOf()

    override fun onCaptureSuccess(image: ImageProxy) {
        results.add(
            ImageProperties(
                size = Size(image.width, image.height),
                format = image.format,
                rotationDegrees = image.imageInfo.rotationDegrees,
                cropRect = image.cropRect,
                exif = getExif(image),
            )
        )
        image.close()
        latch.countDown()
    }

    override fun onError(exception: ImageCaptureException) {
        errors.add(exception)
        latch.countDown()
    }

    private fun getExif(image: ImageProxy): Exif? {
        if (image.format == ImageFormat.JPEG || image.format == ImageFormat.JPEG_R) {
            val planes = image.planes
            val buffer = planes[0].buffer
            val data = ByteArray(buffer.capacity())
            buffer[data]
            return Exif.createFromInputStream(ByteArrayInputStream(data))
        }
        return null
    }

    public suspend fun awaitCaptures(timeout: Long = CAPTURE_TIMEOUT) {
        Truth.assertThat(withTimeoutOrNull(timeout) { latch.await() }).isNotNull()
    }

    public suspend fun awaitCapturesAndAssert(
        timeout: Long = CAPTURE_TIMEOUT,
        capturedImagesCount: Int = 0,
        errorsCount: Int = 0
    ) {
        Truth.assertThat(withTimeoutOrNull(timeout) { latch.await() }).isNotNull()
        Truth.assertThat(results.size).isEqualTo(capturedImagesCount)
        Truth.assertThat(errors.size).isEqualTo(errorsCount)
    }

    private class CountdownDeferred(val count: Int) {

        private val deferredItems =
            mutableListOf<CompletableDeferred<Unit>>().apply {
                repeat(count) { add(CompletableDeferred()) }
            }
        private var index = 0

        fun countDown() {
            if (index < count) {
                deferredItems[index++].complete(Unit)
            } else {
                throw IllegalStateException("Countdown already finished")
            }
        }

        suspend fun await() {
            deferredItems.forEach { it.await() }
        }
    }
}
