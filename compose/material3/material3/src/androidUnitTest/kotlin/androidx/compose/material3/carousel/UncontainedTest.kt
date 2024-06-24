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

package androidx.compose.material3.carousel

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(JUnit4::class)
class UncontainedTest {

    private val Density = Density(1f)

    @Test
    fun testLargeItem_withFullCarouselWidth() {
        val itemSize = 500f
        val carouselSize = 500f
        val keylineList =
            uncontainedKeylineList(
                density = Density,
                carouselMainAxisSize = carouselSize,
                itemSize = itemSize,
                itemSpacing = 0f
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = carouselSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val keylines = strategy.defaultKeylines
        val anchorSize = with(Density) { CarouselDefaults.AnchorSize.toPx() }

        // A fullscreen layout should be [xSmall-large-xSmall] where the xSmall items are
        // outside the bounds of the carousel container and the large item takes up the
        // containers full width.
        assertThat(keylines.size).isEqualTo(3)
        assertThat(keylines[0].offset).isEqualTo(-anchorSize / 2f)
        assertThat(keylines[1].size).isEqualTo(carouselSize)
        assertThat(keylines[2].offset).isEqualTo(carouselSize + anchorSize / 2f)
    }

    @Test
    fun testLargeItem_largerThanFullCarouselWidth() {
        val carouselSize = 400f
        val itemSize = 500f
        val keylineList =
            uncontainedKeylineList(
                density = Density,
                carouselMainAxisSize = carouselSize,
                itemSize = itemSize,
                itemSpacing = 0f
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = carouselSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val keylines = strategy.defaultKeylines
        val anchorSize = with(Density) { CarouselDefaults.AnchorSize.toPx() }

        // The layout should be [xSmall-large-xSmall] where the xSmall items are
        // outside the bounds of the carousel container and the large item takes up the
        // containers full width.
        assertThat(keylines.size).isEqualTo(3)
        assertThat(keylines[0].offset).isEqualTo(-anchorSize / 2f)
        assertThat(keylines[1].size).isEqualTo(carouselSize)
        assertThat(keylines[2].offset).isEqualTo(carouselSize + anchorSize / 2f)
    }

    @Test
    fun testRemainingSpaceWithItemSize_fitsItemWithThirdCutoff() {
        val carouselSize = 400f
        // With size 125px, 3 large items can fit with in 400px, with 25px left. 25px * 3 = 75px,
        // which will be the size of the medium item since it can be a third cut off and it is less
        // than the threshold percentage * large item size.
        val itemSize = 125f
        val keylineList =
            uncontainedKeylineList(
                density = Density,
                carouselMainAxisSize = carouselSize,
                itemSize = itemSize,
                itemSpacing = 0f
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = carouselSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val keylines = strategy.defaultKeylines
        val rightAnchorSize = with(Density) { CarouselDefaults.AnchorSize.toPx() }

        // The layout should be [xSmall-large-large-large-medium-xSmall] where medium is a size
        // such that a third of it is cut off.
        assertThat(keylines.size).isEqualTo(6)
        assertThat(keylines[1].size).isEqualTo(itemSize)
        assertThat(keylines[2].size).isEqualTo(itemSize)
        assertThat(keylines[3].size).isEqualTo(itemSize)
        // The cutoff size should be a size that has a third of itself cut off with the given
        // remaining space, which is 25f as explained above.
        assertThat(keylines[4].size).isEqualTo(25f * 1.5f)
        assertThat(keylines[4].offset).isEqualTo(393.75f) // itemSize * 3 + (25 * 1.5f / 2)
        assertThat(keylines[0].size).isEqualTo(18.75f) // half the med size is the anchor size
        assertThat(keylines[0].offset).isEqualTo(-9.375f) // -18.75f/2
        assertThat(keylines[5].size).isEqualTo(rightAnchorSize)
        assertThat(keylines[5].offset)
            .isEqualTo(itemSize * 3 + (25f * 1.5f) + (rightAnchorSize / 2f))
    }

    @Test
    fun testRemainingSpaceWithItemSize_fitsMediumItemWithCutoff() {
        val carouselSize = 400f
        // With size 105px, 3 large items can fit with in 400px, with 85px left over.  85*3 = 255
        // which is well over the size of the large item, so the medium size will be limited to
        // whichever is larger between 85% of the large size, or 110% of the remainingSpace to make
        // it at most 10% cut off.
        val itemSize = 105f
        val keylineList =
            uncontainedKeylineList(
                density = Density,
                carouselMainAxisSize = carouselSize,
                itemSize = itemSize,
                itemSpacing = 0f
            )
        val strategy =
            Strategy(
                defaultKeylines = keylineList,
                availableSpace = carouselSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val keylines = strategy.defaultKeylines
        val rightAnchorSize = with(Density) { CarouselDefaults.AnchorSize.toPx() }

        // The layout should be [xSmall-large-large-large-medium-xSmall]
        assertThat(keylines.size).isEqualTo(6)
        assertThat(keylines[1].size).isEqualTo(itemSize)
        assertThat(keylines[2].size).isEqualTo(itemSize)
        assertThat(keylines[3].size).isEqualTo(itemSize)
        // remainingSpace * 120%
        assertThat(keylines[4].size).isEqualTo(85 * 1.2f)
        assertThat(keylines[0].size).isEqualTo(85 * 1.2f * 0.5f)
        assertThat(keylines[5].size).isEqualTo(rightAnchorSize)
        assertThat(keylines[0].offset).isEqualTo(-(85 * 1.2f * 0.5f) / 2f)
        assertThat(keylines[5].offset).isEqualTo(itemSize * 3 + 85 * 1.2f + rightAnchorSize / 2f)
    }
}
