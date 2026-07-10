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

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GOLDEN_DIRECTORY
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.samples.GlimmerHorizontalPagerSample
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.captureToImage
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class GlimmerHorizontalPagerScreenshotTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun horizontalPager_initialState() {
        rule.setGlimmerThemeContent { GlimmerHorizontalPagerSample() }

        assertRootAgainstGolden("horizontalPager_initialState")
    }

    @Test
    fun horizontalPager_scroll20Percent() {
        rule.setGlimmerThemeContent { GlimmerHorizontalPagerSample() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.2f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_scroll20Percent")
    }

    @Test
    fun horizontalPager_scroll40Percent() {
        rule.setGlimmerThemeContent { GlimmerHorizontalPagerSample() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.4f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_scroll40Percent")
    }

    @Test
    fun horizontalPager_scroll60Percent() {
        rule.setGlimmerThemeContent { GlimmerHorizontalPagerSample() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.6f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_scroll60Percent")
    }

    @Test
    fun horizontalPager_scroll80Percent() {
        rule.setGlimmerThemeContent { GlimmerHorizontalPagerSample() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.8f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_scroll80Percent")
    }

    @Test
    fun horizontalPager_fullScroll() {
        rule.setGlimmerThemeContent { GlimmerHorizontalPagerSample() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = -width.toFloat(), y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_fullScroll")
    }

    @Test
    fun horizontalPager_varyingSizePages_initialState() {
        rule.setGlimmerThemeContent { HorizontalGlimmerPagerWithVaryingSizePages() }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_varyingSizePages_initialState")
    }

    @Test
    fun horizontalPager_varyingSizePages_scroll20Percent() {
        rule.setGlimmerThemeContent { HorizontalGlimmerPagerWithVaryingSizePages() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.2f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_varyingSizePages_scroll20Percent")
    }

    @Test
    fun horizontalPager_varyingSizePages_scroll40Percent() {
        rule.setGlimmerThemeContent { HorizontalGlimmerPagerWithVaryingSizePages() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.4f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_varyingSizePages_scroll40Percent")
    }

    @Test
    fun horizontalPager_varyingSizePages_scroll60Percent() {
        rule.setGlimmerThemeContent { HorizontalGlimmerPagerWithVaryingSizePages() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.6f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_varyingSizePages_scroll60Percent")
    }

    @Test
    fun horizontalPager_varyingSizePages_scroll80Percent() {
        rule.setGlimmerThemeContent { HorizontalGlimmerPagerWithVaryingSizePages() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = width * -0.8f, y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_varyingSizePages_scroll80Percent")
    }

    @Test
    fun horizontalPager_varyingSizePages_fullScroll() {
        rule.setGlimmerThemeContent { HorizontalGlimmerPagerWithVaryingSizePages() }

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = -width.toFloat(), y = centerY))
        }

        rule.waitForIdle()

        assertRootAgainstGolden("horizontalPager_varyingSizePages_fullScroll")
    }

    @Composable
    private fun HorizontalGlimmerPagerWithVaryingSizePages() {
        val pagerState = rememberGlimmerPagerState { 3 }
        GlimmerHorizontalPager(
            state = pagerState,
            modifier = Modifier.height(330.dp).fillMaxWidth(),
        ) { page ->
            Card(modifier = Modifier.fillMaxHeight(if (page % 2 == 0) 0.5f else 1f)) {
                Text("Page $page")
            }
        }
    }

    private fun assertRootAgainstGolden(goldenName: String) {
        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName, MSSIMMatcher(0.995))
    }
}
