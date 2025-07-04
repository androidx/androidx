/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class PagerScaffoldTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun horizontal_pager_scaffold_is_composed() {
        rule.setContentWithTheme { TestHorizontalPagerScaffold() }

        rule.onNodeWithTag(PAGER_SCAFFOLD_TAG).assertExists()
    }

    @Test
    fun vertical_pager_scaffold_is_composed() {
        rule.setContentWithTheme { TestVerticalPagerScaffold() }

        rule.onNodeWithTag(PAGER_SCAFFOLD_TAG).assertExists()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun horizontal_page_indicator_does_not_fade_out_when_animation_spec_set_to_null() {
        val pageIndicatorColor = Color.Blue

        create_pager_scaffold_and_swipe_one_page(
            orientation = Orientation.Horizontal,
            pageIndicatorColor = pageIndicatorColor,
            pageIndicatorAnimationSpec = null,
        )

        wait_for_page_indicator_timeout_and_assert_page_indicator_visibility(
            pageIndicatorColor = pageIndicatorColor,
            assertVisible = true,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun vertical_page_indicator_does_not_fade_out_when_animation_spec_set_to_null() {
        val pageIndicatorColor = Color.Blue

        create_pager_scaffold_and_swipe_one_page(
            orientation = Orientation.Vertical,
            pageIndicatorColor = pageIndicatorColor,
            pageIndicatorAnimationSpec = null,
        )

        wait_for_page_indicator_timeout_and_assert_page_indicator_visibility(
            pageIndicatorColor = pageIndicatorColor,
            assertVisible = true,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun horizontal_page_indicator_fades_out_when_animation_spec_set() {
        val pageIndicatorColor = Color.Blue

        create_pager_scaffold_and_swipe_one_page(
            orientation = Orientation.Horizontal,
            pageIndicatorColor = pageIndicatorColor,
            pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimationSpec,
        )

        wait_for_page_indicator_timeout_and_assert_page_indicator_visibility(
            pageIndicatorColor = pageIndicatorColor,
            assertVisible = false,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun vertical_page_indicator_fades_out_when_animation_spec_set() {
        val pageIndicatorColor = Color.Blue

        create_pager_scaffold_and_swipe_one_page(
            orientation = Orientation.Vertical,
            pageIndicatorColor = pageIndicatorColor,
            pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimationSpec,
        )

        wait_for_page_indicator_timeout_and_assert_page_indicator_visibility(
            pageIndicatorColor = pageIndicatorColor,
            assertVisible = false,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun edge_button_doesnt_disappear() {
        val TLC_TAG = "TLC_TAG"
        val EHB_TAG = "EHB_TAG"
        val EHB_COLOR = Color.Red
        val pageCount = 3
        val pagerState = PagerState(currentPage = 0, currentPageOffsetFraction = 0f) { pageCount }
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            HorizontalPagerScaffold(
                pagerState = pagerState,
                pageIndicator = {},
                modifier = Modifier.size(300.dp),
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex
                    ->
                    val scrollState = rememberTransformingLazyColumnState()
                    val overscrollEffect = rememberOverscrollEffect()

                    ScreenScaffold(
                        scrollState = scrollState,
                        overscrollEffect = overscrollEffect,
                        modifier = Modifier.fillMaxSize(),
                        edgeButton = {
                            EdgeButton(
                                onClick = {},
                                modifier = Modifier.testTag(EHB_TAG + pageIndex.toString()),
                                colors = ButtonDefaults.buttonColors(containerColor = EHB_COLOR),
                            ) {
                                Text("Edge Button")
                            }
                        },
                    ) { paddingValuesFromScaffold ->
                        TransformingLazyColumn(
                            state = scrollState,
                            contentPadding = paddingValuesFromScaffold,
                            overscrollEffect = overscrollEffect?.withoutVisualEffect(),
                            modifier =
                                Modifier.fillMaxSize().testTag(TLC_TAG + pageIndex.toString()),
                        ) {
                            items(20) { Text("Item #$it") }
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag(TLC_TAG + "0").performTouchInput { repeat(10) { swipeUp() } }

        // Edge Button should be visible after scroll
        rule.onNodeWithTag(EHB_TAG + "0").captureToImage().assertContainsColor(EHB_COLOR)

        // Go to the second page and back
        rule.runOnIdle { scope.launch { pagerState.scrollToPage(1) } }
        rule.runOnIdle { scope.launch { pagerState.scrollToPage(0) } }

        // Edge Button should be visible again
        rule.onNodeWithTag(EHB_TAG + "0").captureToImage().assertContainsColor(EHB_COLOR)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun create_pager_scaffold_and_swipe_one_page(
        orientation: Orientation,
        pageIndicatorColor: Color,
        pageIndicatorAnimationSpec: AnimationSpec<Float>?,
    ) {
        rule.setContentWithTheme {
            if (orientation == Orientation.Horizontal) {
                TestHorizontalPagerScaffold(
                    pageIndicatorColor = pageIndicatorColor,
                    pageIndicatorAnimationSpec = pageIndicatorAnimationSpec,
                )
            } else {
                TestVerticalPagerScaffold(
                    pageIndicatorColor = pageIndicatorColor,
                    pageIndicatorAnimationSpec = pageIndicatorAnimationSpec,
                )
            }
        }

        // Page indicator visible when pager scaffold first composed
        rule
            .onNodeWithTag(PAGER_SCAFFOLD_TAG)
            .captureToImage()
            .assertContainsColor(pageIndicatorColor)

        if (orientation == Orientation.Horizontal) {
            rule.onNodeWithTag(PAGER_SCAFFOLD_TAG).performTouchInput { swipeLeft() }
        } else {
            rule.onNodeWithTag(PAGER_SCAFFOLD_TAG).performTouchInput { swipeDown() }
        }

        // Page indicator still visible immediately after swiping
        rule
            .onNodeWithTag(PAGER_SCAFFOLD_TAG)
            .captureToImage()
            .assertContainsColor(pageIndicatorColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun wait_for_page_indicator_timeout_and_assert_page_indicator_visibility(
        pageIndicatorColor: Color,
        assertVisible: Boolean,
    ) {
        // After a 2500 delay, the scroll indicator is animated away. Allow a little longer for the
        // animation to complete.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(4000)

        if (assertVisible) {
            rule
                .onNodeWithTag(PAGER_SCAFFOLD_TAG)
                .captureToImage()
                .assertContainsColor(pageIndicatorColor)
        } else {
            rule
                .onNodeWithTag(PAGER_SCAFFOLD_TAG)
                .captureToImage()
                .assertDoesNotContainColor(pageIndicatorColor)
        }
    }

    @Composable
    private fun TestHorizontalPagerScaffold(
        pageIndicatorColor: Color = Color.Black,
        pageIndicatorAnimationSpec: AnimationSpec<Float>? = null,
    ) {
        AppScaffold {
            val pagerState = rememberPagerState(pageCount = { 10 })

            HorizontalPagerScaffold(
                modifier = Modifier.testTag(PAGER_SCAFFOLD_TAG),
                pagerState = pagerState,
                pageIndicator = {
                    HorizontalPageIndicator(
                        pagerState = pagerState,
                        backgroundColor = pageIndicatorColor,
                    )
                },
                pageIndicatorAnimationSpec = pageIndicatorAnimationSpec,
            ) {
                HorizontalPager(
                    state = pagerState,
                    flingBehavior =
                        PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                ) { page ->
                    AnimatedPage(pageIndex = page, pagerState = pagerState) {
                        ScreenScaffold {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Page $page")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TestVerticalPagerScaffold(
        pageIndicatorColor: Color = Color.Black,
        pageIndicatorAnimationSpec: AnimationSpec<Float>? = null,
    ) {
        AppScaffold {
            val pagerState = rememberPagerState(pageCount = { 10 })

            VerticalPagerScaffold(
                modifier = Modifier.testTag(PAGER_SCAFFOLD_TAG),
                pagerState = pagerState,
                pageIndicator = {
                    VerticalPageIndicator(
                        pagerState = pagerState,
                        backgroundColor = pageIndicatorColor,
                    )
                },
                pageIndicatorAnimationSpec = pageIndicatorAnimationSpec,
            ) {
                VerticalPager(
                    state = pagerState,
                    flingBehavior =
                        PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                ) { page ->
                    AnimatedPage(pageIndex = page, pagerState = pagerState) {
                        ScreenScaffold {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Page $page")
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val PAGER_SCAFFOLD_TAG = "PagerScaffoldTag"
