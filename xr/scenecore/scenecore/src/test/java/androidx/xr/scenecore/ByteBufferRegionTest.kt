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

package androidx.xr.scenecore

import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ByteBufferRegionTest {

    @Test
    fun create_withValidArguments_succeeds() {
        val buffer = ByteBuffer.allocateDirect(10)
        val region = ByteBufferRegion(buffer, offset = 2, size = 5)
        assertThat(region.buffer).isSameInstanceAs(buffer)
        assertThat(region.offset).isEqualTo(2)
        assertThat(region.size).isEqualTo(5)
    }

    @Test
    fun create_withNegativeOffset_throwsIllegalArgumentException() {
        val buffer = ByteBuffer.allocateDirect(10)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                ByteBufferRegion(buffer, offset = -1, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("offset must not be negative")
    }

    @Test
    fun create_withNegativeSize_throwsIllegalArgumentException() {
        val buffer = ByteBuffer.allocateDirect(10)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                ByteBufferRegion(buffer, offset = 2, size = -5)
            }
        assertThat(exception).hasMessageThat().contains("size must not be negative")
    }

    @Test
    fun create_sizePlusOffsetExceedsCapacity_throwsIllegalArgumentException() {
        val buffer = ByteBuffer.allocateDirect(10)
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                ByteBufferRegion(buffer, offset = 6, size = 5)
            }
        assertThat(exception).hasMessageThat().contains("size + offset must not exceed capacity")
    }
}
