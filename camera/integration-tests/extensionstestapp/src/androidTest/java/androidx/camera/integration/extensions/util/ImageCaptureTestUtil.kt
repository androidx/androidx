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

package androidx.camera.integration.extensions.util

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue

/**
 * Validate the image can be correctly decoded from a jpeg format to a bitmap.
 */
fun assertImageIsValid(image: Image, width: Int, height: Int) {
    assertThat(image.width).isEqualTo(width)
    assertThat(image.height).isEqualTo(height)
    assertThat(image.format).isEqualTo(ImageFormat.JPEG)

    val data = imageData(image)
    assertTrue("Invalid image data", data.isNotEmpty())

    val bmpOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeByteArray(data, 0, data.size, bmpOptions)

    assertThat(width).isEqualTo(bmpOptions.outWidth)
    assertThat(height).isEqualTo(bmpOptions.outHeight)

    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
    assertNotNull("Decoding jpeg failed", bitmap)
}

private fun imageData(image: Image): ByteArray {
    val planes = image.planes
    assertTrue("Fail to get image planes", planes != null && planes.isNotEmpty())

    val buffer = planes[0].buffer
    assertNotNull("Fail to get jpeg ByteBuffer", buffer)

    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    buffer.rewind()
    return data
}
