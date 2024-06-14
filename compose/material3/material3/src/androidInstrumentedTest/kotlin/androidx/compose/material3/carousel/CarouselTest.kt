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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.setMaterialContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
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
@OptIn(ExperimentalMaterial3Api::class)
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
            },
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
                    afterContentPadding = 0f
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

        // verify that the a11y sees the correct semantics node size
        rule.runOnUiThread {
            val item1NodeInfo =
                androidView.accessibilityNodeProvider.createAccessibilityNodeInfo(item1.id)
            val item2NodeInfo =
                androidView.accessibilityNodeProvider.createAccessibilityNodeInfo(item2.id)
            val bounds = Rect(-1, -1, -1, -1)
            item1NodeInfo?.getBoundsInScreen(bounds)
            assertThat(bounds.width().toFloat()).isWithin(1f).of(item1.size.width.toFloat())
            item2NodeInfo?.getBoundsInScreen(bounds)
            assertThat(bounds.width().toFloat()).isWithin(1f).of(item2.size.width.toFloat())
        }
    }

    @Composable
    internal fun Item(index: Int) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Blue).testTag("$index").focusable(),
            contentAlignment = Alignment.Center
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
            @Composable {
                CarouselDefaults.singleAdvanceFlingBehavior(
                    state = it,
                )
            },
        content: @Composable CarouselItemScope.(item: Int) -> Unit = { Item(index = it) }
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
        content: @Composable CarouselItemScope.(item: Int) -> Unit = { Item(index = it) }
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
}

internal const val DefaultItemCount = 10
internal const val CarouselTestTag = "carousel"
