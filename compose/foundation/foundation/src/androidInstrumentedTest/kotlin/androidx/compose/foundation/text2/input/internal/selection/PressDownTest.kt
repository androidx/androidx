/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val TargetTag = "TargetLayout"

@RunWith(JUnit4::class)
class PressDownTest {

    @get:Rule
    val rule = createComposeRule()

    /**
     * null; gesture detector has never seen a down event.
     * true; at least a pointer is currently down.
     * false; there was at least one down event but right now all pointers are up. Also, this
     * requires onUp to be defined.
     */
    private var down: Boolean? = null

    private val downDetector = layoutWithGestureDetector {
        detectPressDownGesture(onDown = { down = true })
    }

    private val downUpDetector = layoutWithGestureDetector {
        detectPressDownGesture(
            onDown = { down = true },
            onUp = { down = false }
        )
    }

    private val nothingHandler: PointerInputChange.() -> Unit = {}

    private var initialPass: PointerInputChange.() -> Unit = nothingHandler
    private var finalPass: PointerInputChange.() -> Unit = nothingHandler

    @Before
    fun setup() {
        down = null
    }

    private fun layoutWithGestureDetector(
        gestureDetector: suspend PointerInputScope.() -> Unit,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalDensity provides Density(1f),
            LocalViewConfiguration provides TestViewConfiguration(
                minimumTouchTargetSize = DpSize.Zero
            )
        ) {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // Some tests execute a lambda before the initial and final passes
                        // so they are called here, higher up the chain, so that the
                        // calls happen prior to the gestureDetector below. The lambdas
                        // do things like consume events on the initial pass or validate
                        // consumption on the final pass.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach {
                                        initialPass(it)
                                    }
                                    awaitPointerEvent(PointerEventPass.Final)
                                    event.changes.forEach {
                                        finalPass(it)
                                    }
                                }
                            }
                        }
                        .wrapContentSize(AbsoluteAlignment.TopLeft)
                        .size(10.toDp())
                        .pointerInput(gestureDetector, gestureDetector)
                        .testTag(TargetTag)
                )
            }
        }
    }

    private fun performTouch(
        initialPass: PointerInputChange.() -> Unit = nothingHandler,
        finalPass: PointerInputChange.() -> Unit = nothingHandler,
        block: TouchInjectionScope.() -> Unit
    ) {
        this.initialPass = initialPass
        this.finalPass = finalPass
        rule.onNodeWithTag(TargetTag).performTouchInput(block)
        rule.waitForIdle()
        this.initialPass = nothingHandler
        this.finalPass = nothingHandler
    }

    @Test
    fun normalDown() {
        rule.setContent(downUpDetector)

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) {
            down(0, Offset(5f, 5f))
        }

        Assert.assertTrue(down!!)

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) {
            up(0)
        }

        Assert.assertFalse(down!!)
    }

    @Test
    fun normalDown_upNotSpecified() {
        rule.setContent(downDetector)

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) {
            down(0, Offset(5f, 5f))
        }

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) {
            up(0)
        }

        Assert.assertTrue(down!!)
    }

    @Test
    fun consumedDown() {
        rule.setContent(downUpDetector)

        performTouch(initialPass = { consume() }) { down(0, Offset(5f, 5f)) }

        Assert.assertTrue(down!!)

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) { up(0) }

        Assert.assertFalse(down!!)
    }

    @Test
    fun downLongPress() {
        rule.setContent(downUpDetector)

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) {
            down(0, Offset(5f, 5f))
        }

        Assert.assertTrue(down!!)

        rule.mainClock.advanceTimeBy(10_000L)

        Assert.assertTrue(down!!)

        rule.mainClock.advanceTimeBy(500)

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) {
            up(0)
        }

        Assert.assertFalse(down!!)
    }

    @Test
    fun downAndMoveOut() {
        rule.setContent(downUpDetector)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
        }

        performTouch(finalPass = { Assert.assertFalse(isConsumed) }) {
            up(0)
        }

        Assert.assertFalse(down!!)
    }
}
