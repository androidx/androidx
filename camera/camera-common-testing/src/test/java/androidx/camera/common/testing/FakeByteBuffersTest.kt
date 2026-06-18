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

import androidx.camera.common.testing.FakeByteBuffers.sliceNative
import androidx.camera.common.testing.FakeByteBuffers.toNativeByteOrder
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class FakeByteBuffersTest {

    @Test
    fun allocateNative_createsDirectByteBufferWithNativeOrder() {
        val buffer = FakeByteBuffers.allocateNative(100)
        assertThat(buffer.isDirect).isTrue()
        assertThat(buffer.capacity()).isEqualTo(100)
        assertThat(buffer.order()).isEqualTo(ByteOrder.nativeOrder())
    }

    @Test
    fun toNativeByteOrder_setsNativeByteOrder() {
        val buffer = ByteBuffer.allocate(50).order(ByteOrder.BIG_ENDIAN)
        assertThat(buffer.order()).isEqualTo(ByteOrder.BIG_ENDIAN)

        buffer.toNativeByteOrder()
        assertThat(buffer.order()).isEqualTo(ByteOrder.nativeOrder())
    }

    @Test
    fun sliceNative_returnsCorrectSliceWithNativeOrder() {
        val parent = FakeByteBuffers.allocateNative(100)
        // Put some dummy data
        for (i in 0 until 100) {
            parent.put(i.toByte())
        }

        val slice = parent.sliceNative(10, 40)

        // Verify capacity is 30 (40 - 10)
        assertThat(slice.capacity()).isEqualTo(30)
        assertThat(slice.order()).isEqualTo(ByteOrder.nativeOrder())

        // Verify content matches expected subsequence
        for (i in 0 until 30) {
            assertThat(slice.get(i)).isEqualTo((i + 10).toByte())
        }

        // Verify parent position/limit were preserved
        assertThat(parent.position()).isEqualTo(100) // Put loop advances position to capacity
        assertThat(parent.limit()).isEqualTo(100)
    }
}
