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
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.sendLongClick
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.ClickableTestBox.defaultSize
import androidx.ui.test.util.ClickableTestBox.defaultTag
import androidx.ui.test.util.isAlmostEqualTo
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests [sendLongClick] with arguments. Verifies that the click is in the middle
 * of the component, that the gesture has a duration of 600 milliseconds and that all input
 * events were on the same location.
 */
@MediumTest
@RunWith(Parameterized::class)
class SendLongClickTest(private val config: TestConfig) {
    data class TestConfig(val position: PxPosition?)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (x in listOf(1.0f, defaultSize / 4)) {
                    for (y in listOf(1.0f, defaultSize / 3)) {
                        add(TestConfig(PxPosition(x, y)))
                    }
                }
                add(TestConfig(null))
            }
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val recordedLongClicks = mutableListOf<PxPosition>()
    private val expectedClickPosition =
        config.position ?: PxPosition(defaultSize / 2, defaultSize / 2)

    private fun recordLongPress(position: PxPosition) {
        recordedLongClicks.add(position)
    }

    @Test
    fun testLongClick() {
        // Given some content
        composeTestRule.setContent {
            Stack(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                ClickableTestBox(Modifier.longPressGestureFilter(::recordLongPress))
            }
        }

        // When we inject a long click
        findByTag(defaultTag).doGesture {
            if (config.position != null) {
                sendLongClick(config.position)
            } else {
                sendLongClick()
            }
        }

        // Then we record 1 long click at the expected position
        assertThat(recordedLongClicks).hasSize(1)
        recordedLongClicks[0].isAlmostEqualTo(expectedClickPosition)
    }
}
