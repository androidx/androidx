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

package androidx.camera.common.testing

import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Test

class FakeImagePlaneTest {

    @Test
    fun fakeImagePlane_lazyAllocation() {
        val plane = FakeImagePlane(rowStride = 100, rowCount = 50)
        assertThat(plane.buffer.capacity()).isEqualTo(5000)
    }

    @Test
    fun fakeImagePlane_unwrapAs_byteBuffer() {
        val plane = FakeImagePlane(rowStride = 100, rowCount = 50)
        assertThat(plane.unwrapAs(ByteBuffer::class.java)).isSameInstanceAs(plane.buffer)
        assertThat(plane.unwrapAs(FakeImagePlane::class.java)).isSameInstanceAs(plane)
    }

    @Test
    fun fakeImagePlane_wrapExistingBuffer() {
        val buf = ByteBuffer.allocateDirect(10)
        val plane = FakeImagePlane(rowStride = 10, buffer = buf)
        assertThat(plane.buffer).isSameInstanceAs(buf)
    }
}
