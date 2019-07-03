/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.text.font

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontWeightTest {

    @Test
    fun `lerp with null parameters`() {
        assertThat(FontWeight.lerp(null, null, 0.0f)).isEqualTo(
            FontWeight.normal
        )
    }

    @Test
    fun `lerp with one null parameter should use normal for null value`() {
        assertThat(
            FontWeight.lerp(
                FontWeight.w200,
                null,
                0.5f
            )
        ).isEqualTo(FontWeight.w300)
        assertThat(
            FontWeight.lerp(
                null,
                FontWeight.w200,
                0.5f
            )
        ).isEqualTo(FontWeight.w300)
    }

    @Test
    fun `lerp at start returns start value`() {
        assertThat(
            FontWeight.lerp(
                FontWeight.w200,
                FontWeight.w400,
                0.0f
            )
        ).isEqualTo(FontWeight.w200)
    }

    @Test
    fun `lerp at end returns end value`() {
        assertThat(
            FontWeight.lerp(
                FontWeight.w200,
                FontWeight.w400,
                1.0f
            )
        ).isEqualTo(FontWeight.w400)
    }

    @Test
    fun `lerp in the mid-time`() {
        assertThat(
            FontWeight.lerp(
                FontWeight.w200,
                FontWeight.w800,
                0.5f
            )
        ).isEqualTo(FontWeight.w500)
    }

    @Test
    fun `lerp in the mid-time with odd distance should be rounded to up`() {
        assertThat(
            FontWeight.lerp(
                FontWeight.w200,
                FontWeight.w900,
                0.5f
            )
        ).isEqualTo(FontWeight.w600)
    }

    @Test
    fun `toString return FontsWeight`() {
        assertThat(FontWeight.w100.toString()).isEqualTo("FontWeight.w100")
        assertThat(FontWeight.w900.toString()).isEqualTo("FontWeight.w900")
    }

    @Test
    fun `values return all weights`() {
        val expectedValues = listOf(
            FontWeight.w100,
            FontWeight.w200,
            FontWeight.w300,
            FontWeight.w400,
            FontWeight.w500,
            FontWeight.w600,
            FontWeight.w700,
            FontWeight.w800,
            FontWeight.w900
        )
        assertThat(FontWeight.values).isEqualTo(expectedValues)
    }

    @Test
    fun `weight returns collect values`() {
        val fontWeights = mapOf(
            FontWeight.w100 to 100,
            FontWeight.w200 to 200,
            FontWeight.w300 to 300,
            FontWeight.w400 to 400,
            FontWeight.w500 to 500,
            FontWeight.w600 to 600,
            FontWeight.w700 to 700,
            FontWeight.w800 to 800,
            FontWeight.w900 to 900
        )

        // TODO(b/130795950): IR compiler bug was here
        for (weightPair in fontWeights) {
            val (fontWeight, expectedWeight) = weightPair
            assertThat(fontWeight.weight).isEqualTo(expectedWeight)
        }
    }

    @Test
    fun `compareTo`() {
        assertThat(FontWeight.w400.compareTo(FontWeight.w400)).isEqualTo(0)
        assertThat(FontWeight.w400.compareTo(FontWeight.w300)).isEqualTo(1)
        assertThat(FontWeight.w400.compareTo(FontWeight.w500)).isEqualTo(-1)
    }
}