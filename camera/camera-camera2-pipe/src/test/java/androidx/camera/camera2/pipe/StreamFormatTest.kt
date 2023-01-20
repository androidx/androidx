/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class StreamFormatTest {
    @Test
    fun streamFormatsAreEqual() {
        assertThat(StreamFormat(0x23)).isEqualTo(StreamFormat.YUV_420_888)
    }

    @Test
    fun streamFormatsHaveNames() {
        assertThat(StreamFormat(0x23).name).isEqualTo("YUV_420_888")
        assertThat(StreamFormat.RAW10.name).isEqualTo("RAW10")
    }

    @Test
    fun streamFormatsHaveToString() {
        assertThat(StreamFormat(0x23).toString()).contains("YUV_420_888")
        assertThat(StreamFormat(0x23).toString()).contains("StreamFormat")
        assertThat(StreamFormat.RAW10.toString()).contains("RAW10")
        assertThat(StreamFormat.RAW10.toString()).contains("StreamFormat")
    }

    @Test
    fun streamFormatsHaveBitsPerPixel() {
        assertThat(StreamFormat(0x23).bitsPerPixel).isEqualTo(12)
        assertThat(StreamFormat.RAW10.bitsPerPixel).isEqualTo(10)
    }
}
