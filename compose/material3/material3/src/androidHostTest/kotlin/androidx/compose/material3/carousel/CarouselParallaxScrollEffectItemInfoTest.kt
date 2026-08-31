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

package androidx.compose.material3.carousel

import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(JUnit4::class)
class CarouselParallaxScrollEffectItemInfoTest {

    @Test
    fun properties_areStoredCorrectly() {
        val info =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 10f,
                maskEnd = 80f,
                parallax = 15f,
                maskProgress = 0.4f,
            )

        assertThat(info.maskStart).isEqualTo(10f)
        assertThat(info.maskEnd).isEqualTo(80f)
        assertThat(info.parallax).isEqualTo(15f)
        assertThat(info.maskProgress).isEqualTo(0.4f)
    }

    @Test
    fun size_isCalculatedFromMaskStartAndMaskEnd() {
        val info =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 20f,
                maskEnd = 80f,
                parallax = 0f,
                maskProgress = 0f,
            )
        assertThat(info.size).isEqualTo(60f)

        // Ensure negative sizes clamp to 0
        val negativeSizeInfo =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 80f,
                maskEnd = 20f,
                parallax = 0f,
                maskProgress = 0f,
            )
        assertThat(negativeSizeInfo.size).isEqualTo(0f)
    }

    @Test
    fun equalityAndHashCode_areValueBased() {
        val info1 =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 10f,
                maskEnd = 80f,
                parallax = 15f,
                maskProgress = 0.4f,
            )
        val info2 =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 10f,
                maskEnd = 80f,
                parallax = 15f,
                maskProgress = 0.4f,
            )

        assertThat(info1).isEqualTo(info2)
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode())

        val differentMaskStart =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 20f,
                maskEnd = 80f,
                parallax = 15f,
                maskProgress = 0.4f,
            )
        assertThat(info1).isNotEqualTo(differentMaskStart)

        val differentMaskEnd =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 10f,
                maskEnd = 90f,
                parallax = 15f,
                maskProgress = 0.4f,
            )
        assertThat(info1).isNotEqualTo(differentMaskEnd)

        val differentParallax =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 10f,
                maskEnd = 80f,
                parallax = 25f,
                maskProgress = 0.4f,
            )
        assertThat(info1).isNotEqualTo(differentParallax)

        val differentMaskProgress =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 10f,
                maskEnd = 80f,
                parallax = 15f,
                maskProgress = 0.8f,
            )
        assertThat(info1).isNotEqualTo(differentMaskProgress)
    }

    @Test
    fun toString_containsPropertyValues() {
        val info =
            CarouselParallaxScrollEffectItemInfo(
                maskStart = 10f,
                maskEnd = 80f,
                parallax = 15f,
                maskProgress = 0.4f,
            )

        val str = info.toString()
        assertThat(str).contains("maskStart=10.0")
        assertThat(str).contains("maskEnd=80.0")
        assertThat(str).contains("parallax=15.0")
        assertThat(str).contains("maskProgress=0.4")
        assertThat(str).contains("size=70.0")
    }
}
