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

package androidx.ui.text.selection

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.Providers
import androidx.compose.mutableStateOf
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.HapticFeedBackAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedbackType
import androidx.ui.core.selection.Selection
import androidx.ui.core.selection.SelectionContainer
import androidx.ui.layout.fillMaxSize
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.text.AnnotatedString
import androidx.ui.text.CoreText
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.ResourceFont
import androidx.ui.text.font.asFontFamily
import androidx.ui.text.font.test.R
import androidx.ui.text.style.TextOverflow
import androidx.ui.unit.px
import androidx.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
    val composeTestRule = AndroidComposeTestRule<ComponentActivity>()

    private val activity
        get() = composeTestRule.activityTestRule.activity

    private lateinit var view: View

    private val textContent = "Text Demo Text Demo"
    private val fontFamily = ResourceFont(
        resId = R.font.sample_font,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).asFontFamily()

    private lateinit var gestureCountDownLatch: CountDownLatch

    private val selection = mutableStateOf<Selection?>(null)
    private val fontSize = 10.sp

    private val hapticFeedback = mock<HapticFeedback>()

    @Before
    fun setup() {
        composeTestRule.setContent {
            Providers(
                HapticFeedBackAmbient provides hapticFeedback
            ) {
                SelectionContainer(
                    selection = selection.value,
                    onSelectionChange = {
                        selection.value = it
                        gestureCountDownLatch.countDown()
                    }
                ) {
                    CoreText(
                        AnnotatedString(textContent),
                        Modifier.fillMaxSize(),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                        softWrap = true,
                        overflow = TextOverflow.Clip,
                        maxLines = Int.MAX_VALUE,
                        inlineContent = mapOf(),
                        onTextLayout = {}
                    )
                }
            }
        }
        view = activity.findViewById<ViewGroup>(android.R.id.content)
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun press_to_cancel() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        val position = 50.px
        longPress(x = position.value, y = position.value)
        runOnIdleCompose {
            assertThat(selection.value).isNotNull()
        }

        // Act.
        press(x = position.value, y = position.value)

        // Assert.
        runOnIdleCompose {
            assertThat(selection.value).isNull()
            verify(
                hapticFeedback,
                times(2)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @Test
    fun long_press_select_a_word() {
        // Setup.
        val characterSize = with(composeTestRule.density) { fontSize.toPx() }

        // Act.
        longPress(
            x = textContent.indexOf('m') * characterSize,
            y = 0.5f * characterSize
        )

        // Assert. Should select "Demo".
        runOnIdleCompose {
            assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('D'))
            assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
            verify(
                hapticFeedback,
                times(1)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun long_press_and_drag_select_text_range() {
        // Setup. Want to selection "Dem".
        val startOffset = textContent.indexOf('D')
        val endOffset = textContent.indexOf('m') + 1
        val characterSize = with(composeTestRule.density) { fontSize.toPx() }

        // Act.
        longPressAndDrag(
            startX = startOffset * characterSize,
            startY = 0.5f * characterSize,
            endX = endOffset * characterSize,
            endY = 0.5f * characterSize
        )

        // Assert.
        runOnIdleCompose {
            assertThat(selection.value!!.start.offset).isEqualTo(startOffset)
            assertThat(selection.value!!.end.offset).isEqualTo("Text Demo".length)
            verify(
                hapticFeedback,
                times(1)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        runOnIdleCompose(block)
        gestureCountDownLatch.await(750, TimeUnit.MILLISECONDS)
    }

    private fun waitForOtherGesture(block: () -> Unit) {
        runOnIdleCompose(block)
    }
}
