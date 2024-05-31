/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopMenuTest {
    private val windowSize = IntSize(200, 200)

    // Standard case: enough room to position below the anchor and align left
    @Test
    fun menu_positioning_alignLeft_belowAnchor() {
        val anchorBounds = IntRect(offset = IntOffset(10, 50), size = IntSize(50, 20))
        val popupSize = IntSize(70, 70)

        val position =
            DropdownMenuPositionProvider(DpOffset.Zero, Density(1f))
                .calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        assertThat(position).isEqualTo(anchorBounds.bottomLeft)
    }

    // Standard RTL case: enough room to position below the anchor and align right
    @Test
    fun menu_positioning_rtl_alignRight_belowAnchor() {
        val anchorBounds = IntRect(offset = IntOffset(30, 50), size = IntSize(50, 20))
        val popupSize = IntSize(70, 70)

        val position =
            DropdownMenuPositionProvider(DpOffset.Zero, Density(1f))
                .calculatePosition(anchorBounds, windowSize, LayoutDirection.Rtl, popupSize)

        assertThat(position)
            .isEqualTo(IntOffset(x = anchorBounds.right - popupSize.width, y = anchorBounds.bottom))
    }

    // Not enough room to position the popup below the anchor, but enough room above
    @Test
    fun menu_positioning_alignLeft_aboveAnchor() {
        val anchorBounds = IntRect(offset = IntOffset(10, 150), size = IntSize(50, 30))
        val popupSize = IntSize(70, 30)

        val position =
            DropdownMenuPositionProvider(DpOffset.Zero, Density(1f))
                .calculatePosition(anchorBounds, windowSize, LayoutDirection.Ltr, popupSize)

        assertThat(position)
            .isEqualTo(IntOffset(x = anchorBounds.left, y = anchorBounds.top - popupSize.height))
    }

    // Anchor left is at negative coordinates, so align popup to the left of the window
    @Test
    fun menu_positioning_windowLeft_belowAnchor() {
        val anchorBounds = IntRect(offset = IntOffset(-10, 50), size = IntSize(50, 20))
        val popupSize = IntSize(70, 50)

        val position =
            DropdownMenuPositionProvider(DpOffset.Zero, Density(1f))
                .calculatePosition(
                    anchorBounds = anchorBounds,
                    windowSize,
                    LayoutDirection.Ltr,
                    popupSize
                )

        assertThat(position).isEqualTo(IntOffset(0, anchorBounds.bottom))
    }
}
