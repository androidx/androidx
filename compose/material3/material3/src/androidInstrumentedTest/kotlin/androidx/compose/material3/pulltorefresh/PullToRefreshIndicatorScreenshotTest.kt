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

package androidx.compose.material3.pulltorefresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularAnimationProgressDuration
import androidx.compose.material3.CircularIndicatorDiameter
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.GOLDEN_MATERIAL3
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.setMaterialContent
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class PullRefreshIndicatorScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val testTag = "PullRefresh"
    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)

    @Test
    fun pullRefreshIndicator_refreshing() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(testTag)) {
                PullToRefreshDefaults.Indicator(
                    state = mockState,
                    isRefreshing = true,
                    maxDistance = CircularIndicatorDiameter,
                )
            }
        }
        rule.mainClock.advanceTimeBy(CircularAnimationProgressDuration / 3L * 4L)

        assertAgainstGolden("pullRefreshIndicator_${scheme.name}_refreshing")
    }

    @Test
    fun pullRefreshIndicator_notRefreshing() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(testTag)) {
                PullToRefreshDefaults.Indicator(
                    state = mockState,
                    maxDistance = CircularIndicatorDiameter,
                    isRefreshing = false,
                )
            }
        }

        assertAgainstGolden("pullRefreshIndicator_${scheme.name}_progress")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(testTag).captureToImage().assertAgainstGolden(screenshotRule, goldenName)
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    private val mockState =
        object : PullToRefreshState {
            override val distanceFraction: Float
                get() = 1f

            override val isAnimating: Boolean
                get() = false

            override suspend fun animateToThreshold() {}

            override suspend fun animateToHidden() {}

            override suspend fun snapTo(targetValue: Float) {}
        }
}
