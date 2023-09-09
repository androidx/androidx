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

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import androidx.annotation.UiContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowMetricsCalculatorDecorator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class WindowSizeAsStateTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun test_windowSizeAsState() {
        lateinit var actualWindowSize: State<IntSize>

        val mockWindowSize = mutableStateOf(MockWindowSize1)
        WindowMetricsCalculator.overrideDecorator(
            MockWindowMetricsCalculatorDecorator(mockWindowSize)
        )

        rule.setContent {
            val testConfiguration = Configuration(LocalConfiguration.current)
            testConfiguration.screenWidthDp = mockWindowSize.value.width
            testConfiguration.screenHeightDp = mockWindowSize.value.height
            CompositionLocalProvider(LocalConfiguration provides testConfiguration) {
                actualWindowSize = windowSizeAsState()
            }
        }

        rule.runOnIdle {
            assertThat(actualWindowSize.value).isEqualTo(MockWindowSize1)
        }

        mockWindowSize.value = MockWindowSize2

        rule.runOnIdle {
            assertThat(actualWindowSize.value).isEqualTo(MockWindowSize2)
        }
    }

    companion object {
        val MockWindowSize1 = IntSize(1000, 600)
        val MockWindowSize2 = IntSize(800, 400)
    }
}

internal class MockWindowMetricsCalculatorDecorator(
    private val mockWindowSize: State<IntSize>
) : WindowMetricsCalculatorDecorator {
    override fun decorate(calculator: WindowMetricsCalculator): WindowMetricsCalculator {
        return MockWindowMetricsCalculator(mockWindowSize)
    }
}

internal class MockWindowMetricsCalculator(
    private val mockWindowSize: State<IntSize>
) : WindowMetricsCalculator {
    override fun computeCurrentWindowMetrics(activity: Activity): WindowMetrics {
        return WindowMetrics(
            Rect(0, 0, mockWindowSize.value.width, mockWindowSize.value.height)
        )
    }

    override fun computeMaximumWindowMetrics(activity: Activity): WindowMetrics {
        return computeCurrentWindowMetrics(activity)
    }

    override fun computeCurrentWindowMetrics(@UiContext context: Context): WindowMetrics {
        return WindowMetrics(
            Rect(0, 0, mockWindowSize.value.width, mockWindowSize.value.height)
        )
    }
}
