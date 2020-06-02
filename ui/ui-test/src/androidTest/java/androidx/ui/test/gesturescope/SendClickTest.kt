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

package androidx.ui.test.gesturescope

import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.changedToUp
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.test.ActivityWithActionBar
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.test.sendClick
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.RecordingFilter
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class SendClickTest(private val config: TestConfig) {
    data class TestConfig(
        val position: PxPosition?,
        val activityClass: Class<out ComponentActivity>
    )

    companion object {
        private const val squareSize = 10.0f
        private val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green, Color.Cyan)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (x in listOf(0.0f, squareSize - 1.0f)) {
                    for (y in listOf(0.0f, squareSize - 1.0f)) {
                        add(TestConfig(PxPosition(x, y), ComponentActivity::class.java))
                        add(TestConfig(PxPosition(x, y), ActivityWithActionBar::class.java))
                    }
                }
                add(TestConfig(null, ComponentActivity::class.java))
                add(TestConfig(null, ActivityWithActionBar::class.java))
            }
        }
    }

    private data class ClickData(
        val componentIndex: Int,
        val position: PxPosition
    )

    private class ClickRecorder(
        private val componentIndex: Int,
        private val recordedClicks: MutableList<ClickData>
    ) : PointerInputModifier {
        override val pointerInputFilter: PointerInputFilter = RecordingFilter { changes ->
            changes.forEach {
                if (it.changedToUp()) {
                    recordedClicks.add(ClickData(componentIndex, it.current.position!!))
                }
            }
        }
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        ActivityTestRule(config.activityClass),
        disableTransitions = true
    )

    private val recordedClicks = mutableListOf<ClickData>()
    private val expectedClickPosition =
        config.position ?: PxPosition(squareSize / 2, squareSize / 2)

    @Test
    fun testClick() {
        // Given a column of 5 small components
        val activity = composeTestRule.activityTestRule.activity
        if (activity is ActivityWithActionBar) {
            runOnUiThread { activity.setContent { ColumnOfSquares(5) } }
        } else {
            composeTestRule.setContent { ColumnOfSquares(5) }
        }

        // When I click the first and last of these components
        click("square0")
        click("square4")

        // Then those components have registered a click
        runOnIdleCompose {
            assertThat(recordedClicks).isEqualTo(
                listOf(
                    ClickData(0, expectedClickPosition),
                    ClickData(4, expectedClickPosition)
                )
            )
        }
    }

    private fun click(tag: String) {
        findByTag(tag).doGesture {
            if (config.position != null) {
                sendClick(config.position)
            } else {
                sendClick()
            }
        }
    }

    @Composable
    @Suppress("SameParameterValue")
    private fun ColumnOfSquares(numberOfSquares: Int) {
        Column {
            repeat(numberOfSquares) { i ->
                ClickableTestBox(
                    modifier = ClickRecorder(i, recordedClicks),
                    width = squareSize,
                    height = squareSize,
                    color = colors[i],
                    tag = "square$i"
                )
            }
        }
    }
}
