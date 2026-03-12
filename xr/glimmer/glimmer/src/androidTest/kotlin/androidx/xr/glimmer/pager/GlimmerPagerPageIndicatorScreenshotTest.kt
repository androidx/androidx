/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.xr.glimmer.GOLDEN_DIRECTORY
import androidx.xr.glimmer.assertRootAgainstGolden
import androidx.xr.glimmer.setGlimmerThemeContent
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class GlimmerPagerPageIndicatorScreenshotTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun pageIndicator_initialState() {
        rule.setGlimmerThemeContent {
            Box {
                val state = remember { GlimmerPagerState(pageCount = { 5 }) }
                GlimmerHorizontalPagerDefaults.PageIndicator(state = state)
            }
        }

        rule.assertRootAgainstGolden("pageIndicator_initialState", screenshotRule)
    }

    @Test
    fun pageIndicator_singlePage() {
        rule.setGlimmerThemeContent {
            Box {
                val state = remember { GlimmerPagerState(pageCount = { 1 }) }
                GlimmerHorizontalPagerDefaults.PageIndicator(state = state)
            }
        }

        rule.assertRootAgainstGolden("pageIndicator_singlePage", screenshotRule)
    }

    @Test
    fun pageIndicator_atLastPage() {
        val pageCount = 5
        rule.setGlimmerThemeContent {
            Box {
                val state = remember {
                    GlimmerPagerState(currentPage = pageCount - 1, pageCount = { pageCount })
                }
                GlimmerHorizontalPagerDefaults.PageIndicator(state = state)
            }
        }

        rule.assertRootAgainstGolden("pageIndicator_atLastPage", screenshotRule)
    }

    @Test
    fun pageIndicator_initialState_rtl() {
        val pageCount = 3
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box {
                    val state = remember {
                        GlimmerPagerState(currentPage = 0, pageCount = { pageCount })
                    }
                    GlimmerHorizontalPagerDefaults.PageIndicator(state = state)
                }
            }
        }

        rule.assertRootAgainstGolden("pageIndicator_initialState_rtl", screenshotRule)
    }

    @Test
    fun pageIndicator_pageTransition() {
        val pageCount = 3
        rule.setGlimmerThemeContent {
            Box {
                val state = remember {
                    GlimmerPagerState(
                        currentPage = 0,
                        currentPageOffsetFraction = 0.5f,
                        pageCount = { pageCount },
                    )
                }
                GlimmerHorizontalPagerDefaults.PageIndicator(state = state)
            }
        }

        rule.assertRootAgainstGolden("pageIndicator_pageTransition", screenshotRule)
    }

    @Test
    fun pageIndicator_largePageCount_middlePageSelected() {
        val pageCount = 20
        val selectedPage = 10

        rule.setGlimmerThemeContent {
            Box {
                val state = remember {
                    GlimmerPagerState(currentPage = selectedPage, pageCount = { pageCount })
                }
                GlimmerHorizontalPagerDefaults.PageIndicator(state = state)
            }
        }

        rule.assertRootAgainstGolden(
            "pageIndicator_largePageCount_middlePageSelected",
            screenshotRule,
        )
    }
}
