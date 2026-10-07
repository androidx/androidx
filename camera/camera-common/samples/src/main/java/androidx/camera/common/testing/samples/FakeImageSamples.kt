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

package androidx.camera.common.testing.samples

import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.annotation.Sampled
import androidx.camera.common.ImagePlane
import androidx.camera.common.ImageWrapper
import androidx.camera.common.testing.FakeImage
import androidx.camera.common.testing.FakeImagePlane
import java.nio.ByteBuffer

@Sampled
fun fakeImageSample() {
    // Create a FakeImage for unit testing without needing camera hardware or native Image buffers.
    val fakeImage =
        FakeImage(
            width = 1920,
            height = 1080,
            format = ImageFormat.YUV_420_888,
            timestamp = 1_000_000_000L,
            cropRect = Rect(0, 0, 1920, 1080),
        )

    // Access image properties, planes, and pixel buffers in test assertions:
    val imageWrapper: ImageWrapper = fakeImage
    val planes: List<ImagePlane> = imageWrapper.imagePlanes
    val yBuffer: ByteBuffer = planes[0].buffer
    val yRowStride: Int = planes[0].rowStride

    // Close the fake image when finished and verify lifecycle state:
    fakeImage.close()
    val isClosed: Boolean = fakeImage.isClosed
    val closeCount: Int = fakeImage.closeCount
}

@Sampled
fun fakeImageCustomPlanesSample() {
    // Configure custom planes when testing padded row strides or formats without automatic plane
    // generation (such as RAW_SENSOR).
    val width = 640
    val height = 480
    val paddedRowStride = 672
    val rawPlane =
        FakeImagePlane(
            rowStride = paddedRowStride * 2,
            rowCount = height,
            pixelStride = 2,
        )

    val fakeImage =
        FakeImage(
            width = width,
            height = height,
            format = ImageFormat.RAW_SENSOR,
            timestamp = 1_000_000_000L,
            imagePlanes = listOf(rawPlane),
        )

    val plane: ImagePlane = fakeImage.imagePlanes.single()
    val buffer: ByteBuffer = plane.buffer
    fakeImage.close()
}
