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
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.OnPositioned
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

    private val parentGlobalPosition = IntPxPosition(IntPx(50), IntPx(50))
    private val offset = IntPxPosition(IntPx(10), IntPx(10))
    private val parentSize = IntPxSize(IntPx(100), IntPx(100))
    private val popupSize = IntPxSize(IntPx(40), IntPx(20))

    private var craneViewAbsolutePosition = IntPxPosition(IntPx(0), IntPx(0))

    // TODO(b/140215440): Some tests are calling the OnChildPosition method inside the Popup too
    //  many times
    private fun createPopupWithAlignmentRule(alignment: Alignment, measureLatch: CountDownLatch) {
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
                            // This is called after the OnChildPosition method in Popup() which updates the popup to
                            // its final position
                            OnPositioned {
                                measureLatch.countDown()
                            }
                            Container(width = popupWidthDp, height = popupHeightDp) {}
                        }
                    }
                }
            }
        }

        provideAndroidCraneViewOffset()
    }

    private fun popupMatches(viewMatcher: Matcher<in View>) {
        Espresso.onView(instanceOf(AndroidCraneView::class.java))
            .inRoot(isPlatformPopup())
            .check(matches(viewMatcher))
    }

    private fun isNotPlatformPopup(): TypeSafeMatcher<Root> {
        return object : TypeSafeMatcher<Root>() {
            override fun describeTo(description: Description?) {
                description?.appendText("isNotPopupPlatform")
            }

            override fun matchesSafely(item: Root?): Boolean {
                return !RootMatchers.isPlatformPopup().matches(item)
            }
        }
    }

    private fun saveAndroidCraneViewOffset(): ViewAction {
        return object : ViewAction {
            override fun getDescription(): String {
                return "Get AndroidCraneView offset"
            }

            override fun getConstraints(): Matcher<View> {
                return matchesAndroidCraneView()
            }

            override fun perform(uiController: UiController?, view: View?) {
                val positionArray = IntArray(2)
                view?.getLocationOnScreen(positionArray)

                craneViewAbsolutePosition = IntPxPosition(
                    IntPx(positionArray[0]),
                    IntPx(positionArray[1])
                )
            }
        }
    }

    private fun provideAndroidCraneViewOffset() {
        Espresso.onView(instanceOf(AndroidCraneView::class.java))
            .inRoot(isNotPlatformPopup())
            .perform(saveAndroidCraneViewOffset())
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

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionTopLeft))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionTopCenter))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionTopRight))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionCenterRight))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionBottomRight))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionBottomCenter))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionBottomLeft))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionCenterLeft))
    }

    @FlakyTest(bugId = 140549636, detail = "Final position might not be updated before check")
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
        popupMatches(matchesPosition(craneViewAbsolutePosition + expectedPositionCenter))
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

    private fun matchesAndroidCraneView(): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return (item is AndroidCraneView)
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with no AndroidCraneView")
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