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

package androidx.pdf.view

import androidx.pdf.viewer.fragment.util.getCenter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntRangeExtensionsTest {

    @Test
    fun getCenter_emptyRange() {
        val range = 0..0
        assertThat(range.getCenter()).isEqualTo(0)
    }

    @Test
    fun getCenter_oneElementRange() {
        val range = 0..1
        assertThat(range.getCenter()).isEqualTo(0)
    }

    @Test
    fun getCenter_oddRange() {
        val range = 0..2
        assertThat(range.getCenter()).isEqualTo(1)
    }

    @Test
    fun getCenter_evenRange() {
        val range = 0..3
        assertThat(range.getCenter()).isEqualTo(1)
    }
}
