/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test.gesturescope

import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.doubleTapGestureFilter
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendDoubleClick
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.ClickableTestBox.defaultSize
import androidx.ui.test.util.ClickableTestBox.defaultTag
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
class SendDoubleClickWithoutArgumentsTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true
    )

    private val recordedDoubleClicks = mutableListOf<PxPosition>()
    private val expectedClickPosition = PxPosition(defaultSize / 2, defaultSize / 2)

    private fun recordDoubleClick(position: PxPosition) {
        recordedDoubleClicks.add(position)
    }

    @Test
    fun testDoubleClick() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox(Modifier.doubleTapGestureFilter(this::recordDoubleClick))
        }

        // When we inject a double click
        findByTag(defaultTag).doGesture { sendDoubleClick() }

        runOnIdleCompose {
            // Then we record 1 double click at the expected position
            assertThat(recordedDoubleClicks).isEqualTo(listOf(expectedClickPosition))
        }
    }
}

@MediumTest
@RunWith(Parameterized::class)
class SendDoubleClickWithArgumentsTest(private val config: TestConfig) {
    data class TestConfig(val position: PxPosition)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (x in listOf(1.0f, 33.0f, 99.0f)) {
                    for (y in listOf(1.0f, 33.0f, 99.0f)) {
                        add(TestConfig(PxPosition(x, y)))
                    }
                }
            }
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @get:Rule
    val inputDispatcherRule: TestRule = AndroidInputDispatcher.TestRule(
        disableDispatchInRealTime = true
    )

    private val recordedDoubleClicks = mutableListOf<PxPosition>()
    private val expectedClickPosition = config.position

    private fun recordDoubleClick(position: PxPosition) {
        recordedDoubleClicks.add(position)
    }

    @Test
    fun testDoubleClickOnPosition() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox(Modifier.doubleTapGestureFilter(this::recordDoubleClick))
        }

        // When we inject a double click
        findByTag(defaultTag).doGesture { sendDoubleClick(config.position) }

        runOnIdleCompose {
            // Then we record 1 double click at the expected position
            assertThat(recordedDoubleClicks).isEqualTo(listOf(expectedClickPosition))
        }
    }
}
