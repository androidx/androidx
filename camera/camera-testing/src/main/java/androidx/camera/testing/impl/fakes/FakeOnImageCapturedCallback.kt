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
import androidx.camera.core.Logger
import androidx.camera.core.impl.utils.Exif
import com.google.common.truth.Truth
import java.io.ByteArrayInputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A fake implementation of the [ImageCapture.OnImageCapturedCallback] that is used for test.
 *
 * @param captureCount Number of captures to wait for.
 * @property closeImageOnSuccess Whether to close images immediately on [onCaptureSuccess]
 *   callbacks. This is true by default. If set to false, it is the user's responsibility to close
 *   the images.
 */
public class FakeOnImageCapturedCallback(
    captureCount: Int = 1,
    private val closeImageOnSuccess: Boolean = true
) : ImageCapture.OnImageCapturedCallback() {
    public data class CapturedImage(val image: ImageProxy, val properties: ImageProperties)

    /** Data class of various image properties which are tested. */
    public data class ImageProperties(
        val size: Size? = null,
        val format: Int = -1,
        val rotationDegrees: Int = -1,
        val cropRect: Rect? = null,
        val exif: Exif? = null,
    )

    private val latch = CountdownDeferred(captureCount)

    /**
     * List of [CapturedImage] obtained in [onCaptureSuccess] callback.
     *
     * If [closeImageOnSuccess] is true, the [CapturedImage.image] will be closed as soon as
     * `onCaptureSuccess` is invoked. Otherwise, it will be the user's responsibility to close the
     * images.
     */
    public val results: MutableList<CapturedImage> = mutableListOf()
    public val errors: MutableList<ImageCaptureException> = mutableListOf()

    override fun onCaptureSuccess(image: ImageProxy) {
        Logger.d(TAG, "onCaptureSuccess: image = $image")
        results.add(
            CapturedImage(
                image = image,
                properties =
                    ImageProperties(
                        size = Size(image.width, image.height),
                        format = image.format,
                        rotationDegrees = image.imageInfo.rotationDegrees,
                        cropRect = image.cropRect,
                        exif = getExif(image),
                    )
            )
        )
        if (closeImageOnSuccess) {
            image.close()
        }
        latch.countDown()
    }

    override fun onError(exception: ImageCaptureException) {
        Logger.d(TAG, "onError", exception)
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

    public suspend fun awaitCaptures(timeout: Duration = CAPTURE_TIMEOUT) {
        Truth.assertThat(withTimeoutOrNull(timeout) { latch.await() }).isNotNull()
    }

    /** Asserts that capture hasn't been completed within the provided `duration`. */
    public suspend fun assertNoCapture(timeout: Duration = CAPTURE_TIMEOUT) {
        Truth.assertThat(withTimeoutOrNull(timeout) { latch.await() }).isNull()
    }

    public suspend fun awaitCapturesAndAssert(
        timeout: Duration = CAPTURE_TIMEOUT,
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

    public companion object {
        private const val TAG = "FakeOnImageCaptureCallback"
        private val CAPTURE_TIMEOUT = 15.seconds
    }
}
