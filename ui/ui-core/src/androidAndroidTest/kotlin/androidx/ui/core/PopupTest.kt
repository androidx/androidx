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
import androidx.compose.Providers
import androidx.compose.ambientOf
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
import androidx.ui.test.runOnIdle
import androidx.ui.unit.IntBounds
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import androidx.ui.unit.height
import androidx.ui.unit.width
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

    private val parentBounds = IntBounds(50, 50, 150, 150)
    private val offset = IntOffset(10, 10)
    private val parentSize = IntSize(parentBounds.width, parentBounds.height)
    private val popupSize = IntSize(40, 20)

    private var composeViewAbsolutePosition = IntOffset(0, 0)

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
                val composeView = ViewAmbient.current
                val positionArray = IntArray(2)
                composeView.getLocationOnScreen(positionArray)
                composeViewAbsolutePosition = IntOffset(
                    positionArray[0],
                    positionArray[1]
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
        runOnIdle { }
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

        popupMatches(matchesSize(popupSize.width, popupSize.height))
    }

    @Test
    fun popup_correctPosition_alignmentTopStart() {
        /* Expected TopStart Position
           x = offset.x
           y = offset.y
        */
        val expectedPositionTopStart = IntOffset(10, 10)
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
        val expectedPositionTopStart = IntOffset(50, 10)
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
        val expectedPositionTopCenter = IntOffset(40, 10)
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
        val expectedPositionTopCenter = IntOffset(20, 10)
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
        val expectedPositionTopEnd = IntOffset(70, 10)
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
        val expectedPositionTopEnd = IntOffset(0, 10)
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
        val expectedPositionCenterEnd = IntOffset(70, 50)
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
        val expectedPositionCenterEnd = IntOffset(0, 50)
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
        val expectedPositionBottomEnd = IntOffset(70, 90)
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
        val expectedPositionBottomEnd = IntOffset(0, 90)
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
        val expectedPositionBottomCenter = IntOffset(40, 90)
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
        val expectedPositionBottomCenter = IntOffset(20, 90)
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
        val expectedPositionBottomStart = IntOffset(10, 90)
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
        val expectedPositionBottomStart = IntOffset(50, 90)
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
        val expectedPositionCenterStart = IntOffset(10, 50)
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
        val expectedPositionCenterStart = IntOffset(50, 50)
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
        val expectedPositionCenter = IntOffset(40, 50)
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
        val expectedPositionCenter = IntOffset(20, 50)
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
        val expectedPositionTopStart = IntOffset(60, 60)

        val positionTopStart =
            AlignmentOffsetPositionProvider(Alignment.TopStart, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionTopStart = IntOffset(100, 60)

        val positionTopStart =
            AlignmentOffsetPositionProvider(Alignment.TopStart, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionTopCenter = IntOffset(90, 60)

        val positionTopCenter =
            AlignmentOffsetPositionProvider(Alignment.TopCenter, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionTopCenter = IntOffset(70, 60)

        val positionTopCenter =
            AlignmentOffsetPositionProvider(Alignment.TopCenter, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionTopEnd = IntOffset(120, 60)

        val positionTopEnd =
            AlignmentOffsetPositionProvider(Alignment.TopEnd, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionTopEnd = IntOffset(40, 60)

        val positionTopEnd =
            AlignmentOffsetPositionProvider(Alignment.TopEnd, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionCenterEnd = IntOffset(120, 100)

        val positionCenterEnd =
            AlignmentOffsetPositionProvider(Alignment.CenterEnd, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionCenterEnd = IntOffset(40, 100)

        val positionCenterEnd =
            AlignmentOffsetPositionProvider(Alignment.CenterEnd, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionBottomEnd = IntOffset(120, 140)

        val positionBottomEnd =
            AlignmentOffsetPositionProvider(Alignment.BottomEnd, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionBottomEnd = IntOffset(40, 140)

        val positionBottomEnd =
            AlignmentOffsetPositionProvider(Alignment.BottomEnd, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionBottomCenter = IntOffset(90, 140)

        val positionBottomCenter =
            AlignmentOffsetPositionProvider(Alignment.BottomCenter, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionBottomCenter = IntOffset(70, 140)

        val positionBottomCenter =
            AlignmentOffsetPositionProvider(Alignment.BottomCenter, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionBottomStart = IntOffset(60, 140)

        val positionBottomStart =
            AlignmentOffsetPositionProvider(Alignment.BottomStart, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionBottomStart = IntOffset(100, 140)

        val positionBottomStart =
            AlignmentOffsetPositionProvider(Alignment.BottomStart, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionCenterStart = IntOffset(60, 100)

        val positionCenterStart =
            AlignmentOffsetPositionProvider(Alignment.CenterStart, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionCenterStart = IntOffset(100, 100)

        val positionCenterStart =
            AlignmentOffsetPositionProvider(Alignment.CenterStart, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionCenter = IntOffset(90, 100)

        val positionCenter =
            AlignmentOffsetPositionProvider(Alignment.Center, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionCenter = IntOffset(70, 100)

        val positionCenter =
            AlignmentOffsetPositionProvider(Alignment.Center, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionLeft = IntOffset(60, 160)

        val positionLeft =
            DropdownPositionProvider(DropDownAlignment.Start, offset).calculatePosition(
                parentBounds,
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
        val expectedPosition = IntOffset(100, 160)

        val positionLeft =
            DropdownPositionProvider(DropDownAlignment.Start, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionRight = IntOffset(160, 160)

        val positionRight =
            DropdownPositionProvider(DropDownAlignment.End, offset).calculatePosition(
                parentBounds,
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
        val expectedPositionRight = IntOffset(0, 160)

        val positionRight =
            DropdownPositionProvider(DropDownAlignment.End, offset).calculatePosition(
                parentBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionRight).isEqualTo(expectedPositionRight)
    }

    @Test
    fun popup_preservesAmbients() {
        val ambient = ambientOf<Float>()
        var value = 0f
        composeTestRule.setContent {
            Providers(ambient provides 1f) {
                Popup {
                    value = ambient.current
                }
            }
        }
        runOnIdle {
            Truth.assertThat(value).isEqualTo(1f)
        }
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

    private fun matchesPosition(expectedPosition: IntOffset): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            // (-1, -1) no position found
            var positionFound = IntOffset(-1, -1)

            override fun matchesSafely(item: View?): Boolean {
                val position = IntArray(2)
                item?.getLocationOnScreen(position)
                positionFound = IntOffset(position[0], position[1])

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
    Layout(children) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        // The child cannot be larger than our max constraints, but we ignore min constraints.
        val placeable = measurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))

        // The layout is as large as possible for bounded constraints,
        // or wrap content otherwise.
        val layoutWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            placeable?.width ?: constraints.minWidth
        }
        val layoutHeight = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            placeable?.height ?: constraints.minHeight
        }

        layout(layoutWidth, layoutHeight) {
            if (placeable != null) {
                val position = Alignment.TopStart.align(
                    IntSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}
