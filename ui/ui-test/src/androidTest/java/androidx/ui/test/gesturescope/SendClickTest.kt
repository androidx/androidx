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

import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.changedToUp
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
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

private const val tag = "widget"
private const val numberOfSquares = 5
private const val first = 0
private const val last = numberOfSquares - 1
private const val squareSize = 10.0f
private val center = PxPosition(squareSize / 2, squareSize / 2)
private val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green, Color.Cyan)

private data class ClickData(
    val componentIndex: Int,
    val position: PxPosition
)

private class ClickRecorder(
    private val componentIndex: Int,
    private val recordedClicks: MutableList<ClickData>
) : PointerInputModifier {
    override val pointerInputFilter: PointerInputFilter = RecordingFilter {
        if (it.changedToUp()) {
            recordedClicks.add(ClickData(componentIndex, it.current.position!!))
        }
    }
}

// The presence of an ActionBar follows from the theme set in AndroidManifest.xml
class ActivityWithActionBar : ComponentActivity() {
    private lateinit var composeHolder: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        composeHolder = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(100, 100).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            // Set background color for recognizability on screen / in screenshots
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }
        root.addView(composeHolder)
        setContentView(root)
    }

    fun setContent(composable: @Composable () -> Unit) {
        composeHolder.setContent(Recomposer.current(), composable)
    }
}

private fun AndroidComposeTestRule<*>.setContent(recordedClicks: MutableList<ClickData>) {
    val content = @Composable {
        Column {
            repeat(numberOfSquares) { i ->
                ClickableTestBox(
                    modifier = ClickRecorder(i, recordedClicks),
                    width = squareSize,
                    height = squareSize,
                    color = colors[i],
                    tag = "$tag$i"
                )
            }
        }
    }

    val activity = activityTestRule.activity
    if (activity is ActivityWithActionBar) {
        runOnUiThread {
            activity.setContent(content)
        }
    } else {
        setContent(content)
    }
}

@MediumTest
@RunWith(Parameterized::class)
class SendClickWithoutArgumentsTest(config: TestConfig) {
    data class TestConfig(
        val activityClass: Class<out ComponentActivity>
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> = listOf(
            TestConfig(ComponentActivity::class.java),
            TestConfig(ActivityWithActionBar::class.java)
        )
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        ActivityTestRule(config.activityClass),
        disableTransitions = true
    )

    @Test
    fun testClickWithoutArguments() {
        // Given a column of 5 small components
        val recordedClicks = mutableListOf<ClickData>()
        composeTestRule.setContent(recordedClicks)

        // When I click the first and last of these components
        findByTag("$tag$first").doGesture { sendClick() }
        findByTag("$tag$last").doGesture { sendClick() }

        // Then those components have registered a click
        runOnIdleCompose {
            assertThat(recordedClicks).isEqualTo(
                listOf(
                    ClickData(first, center),
                    ClickData(last, center)
                )
            )
        }
    }
}

@MediumTest
@RunWith(Parameterized::class)
class SendClickWithArgumentsTest(private val config: TestConfig) {
    data class TestConfig(
        val position: PxPosition,
        val activityClass: Class<out ComponentActivity>
    )

    companion object {
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
            }
        }
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        ActivityTestRule(config.activityClass),
        disableTransitions = true
    )

    @Test
    fun testClickWithArguments() {
        // Given a column of 5 small components
        val recordedClicks = mutableListOf<ClickData>()
        composeTestRule.setContent(recordedClicks)

        // When I click the first and last of these components
        findByTag("$tag$first").doGesture { sendClick(config.position) }
        findByTag("$tag$last").doGesture { sendClick(config.position) }

        // Then those components have registered a click
        runOnIdleCompose {
            assertThat(recordedClicks).isEqualTo(
                listOf(
                    ClickData(first, config.position),
                    ClickData(last, config.position)
                )
            )
        }
    }
}
