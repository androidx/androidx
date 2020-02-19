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

package androidx.ui.test

import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.PointerInput
import androidx.ui.core.TestTag
import androidx.ui.core.gesture.LongPressGestureDetector
import androidx.ui.core.gesture.LongPressTimeout
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.Align
import androidx.ui.layout.LayoutSize
import androidx.ui.semantics.Semantics
import androidx.ui.test.util.PointerInputRecorder
import androidx.ui.test.util.areAlmostEqualTo
import androidx.ui.test.util.assertOnlyLastEventIsUp
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.recordedDuration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized

private const val tag = "widget"
private val width = 100.px
private val height = 100.px
private val expectedDuration = LongPressTimeout + 100.milliseconds

@Composable
private fun Ui(recorder: PointerInputRecorder, onLongPress: (PxPosition) -> Unit) {
    Align(alignment = Alignment.BottomRight) {
        TestTag(tag) {
            Semantics(container = true) {
                LongPressGestureDetector(onLongPress = onLongPress) {
                    PointerInput(
                        pointerInputHandler = recorder::onPointerInput,
                        cancelHandler = {}
                    ) {
                        with(DensityAmbient.current) {
                            Box(
                                LayoutSize(width.toDp(), height.toDp()),
                                backgroundColor = Color.Yellow
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tests [GestureScope.sendLongClick] without arguments. Verifies that the click is in the middle
 * of the component, that the gesture has a duration of 600 milliseconds and that all input
 * events were on the same location.
 */
@MediumTest
@RunWith(JUnit4::class)
class SendLongClickWithoutArgumentsTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val recorder = PointerInputRecorder()
    private val recordedLongClicks = mutableListOf<PxPosition>()
    private val expectedPosition = PxPosition(width / 2, height / 2)

    private fun recordLongPress(position: PxPosition) {
        recordedLongClicks.add(position)
    }

    @Test
    fun testLongClick() {
        // Given some content
        composeTestRule.setContent { Ui(recorder, ::recordLongPress) }

        // When we inject a long click
        findByTag(tag).doGesture { sendLongClick() }

        // Then we record 1 long click
        assertThat(recordedLongClicks).hasSize(1)

        // And all events are at the click location
        composeTestRule.runOnUiThread {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                events.areAlmostEqualTo(expectedPosition)
                assertThat(recordedDuration).isEqualTo(expectedDuration)
            }
        }
    }
}

/**
 * Tests [GestureScope.sendLongClick] with arguments. Verifies that the click is in the middle
 * of the component, that the gesture has a duration of 600 milliseconds and that all input
 * events were on the same location.
 */
@MediumTest
@RunWith(Parameterized::class)
class SendLongClickWithArgumentsTest(private val config: TestConfig) {
    data class TestConfig(
        val position: PxPosition
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (x in listOf(1.px, width / 4)) {
                    for (y in listOf(1.px, height / 4)) {
                        add(TestConfig(PxPosition(x, y)))
                    }
                }
            }
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val recorder = PointerInputRecorder()
    private val recordedLongClicks = mutableListOf<PxPosition>()

    private fun recordLongPress(position: PxPosition) {
        recordedLongClicks.add(position)
    }

    @Test
    fun testLongClick() {
        // Given some content
        composeTestRule.setContent { Ui(recorder, ::recordLongPress) }

        // When we inject a long click
        findByTag(tag).doGesture { sendLongClick(config.position) }

        // Then we record 1 long click
        assertThat(recordedLongClicks).hasSize(1)

        // And all events are at the click location
        composeTestRule.runOnUiThread {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertOnlyLastEventIsUp()
                events.areAlmostEqualTo(config.position)
                assertThat(recordedDuration).isEqualTo(expectedDuration)
            }
        }
    }
}
