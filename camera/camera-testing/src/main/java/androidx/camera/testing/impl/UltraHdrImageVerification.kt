/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Assume

public object UltraHdrImageVerification {
    /**
     * Asserts that the image at the given file path can be decoded and has a gain map.
     *
     * This function is intended for use in tests. It will:
     * 1. Skip execution on emulators, as gain map support or behavior might differ.
     * 2. Attempt to decode the image file from `imageFilePath`.
     * 3. Assert that the decoding was successful (bitmap is not null).
     * 4. Assert that the decoded bitmap has a gain-map.
     * 5. Ensure the bitmap is recycled, regardless of assertion outcomes.
     *
     * @param imageFilePath The absolute file path for an image file.
     * @throws AssertionError if any of the assertions fail (e.g., image cannot be decoded, or it
     *   does not have a gain map).
     * @throws org.junit.AssumptionViolatedException if run on an emulator.
     */
    @RequiresApi(34) // Bitmap.hasGainmap() requires API 34
    @JvmStatic
    public fun assertImageFileIsUltraHdr(imageFilePath: String) {
        // Assume this test should only run on physical devices,
        // as emulator support for gain maps is inconsistent.
        Assume.assumeFalse(
            "Skipping Ultra HDR (gain map) assertion on emulator.",
            AndroidUtil.isEmulator(),
        )

        val bitmap = BitmapFactory.decodeFile(imageFilePath)

        Truth.assertWithMessage(
                "Failed to decode bitmap from path: $imageFilePath. " +
                    "Ensure the file exists, is a valid image, and the app has read permissions."
            )
            .that(bitmap)
            .isNotNull()

        bitmap!!.use { it.assertGainMap() }
    }

    /**
     * Asserts that the [ImageProxy] contains a JPEG/R image (Ultra HDR) and has a gain map.
     *
     * This function is intended for use in tests. It will:
     * 1. Assert that the [ImageProxy.getFormat] is [ImageFormat.JPEG_R].
     * 2. Attempt to convert the [ImageProxy] to a [Bitmap] using the `toBitmap()` extension.
     * 3. Assert that the conversion was successful (bitmap is not null).
     * 4. Assert that the decoded bitmap has a gain map.
     * 5. Ensure the created bitmap is recycled.
     *
     * @receiver The [ImageProxy] to verify.
     * @throws AssertionError if any of the assertions fail.
     */
    @JvmStatic
    @RequiresApi(34) // ImageFormat.JPEG_R and Bitmap.hasGainmap() require API 34
    public fun ImageProxy.assertJpegUltraHdr() {
        assertThat(format).isEqualTo(ImageFormat.JPEG_R)

        // Assuming toBitmap() is an available extension function that converts ImageProxy to Bitmap
        // and creates a new Bitmap instance that needs to be managed.
        val bitmap: Bitmap? = toBitmap()

        Truth.assertWithMessage(
                "Failed to convert ImageProxy to Bitmap. Ensure ImageProxy is valid, not closed" +
                    ", and toBitmap() is implemented correctly."
            )
            .that(bitmap)
            .isNotNull()

        bitmap!!.use { it.assertGainMap() }
    }

    @RequiresApi(34) // Bitmap.hasGainmap() requires API 34
    private fun Bitmap.assertGainMap() {
        Truth.assertWithMessage(
                "Bitmap was expected to have a gain map, but it does not. " +
                    "Ensure the image was captured or processed as Ultra HDR."
            )
            .that(hasGainmap())
            .isTrue()
    }

    /**
     * A utility extension function to ensure a [Bitmap] is recycled after use. This is similar to
     * [java.io.Closeable.use] but for [Bitmap] objects.
     *
     * @param block The block of code to execute with the bitmap.
     * @return The result of the [block].
     */
    internal inline fun <R> Bitmap.use(block: (Bitmap) -> R): R {
        try {
            return block(this)
        } finally {
            // recycle() is a no-op if the bitmap is already recycled.
            if (!isRecycled) {
                recycle()
            }
        }
    }
}
