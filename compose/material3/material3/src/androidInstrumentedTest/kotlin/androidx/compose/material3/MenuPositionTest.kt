/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MenuPositionTest {
    private val windowSize = IntSize(width = 500, height = 1000)
    private val anchorBounds = IntRect(offset = IntOffset(100, 100), size = IntSize(50, 50))
    private val menuSize = IntSize(width = 100, height = 200)

    @Test
    fun menuPosition_horizontal_anchorAlignment_ltr() {
        assertThat(
            MenuPosition.startToAnchorStart().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(anchorBounds.left)

        assertThat(
            MenuPosition.endToAnchorEnd().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(anchorBounds.right - menuSize.width)

        assertThat(
            AnchorAlignmentOffsetPosition.Horizontal(
                menuAlignment = Alignment.Start,
                anchorAlignment = Alignment.CenterHorizontally,
                offset = 0,
            ).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(anchorBounds.center.x)
    }

    @Test
    fun menuPosition_horizontal_anchorAlignment_rtl() {
        assertThat(
            MenuPosition.startToAnchorStart().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Rtl,
            )
        ).isEqualTo(anchorBounds.right - menuSize.width)

        assertThat(
            MenuPosition.endToAnchorEnd().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Rtl,
            )
        ).isEqualTo(anchorBounds.left)

        assertThat(
            AnchorAlignmentOffsetPosition.Horizontal(
                menuAlignment = Alignment.Start,
                anchorAlignment = Alignment.CenterHorizontally,
                offset = 0,
            ).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Rtl,
            )
        ).isEqualTo(anchorBounds.center.x - menuSize.width)
    }

    @Test
    fun menuPosition_horizontal_anchorAlignment_withOffset() {
        val offset = 10
        assertThat(
            MenuPosition.startToAnchorStart(offset).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(anchorBounds.left + offset)

        assertThat(
            MenuPosition.startToAnchorStart(offset).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Rtl,
            )
        ).isEqualTo(anchorBounds.right - menuSize.width - offset)
    }

    @Test
    fun menuPosition_horizontal_windowAlignment() {
        assertThat(
            MenuPosition.leftToWindowLeft().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(0)

        assertThat(
            MenuPosition.rightToWindowRight().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(windowSize.width - menuSize.width)

        assertThat(
            MenuPosition.leftToWindowLeft().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Rtl,
            )
        ).isEqualTo(0)

        assertThat(
            MenuPosition.rightToWindowRight().position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Rtl,
            )
        ).isEqualTo(windowSize.width - menuSize.width)
    }

    @Test
    fun menuPosition_horizontal_windowAlignment_withMargin() {
        val margin = 150
        assertThat(
            MenuPosition.leftToWindowLeft(margin).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(margin)

        assertThat(
            MenuPosition.rightToWindowRight(margin).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo(windowSize.width - menuSize.width - margin)
    }

    @Test
    fun menuPosition_horizontal_windowAlignment_withTooLargeMargin_centersHorizontallyInstead() {
        val margin = 220
        assertThat(margin * 2 + menuSize.width).isGreaterThan(windowSize.width)

        assertThat(
            MenuPosition.leftToWindowLeft(margin).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo((windowSize.width - menuSize.width) / 2)

        assertThat(
            MenuPosition.rightToWindowRight(margin).position(
                anchorBounds,
                windowSize,
                menuSize.width,
                LayoutDirection.Ltr,
            )
        ).isEqualTo((windowSize.width - menuSize.width) / 2)
    }

    @Test
    fun menuPosition_vertical_anchorAlignment() {
        assertThat(
            MenuPosition.topToAnchorBottom().position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(anchorBounds.bottom)

        assertThat(
            MenuPosition.bottomToAnchorTop().position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(anchorBounds.top - menuSize.height)

        assertThat(
            MenuPosition.centerToAnchorTop().position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(anchorBounds.top - menuSize.height / 2)
    }

    @Test
    fun menuPosition_vertical_anchorAlignment_withOffset() {
        val offset = 10
        assertThat(
            MenuPosition.topToAnchorBottom(offset).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(anchorBounds.bottom + offset)

        assertThat(
            MenuPosition.bottomToAnchorTop(offset).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(anchorBounds.top - menuSize.height + offset)

        assertThat(
            MenuPosition.centerToAnchorTop(offset).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(anchorBounds.top - menuSize.height / 2 + offset)
    }

    @Test
    fun menuPosition_vertical_windowAlignment() {
        assertThat(
            MenuPosition.topToWindowTop().position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(0)

        assertThat(
            MenuPosition.bottomToWindowBottom().position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(windowSize.height - menuSize.height)

        assertThat(
            WindowAlignmentMarginPosition.Vertical(
                alignment = Alignment.CenterVertically,
                margin = 0,
            ).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo((windowSize.height - menuSize.height) / 2)
    }

    @Test
    fun menuPosition_vertical_windowAlignment_withMargin() {
        val margin = 150
        assertThat(
            MenuPosition.topToWindowTop(margin).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(margin)

        assertThat(
            MenuPosition.bottomToWindowBottom(margin).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo(windowSize.height - menuSize.height - margin)

        assertThat(
            WindowAlignmentMarginPosition.Vertical(
                alignment = Alignment.CenterVertically,
                margin = margin,
            ).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo((windowSize.height - menuSize.height) / 2)
    }

    @Test
    fun menuPosition_vertical_windowAlignment_withTooLargeMargin_centersVerticallyInstead() {
        val margin = 450
        assertThat(margin * 2 + menuSize.height).isGreaterThan(windowSize.height)

        assertThat(
            MenuPosition.topToWindowTop(margin).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo((windowSize.height - menuSize.height) / 2)

        assertThat(
            MenuPosition.bottomToWindowBottom(margin).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo((windowSize.height - menuSize.height) / 2)

        assertThat(
            WindowAlignmentMarginPosition.Vertical(
                alignment = Alignment.CenterVertically,
                margin = margin,
            ).position(
                anchorBounds,
                windowSize,
                menuSize.height,
            )
        ).isEqualTo((windowSize.height - menuSize.height) / 2)
    }

    @Test
    fun menuPositionProvider_toBottomStartOfAnchor() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowSize = IntSize(screenWidth, screenHeight)
        val anchorPosition = IntOffset(100, 200)
        val anchorSize = IntSize(10, 20)
        val offsetX = 20
        val offsetY = 40
        val popupSize = IntSize(50, 80)

        val ltrPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(ltrPosition.x).isEqualTo(
            anchorPosition.x + offsetX
        )
        assertThat(ltrPosition.y).isEqualTo(
            anchorPosition.y + anchorSize.height + offsetY
        )

        val rtlPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(rtlPosition.x).isEqualTo(
            anchorPosition.x + anchorSize.width - popupSize.width - offsetX
        )
        assertThat(rtlPosition.y).isEqualTo(
            anchorPosition.y + anchorSize.height + offsetY
        )
    }

    @Test
    fun menuPositionProvider_toTopEndOfAnchor() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowSize = IntSize(screenWidth, screenHeight)
        val anchorPosition = IntOffset(450, 950)
        val anchorPositionRtl = IntOffset(50, 950)
        val anchorSize = IntSize(10, 20)
        val offsetX = 20
        val offsetY = -40
        val popupSize = IntSize(150, 80)

        val ltrPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(ltrPosition.x).isEqualTo(
            anchorPosition.x + anchorSize.width - popupSize.width + offsetX
        )
        assertThat(ltrPosition.y).isEqualTo(
            anchorPosition.y - popupSize.height + offsetY
        )

        val rtlPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntRect(anchorPositionRtl, anchorSize),
            windowSize,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(rtlPosition.x).isEqualTo(
            anchorPositionRtl.x - offsetX
        )
        assertThat(rtlPosition.y).isEqualTo(
            anchorPositionRtl.y - popupSize.height + offsetY
        )
    }

    @Test
    fun menuPositionProvider_toTopOfWindow() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowSize = IntSize(screenWidth, screenHeight)
        val anchorPosition = IntOffset(0, 0)
        val anchorSize = IntSize(50, 20)
        val popupSize = IntSize(150, 500)

        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }

        val position = DropdownMenuPositionProvider(
            DpOffset.Zero,
            density
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position.y).isEqualTo(
            verticalMargin
        )
    }

    @Test
    fun menuPositionProvider_anchorPartiallyVisible() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowSize = IntSize(screenWidth, screenHeight)
        val anchorPosition = IntOffset(-25, -10)
        val anchorPositionRtl = IntOffset(525, -10)
        val anchorSize = IntSize(50, 20)
        val popupSize = IntSize(150, 500)

        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }

        val position = DropdownMenuPositionProvider(
            DpOffset.Zero,
            density
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position.x).isEqualTo(
            0
        )
        assertThat(position.y).isEqualTo(
            verticalMargin
        )

        val rtlPosition = DropdownMenuPositionProvider(
            DpOffset.Zero,
            density
        ).calculatePosition(
            IntRect(anchorPositionRtl, anchorSize),
            windowSize,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(rtlPosition.x).isEqualTo(
            screenWidth - popupSize.width
        )
        assertThat(rtlPosition.y).isEqualTo(
            verticalMargin
        )
    }

    @Test
    fun menuPositionProvider_callback() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowSize = IntSize(screenWidth, screenHeight)
        val anchorPosition = IntOffset(100, 200)
        val anchorSize = IntSize(10, 20)
        val offsetX = 20
        val offsetY = 40
        val popupSize = IntSize(50, 80)

        var obtainedAnchorBounds = IntRect.Zero
        var obtainedMenuBounds = IntRect.Zero
        DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ) { anchorBounds, menuBounds ->
            obtainedAnchorBounds = anchorBounds
            obtainedMenuBounds = menuBounds
        }.calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(obtainedAnchorBounds).isEqualTo(IntRect(anchorPosition, anchorSize))
        assertThat(obtainedMenuBounds).isEqualTo(
            IntRect(
                offset = IntOffset(
                    x = anchorPosition.x + offsetX,
                    y = anchorPosition.y + anchorSize.height + offsetY
                ),
                size = popupSize,
            )
        )
    }
}
