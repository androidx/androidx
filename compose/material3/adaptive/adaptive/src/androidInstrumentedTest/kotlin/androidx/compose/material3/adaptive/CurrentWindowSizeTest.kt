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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CurrentWindowSizeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun test_currentWindowSize() {
        var actualWindowSize: IntSize = IntSize.Zero

        val mockWindowSize = mutableStateOf(MockWindowSize1)

        rule.setContent {
            CompositionLocalProvider(LocalWindowInfo provides MockWindowInfo(mockWindowSize)) {
                actualWindowSize = currentWindowSize()
            }
        }

        rule.runOnIdle { assertThat(actualWindowSize).isEqualTo(MockWindowSize1) }

        mockWindowSize.value = MockWindowSize2

        rule.runOnIdle { assertThat(actualWindowSize).isEqualTo(MockWindowSize2) }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Test
    fun test_currentWindowDpSize() {
        var actualWindowSize: DpSize = DpSize.Zero

        val mockWindowSize = mutableStateOf(MockWindowSize1)

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides MockWindowDensity,
                LocalWindowInfo provides MockWindowInfo(mockWindowSize),
            ) {
                actualWindowSize = currentWindowDpSize()
            }
        }

        rule.runOnIdle {
            assertThat(actualWindowSize)
                .isEqualTo(with(MockWindowDensity) { MockWindowSize1.toSize().toDpSize() })
        }

        mockWindowSize.value = MockWindowSize2

        rule.runOnIdle {
            assertThat(actualWindowSize)
                .isEqualTo(with(MockWindowDensity) { MockWindowSize2.toSize().toDpSize() })
        }
    }
}

internal class MockWindowInfo(private val mockWindowSize: State<IntSize>) : WindowInfo {
    override val isWindowFocused: Boolean = false
    override val containerSize: IntSize
        get() = mockWindowSize.value
}

private val MockWindowSize1 = IntSize(1000, 600)
private val MockWindowSize2 = IntSize(800, 400)
private val MockWindowDensity = Density(2.5F)
