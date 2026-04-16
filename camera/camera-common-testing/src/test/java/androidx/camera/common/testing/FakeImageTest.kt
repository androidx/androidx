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
import org.junit.Test

class FakeImageTest {

    @Test
    fun defaultDataSpace_isUnknown() {
        val fakeImage = FakeImage(width = 640, height = 480, format = 35, timestamp = 0L)
        assertThat(fakeImage.dataSpace).isEqualTo(android.hardware.DataSpace.DATASPACE_UNKNOWN)
    }

    @Test
    fun isClosed_initiallyFalse() {
        val fakeImage = FakeImage(width = 640, height = 480, format = 35, timestamp = 0L)
        assertThat(fakeImage.isClosed).isFalse()
        assertThat(fakeImage.closeCount).isEqualTo(0)
    }

    @Test
    fun close_incrementsCloseCountAndIsClosed() {
        val fakeImage = FakeImage(width = 640, height = 480, format = 35, timestamp = 0L)

        fakeImage.close()

        assertThat(fakeImage.isClosed).isTrue()
        assertThat(fakeImage.closeCount).isEqualTo(1)

        fakeImage.close()

        assertThat(fakeImage.isClosed).isTrue()
        assertThat(fakeImage.closeCount).isEqualTo(2)
    }
}
