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

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.IndirectPointerInjectionScope
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performIndirectPointerInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.verify
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Test for [IndirectPointerInjectionScope.click] */
@MediumTest
@RunWith(Parameterized::class)
class ClickTest(private val config: TestConfig) {
    data class TestConfig(val position: Offset?)

    companion object {
        private const val squareSize = 10.0f
        private val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green, Color.Cyan)

        private val inputDeviceSize = IntSize(3082, 616)
        private val inputDeviceTopLeft = Offset(10f, 10f)
        private val inputDeviceBottomRight = Offset(3072f, 606f)
        private val inputDeviceCenter = Offset(1541f, 308f)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return listOf(
                TestConfig(inputDeviceTopLeft),
                TestConfig(inputDeviceBottomRight),
                TestConfig(null),
            )
        }
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val expectedClickPosition = config.position ?: inputDeviceCenter

    @Test
    fun click() {
        val firstRecorder = SinglePointerInputRecorder()
        val lastRecorder = SinglePointerInputRecorder()

        // Given a column of 5 small components
        rule.setContent {
            Column {
                ClickableTestBox(firstRecorder, squareSize, squareSize, colors[0], "first")
                ClickableTestBox(Modifier, squareSize, squareSize, colors[1])
                ClickableTestBox(Modifier, squareSize, squareSize, colors[2])
                ClickableTestBox(Modifier, squareSize, squareSize, colors[3])
                ClickableTestBox(lastRecorder, squareSize, squareSize, colors[4], "last")
            }
        }

        // When I click the first and last of these components
        rule.click("first")
        rule.click("last")

        // Then those components have registered a click
        rule.runOnIdle {
            firstRecorder.assertIsClick(expectedClickPosition)
            lastRecorder.assertIsClick(expectedClickPosition)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun SinglePointerInputRecorder.assertIsClick(position: Offset) {
        assertThat(events).hasSize(3)
        val t0 = events[0].timestamp
        val id = events[0].id

        events[0].verify(t0 + 0, id, true, position, Touch, Press)
        events[1].verify(t0 + eventPeriodMillis, id, true, position, Touch, Move)
        events[2].verify(t0 + eventPeriodMillis, id, false, position, Touch, Release)
    }

    private fun ComposeTestRule.click(tag: String) {
        onNodeWithTag(tag).requestFocus()
        onNodeWithTag(tag).performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            if (config.position != null) {
                click(config.position)
            } else {
                click()
            }
        }
    }
}
