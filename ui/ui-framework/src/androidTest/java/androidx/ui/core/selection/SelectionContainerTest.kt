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

package androidx.ui.core.selection

import android.R
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.Model
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.SmallTest
import androidx.ui.core.Text
import androidx.ui.core.gesture.MotionEvent
import androidx.ui.core.gesture.PointerCoords
import androidx.ui.core.gesture.PointerProperties
import androidx.ui.core.px
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.createComposeRule
import androidx.ui.text.TextStyle
import androidx.ui.text.font.Font
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.asFontFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class SelectionContainerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val activity
        get() = (composeTestRule as AndroidComposeTestRule).activityTestRule.activity

    private lateinit var view: View

    private val textContent = "Text Demo Text Demo"
    private val fontFamily = Font(
        name = "sample_font.ttf",
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).asFontFamily()

    private lateinit var gestureCountDownLatch: CountDownLatch

    @Model
    private data class SelectionHolder(var state: Selection?)

    private val selection = SelectionHolder(state = null)
    private val fontSize = 10.sp

    @Before
    fun setup() {
        withDensity(composeTestRule.density) {
            composeTestRule.setContent {
                // Use this state to make sure SelectionContainer could be recompose once selection
                // changes. @Model doesn't work that well.
                val selectionState = +state<Selection?> { null }
                SelectionContainer(
                    selection = selectionState.value,
                    onSelectionChange = {
                        selectionState.value = it
                        selection.state = it
                        gestureCountDownLatch.countDown()
                    }
                ) {
                    Text(
                        textContent,
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
                    )
                }
            }
            view = activity.findViewById<ViewGroup>(R.id.content)
        }
    }

    @Test
    fun press_to_cancel() {
        withDensity(composeTestRule.density) {
            // Setup. Long press to create a selection.
            // A reasonable number.
            val position = 50.px
            longPress(x = position.value, y = position.value)
            composeTestRule.runOnIdleCompose {
                assertThat(selection.state).isNotNull()
            }

            // Act.
            press(x = position.value, y = position.value)

            // Assert.
            composeTestRule.runOnIdleCompose {
                assertThat(selection.state).isNull()
            }
        }
    }

    @Test
    fun long_press_select_a_word() {
        withDensity(composeTestRule.density) {
            // Setup.
            val characterSize = fontSize.toPx().value

            // Act.
            longPress(
                x = textContent.indexOf('m') * characterSize,
                y = 0.5f * characterSize
            )

            // Assert. Should select "Demo".
            composeTestRule.runOnIdleCompose {
                assertThat(selection.state!!.start.offset).isEqualTo(textContent.indexOf('D'))
                assertThat(selection.state!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
            }
        }
    }

    @Test
    fun long_press_and_drag_select_text_range() {
        withDensity(composeTestRule.density) {
            // Setup. Want to selection "Dem".
            val startOffset = textContent.indexOf('D')
            val endOffset = textContent.indexOf('m') + 1
            val characterSize = fontSize.toPx().value

            // Act.
            longPressAndDrag(
                startX = startOffset * characterSize,
                startY = 0.5f * characterSize,
                endX = endOffset * characterSize,
                endY = 0.5f * characterSize
            )

            // Assert.
            composeTestRule.runOnIdleCompose {
                assertThat(selection.state!!.start.offset).isEqualTo(startOffset)
                assertThat(selection.state!!.end.offset).isEqualTo(endOffset)
            }
        }
    }

    private fun longPress(x: Float, y: Float) {
        waitForLongPress {
            view.dispatchTouchEvent(getDownEvent(x, y))
        }
        waitForOtherGesture {
            view.dispatchTouchEvent(getUpEvent(x, y))
        }
    }

    private fun longPressAndDrag(startX: Float, startY: Float, endX: Float, endY: Float) {
        waitForLongPress {
            view.dispatchTouchEvent(getDownEvent(startX, startY))
        }
        waitForOtherGesture {
            view.dispatchTouchEvent(getMoveEvent(endX, endY))
        }
    }

    private fun press(x: Float, y: Float) {
        waitForOtherGesture {
            view.dispatchTouchEvent(getDownEvent(x, y))
        }
        waitForOtherGesture {
            view.dispatchTouchEvent(getUpEvent(x, y))
        }
    }

    private fun getDownEvent(x: Float, y: Float): MotionEvent {
        return MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(x, y))
        )
    }

    private fun getUpEvent(x: Float, y: Float): MotionEvent {
        return MotionEvent(
            0,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(x, y))
        )
    }

    private fun getMoveEvent(x: Float, y: Float): MotionEvent {
        return MotionEvent(
            0,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(x, y))
        )
    }

    private fun waitForLongPress(block: () -> Unit) {
        gestureCountDownLatch = CountDownLatch(1)
        composeTestRule.runOnIdleCompose(block)
        gestureCountDownLatch.await(750, TimeUnit.MILLISECONDS)
    }

    private fun waitForOtherGesture(block: () -> Unit) {
        gestureCountDownLatch = CountDownLatch(1)
        composeTestRule.runOnIdleCompose(block)
    }
}
