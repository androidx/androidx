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

import androidx.annotation.Sampled
import androidx.camera.common.testing.FakeImagePlane
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Sampled
fun fakeImagePlaneSample() {
    // 1. Standalone allocation by dimensions (allocates rowStride * rowCount bytes on access):
    val yPlane = FakeImagePlane(rowStride = 640, rowCount = 480)
    val yRowStride: Int = yPlane.rowStride
    val yCapacity: Int = yPlane.buffer.capacity()

    // 2. Custom pixel stride for interleaved UV planes:
    val uvPlane = FakeImagePlane(rowStride = 640, rowCount = 240, pixelStride = 2)
    val uvPixelStride: Int = uvPlane.pixelStride
    val uvBuffer: ByteBuffer = uvPlane.buffer

    // 3. Backing by an existing native-order ByteBuffer slice:
    val parentBuffer = ByteBuffer.allocateDirect(640 * 480).order(ByteOrder.nativeOrder())
    val slice = parentBuffer.slice().order(ByteOrder.nativeOrder())
    val slicedPlane =
        FakeImagePlane(rowStride = 640, rowCount = 480, pixelStride = 1, buffer = slice)
    val slicedBuffer: ByteBuffer = slicedPlane.buffer
}
