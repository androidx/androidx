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

package androidx.glance.adaptive.core.templates

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ChartBarTest {

    @Test
    fun bar_isNotEmphasizedByDefault() {
        val partial = ChartBar(0.85f, "T")
        assertThat(partial.value).isEqualTo(0.85f)
        assertThat(partial.label).isEqualTo("T")
        assertThat(partial.isEmphasized).isFalse()

        val full = ChartBar(1.0f, "W")
        assertThat(full.isEmphasized).isFalse()
    }

    @Test
    fun bar_isEmphasizedOnlyWhenRequested() {
        val highlightedPartial = ChartBar(0.65f, "S", isEmphasized = true)
        assertThat(highlightedPartial.isEmphasized).isTrue()

        val highlightedFull = ChartBar(1.0f, "S", isEmphasized = true)
        assertThat(highlightedFull.isEmphasized).isTrue()
    }

    @Test
    fun bar_rejectsOutOfRangeOrNonFiniteValues() {
        val high = assertThrows(IllegalArgumentException::class.java) { ChartBar(1.2f) }
        assertThat(high).hasMessageThat().isEqualTo("Bar value must be in [0, 1], was 1.2.")
        assertThrows(IllegalArgumentException::class.java) { ChartBar(-0.1f) }
        assertThrows(IllegalArgumentException::class.java) { ChartBar(Float.NaN) }
    }

    @Test
    fun bar_rejectsBlankLabel() {
        val error =
            assertThrows(IllegalArgumentException::class.java) { ChartBar(0.5f, label = " ") }
        assertThat(error).hasMessageThat().isEqualTo("Bar label must not be blank.")
    }

    @Test
    fun equalsAndHashCode_compareAllProperties() {
        val a = ChartBar(0.5f, "M", isEmphasized = true)
        val b = ChartBar(0.5f, "M", isEmphasized = true)
        val c = ChartBar(0.5f, "M", isEmphasized = false)

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a).isNotEqualTo(c)
    }
}
