/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.unit

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DpSizeTest {

    @Test
    fun identity() {
        assertThat(DpSize(1.dp, 2.dp)).isEqualTo(DpSize(1.dp, 2.dp))
        assertThat(DpSize(1.dp, 2.dp)).isNotEqualTo(DpSize(2.dp, 1.dp))
    }

    @Test
    fun copy() {
        assertThat(DpSize.Zero.copy(height = 2.dp)).isEqualTo(DpSize(0.dp, 2.dp))
        assertThat(DpSize.Zero.copy(width = 2.dp)).isEqualTo(DpSize(2.dp, 0.dp))
    }

    @Test
    fun plus() {
        assertThat(DpSize(1.dp, 2.dp) + DpSize(3.dp, 4.dp)).isEqualTo(DpSize(4.dp, 6.dp))
    }

    @Test
    fun minus() {
        assertThat(DpSize(1.dp, 2.dp) - DpSize(3.dp, 4.dp)).isEqualTo(DpSize(-2.dp, -2.dp))
    }

    @Test
    fun components() {
        val (width, height) = DpSize(1.dp, 2.dp)
        assertThat(width).isEqualTo(1.dp)
        assertThat(height).isEqualTo(2.dp)
    }

    @Test
    fun times() {
        assertThat(DpSize(1.dp, 2.dp) * 2).isEqualTo(DpSize(2.dp, 4.dp))
        assertThat(DpSize(1.dp, 2.dp) * 2.5f).isEqualTo(DpSize(2.5.dp, 5.dp))
        assertThat(2 * DpSize(1.dp, 2.dp)).isEqualTo(DpSize(2.dp, 4.dp))
        assertThat(2.5f * DpSize(1.dp, 2.dp)).isEqualTo(DpSize(2.5.dp, 5.dp))
    }

    @Test
    fun div() {
        assertThat(DpSize(1.dp, 2.dp) / 2).isEqualTo(DpSize(0.5.dp, 1.dp))
        assertThat(DpSize(2.5.dp, 5.dp) / 2.5f).isEqualTo(DpSize(1.dp, 2.dp))
    }
}