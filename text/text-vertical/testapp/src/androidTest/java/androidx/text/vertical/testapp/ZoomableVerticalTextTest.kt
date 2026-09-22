/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The tag of the box that stands for the viewport. */
private const val VIEWPORT_TAG = "viewport"

/** The tag of the box that stands for the text. */
private const val TEXT_TAG = "text"

/** The largest accepted error in pixels. The layout rounds the pan offset to a whole pixel. */
private const val POSITION_TOLERANCE_PX = 1f

/** The number of swipes that each test makes. Each swipe moves less than the full overflow. */
private const val SWIPE_COUNT = 3

/** Tests the pan limits of [ZoomableVerticalText]. */
@RunWith(AndroidJUnit4::class)
class ZoomableVerticalTextTest {

    @get:Rule val rule = createComposeRule()

    /** The size of the viewport. */
    private val viewportSize = 100.dp

    /** The size of the text. The text is larger than the viewport, so the text overflows. */
    private val textSize = 200.dp

    /** The length of the part of the text that is outside of the viewport, in pixels. */
    private val overflowPx: Float
        get() = with(rule.density) { (textSize.roundToPx() - viewportSize.roundToPx()).toFloat() }

    @Test
    fun verticalText_beforeAPan_showsTheStartOfTheText() {
        setContent(isVertical = true)

        // Vertical text starts at the right edge, so the text hangs off to the left.
        assertThat(textPosition().x).isWithin(POSITION_TOLERANCE_PX).of(-overflowPx)
    }

    @Test
    fun verticalText_panToTheEnd_stopsAtTheEndOfTheText() {
        setContent(isVertical = true)

        swipeManyTimes { swipeRight() }

        assertThat(textPosition().x).isWithin(POSITION_TOLERANCE_PX).of(0f)
    }

    @Test
    fun verticalText_panBackToTheStart_stopsAtTheStartOfTheText() {
        setContent(isVertical = true)

        swipeManyTimes { swipeRight() }
        swipeManyTimes { swipeLeft() }

        assertThat(textPosition().x).isWithin(POSITION_TOLERANCE_PX).of(-overflowPx)
    }

    @Test
    fun horizontalText_panToTheEnd_stopsAtTheEndOfTheText() {
        setContent(isVertical = false)

        swipeManyTimes { swipeUp() }

        assertThat(textPosition().y).isWithin(POSITION_TOLERANCE_PX).of(-overflowPx)
    }

    @Test
    fun horizontalText_panBackToTheStart_stopsAtTheStartOfTheText() {
        setContent(isVertical = false)

        swipeManyTimes { swipeUp() }
        swipeManyTimes { swipeDown() }

        assertThat(textPosition().y).isWithin(POSITION_TOLERANCE_PX).of(0f)
    }

    /**
     * Shows a [ZoomableVerticalText] that is larger than its viewport.
     *
     * The viewport gives the content a fixed size on the axis that does not scroll. The content
     * therefore overflows only on the scroll axis.
     */
    private fun setContent(isVertical: Boolean) {
        rule.setContent {
            Box(Modifier.size(viewportSize).testTag(VIEWPORT_TAG)) {
                ZoomableVerticalText(isVertical = isVertical) {
                    Box(Modifier.size(textSize).testTag(TEXT_TAG))
                }
            }
        }
    }

    /** Returns the position of the text in the root, in pixels. Clipping does not change it. */
    private fun textPosition(): Offset =
        rule.onNodeWithTag(TEXT_TAG).fetchSemanticsNode().positionInRoot

    /**
     * Makes the same swipe [SWIPE_COUNT] times.
     *
     * One swipe crosses the viewport one time, and the gesture detector removes the touch slop from
     * it. Several swipes therefore move more than the overflow and push the pan into its limit.
     */
    private fun swipeManyTimes(swipe: TouchInjectionScope.() -> Unit) {
        repeat(SWIPE_COUNT) { rule.onNodeWithTag(VIEWPORT_TAG).performTouchInput(swipe) }
    }
}
