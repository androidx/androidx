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
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.longPressGestureFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.longClick
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.ClickableTestBox.defaultSize
import androidx.ui.test.util.ClickableTestBox.defaultTag
import androidx.ui.test.util.SinglePointerInputRecorder
import androidx.ui.test.util.isAlmostEqualTo
import androidx.ui.test.util.recordedDuration
import androidx.ui.test.waitForIdle
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests [longClick] with arguments. Verifies that the click is in the middle
 * of the component, that the gesture has a duration of 600 milliseconds and that all input
 * events were on the same location.
 */
@MediumTest
@RunWith(Parameterized::class)
class SendLongClickTest(private val config: TestConfig) {
    data class TestConfig(val position: Offset?, val duration: Duration?)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (duration in listOf(null, 700.milliseconds)) {
                    for (x in listOf(1.0f, defaultSize / 4)) {
                        for (y in listOf(1.0f, defaultSize / 3)) {
                            add(TestConfig(Offset(x, y), duration))
                        }
                    }
                    add(TestConfig(null, duration))
                }
            }
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val recordedLongClicks = mutableListOf<Offset>()
    private val expectedClickPosition =
        config.position ?: Offset(defaultSize / 2, defaultSize / 2)
    private val expectedDuration = config.duration ?: 600.milliseconds

    private fun recordLongPress(position: Offset) {
        recordedLongClicks.add(position)
    }

    @Test
    fun testLongClick() {
        // Given some content
        val recorder = SinglePointerInputRecorder()
        composeTestRule.setContent {
            Stack(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                ClickableTestBox(Modifier.longPressGestureFilter(::recordLongPress) + recorder)
            }
        }

        // When we inject a long click
        onNodeWithTag(defaultTag).performGesture {
            if (config.position != null && config.duration != null) {
                longClick(config.position, config.duration)
            } else if (config.position != null) {
                longClick(config.position)
            } else if (config.duration != null) {
                longClick(duration = config.duration)
            } else {
                longClick()
            }
        }

        waitForIdle()

        // Then we record 1 long click at the expected position
        assertThat(recordedLongClicks).hasSize(1)
        recordedLongClicks[0].isAlmostEqualTo(expectedClickPosition)

        // And that the duration was as expected
        assertThat(recorder.recordedDuration).isEqualTo(expectedDuration)
    }
}
