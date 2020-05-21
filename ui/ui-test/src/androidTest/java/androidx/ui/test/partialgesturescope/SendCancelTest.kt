/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test.partialgesturescope

import androidx.test.filters.MediumTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.createComposeRule
import androidx.ui.test.doPartialGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendCancel
import androidx.ui.test.sendDown
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.PointerInputRecorder
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class SendCancelTest(private val config: TestConfig) {
    data class TestConfig(val cancelPosition: PxPosition?) {
        val downPosition = PxPosition(1f, 1f)
    }

    companion object {
        private const val tag = "widget"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (x in listOf(2f, 99f)) {
                    for (y in listOf(3f, 53f)) {
                        add(TestConfig(PxPosition(x, y)))
                    }
                }
                add(TestConfig(null))
            }
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule =
        AndroidInputDispatcher.TestRule(disableDispatchInRealTime = true)

    private val recorder = PointerInputRecorder()

    @Test
    fun testSendCancel() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox(recorder, tag = tag)
        }

        // When we inject a down event followed by a cancel event
        findByTag(tag).doPartialGesture { sendDown(config.downPosition) }
        findByTag(tag).doPartialGesture { sendCancel(config.cancelPosition) }

        runOnIdleCompose {
            recorder.run {
                // Then we have only recorded 1 down event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(1)
            }
        }
    }
}
