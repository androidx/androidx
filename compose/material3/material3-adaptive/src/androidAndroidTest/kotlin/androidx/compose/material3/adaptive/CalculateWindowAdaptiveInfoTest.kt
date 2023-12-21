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

package androidx.compose.material3.adaptive

import android.content.res.Configuration
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
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
class CalculateWindowAdaptiveInfoTest {
    private val composeRule = createComposeRule()
    private val layoutInfoRule = WindowLayoutInfoPublisherRule()

    @get:Rule
    val testRule: TestRule
    init {
        testRule = RuleChain.outerRule(layoutInfoRule).around(composeRule)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun test_calculateWindowAdaptiveInfo() {
        lateinit var actualAdaptiveInfo: WindowAdaptiveInfo
        val mockWindowSize = mutableStateOf(MockWindowSize1)
        WindowMetricsCalculator.overrideDecorator(
            MockWindowMetricsCalculatorDecorator(mockWindowSize)
        )

        composeRule.setContent {
            val testConfiguration = Configuration(LocalConfiguration.current)
            testConfiguration.screenWidthDp = mockWindowSize.value.width
            testConfiguration.screenHeightDp = mockWindowSize.value.height
            CompositionLocalProvider(
                LocalDensity provides MockDensity,
                LocalConfiguration provides testConfiguration
            ) {
                actualAdaptiveInfo = calculateWindowAdaptiveInfo()
            }
        }

        layoutInfoRule.overrideWindowLayoutInfo(
            WindowLayoutInfo(MockFoldingFeatures1)
        )

        composeRule.runOnIdle {
            assertThat(actualAdaptiveInfo.windowSizeClass).isEqualTo(
                WindowSizeClass.calculateFromSize(MockWindowSize1.toSize(), MockDensity))
            assertThat(actualAdaptiveInfo.posture).isEqualTo(calculatePosture(MockFoldingFeatures1))
        }

        layoutInfoRule.overrideWindowLayoutInfo(
            WindowLayoutInfo(MockFoldingFeatures2)
        )
        mockWindowSize.value = MockWindowSize2

        composeRule.runOnIdle {
            assertThat(actualAdaptiveInfo.windowSizeClass).isEqualTo(
                WindowSizeClass.calculateFromSize(MockWindowSize2.toSize(), MockDensity))
            assertThat(actualAdaptiveInfo.posture).isEqualTo(calculatePosture(MockFoldingFeatures2))
        }
    }

    companion object {
        private val MockFoldingFeatures1 = listOf(
            MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL),
            MockFoldingFeature(orientation = FoldingFeature.Orientation.VERTICAL),
            MockFoldingFeature(orientation = FoldingFeature.Orientation.HORIZONTAL)
        )

        private val MockFoldingFeatures2 = listOf(
            MockFoldingFeature(
                isSeparating = false,
                orientation = FoldingFeature.Orientation.HORIZONTAL,
                state = FoldingFeature.State.FLAT
            ),
        )

        private val MockWindowSize1 = IntSize(400, 800)
        private val MockWindowSize2 = IntSize(800, 400)

        private val MockDensity = Density(1f, 1f)
    }
}