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

package androidx.ui.test

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.test.filters.MediumTest
import androidx.ui.core.DensityAmbient
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputHandler
import androidx.ui.core.TestTag
import androidx.ui.core.changedToUp
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutSize
import androidx.ui.semantics.Semantics
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val tag = "widget"
private const val numberOfSquares = 5
private const val first = 0
private const val last = numberOfSquares - 1
private val squareSize = 10.px
private val center = PxPosition(squareSize / 2, squareSize / 2)
private val colors = listOf(Color.Red, Color.Yellow, Color.Blue, Color.Green, Color.Cyan)

private data class ClickData(
    val componentIndex: Int,
    val position: PxPosition
)

// The presence of an ActionBar follows from the theme set in AndroidManifest.xml
class ActivityWithActionBar : Activity() {
    private lateinit var composeHolder: FrameLayout
    private var composition: Composition? = null

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

    override fun onDestroy() {
        composition?.dispose()
        super.onDestroy()
    }

    fun setContent(composable: @Composable() () -> Unit) {
        composition = composeHolder.setContent(composable)
    }
}

private fun <T : Activity> AndroidComposeTestRule<T>.setContent(
    recordedClicks: MutableList<ClickData>
) {
    val activity = activityTestRule.activity
    if (activity is ActivityWithActionBar) {
        runOnUiThread {
            activity.setContent { Ui(recordedClicks) }
        }
    } else {
        setContent { Ui(recordedClicks) }
    }
}

@Composable
private fun Ui(recordedClicks: MutableList<ClickData>) {
    with(DensityAmbient.current) {
        Column {
            for (i in first..last) {
                TestTag(tag = "$tag$i") {
                    Semantics(container = true) {
                        val pointerInputModifier =
                            PointerInputModifier(object : PointerInputFilter() {
                                override val pointerInputHandler: PointerInputHandler =
                                    { changes, pass, _ ->
                                        if (pass == PointerEventPass.InitialDown) {
                                            changes.filter { it.changedToUp() }.forEach {
                                                recordedClicks.add(
                                                    ClickData(i, it.current.position!!)
                                                )
                                            }
                                        }
                                        changes
                                    }
                                override val cancelHandler: () -> Unit = {}
                            })
                        Box(
                            pointerInputModifier + LayoutSize(squareSize.toDp()),
                            backgroundColor = colors[i]
                        )
                    }
                }
            }
        }
    }
}

@MediumTest
@RunWith(Parameterized::class)
class SendClickWithoutArgumentsTest(config: TestConfig) {
    data class TestConfig(
        val activityClass: Class<out Activity>
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> = listOf(
            TestConfig(Activity::class.java),
            TestConfig(ActivityWithActionBar::class.java)
        )
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(config.activityClass, disableTransitions = true)

    @Test
    fun testClickWithoutArguments() {
        // Given a column of 5 small components
        val recordedClicks = mutableListOf<ClickData>()
        composeTestRule.setContent(recordedClicks)

        // When I click each of the components
        findByTag("${tag}$first").doGesture { sendClick() }
        findByTag("${tag}$last").doGesture { sendClick() }

        // Then each component has registered a click
        composeTestRule.runOnIdleCompose {
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
        val activityClass: Class<out Activity>
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> {
            return mutableListOf<TestConfig>().apply {
                for (x in listOf(0.px, squareSize - 1.px)) {
                    for (y in listOf(0.px, squareSize - 1.px)) {
                        add(TestConfig(PxPosition(x, y), Activity::class.java))
                        add(TestConfig(PxPosition(x, y), ActivityWithActionBar::class.java))
                    }
                }
            }
        }
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(config.activityClass, disableTransitions = true)

    @Test
    fun testClickWithArguments() {
        // Given a column of 5 small components
        val recordedClicks = mutableListOf<ClickData>()
        composeTestRule.setContent(recordedClicks)

        // When I click each of the components
        findByTag("${tag}$first").doGesture { sendClick(config.position) }
        findByTag("${tag}$last").doGesture { sendClick(config.position) }

        // Then each component has registered a click
        composeTestRule.runOnIdleCompose {
            assertThat(recordedClicks).isEqualTo(
                listOf(
                    ClickData(first, config.position),
                    ClickData(last, config.position)
                )
            )
        }
    }
}
