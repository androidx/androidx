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
package androidx.ui.foundation

import android.view.View
import androidx.compose.composer
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.filters.MediumTest
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.Text
import androidx.ui.core.toPxPosition
import androidx.ui.core.toPxSize
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Container
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class PopupTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)
    private val popupText = "popupText"

    private val parentGlobalPosition = IntPxPosition(IntPx(50), IntPx(50))
    private val offset = IntPxPosition(IntPx(10), IntPx(10))
    private val parentSize = IntPxSize(IntPx(100), IntPx(100))
    private val popupSize = IntPxSize(IntPx(40), IntPx(20))

    private fun createPopupWithAlignmentRule(alignment: Alignment) {
        withDensity(composeTestRule.density) {
            val popupWidthDp = popupSize.width.toDp()
            val popupHeightDp = popupSize.height.toDp()
            val parentWidthDp = parentSize.width.toDp()
            val parentHeightDp = parentSize.height.toDp()

            composeTestRule.setContent {
                // Align the parent of the popup on the TopLeft corner, this results in the global
                // position of the parent to be (0, 0)
                Align(alignment = Alignment.TopLeft) {
                    Container(width = parentWidthDp, height = parentHeightDp) {
                        Popup(alignment = alignment, offset = offset) {
                            Container(width = popupWidthDp, height = popupHeightDp) {}
                        }
                    }
                }
            }
        }
    }

    private fun popupMatches(viewMatcher: Matcher<in View>) {
        Espresso.onView(instanceOf(AndroidCraneView::class.java))
            .inRoot(isPlatformPopup())
            .check(matches(viewMatcher))
    }

    @Test
    fun popup_isShowing() {
        composeTestRule.setContent {
            Container {
                Popup(alignment = Alignment.Center) {
                    Text(popupText)
                }
            }
        }

        popupMatches(isDisplayed())
    }

    @Test
    fun popup_hasActualSize() {
        val popupWidthDp = withDensity(composeTestRule.density) {
            popupSize.width.toDp()
        }
        val popupHeightDp = withDensity(composeTestRule.density) {
            popupSize.height.toDp()
        }

        composeTestRule.setContent {
            Container {
                Popup(alignment = Alignment.Center) {
                    Container(width = popupWidthDp, height = popupHeightDp) {}
                }
            }
        }

        popupMatches(matchesSize(popupSize.width.value, popupSize.height.value))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentTopLeft() {
        /* Expected TopLeft Position
           x = offset.x
           y = offset.y
        */
        val expectedPositionTopLeft = IntPxPosition(IntPx(10), IntPx(10))

        createPopupWithAlignmentRule(alignment = Alignment.TopLeft)

        popupMatches(matchesPosition(expectedPositionTopLeft))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentTopCenter() {
        /* Expected TopCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y
        */
        val expectedPositionTopCenter = IntPxPosition(IntPx(40), IntPx(10))

        createPopupWithAlignmentRule(alignment = Alignment.TopCenter)

        popupMatches(matchesPosition(expectedPositionTopCenter))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentTopRight() {
        /* Expected TopRight Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPositionTopRight = IntPxPosition(IntPx(70), IntPx(10))

        createPopupWithAlignmentRule(alignment = Alignment.TopRight)

        popupMatches(matchesPosition(expectedPositionTopRight))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentCenterRight() {
        /* Expected CenterRight Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterRight = IntPxPosition(IntPx(70), IntPx(50))

        createPopupWithAlignmentRule(alignment = Alignment.CenterRight)

        popupMatches(matchesPosition(expectedPositionCenterRight))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentBottomRight() {
        /* Expected BottomRight Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomRight = IntPxPosition(IntPx(70), IntPx(90))

        createPopupWithAlignmentRule(alignment = Alignment.BottomRight)

        popupMatches(matchesPosition(expectedPositionBottomRight))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentBottomCenter() {
        /* Expected BottomCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntPxPosition(IntPx(40), IntPx(90))

        createPopupWithAlignmentRule(alignment = Alignment.BottomCenter)

        popupMatches(matchesPosition(expectedPositionBottomCenter))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentBottomLeft() {
        /* Expected BottomLeft Position
           x = offset.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomLeft = IntPxPosition(IntPx(10), IntPx(90))

        createPopupWithAlignmentRule(alignment = Alignment.BottomLeft)

        popupMatches(matchesPosition(expectedPositionBottomLeft))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentCenterLeft() {
        /* Expected CenterLeft Position
           x = offset.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterLeft = IntPxPosition(IntPx(10), IntPx(50))

        createPopupWithAlignmentRule(alignment = Alignment.CenterLeft)

        popupMatches(matchesPosition(expectedPositionCenterLeft))
    }

    @Ignore("Disabled due to b/139875128")
    @Test
    fun popup_correctPosition_alignmentCenter() {
        /* Expected Center Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntPxPosition(IntPx(40), IntPx(50))

        createPopupWithAlignmentRule(alignment = Alignment.Center)

        popupMatches(matchesPosition(expectedPositionCenter))
    }

    @Test
    fun popup_calculateGlobalPositionTopLeft() {
        /* Expected TopLeft Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopLeft = IntPxPosition(IntPx(60), IntPx(60))
        val positionTopLeft = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.TopLeft,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionTopLeft).isEqualTo(expectedPositionTopLeft)
    }

    @Test
    fun popup_calculateGlobalPositionTopCenter() {
        /* Expected TopCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopCenter = IntPxPosition(IntPx(90), IntPx(60))
        val positionTopCenter = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.TopCenter,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionTopCenter).isEqualTo(expectedPositionTopCenter)
    }

    @Test
    fun popup_calculateGlobalPositionTopRight() {
        /* Expected TopRight Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopRight = IntPxPosition(IntPx(120), IntPx(60))
        val positionTopRight = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.TopRight,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionTopRight).isEqualTo(expectedPositionTopRight)
    }

    @Test
    fun popup_calculateGlobalPositionCenterRight() {
        /* Expected CenterRight Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterRight = IntPxPosition(IntPx(120), IntPx(100))
        val positionCenterRight = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.CenterRight,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionCenterRight).isEqualTo(expectedPositionCenterRight)
    }

    @Test
    fun popup_calculateGlobalPositionBottomRight() {
        /* Expected BottomRight Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomRight = IntPxPosition(IntPx(120), IntPx(140))
        val positionBottomRight = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.BottomRight,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionBottomRight).isEqualTo(expectedPositionBottomRight)
    }

    @Test
    fun popup_calculateGlobalPositionBottomCenter() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntPxPosition(IntPx(90), IntPx(140))
        val positionBottomCenter = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.BottomCenter,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionBottomCenter).isEqualTo(expectedPositionBottomCenter)
    }

    @Test
    fun popup_calculateGlobalPositionBottomLeft() {
        /* Expected BottomLeft Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomLeft = IntPxPosition(IntPx(60), IntPx(140))
        val positionBottomLeft = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.BottomLeft,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionBottomLeft).isEqualTo(expectedPositionBottomLeft)
    }

    @Test
    fun popup_calculateGlobalPositionCenterLeft() {
        /* Expected CenterLeft Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterLeft = IntPxPosition(IntPx(60), IntPx(100))
        val positionCenterLeft = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.CenterLeft,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionCenterLeft).isEqualTo(expectedPositionCenterLeft)
    }

    @Test
    fun popup_calculateGlobalPositionCenter() {
        /* Expected Center Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntPxPosition(IntPx(90), IntPx(100))
        val positionCenter = calculatePopupGlobalPosition(
            parentPos = parentGlobalPosition.toPxPosition(),
            alignment = Alignment.Center,
            offset = offset,
            parentSize = parentSize.toPxSize(),
            popupSize = popupSize.toPxSize()
        )

        Truth.assertThat(positionCenter).isEqualTo(expectedPositionCenter)
    }

    private fun matchesSize(width: Int, height: Int): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return item?.width == width && item.height == height
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with width = $width height = $height")
            }
        }
    }

    private fun matchesPosition(expectedPosition: IntPxPosition): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                val position = IntArray(2)
                item?.getLocationOnScreen(position)

                return expectedPosition == IntPxPosition(IntPx(position[0]), IntPx(position[1]))
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with expected position: $expectedPosition")
            }
        }
    }
}
