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
        assertThat(lerp(null, null, 0.0f)).isEqualTo(
            FontWeight.Normal
        )
    }

    @Test
    fun `lerp with one null parameter should use normal for null value`() {
        assertThat(
            lerp(
                FontWeight.W200,
                null,
                0.5f
            )
        ).isEqualTo(FontWeight.W300)
        assertThat(
            lerp(
                null,
                FontWeight.W200,
                0.5f
            )
        ).isEqualTo(FontWeight.W300)
    }

    @Test
    fun `lerp at start returns start value`() {
        assertThat(
            lerp(
                FontWeight.W200,
                FontWeight.W400,
                0.0f
            )
        ).isEqualTo(FontWeight.W200)
    }

    @Test
    fun `lerp at end returns end value`() {
        assertThat(
            lerp(
                FontWeight.W200,
                FontWeight.W400,
                1.0f
            )
        ).isEqualTo(FontWeight.W400)
    }

    @Test
    fun `lerp in the mid-time`() {
        assertThat(
            lerp(
                FontWeight.W200,
                FontWeight.W800,
                0.5f
            )
        ).isEqualTo(FontWeight.W500)
    }

    @Test
    fun `lerp in the mid-time with odd distance should be rounded to up`() {
        assertThat(
            lerp(
                FontWeight.W200,
                FontWeight.W900,
                0.5f
            )
        ).isEqualTo(FontWeight.W600)
    }

    @Test
    fun `toString return FontsWeight`() {
        assertThat(FontWeight.W100.toString()).isEqualTo("FontWeight.W100")
        assertThat(FontWeight.W900.toString()).isEqualTo("FontWeight.W900")
    }

    @Test
    fun `values return all weights`() {
        val expectedValues = listOf(
            FontWeight.W100,
            FontWeight.W200,
            FontWeight.W300,
            FontWeight.W400,
            FontWeight.W500,
            FontWeight.W600,
            FontWeight.W700,
            FontWeight.W800,
            FontWeight.W900
        )
        assertThat(FontWeight.values).isEqualTo(expectedValues)
    }

    @Test
    fun `weight returns collect values`() {
        val fontWeights = mapOf(
            FontWeight.W100 to 100,
            FontWeight.W200 to 200,
            FontWeight.W300 to 300,
            FontWeight.W400 to 400,
            FontWeight.W500 to 500,
            FontWeight.W600 to 600,
            FontWeight.W700 to 700,
            FontWeight.W800 to 800,
            FontWeight.W900 to 900
        )

        // TODO(b/130795950): IR compiler bug was here
        for (weightPair in fontWeights) {
            val (fontWeight, expectedWeight) = weightPair
            assertThat(fontWeight.weight).isEqualTo(expectedWeight)
        }
    }

    @Test
    fun `compareTo`() {
        assertThat(FontWeight.W400.compareTo(FontWeight.W400)).isEqualTo(0)
        assertThat(FontWeight.W400.compareTo(FontWeight.W300)).isEqualTo(1)
        assertThat(FontWeight.W400.compareTo(FontWeight.W500)).isEqualTo(-1)
    }
}