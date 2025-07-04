/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(JUnit4::class)
class CenteredHeroTest {

    private val Density = Density(1f)

    @Test
    fun enoughRoom_shouldFitOneLargeTwoSmall() {
        val keylineList =
            heroKeylineList(
                density = Density,
                carouselMainAxisSize = 100f + 40f + 40f,
                maxItemSize = null,
                itemSpacing = 0f,
                itemCount = 6,
                isCentered = true,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = 100f + 40f + 40f,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )

        assertThat(strategy.itemMainAxisSize).isEqualTo(100f)
    }

    @Test
    fun lessThan3Items_shouldChangeToLeftAlignedArrangement() {
        val keylineList =
            heroKeylineList(
                density = Density,
                carouselMainAxisSize = 100f + 40f + 40f,
                maxItemSize = null,
                itemSpacing = 0f,
                itemCount = 2,
                isCentered = true,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = 100f + 40f + 40f,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )

        assertThat(strategy.itemMainAxisSize).isGreaterThan(100f)
        assertThat(strategy.defaultKeylines.firstFocalIndex).isEqualTo(1)
    }

    @Test
    fun lessThanFullscreenThreshold_shouldGoFullscreen() {
        val keylineList =
            heroKeylineList(
                density = Density,
                carouselMainAxisSize = 40f + 40f + 40f,
                maxItemSize = null,
                itemSpacing = 0f,
                itemCount = 6,
                isCentered = true,
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = 40f + 40f + 40f,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f,
            )

        assertThat(strategy.itemMainAxisSize).isEqualTo(120f)
    }
}
