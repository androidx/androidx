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

package androidx.compose.foundation.contextmenu

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContextMenuPopupPositionProviderTest {
    // region ContextMenuPopupPositionProvider Tests
    @Test
    fun calculatePosition_onlyFitsInXDirection() {
        val layoutDirection = LayoutDirection.Ltr
        val windowSize = IntSize(width = 100, height = 100)
        val anchorBounds = IntRect(left = 10, top = 20, right = 90, bottom = 80)
        val position = IntOffset(x = 40, y = 30) // 50, 50 when translated to window
        val popupContentSize = IntSize(width = 10, height = 200)

        val subject = ContextMenuPopupPositionProvider(position)
        val actual =
            subject.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)

        assertThat(actual).isEqualTo(IntOffset(50, 0))
    }

    @Test
    fun calculatePosition_onlyFitsInYDirection() {
        val layoutDirection = LayoutDirection.Ltr
        val windowSize = IntSize(width = 100, height = 100)
        val anchorBounds = IntRect(left = 10, top = 20, right = 90, bottom = 80)
        val position = IntOffset(x = 40, y = 30) // 50, 50 when translated to window
        val popupContentSize = IntSize(width = 200, height = 10)

        val subject = ContextMenuPopupPositionProvider(position)
        val actual =
            subject.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)

        assertThat(actual).isEqualTo(IntOffset(0, 50))
    }

    @Test
    fun calculatePosition_windowIsAccountedFor_ltr() {
        val layoutDirection = LayoutDirection.Ltr
        val windowSize = IntSize(width = 100, height = 100)
        val anchorBounds = IntRect(left = 10, top = 20, right = 90, bottom = 80)
        val position = IntOffset(x = 40, y = 30) // 50, 50 when translated to window
        val popupContentSize = IntSize(width = 10, height = 10)

        val subject = ContextMenuPopupPositionProvider(position)
        val actual =
            subject.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)

        assertThat(actual).isEqualTo(IntOffset(50, 50))
    }

    @Test
    fun calculatePosition_windowIsAccountedFor_rtl() {
        val layoutDirection = LayoutDirection.Rtl
        val windowSize = IntSize(width = 100, height = 100)
        val anchorBounds = IntRect(left = 10, top = 20, right = 90, bottom = 80)
        val position = IntOffset(x = 40, y = 30) // 50, 50 when translated to window
        val popupContentSize = IntSize(width = 10, height = 10)

        val subject = ContextMenuPopupPositionProvider(position)
        val actual =
            subject.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)

        assertThat(actual).isEqualTo(IntOffset(40, 50))
    }

    // endregion ContextMenuPopupPositionProvider Tests

    // region alignPopupAxis Tests
    @Test
    fun alignPopupAxis_closeAffinity_popupLargerThanWindow() =
        alignPopupAxisTest(position = 50, popupLength = 200, closeAffinity = true, expected = 0)

    @Test
    fun alignPopupAxis_closeAffinity_popupFitsNeitherSide() =
        alignPopupAxisTest(position = 50, popupLength = 75, closeAffinity = true, expected = 25)

    @Test
    fun alignPopupAxis_closeAffinity_popupFitsCloseSide() =
        alignPopupAxisTest(position = 90, popupLength = 30, closeAffinity = true, expected = 60)

    @Test
    fun alignPopupAxis_closeAffinity_popupFitsFarSide() =
        alignPopupAxisTest(position = 20, popupLength = 30, closeAffinity = true, expected = 20)

    @Test
    fun alignPopupAxis_closeAffinity_popupFitsEitherSide() =
        alignPopupAxisTest(position = 50, popupLength = 10, closeAffinity = true, expected = 50)

    @Test
    fun alignPopupAxis_farAffinity_popupLargerThanWindow() =
        alignPopupAxisTest(position = 50, popupLength = 200, closeAffinity = false, expected = -100)

    @Test
    fun alignPopupAxis_farAffinity_popupFitsNeitherSide() =
        alignPopupAxisTest(position = 50, popupLength = 75, closeAffinity = false, expected = 0)

    @Test
    fun alignPopupAxis_farAffinity_popupFitsCloseSide() =
        alignPopupAxisTest(position = 90, popupLength = 30, closeAffinity = false, expected = 60)

    @Test
    fun alignPopupAxis_farAffinity_popupFitsFarSide() =
        alignPopupAxisTest(position = 20, popupLength = 30, closeAffinity = false, expected = 20)

    @Test
    fun alignPopupAxis_farAffinity_popupFitsEitherSide() =
        alignPopupAxisTest(position = 50, popupLength = 10, closeAffinity = false, expected = 40)

    private fun alignPopupAxisTest(
        position: Int,
        popupLength: Int,
        closeAffinity: Boolean,
        expected: Int
    ) {
        val actual =
            alignPopupAxis(
                position = position,
                popupLength = popupLength,
                windowLength = 100,
                closeAffinity = closeAffinity
            )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun alignPopupAxis_popupBarelyFitsInAfterSpace() {
        val actual =
            alignPopupAxis(
                position = 74,
                popupLength = 25,
                windowLength = 100,
            )
        assertThat(actual).isEqualTo(74)
    }

    @Test
    fun alignPopupAxis_popupBarelyDoesNotFitInAfterSpace() {
        val actual =
            alignPopupAxis(
                position = 75,
                popupLength = 25,
                windowLength = 100,
            )
        assertThat(actual).isEqualTo(50)
    }

    @Test
    fun alignPopupAxis_popupBarelyFitsInBeforeSpace() {
        val actual =
            alignPopupAxis(
                position = 25,
                popupLength = 25,
                windowLength = 100,
                closeAffinity = false
            )
        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun alignPopupAxis_popupBarelyDoesNotFitInBeforeSpace() {
        val actual =
            alignPopupAxis(
                position = 24,
                popupLength = 25,
                windowLength = 100,
                closeAffinity = false
            )
        assertThat(actual).isEqualTo(24)
    }
    // endregion alignPopupAxis Tests
}
