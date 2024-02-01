/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import android.view.KeyEvent.ACTION_DOWN
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.invokeGlobalAssertions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.core.view.InputDeviceCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldHandwritingTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField2"

    private val imm = FakeInputMethodManager()

    @Test
    fun textField_startStylusHandwriting_unfocused() {
        testStylusHandwriting(stylusHandwritingStarted = true) {
            performStylusHandwriting()
        }
    }

    @Test
    fun textField_startStylusHandwriting_focused() {
        testStylusHandwriting(stylusHandwritingStarted = true) {
            requestFocus()
            performStylusHandwriting()
        }
    }

    @Test
    fun textField_click_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) {
            performStylusInput {
                down(visibleSize.center.toOffset())
                move()
                up()
            }
        }
    }

    @Test
    fun textField_longClick_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) {
            performStylusInput {
                down(visibleSize.center.toOffset())
                move(viewConfiguration.longPressTimeoutMillis + 1)
                up()
            }
        }
    }

    @Test
    fun textField_longPressAndDrag_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) {
            performStylusInput {
                val startPosition = visibleSize.center.toOffset()
                down(visibleSize.center.toOffset())
                val position = startPosition + Offset(viewConfiguration.handwritingSlop * 2, 0f)
                moveTo(
                    position = position,
                    delayMillis = viewConfiguration.longPressTimeoutMillis + 1
                )
                up()
            }
        }
    }

    @Test
    fun textField_disabled_notStartStylusHandwriting() {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                enabled = false
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()

        rule.runOnIdle {
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun textField_readOnly_notStartStylusHandwriting() {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                readOnly = true
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()

        rule.runOnIdle {
            imm.expectNoMoreCalls()
        }
    }

    private fun testStylusHandwriting(
        stylusHandwritingStarted: Boolean,
        interaction: SemanticsNodeInteraction.() -> Unit
    ) {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag)
            )
        }

        interaction.invoke(rule.onNodeWithTag(Tag))

        rule.runOnIdle {
            if (stylusHandwritingStarted) {
                imm.expectCall("startStylusHandwriting")
            }
            imm.expectNoMoreCalls()
        }
    }

    /** Start stylus handwriting on the target element. */
    private fun SemanticsNodeInteraction.performStylusHandwriting() {
        performStylusInput {
            val startPosition = visibleSize.center.toOffset()
            down(startPosition)
            moveTo(startPosition + Offset(viewConfiguration.handwritingSlop * 2, 0f))
            up()
        }
    }

    private fun SemanticsNodeInteraction.performStylusInput(
        block: TouchInjectionScope.() -> Unit
    ): SemanticsNodeInteraction {
        @OptIn(ExperimentalTestApi::class)
        invokeGlobalAssertions()
        val node = fetchSemanticsNode("Failed to inject stylus input.")
        val stylusInjectionScope = StylusInjectionScope(node)
        block.invoke(stylusInjectionScope)
        return this
    }

    // We don't have StylusInjectionScope at the moment. This is a simplified implementation for
    // the basic use cases in this test. It only supports single stylus pointer, and the pointerId
    // is totally ignored.
    private inner class StylusInjectionScope(
        semanticsNode: SemanticsNode
    ) : TouchInjectionScope, Density by semanticsNode.layoutInfo.density {
        private val root = semanticsNode.root as ViewRootForTest
        private val downTime: Long = System.currentTimeMillis()

        private var lastPosition: Offset = Offset.Unspecified
        private var currentTime: Long = System.currentTimeMillis()
        private val boundsInRoot = semanticsNode.boundsInRoot

        override val visibleSize: IntSize =
            IntSize(boundsInRoot.width.roundToInt(), boundsInRoot.height.roundToInt())

        override val viewConfiguration: ViewConfiguration =
            semanticsNode.layoutInfo.viewConfiguration

        private fun localToRoot(position: Offset): Offset {
            return position + boundsInRoot.topLeft
        }

        override fun advanceEventTime(durationMillis: Long) {
            require(durationMillis >= 0) {
                "duration of a delay can only be positive, not $durationMillis"
            }
            currentTime += durationMillis
        }

        override fun currentPosition(pointerId: Int): Offset? {
            return lastPosition
        }

        override fun down(pointerId: Int, position: Offset) {
            val rootPosition = localToRoot(position)
            lastPosition = rootPosition
            sendTouchEvent(ACTION_DOWN)
        }

        override fun updatePointerTo(pointerId: Int, position: Offset) {
            lastPosition = localToRoot(position)
        }

        override fun move(delayMillis: Long) {
            advanceEventTime(delayMillis)
            sendTouchEvent(ACTION_MOVE)
        }

        @ExperimentalTestApi
        override fun moveWithHistoryMultiPointer(
            relativeHistoricalTimes: List<Long>,
            historicalCoordinates: List<List<Offset>>,
            delayMillis: Long
        ) {
            // Not needed for this test because Android only support one stylus pointer.
        }

        override fun up(pointerId: Int) {
            sendTouchEvent(ACTION_UP)
        }

        override fun cancel(delayMillis: Long) {
            sendTouchEvent(ACTION_CANCEL)
        }

        private fun sendTouchEvent(action: Int) {
            val positionInScreen = run {
                val array = intArrayOf(0, 0)
                root.view.getLocationOnScreen(array)
                Offset(array[0].toFloat(), array[1].toFloat())
            }
            val motionEvent = MotionEvent.obtain(
                /* downTime = */ downTime,
                /* eventTime = */ currentTime,
                /* action = */ action,
                /* pointerCount = */ 1,
                /* pointerProperties = */ arrayOf(
                    MotionEvent.PointerProperties().apply {
                        id = 0
                        toolType = MotionEvent.TOOL_TYPE_STYLUS
                    }
                ),
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        val startOffset = lastPosition

                        // Allows for non-valid numbers/Offsets to be passed along to Compose to
                        // test if it handles them properly (versus breaking here and we not knowing
                        // if Compose properly handles these values).
                        x = if (startOffset.isValid()) {
                            positionInScreen.x + startOffset.x
                        } else {
                            Float.NaN
                        }

                        y = if (startOffset.isValid()) {
                            positionInScreen.y + startOffset.y
                        } else {
                            Float.NaN
                        }
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 1f,
                /* yPrecision = */ 1f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDeviceCompat.SOURCE_TOUCHSCREEN,
                /* flags = */ 0
            )

            rule.runOnUiThread {
                root.view.dispatchTouchEvent(motionEvent)
            }
        }
    }
}
