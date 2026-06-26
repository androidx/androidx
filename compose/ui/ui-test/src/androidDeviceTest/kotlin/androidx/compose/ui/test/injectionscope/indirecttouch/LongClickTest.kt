/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.indirecttouch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.testutils.WithViewConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.IndirectPointerInjectionScope
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performIndirectPointerInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.verify
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests [IndirectPointerInjectionScope.longClick] with arguments. Verifies that the click is at the
 * expected position, that the gesture has the expected duration and that all input events were at
 * the same location.
 */
@MediumTest
@RunWith(Parameterized::class)
class LongClickTest(private val config: TestConfig) {
    data class TestConfig(val position: Offset?, val durationMillis: Long?)

    companion object {
        private const val LongPressTimeoutMillis = 300L
        private val testViewConfiguration =
            TestViewConfiguration(longPressTimeoutMillis = LongPressTimeoutMillis)

        private val inputDeviceSize = IntSize(3082, 616)
        private val inputDeviceCenter = Offset(1541f, 308f)
        private val inputDeviceTopLeft = Offset(10f, 10f)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (duration in listOf(null, 700L)) {
                    add(TestConfig(inputDeviceTopLeft, duration))
                    add(TestConfig(null, duration))
                }
            }
        }
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val expectedClickPosition = config.position ?: inputDeviceCenter
    private val expectedDuration = config.durationMillis ?: (LongPressTimeoutMillis + 100L)

    @Test
    fun longClick() {
        val recorder = SinglePointerInputRecorder()
        rule.setContent {
            WithViewConfiguration(testViewConfiguration) {
                Box(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
                    ClickableTestBox(recorder)
                }
            }
        }
        rule.onNodeWithTag(defaultTag).requestFocus()

        // When we inject a long click
        rule.onNodeWithTag(defaultTag).performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            if (config.position != null && config.durationMillis != null) {
                longClick(config.position, config.durationMillis)
            } else if (config.position != null) {
                longClick(config.position)
            } else if (config.durationMillis != null) {
                longClick(durationMillis = config.durationMillis)
            } else {
                longClick()
            }
        }

        rule.runOnIdle { recorder.assertIsLongClick(expectedClickPosition) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun SinglePointerInputRecorder.assertIsLongClick(position: Offset) {
        val steps = max(1, (expectedDuration / eventPeriodMillis.toDouble()).roundToInt())
        val t0 = events[0].timestamp
        val id = events[0].id

        assertThat(events).hasSize(steps + 2)
        events.dropLast(1).forEachIndexed { i, event ->
            // Don't check the timestamp
            val t = t0 + (expectedDuration * i / steps.toDouble()).roundToLong()
            val type = if (i == 0) Press else Move
            event.verify(t, id, true, position, Touch, type)
        }
        events.last().verify(t0 + expectedDuration, id, false, position, Touch, Release)
    }
}
