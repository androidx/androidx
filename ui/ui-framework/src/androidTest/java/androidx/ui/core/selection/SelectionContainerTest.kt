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
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.Text
import androidx.ui.core.gesture.MotionEvent
import androidx.ui.core.gesture.PointerCoords
import androidx.ui.core.gesture.PointerProperties
import androidx.ui.core.test.ValueModel
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.ResourceFont
import androidx.ui.text.font.asFontFamily
import androidx.ui.unit.px
import androidx.ui.unit.sp
import androidx.ui.unit.withDensity
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
    val composeTestRule = AndroidComposeTestRule<Activity>()

    private val activity
        get() = composeTestRule.activityTestRule.activity

    private lateinit var view: View

    private val textContent = "Text Demo Text Demo"
    private val fontFamily = ResourceFont(
        resId = androidx.ui.framework.test.R.font.sample_font,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).asFontFamily()

    private lateinit var gestureCountDownLatch: CountDownLatch

    private val selection = ValueModel<Selection?>(null)
    private val fontSize = 10.sp

    @Before
    fun setup() {
        withDensity(composeTestRule.density) {
            composeTestRule.setContent {
                SelectionContainer(
                    selection = selection.value,
                    onSelectionChange = {
                        selection.value = it
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
    @SdkSuppress(minSdkVersion = 27)
    fun press_to_cancel() {
        withDensity(composeTestRule.density) {
            // Setup. Long press to create a selection.
            // A reasonable number.
            val position = 50.px
            longPress(x = position.value, y = position.value)
            composeTestRule.runOnIdleCompose {
                assertThat(selection.value).isNotNull()
            }

            // Act.
            press(x = position.value, y = position.value)

            // Assert.
            composeTestRule.runOnIdleCompose {
                assertThat(selection.value).isNull()
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
                assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('D'))
                assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
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
                assertThat(selection.value!!.start.offset).isEqualTo(startOffset)
                assertThat(selection.value!!.end.offset).isEqualTo("Text Demo".length)
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
        composeTestRule.runOnIdleCompose(block)
    }
}
