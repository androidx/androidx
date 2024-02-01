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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularIndicatorDiameter
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.GOLDEN_MATERIAL3
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.setMaterialContent
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
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

@OptIn(ExperimentalMaterial3Api::class)
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PullRefreshIndicatorScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val testTag = "PullRefresh"
    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)

    @Test
    fun pullRefreshIndicator_refreshing() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(testTag)) {
                val density = LocalDensity.current
                PullToRefreshContainer(
                    state = object : PullToRefreshState {
                        override val positionalThreshold: Float
                            get() = TODO("Not yet implemented")
                        override val progress = 0.0f
                        override val verticalOffset =
                            with(density) { CircularIndicatorDiameter.toPx() }
                        override var nestedScrollConnection: NestedScrollConnection
                            get() = TODO("Not yet implemented")
                            set(_) {}
                        override val isRefreshing = true
                        override fun startRefresh() {
                            TODO("Not yet implemented")
                        }

                        override fun endRefresh() {
                            TODO("Not yet implemented")
                        }
                    },
                )
            }
        }
        rule.mainClock.advanceTimeBy(500)
        assertAgainstGolden("pullRefreshIndicator_${scheme.name}_refreshing")
    }

    @Test
    fun pullRefreshIndicator_notRefreshing() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(testTag)) {
                val density = LocalDensity.current
                PullToRefreshContainer(
                    state = object : PullToRefreshState {
                        override val positionalThreshold: Float
                            get() = TODO("Not yet implemented")
                        override val progress = 1f
                        override val verticalOffset =
                            with(density) { CircularIndicatorDiameter.toPx() }
                        override var nestedScrollConnection: NestedScrollConnection
                            get() = TODO("Not yet implemented")
                            set(_) {}
                        override val isRefreshing = false
                        override fun startRefresh() {
                            TODO("Not yet implemented")
                        }

                        override fun endRefresh() {
                            TODO("Not yet implemented")
                        }
                    },
                )
            }
        }
        assertAgainstGolden("pullRefreshIndicator_${scheme.name}_progress")
    }
    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper("lightTheme", lightColorScheme()),
            ColorSchemeWrapper("darkTheme", darkColorScheme()),
        )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
