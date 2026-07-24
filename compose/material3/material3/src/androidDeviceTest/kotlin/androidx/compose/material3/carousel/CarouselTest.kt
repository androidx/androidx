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

package androidx.compose.material3.carousel

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.setMaterialContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CarouselTest {

    private lateinit var carouselState: CarouselState

    @get:Rule val rule = createComposeRule()

    @Test
    fun carousel_horizontalScrollUpdatesState() {
        // Arrange
        createCarousel(orientation = Orientation.Horizontal)
        assertThat(carouselState.pagerState.currentPage).isEqualTo(0)

        // Act
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 1000f)
        }

        // Assert
        rule.runOnIdle { assertThat(carouselState.pagerState.currentPage).isNotEqualTo(0) }
    }

    @Test
    fun carousel_verticalScrollUpdatesState() {
        // Arrange
        createCarousel(orientation = Orientation.Vertical)
        assertThat(carouselState.pagerState.currentPage).isEqualTo(0)

        // Act
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(bottomCenter, topCenter, 1000f)
        }

        // Assert
        rule.runOnIdle { assertThat(carouselState.pagerState.currentPage).isNotEqualTo(0) }
    }

    @Test
    fun carousel_testInitialItem() {
        // Arrange
        createCarousel(initialItem = 5, orientation = Orientation.Horizontal)

        // Assert
        rule.runOnIdle { assertThat(carouselState.pagerState.currentPage).isEqualTo(5) }
    }

    @Test
    fun carousel_snapsToPage() {
        // Arrange
        createCarousel()

        // Act
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 1000f)
        }

        // Assert
        rule.runOnIdle {
            assertThat(carouselState.pagerState.currentPageOffsetFraction).isEqualTo(0)
        }
    }

    @Test
    fun uncontainedCarousel_doesntSnapToPage() {
        // Arrange
        createUncontainedCarousel()

        // Act
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 1000f)
        }

        // Assert
        rule.runOnIdle {
            assertThat(carouselState.pagerState.currentPageOffsetFraction).isNotEqualTo(0)
        }
    }

    @Test
    fun uncontainedCarousel_userScrollDisabled_doesNotScroll() {
        createCarousel(userScrollEnabled = false)
        assertThat(carouselState.pagerState.currentPage).isEqualTo(0)

        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 1000f)
        }

        rule.runOnIdle { assertThat(carouselState.pagerState.currentPage).isEqualTo(0) }
    }

    @Test
    fun carouselSingleAdvanceFling_capsScroll() {
        // Arrange
        createCarousel()
        assertThat(carouselState.pagerState.currentPage).isEqualTo(0)

        // Act
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 10000f)
        }

        // Assert
        rule.runOnIdle {
            // A swipe from the very right to very left should be capped at
            // the item right after the visible pages onscreen regardless of velocity
            assertThat(carouselState.pagerState.currentPage)
                .isLessThan(carouselState.pagerState.layoutInfo.visiblePagesInfo.size + 1)
        }
    }

    @Test
    fun carouselMultibrowseFling_ScrollsToEnd() {
        // Arrange
        createCarousel(
            flingBehavior = { state: CarouselState ->
                CarouselDefaults.multiBrowseFlingBehavior(state)
            }
        )
        assertThat(carouselState.pagerState.currentPage).isEqualTo(0)

        // Act
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 10000f)
        }

        // Assert
        rule.runOnIdle {
            // A swipe from the very right to very left at a high velocity should go beyond
            // first item after the visible pages as it's not capped
            assertThat(carouselState.pagerState.currentPage)
                .isGreaterThan(carouselState.pagerState.layoutInfo.visiblePagesInfo.size)
        }
    }

    @Test
    fun carousel_correctlyCalculatesMaxScrollOffsetWithItemSpacing() {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberCarouselState { 10 }.also { carouselState = it }
            val strategy =
                Strategy(
                    defaultKeylines =
                        keylineListOf(380f, 0f, CarouselAlignment.Start) {
                            add(10f, isAnchor = true)
                            add(186f)
                            add(122f)
                            add(56f)
                            add(10f, isAnchor = true)
                        },
                    availableSpace = 380f,
                    itemSpacing = 8f,
                    beforeContentPadding = 0f,
                    afterContentPadding = 0f,
                )

            // Max offset should only add item spacing between each item
            val expectedMaxScrollOffset = (186f * 10) + (8f * 9) - 380f

            assertThat(calculateMaxScrollOffset(state, strategy)).isEqualTo(expectedMaxScrollOffset)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun carousel_semanticsBoundsAreReportedCorrectly() {
        lateinit var androidView: View

        createCarousel(modifier = Modifier.width(300.dp).height(300.dp)) {
            androidView = LocalView.current
            Item(index = it)
        }

        // Nodes that are out of place
        val item1 = rule.onNodeWithTag("1").fetchSemanticsNode()
        val item2 = rule.onNodeWithTag("2").fetchSemanticsNode()
        val carouselNode = rule.onNodeWithTag(CarouselTestTag).fetchSemanticsNode()

        // The container width and the layout size of the items (which is always the large item
        // size).
        val containerWidth = carouselNode.size.width.toFloat()
        val itemMainAxisSize = item1.size.width.toFloat()

        // We calculate the expected width of the items after being clipped by the container.
        // Accessibility bounds in Compose are clipped by the parent container bounds, but they
        // currently do not respect the custom graphicsLayer mask clip.
        // Item 1 (index 1) starts at itemMainAxisSize and ends at itemMainAxisSize * 2.
        val expectedItem1Width =
            maxOf(0f, minOf(itemMainAxisSize * 2, containerWidth) - itemMainAxisSize)
        // Item 2 (index 2) starts at itemMainAxisSize * 2 and ends at itemMainAxisSize * 3.
        val expectedItem2Width =
            maxOf(0f, minOf(itemMainAxisSize * 3, containerWidth) - itemMainAxisSize * 2)

        rule.waitForIdle()
        // verify that the a11y sees the correct semantics node size
        rule.runOnUiThread {
            val item1NodeInfo =
                androidView.accessibilityNodeProvider.createAccessibilityNodeInfo(item1.id)
            val item2NodeInfo =
                androidView.accessibilityNodeProvider.createAccessibilityNodeInfo(item2.id)
            val bounds = Rect(-1, -1, -1, -1)

            item1NodeInfo?.getBoundsInScreen(bounds)
            assertThat(bounds.width().toFloat()).isWithin(2f).of(expectedItem1Width)

            item2NodeInfo?.getBoundsInScreen(bounds)
            assertThat(bounds.width().toFloat()).isWithin(2f).of(expectedItem2Width)
        }
    }

    @Test
    fun centeredHeroCarousel_horizontalScrollUpdatesState() {
        // Arrange
        createCenteredHeroCarousel()
        assertThat(carouselState.pagerState.currentPage).isEqualTo(0)

        // Act
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 1000f)
        }

        // Assert
        rule.runOnIdle { assertThat(carouselState.pagerState.currentPage).isNotEqualTo(0) }
    }

    @Test
    fun centeredHeroCarousel_snapsAndCentersFocalItemAfterScroll() {
        // Arrange
        createCenteredHeroCarousel(initialItem = 5)
        assertThat(carouselState.pagerState.currentPage).isEqualTo(5)

        // Act - Swipe left to scroll to next item
        rule.onNodeWithTag(CarouselTestTag).performTouchInput {
            swipeWithVelocity(centerRight, centerLeft, 1000f)
        }

        // Assert
        rule.waitForIdle()
        // 1. Assert it snapped
        assertThat(carouselState.pagerState.currentPageOffsetFraction).isEqualTo(0f)

        // 2. Assert the new focal item is centered
        val newCurrentPage = carouselState.pagerState.currentPage
        val carouselNode = rule.onNodeWithTag(CarouselTestTag).fetchSemanticsNode()
        val focalItemNode = rule.onNodeWithTag("$newCurrentPage").fetchSemanticsNode()

        val carouselCenter = carouselNode.boundsInRoot.left + carouselNode.boundsInRoot.width / 2f
        val focalItemCenter =
            focalItemNode.boundsInRoot.left + focalItemNode.boundsInRoot.width / 2f

        assertThat(focalItemCenter).isWithin(1f).of(carouselCenter)
    }

    @Test
    fun centeredHeroCarousel_emptyState_doesNotCrash() {
        createCenteredHeroCarousel(itemCount = { 0 })
        rule.waitForIdle()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun centeredHeroCarousel_singleItemState_isCentered() {
        createCenteredHeroCarousel(itemCount = { 1 })
        rule.waitForIdle()

        val carouselNode = rule.onNodeWithTag(CarouselTestTag).fetchSemanticsNode()
        val focalItemNode = rule.onNodeWithTag("0").fetchSemanticsNode()

        val carouselCenter = carouselNode.boundsInRoot.left + carouselNode.boundsInRoot.width / 2f
        val focalItemCenter =
            focalItemNode.boundsInRoot.left + focalItemNode.boundsInRoot.width / 2f

        assertThat(focalItemCenter).isWithin(1f).of(carouselCenter)
    }

    @Test
    fun centeredHeroCarousel_extremelySmallContainer_doesNotCrash() {
        createCenteredHeroCarousel(modifier = Modifier.width(10.dp).height(221.dp))
        rule.waitForIdle()
    }

    @Test
    fun centeredHeroCarousel_clickedScrolledItem_triggersClick() {
        var clickedIndex = -1
        createCenteredHeroCarousel(initialItem = 5) { index ->
            Box(
                modifier =
                    Modifier.fillMaxSize().background(Color.Blue).testTag("$index").clickable {
                        clickedIndex = index
                    }
            )
        }

        // Scroll to item 6
        rule.runOnIdle { kotlinx.coroutines.runBlocking { carouselState.scrollToItem(6) } }

        rule.waitForIdle()
        assertThat(carouselState.pagerState.currentPage).isEqualTo(6)

        // Click new focal item (6)
        rule.onNodeWithTag("6").performClick()
        rule.waitForIdle()
        assertThat(clickedIndex).isEqualTo(6)

        // Reset clickedIndex to verify the next click actually changes it
        clickedIndex = -1

        // Click scrolled-out item (5) at a coordinate we know is only inside 5 and not 6
        // Item 5 visible range is roughly [26px, 131px] (at 2.625f density), Item 6 starts at
        // 105px.
        // So 50px is safe and should only land on Item 5.
        rule.onRoot().performTouchInput { click(Offset(20.dp.toPx(), 110.dp.toPx())) }
        rule.waitForIdle()
        assertThat(clickedIndex).isEqualTo(5)
    }

    @Test
    fun centeredHeroCarousel_rtl_focalItemIsCentered() {
        rule.setMaterialContent(lightColorScheme()) {
            val state =
                rememberCarouselState(initialItem = 5, itemCount = { 10 }).also {
                    carouselState = it
                }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalCenteredHeroCarousel(
                    state = state,
                    modifier = Modifier.width(412.dp).height(221.dp).testTag(CarouselTestTag),
                ) { index ->
                    Item(index = index)
                }
            }
        }

        rule.waitForIdle()

        val carouselNode = rule.onNodeWithTag(CarouselTestTag).fetchSemanticsNode()
        val focalItemNode = rule.onNodeWithTag("5").fetchSemanticsNode()

        val carouselCenter = carouselNode.boundsInRoot.left + carouselNode.boundsInRoot.width / 2f
        val focalItemCenter =
            focalItemNode.boundsInRoot.left + focalItemNode.boundsInRoot.width / 2f

        assertThat(focalItemCenter).isWithin(1f).of(carouselCenter)
    }

    @Composable
    internal fun Item(index: Int) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Blue).testTag("$index").focusable(),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = index.toString())
        }
    }

    private fun createCarousel(
        initialItem: Int = 0,
        itemCount: () -> Int = { DefaultItemCount },
        modifier: Modifier = Modifier.width(412.dp).height(221.dp),
        orientation: Orientation = Orientation.Horizontal,
        flingBehavior: @Composable (CarouselState) -> TargetedFlingBehavior =
            @Composable { CarouselDefaults.singleAdvanceFlingBehavior(state = it) },
        userScrollEnabled: Boolean = true,
        content: @Composable CarouselItemScope.(item: Int) -> Unit = { Item(index = it) },
    ) {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberCarouselState(initialItem, itemCount).also { carouselState = it }
            val density = LocalDensity.current
            Carousel(
                state = state,
                orientation = orientation,
                keylineList = { availableSpace, itemSpacing ->
                    multiBrowseKeylineList(
                        density = density,
                        carouselMainAxisSize = availableSpace,
                        preferredItemSize = with(density) { 186.dp.toPx() },
                        itemSpacing = itemSpacing,
                        itemCount = itemCount.invoke(),
                    )
                },
                flingBehavior = flingBehavior(state),
                userScrollEnabled = userScrollEnabled,
                maxNonFocalVisibleItemCount = 2,
                modifier = modifier.testTag(CarouselTestTag),
                itemSpacing = 0.dp,
                contentPadding = PaddingValues(0.dp),
                content = content,
            )
        }
    }

    private fun createUncontainedCarousel(
        initialItem: Int = 0,
        itemCount: () -> Int = { DefaultItemCount },
        modifier: Modifier = Modifier.width(412.dp).height(221.dp),
        content: @Composable CarouselItemScope.(item: Int) -> Unit = { Item(index = it) },
    ) {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberCarouselState(initialItem, itemCount).also { carouselState = it }
            HorizontalUncontainedCarousel(
                state = state,
                itemWidth = 150.dp,
                modifier = modifier.testTag(CarouselTestTag),
                itemSpacing = 0.dp,
                content = content,
            )
        }
    }

    private fun createCenteredHeroCarousel(
        initialItem: Int = 0,
        itemCount: () -> Int = { DefaultItemCount },
        modifier: Modifier = Modifier.width(412.dp).height(221.dp),
        content: @Composable CarouselItemScope.(item: Int) -> Unit = { Item(index = it) },
    ) {
        rule.setMaterialContent(lightColorScheme()) {
            val state = rememberCarouselState(initialItem, itemCount).also { carouselState = it }
            HorizontalCenteredHeroCarousel(
                state = state,
                modifier = modifier.testTag(CarouselTestTag),
                content = content,
            )
        }
    }
}

internal const val DefaultItemCount = 10
internal const val CarouselTestTag = "carousel"
