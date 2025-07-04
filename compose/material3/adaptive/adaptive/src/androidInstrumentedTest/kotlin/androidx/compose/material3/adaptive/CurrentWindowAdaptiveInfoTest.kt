/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class CurrentWindowAdaptiveInfoTest {
    private val composeRule = createComposeRule()
    private val layoutInfoRule = WindowLayoutInfoPublisherRule()

    @get:Rule val testRule: TestRule = RuleChain.outerRule(layoutInfoRule).around(composeRule)

    @Test
    fun test_currentWindowAdaptiveInfo() {
        lateinit var actualAdaptiveInfo: WindowAdaptiveInfo
        val mockWindowSize = mutableStateOf(MockWindowSize1)

        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides MockDensity,
                LocalWindowInfo provides MockWindowInfo(mockWindowSize),
            ) {
                actualAdaptiveInfo = currentWindowAdaptiveInfo()
            }
        }

        layoutInfoRule.overrideWindowLayoutInfo(WindowLayoutInfo(MockFoldingFeatures1))

        composeRule.runOnIdle {
            val mockSize = with(MockDensity) { MockWindowSize1.toSize().toDpSize() }
            assertThat(actualAdaptiveInfo.windowSizeClass)
                .isEqualTo(WindowSizeClass.computeFromDpSize(mockSize))
            assertThat(actualAdaptiveInfo.windowPosture)
                .isEqualTo(calculatePosture(MockFoldingFeatures1))
        }

        layoutInfoRule.overrideWindowLayoutInfo(WindowLayoutInfo(MockFoldingFeatures2))
        mockWindowSize.value = MockWindowSize2

        composeRule.runOnIdle {
            val mockSize = with(MockDensity) { MockWindowSize2.toSize().toDpSize() }
            assertThat(actualAdaptiveInfo.windowSizeClass)
                .isEqualTo(WindowSizeClass.computeFromDpSize(mockSize))
            assertThat(actualAdaptiveInfo.windowPosture)
                .isEqualTo(calculatePosture(MockFoldingFeatures2))
        }
    }

    @Test
    fun test_currentWindowAdaptiveInfo_withLargeAndXLargeWidthSupport() {
        lateinit var actualAdaptiveInfo: WindowAdaptiveInfo
        val mockWindowSize = mutableStateOf(MockWindowSizeXL)

        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides MockDensity,
                LocalWindowInfo provides MockWindowInfo(mockWindowSize),
            ) {
                actualAdaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
            }
        }

        layoutInfoRule.overrideWindowLayoutInfo(WindowLayoutInfo(MockFoldingFeatures1))

        composeRule.runOnIdle {
            val mockSize = with(MockDensity) { MockWindowSizeXL.toSize().toDpSize() }
            assertThat(actualAdaptiveInfo.windowSizeClass)
                .isEqualTo(WindowSizeClass.computeFromDpSizeV2(mockSize))
            assertThat(actualAdaptiveInfo.windowPosture)
                .isEqualTo(calculatePosture(MockFoldingFeatures1))
        }

        layoutInfoRule.overrideWindowLayoutInfo(WindowLayoutInfo(MockFoldingFeatures2))
        mockWindowSize.value = MockWindowSizeL

        composeRule.runOnIdle {
            val mockSize = with(MockDensity) { MockWindowSizeL.toSize().toDpSize() }
            assertThat(actualAdaptiveInfo.windowSizeClass)
                .isEqualTo(WindowSizeClass.computeFromDpSizeV2(mockSize))
            assertThat(actualAdaptiveInfo.windowPosture)
                .isEqualTo(calculatePosture(MockFoldingFeatures2))
        }
    }

    companion object {
        private val MockFoldingFeatures1 =
            listOf(
                MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL),
                MockFoldingFeature(orientation = FoldingFeature.Orientation.VERTICAL),
                MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL),
            )

        private val MockFoldingFeatures2 =
            listOf(
                MockFoldingFeature(
                    isSeparating = false,
                    orientation = FoldingFeature.Orientation.HORIZONTAL,
                    state = FoldingFeature.State.FLAT,
                )
            )

        private val MockWindowSize1 = IntSize(400, 800)
        private val MockWindowSize2 = IntSize(800, 400)

        private val MockWindowSizeL = IntSize(1300, 800)
        private val MockWindowSizeXL = IntSize(1800, 400)

        private val MockDensity = Density(1f, 1f)
    }
}
