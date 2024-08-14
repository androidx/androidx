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
package androidx.glance

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CombinedGlanceModifierTest {
    @Test
    fun foldIn() {
        assertThat(
                testModifier.foldIn(listOf<Int>()) { lst, m ->
                    if (m is Element) lst + m.value else lst
                }
            )
            .isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun foldOut() {
        assertThat(
                testModifier.foldOut(listOf<Int>()) { m, lst ->
                    if (m is Element) lst + m.value else lst
                }
            )
            .isEqualTo(listOf(3, 2, 1))
    }

    @Test
    fun any() {
        assertThat(testModifier.any { it == Element(1) }).isTrue()
        assertThat(testModifier.any { it == Element(2) }).isTrue()
        assertThat(testModifier.any { it == Element(3) }).isTrue()
        assertThat(testModifier.any { it == Element(5) }).isFalse()
    }

    @Test
    fun all() {
        assertThat(testModifier.all { it is Element && it.value < 10 }).isTrue()
        assertThat(testModifier.all { it is Element && it.value > 2 }).isFalse()
    }

    @Test
    fun equalsTest() {
        assertThat(testModifier).isEqualTo(GlanceModifier.element(1).element(2).element(3))
        assertThat(testModifier).isNotEqualTo(GlanceModifier.element(1).element(2).element(4))
        assertThat(testModifier).isNotEqualTo(GlanceModifier.element(1).element(2))
        assertThat(testModifier).isNotEqualTo(GlanceModifier)
    }

    private companion object {
        val testModifier = GlanceModifier.element(1).element(2).element(3)
    }
}

private data class Element(val value: Int) : GlanceModifier.Element

private fun GlanceModifier.element(value: Int) = this.then(Element(value))
