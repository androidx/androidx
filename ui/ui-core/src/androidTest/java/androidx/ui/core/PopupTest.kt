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
import androidx.compose.emptyContent
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.ui.core.selection.SimpleContainer
import androidx.ui.layout.preferredSize
import androidx.ui.layout.rtl
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
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
@FlakyTest(bugId = 150214184)
class PopupTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)
    private val testTag = "testedPopup"

    private val parentGlobalPosition = IntPxPosition(IntPx(50), IntPx(50))
    private val offset = IntPxPosition(IntPx(10), IntPx(10))
    private val parentSize = IntPxSize(IntPx(100), IntPx(100))
    private val popupSize = IntPxSize(IntPx(40), IntPx(20))

    private var composeViewAbsolutePosition = IntPxPosition(IntPx(0), IntPx(0))

    // TODO(b/140215440): Some tests are calling the OnChildPosition method inside the Popup too
    //  many times
    private fun createPopupWithAlignmentRule(
        alignment: Alignment,
        measureLatch: CountDownLatch,
        modifier: Modifier = Modifier
    ) {
        with(composeTestRule.density) {
            val popupWidthDp = popupSize.width.toDp()
            val popupHeightDp = popupSize.height.toDp()
            val parentWidthDp = parentSize.width.toDp()
            val parentHeightDp = parentSize.height.toDp()

            composeTestRule.setContent {
                // Get the compose view position on screen
                @Suppress("DEPRECATION")
                val composeView = OwnerAmbient.current as View
                val positionArray = IntArray(2)
                composeView.getLocationOnScreen(positionArray)
                composeViewAbsolutePosition = IntPxPosition(
                    IntPx(positionArray[0]),
                    IntPx(positionArray[1])
                )

                // Align the parent of the popup on the top left corner, this results in the global
                // position of the parent to be (0, 0)
                TestAlign {
                    SimpleContainer(
                        width = parentWidthDp,
                        height = parentHeightDp,
                        modifier = modifier
                    ) {
                        PopupTestTag(testTag) {
                            Popup(alignment = alignment, offset = offset) {
                                // This is called after the OnChildPosition method in Popup() which
                                // updates the popup to its final position
                                SimpleContainer(
                                    width = popupWidthDp,
                                    height = popupHeightDp,
                                    modifier = Modifier.onPositioned { measureLatch.countDown() },
                                    children = emptyContent()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO(b/139861182): Remove all of this and provide helpers on ComposeTestRule
    private fun popupMatches(viewMatcher: Matcher<in View>) {
        // Make sure that current measurement/drawing is finished
        runOnIdleCompose { }
        Espresso.onView(instanceOf(Owner::class.java))
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
                        SimpleContainer(Modifier.preferredSize(50.dp), children = emptyContent())
                    }
                }
            }
        }

        popupMatches(isDisplayed())
    }

    @Test
    fun popup_hasActualSize() {
        val popupWidthDp = with(composeTestRule.density) {
            popupSize.width.toDp()
        }
        val popupHeightDp = with(composeTestRule.density) {
            popupSize.height.toDp()
        }

        composeTestRule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(
                            width = popupWidthDp,
                            height = popupHeightDp,
                            children = emptyContent()
                        )
                    }
                }
            }
        }

        popupMatches(matchesSize(popupSize.width.value, popupSize.height.value))
    }

    @Test
    fun popup_correctPosition_alignmentTopStart() {
        /* Expected TopStart Position
           x = offset.x
           y = offset.y
        */
        val expectedPositionTopStart = IntPxPosition(IntPx(10), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.TopStart, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopStart))
    }

    @Test
    fun popup_correctPosition_alignmentTopStart_rtl() {
        /* Expected TopStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPositionTopStart = IntPxPosition(IntPx(50), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.TopStart,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopStart))
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
    fun popup_correctPosition_alignmentTopCenter_rtl() {
        /* Expected TopCenter Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y
        */
        val expectedPositionTopCenter = IntPxPosition(IntPx(20), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.TopCenter,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopCenter))
    }

    @Test
    fun popup_correctPosition_alignmentTopEnd() {
        /* Expected TopEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPositionTopEnd = IntPxPosition(IntPx(70), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.TopEnd, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopEnd))
    }

    @Test
    fun popup_correctPosition_alignmentTopEnd_rtl() {
        /* Expected TopEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y
        */
        val expectedPositionTopEnd = IntPxPosition(IntPx(0), IntPx(10))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.TopEnd,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopEnd))
    }

    @Test
    fun popup_correctPosition_alignmentCenterEnd() {
        /* Expected CenterEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntPxPosition(IntPx(70), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.CenterEnd, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterEnd))
    }

    @Test
    fun popup_correctPosition_alignmentCenterEnd_rtl() {
        /* Expected CenterEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntPxPosition(IntPx(0), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.CenterEnd,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterEnd))
    }

    @Test
    fun popup_correctPosition_alignmentBottomEnd() {
        /* Expected BottomEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntPxPosition(IntPx(70), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.BottomEnd, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomEnd))
    }

    @Test
    fun popup_correctPosition_alignmentBottomEnd_rtl() {
        /* Expected BottomEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntPxPosition(IntPx(0), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.BottomEnd,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomEnd))
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
    fun popup_correctPosition_alignmentBottomCenter_rtl() {
        /* Expected BottomCenter Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntPxPosition(IntPx(20), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.BottomCenter,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomCenter))
    }

    @Test
    fun popup_correctPosition_alignmentBottomStart() {
        /* Expected BottomStart Position
           x = offset.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntPxPosition(IntPx(10), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.BottomStart, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomStart))
    }

    @Test
    fun popup_correctPosition_alignmentBottomStart_rtl() {
        /* Expected BottomStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntPxPosition(IntPx(50), IntPx(90))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.BottomStart,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomStart))
    }

    @Test
    fun popup_correctPosition_alignmentCenterStart() {
        /* Expected CenterStart Position
           x = offset.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntPxPosition(IntPx(10), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(alignment = Alignment.CenterStart, measureLatch = measureLatch)

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterStart))
    }

    @Test
    fun popup_correctPosition_alignmentCenterStart_rtl() {
        /* Expected CenterStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntPxPosition(IntPx(50), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.CenterStart,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterStart))
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
    fun popup_correctPosition_alignmentCenter_rtl() {
        /* Expected Center Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntPxPosition(IntPx(20), IntPx(50))
        val measureLatch = CountDownLatch(1)

        createPopupWithAlignmentRule(
            modifier = Modifier.rtl,
            alignment = Alignment.Center,
            measureLatch = measureLatch
        )

        measureLatch.await(1, TimeUnit.SECONDS)
        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenter))
    }

    @Test
    fun popup_calculateGlobalPositionTopStart() {
        /* Expected TopStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopStart = IntPxPosition(IntPx(60), IntPx(60))

        val positionTopStart =
            AlignmentOffsetPositionProvider(Alignment.TopStart, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionTopStart).isEqualTo(expectedPositionTopStart)
    }

    @Test
    fun popup_calculateGlobalPositionTopStart_rtl() {
        /* Expected TopStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopStart = IntPxPosition(IntPx(100), IntPx(60))

        val positionTopStart =
            AlignmentOffsetPositionProvider(Alignment.TopStart, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionTopStart).isEqualTo(expectedPositionTopStart)
    }

    @Test
    fun popup_calculateGlobalPositionTopCenter() {
        /* Expected TopCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopCenter = IntPxPosition(IntPx(90), IntPx(60))

        val positionTopCenter =
            AlignmentOffsetPositionProvider(Alignment.TopCenter, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionTopCenter).isEqualTo(expectedPositionTopCenter)
    }

    @Test
    fun popup_calculateGlobalPositionTopCenter_rtl() {
        /* Expected TopCenter Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopCenter = IntPxPosition(IntPx(70), IntPx(60))

        val positionTopCenter =
            AlignmentOffsetPositionProvider(Alignment.TopCenter, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionTopCenter).isEqualTo(expectedPositionTopCenter)
    }

    @Test
    fun popup_calculateGlobalPositionTopEnd() {
        /* Expected TopEnd Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopEnd = IntPxPosition(IntPx(120), IntPx(60))

        val positionTopEnd =
            AlignmentOffsetPositionProvider(Alignment.TopEnd, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionTopEnd).isEqualTo(expectedPositionTopEnd)
    }

    @Test
    fun popup_calculateGlobalPositionTopEnd_rtl() {
        /* Expected TopEnd Position
           x = parentGlobalPosition.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopEnd = IntPxPosition(IntPx(40), IntPx(60))

        val positionTopEnd =
            AlignmentOffsetPositionProvider(Alignment.TopEnd, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionTopEnd).isEqualTo(expectedPositionTopEnd)
    }

    @Test
    fun popup_calculateGlobalPositionCenterEnd() {
        /* Expected CenterEnd Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntPxPosition(IntPx(120), IntPx(100))

        val positionCenterEnd =
            AlignmentOffsetPositionProvider(Alignment.CenterEnd, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionCenterEnd).isEqualTo(expectedPositionCenterEnd)
    }

    @Test
    fun popup_calculateGlobalPositionCenterEnd_rtl() {
        /* Expected CenterEnd Position
           x = parentGlobalPosition.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntPxPosition(IntPx(40), IntPx(100))

        val positionCenterEnd =
            AlignmentOffsetPositionProvider(Alignment.CenterEnd, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionCenterEnd).isEqualTo(expectedPositionCenterEnd)
    }

    @Test
    fun popup_calculateGlobalPositionBottomEnd() {
        /* Expected BottomEnd Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntPxPosition(IntPx(120), IntPx(140))

        val positionBottomEnd =
            AlignmentOffsetPositionProvider(Alignment.BottomEnd, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionBottomEnd).isEqualTo(expectedPositionBottomEnd)
    }

    @Test
    fun popup_calculateGlobalPositionBottomEnd_rtl() {
        /* Expected BottomEnd Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntPxPosition(IntPx(40), IntPx(140))

        val positionBottomEnd =
            AlignmentOffsetPositionProvider(Alignment.BottomEnd, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionBottomEnd).isEqualTo(expectedPositionBottomEnd)
    }

    @Test
    fun popup_calculateGlobalPositionBottomCenter() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntPxPosition(IntPx(90), IntPx(140))

        val positionBottomCenter =
            AlignmentOffsetPositionProvider(Alignment.BottomCenter, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionBottomCenter).isEqualTo(expectedPositionBottomCenter)
    }

    @Test
    fun popup_calculateGlobalPositionBottomCenter_rtl() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntPxPosition(IntPx(70), IntPx(140))

        val positionBottomCenter =
            AlignmentOffsetPositionProvider(Alignment.BottomCenter, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionBottomCenter).isEqualTo(expectedPositionBottomCenter)
    }

    @Test
    fun popup_calculateGlobalPositionBottomStart() {
        /* Expected BottomStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntPxPosition(IntPx(60), IntPx(140))

        val positionBottomStart =
            AlignmentOffsetPositionProvider(Alignment.BottomStart, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionBottomStart).isEqualTo(expectedPositionBottomStart)
    }

    @Test
    fun popup_calculateGlobalPositionBottomStart_rtl() {
        /* Expected BottomStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntPxPosition(IntPx(100), IntPx(140))

        val positionBottomStart =
            AlignmentOffsetPositionProvider(Alignment.BottomStart, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionBottomStart).isEqualTo(expectedPositionBottomStart)
    }

    @Test
    fun popup_calculateGlobalPositionCenterStart() {
        /* Expected CenterStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntPxPosition(IntPx(60), IntPx(100))

        val positionCenterStart =
            AlignmentOffsetPositionProvider(Alignment.CenterStart, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionCenterStart).isEqualTo(expectedPositionCenterStart)
    }

    @Test
    fun popup_calculateGlobalPositionCenterStart_rtl() {
        /* Expected CenterStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntPxPosition(IntPx(100), IntPx(100))

        val positionCenterStart =
            AlignmentOffsetPositionProvider(Alignment.CenterStart, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionCenterStart).isEqualTo(expectedPositionCenterStart)
    }

    @Test
    fun popup_calculateGlobalPositionCenter() {
        /* Expected Center Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntPxPosition(IntPx(90), IntPx(100))

        val positionCenter =
            AlignmentOffsetPositionProvider(Alignment.Center, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionCenter).isEqualTo(expectedPositionCenter)
    }

    @Test
    fun popup_calculateGlobalPositionCenter_rtl() {
        /* Expected Center Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntPxPosition(IntPx(70), IntPx(100))

        val positionCenter =
            AlignmentOffsetPositionProvider(Alignment.Center, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionCenter).isEqualTo(expectedPositionCenter)
    }

    @Test
    fun popup_hasViewTreeLifecycleOwner() {
        composeTestRule.setContent {
            PopupTestTag(testTag) {
                Popup {}
            }
        }

        Espresso.onView(instanceOf(Owner::class.java))
            .inRoot(PopupLayoutMatcher())
            .check(matches(object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description?) {
                    description?.appendText("ViewTreeLifecycleOwner.get(view) != null")
                }

                override fun matchesSafely(item: View): Boolean {
                    return ViewTreeLifecycleOwner.get(item) != null
                }
            }))
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionStart() {
        /* Expected Dropdown Start Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionLeft = IntPxPosition(IntPx(60), IntPx(160))

        val positionLeft =
            DropdownPositionProvider(DropDownAlignment.Start, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionLeft).isEqualTo(expectedPositionLeft)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionStart_rtl() {
        /* Expected Dropdown Start Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPosition = IntPxPosition(IntPx(100), IntPx(160))

        val positionLeft =
            DropdownPositionProvider(DropDownAlignment.Start, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionLeft).isEqualTo(expectedPosition)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionEnd() {
        /* Expected Dropdown End Position
           x = parentGlobalPosition.x + offset.x + parentSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionRight = IntPxPosition(IntPx(160), IntPx(160))

        val positionRight =
            DropdownPositionProvider(DropDownAlignment.End, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionRight).isEqualTo(expectedPositionRight)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionEnd_rtl() {
        /* Expected Dropdown End Position
           x = parentGlobalPosition.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionRight = IntPxPosition(IntPx(0), IntPx(160))

        val positionRight =
            DropdownPositionProvider(DropDownAlignment.End, offset).calculatePosition(
                parentGlobalPosition,
                parentSize,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionRight).isEqualTo(expectedPositionRight)
    }

    private fun matchesAndroidComposeView(): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return (item is Owner)
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
                description?.appendText(
                    "with expected position: $expectedPosition but position found: $positionFound"
                )
            }
        }
    }
}

@Composable
private fun TestAlign(children: @Composable () -> Unit) {
    Layout(children) { measurables, constraints, _ ->
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
                val position = Alignment.TopStart.align(
                    IntPxSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}
