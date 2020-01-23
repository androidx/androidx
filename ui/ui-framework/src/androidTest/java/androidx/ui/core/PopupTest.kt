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
package androidx.ui.core

import android.view.View
import androidx.compose.Composable
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.filters.MediumTest
import androidx.ui.core.selection.SimpleContainer
import androidx.ui.test.createComposeRule
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.toPxPosition
import androidx.ui.unit.toPxSize
import androidx.ui.unit.withDensity
import com.google.common.truth.Truth
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class PopupTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)
    private val popupText = "popupText"
    private val testTag = "testedPopup"

    private val parentGlobalPosition = IntPxPosition(IntPx(50), IntPx(50))
    private val offset = IntPxPosition(IntPx(10), IntPx(10))
    private val parentSize = IntPxSize(IntPx(100), IntPx(100))
    private val popupSize = IntPxSize(IntPx(40), IntPx(20))

    private var composeViewAbsolutePosition = IntPxPosition(IntPx(0), IntPx(0))

    // TODO(b/140215440): Some tests are calling the OnChildPosition method inside the Popup too
    //  many times
    private fun createPopupWithAlignmentRule(alignment: Alignment, measureLatch: CountDownLatch) {
        withDensity(composeTestRule.density) {
            val popupWidthDp = popupSize.width.toDp()
            val popupHeightDp = popupSize.height.toDp()
            val parentWidthDp = parentSize.width.toDp()
            val parentHeightDp = parentSize.height.toDp()

            composeTestRule.setContent {
                // Get the compose view position on screen
                val composeView = AndroidComposeViewAmbient.current
                val positionArray = IntArray(2)
                composeView.getLocationOnScreen(positionArray)
                composeViewAbsolutePosition = IntPxPosition(
                    IntPx(positionArray[0]),
                    IntPx(positionArray[1])
                )

                // Align the parent of the popup on the TopLeft corner, this results in the global
                // position of the parent to be (0, 0)
                TestAlign {
                    SimpleContainer(width = parentWidthDp, height = parentHeightDp) {
                        PopupTestTag(testTag) {
                            Popup(alignment = alignment, offset = offset) {
                                // This is called after the OnChildPosition method in Popup() which
                                // updates the popup to its final position
                                OnPositioned {
                                    measureLatch.countDown()
                                }
                                SimpleContainer(width = popupWidthDp, height = popupHeightDp) {}
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO(b/139861182): Remove all of this and provide helpers on ComposeTestRule
    private fun popupMatches(viewMatcher: Matcher<in View>) {
        Espresso.onView(instanceOf(AndroidComposeView::class.java))
            .inRoot(PopupLayoutMatcher())
            .check(matches(viewMatcher))
    }

    private inner class PopupLayoutMatcher : TypeSafeMatcher<Root>() {
        override fun describeTo(description: Description?) {
            description?.appendText("PopupLayoutMatcher")
        }

        // TODO(b/141101446): Find a way to match the window used by the popup
        override fun matchesSafely(item: Root?): Boolean {
            return item != null && isPopupLayout(item.decorView, testTag)
        }
    }

    @Test
    fun popup_isShowing() {
        composeTestRule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        Text(popupText)
                    }
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
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(width = popupWidthDp, height = popupHeightDp) {}
                    }
                }
            }
        }

        popupMatches(matchesSize(popupSize.width.value, popupSize.height.value))
    }

    @Test
    fun popup_correctPosition_alignmentTopLeft() {
        /* Expected TopLeft Position
           x = offset.x
           y = offset.y
        */
        val expectedPositionTopLeft = IntPxPosition(IntPx(10), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.TopLeft, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopLeft))
    }

    @Test
    fun popup_correctPosition_alignmentTopCenter() {
        /* Expected TopCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y
        */
        val expectedPositionTopCenter = IntPxPosition(IntPx(40), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.TopCenter, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopCenter))
    }

    @Test
    fun popup_correctPosition_alignmentTopRight() {
        /* Expected TopRight Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPositionTopRight = IntPxPosition(IntPx(70), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.TopRight, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopRight))
    }

    @Test
    fun popup_correctPosition_alignmentCenterRight() {
        /* Expected CenterRight Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterRight = IntPxPosition(IntPx(70), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.CenterRight, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterRight))
    }

    @Test
    fun popup_correctPosition_alignmentBottomRight() {
        /* Expected BottomRight Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomRight = IntPxPosition(IntPx(70), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.BottomRight, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomRight))
    }

    @Test
    fun popup_correctPosition_alignmentBottomCenter() {
        /* Expected BottomCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntPxPosition(IntPx(40), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            alignment = Alignment.BottomCenter,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomCenter))
    }

    @Test
    fun popup_correctPosition_alignmentBottomLeft() {
        /* Expected BottomLeft Position
           x = offset.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomLeft = IntPxPosition(IntPx(10), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.BottomLeft, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomLeft))
    }

    @Test
    fun popup_correctPosition_alignmentCenterLeft() {
        /* Expected CenterLeft Position
           x = offset.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterLeft = IntPxPosition(IntPx(10), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.CenterLeft, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterLeft))
    }

    @Test
    fun popup_correctPosition_alignmentCenter() {
        /* Expected Center Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntPxPosition(IntPx(40), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.Center, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenter))
    }

    @Test
    fun popup_calculateGlobalPositionTopLeft() {
        /* Expected TopLeft Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopLeft = IntPxPosition(IntPx(60), IntPx(60))
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionTopLeft = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.TopLeft
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
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionTopCenter = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.TopCenter
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
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionTopRight = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.TopRight
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
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionBottomRight = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.CenterRight
        )

        Truth.assertThat(positionBottomRight).isEqualTo(expectedPositionCenterRight)
    }

    @Test
    fun popup_calculateGlobalPositionBottomCenter() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntPxPosition(IntPx(90), IntPx(140))
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionBottomCenter = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.BottomCenter
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
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionBottomLeft = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.BottomLeft
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
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionCenterLeft = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.CenterLeft
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
        val popupPositionProperties = PopupPositionProperties(
            offset = offset
        )
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionCenter = calculatePopupGlobalPosition(
            popupPositionProperties,
            Alignment.Center
        )

        Truth.assertThat(positionCenter).isEqualTo(expectedPositionCenter)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionLeft() {
        /* Expected Dropdown Start Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionLeft = IntPxPosition(IntPx(60), IntPx(160))
        val popupPositionProperties = PopupPositionProperties(offset = offset)
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionLeft = calculateDropdownPopupPosition(
            popupPositionProperties,
            DropDownAlignment.Left
        )

        Truth.assertThat(positionLeft).isEqualTo(expectedPositionLeft)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionRight() {
        /* Expected Dropdown End Position
           x = parentGlobalPosition.x + offset.x + parentSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionRight = IntPxPosition(IntPx(160), IntPx(160))
        val popupPositionProperties = PopupPositionProperties(offset = offset)
        popupPositionProperties.parentPosition = parentGlobalPosition.toPxPosition()
        popupPositionProperties.parentSize = parentSize.toPxSize()
        popupPositionProperties.childrenSize = popupSize.toPxSize()

        val positionRight = calculateDropdownPopupPosition(
            popupPositionProperties,
            DropDownAlignment.Right
        )

        Truth.assertThat(positionRight).isEqualTo(expectedPositionRight)
    }

    private fun matchesAndroidComposeView(): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return (item is AndroidComposeView)
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with no AndroidComposeView")
            }
        }
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
            // (-1, -1) no position found
            var positionFound = IntPxPosition(IntPx(-1), IntPx(-1))
            override fun matchesSafely(item: View?): Boolean {
                val position = IntArray(2)
                item?.getLocationOnScreen(position)
                positionFound = IntPxPosition(IntPx(position[0]), IntPx(position[1]))

                return expectedPosition == positionFound
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with expected position: $expectedPosition" +
                        " but position found: $positionFound")
            }
        }
    }
}

@Composable
private fun TestAlign(children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        // The child cannot be larger than our max constraints, but we ignore min constraints.
        val placeable = measurable?.measure(constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx))

        // The layout is as large as possible for bounded constraints,
        // or wrap content otherwise.
        val layoutWidth = if (constraints.maxWidth.isFinite()) {
            constraints.maxWidth
        } else {
            placeable?.width ?: constraints.minWidth
        }
        val layoutHeight = if (constraints.maxHeight.isFinite()) {
            constraints.maxHeight
        } else {
            placeable?.height ?: constraints.minHeight
        }

        layout(layoutWidth, layoutHeight) {
            if (placeable != null) {
                val position = Alignment.TopLeft.align(
                    IntPxSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}
