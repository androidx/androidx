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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.gestures.util.FakeHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalTestApi::class)
internal abstract class AbstractSelectionGesturesTest {

    @get:Rule
    val rule = createComposeRule()

    protected abstract val pointerAreaTag: String

    protected val hapticFeedback = FakeHapticFeedback()
    protected val fontFamily = TEST_FONT_FAMILY
    // small enough to fit in narrow screen in pre-submit,
    // big enough that pointer movement can target a single char on center
    protected val fontSize = 15.sp
    protected val density = Density(1f)

    protected lateinit var textToolbar: TextToolbar

    @Composable
    abstract fun Content()

    @Before
    fun setup() {
        rule.setContent {
            textToolbar = LocalTextToolbar.current
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalViewConfiguration provides TestViewConfiguration(
                    minimumTouchTargetSize = DpSize.Zero
                ),
                LocalHapticFeedback provides hapticFeedback,
            ) {
                Content()
            }
        }
    }

    protected val boundsInRoot
        get() = with(density) {
            rule.onNodeWithTag(pointerAreaTag).getBoundsInRoot().toRect()
        }

    // TODO(b/281584353) When touch mode can be changed globally,
    //  this should change to a single tap outside of the bounds.
    internal fun TouchInjectionScope.enterTouchMode() {
        swipe(
            start = boundsInRoot.center,
            end = boundsInRoot.bottomCenter + Offset(0f, 10f)
        )
    }

    // TODO(b/281584353) When touch mode can be changed globally,
    //  this should change to a mouse movement outside of the bounds.
    internal fun enterMouseMode() {
        mouseDragTo(boundsInRoot.centerLeft, durationMillis = 50)
        mouseDragTo(boundsInRoot.bottomRight, durationMillis = 50)
        mouseDragTo(boundsInRoot.center, durationMillis = 50)
    }

    protected fun performTouchGesture(block: TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag(pointerAreaTag).performTouchInput(block)
        rule.waitForIdle()
    }

    protected fun performMouseGesture(block: MouseInjectionScope.() -> Unit) {
        rule.onNodeWithTag(pointerAreaTag).performMouseInput(block)
        rule.waitForIdle()
    }

    protected fun touchDragTo(
        position: Offset,
        durationMillis: Long = 200L,
    ) {
        require(durationMillis > 0) { "Duration cannot be <= 0" }

        var startVar: Offset? = null
        var dragEventPeriodMillisVar: Long? = null
        performTouchGesture {
            startVar = requireNotNull(currentPosition()) { "No pointer is down to animate." }
            dragEventPeriodMillisVar = this.eventPeriodMillis
        }

        val start = startVar!!
        val dragEventPeriodMillis = dragEventPeriodMillisVar!!

        // How many steps will we take in durationMillis?
        // At least 1, and a number that will bring as as close to eventPeriod as possible
        val steps = max(1, (durationMillis / dragEventPeriodMillis.toFloat()).roundToInt())

        var previousTime = 0L
        for (step in 1..steps) {
            val progress = step / steps.toFloat()
            val nextTime = lerp(0, stop = durationMillis, fraction = progress)
            val nextPosition = lerp(start, position, nextTime / durationMillis.toFloat())
            performTouchGesture {
                moveTo(nextPosition, delayMillis = nextTime - previousTime)
            }
            previousTime = nextTime
        }
    }

    protected fun touchDragBy(
        delta: Offset,
        durationMillis: Long = 100L
    ) {
        var startVar: Offset? = null
        performTouchGesture {
            startVar = requireNotNull(currentPosition()) { "No pointer is down to animate." }
        }
        val start = startVar!!
        touchDragTo(start + delta, durationMillis)
    }

    protected fun mouseDragTo(
        position: Offset,
        durationMillis: Long = 200L,
    ) {
        require(durationMillis > 0) { "Duration cannot be <= 0" }

        var startVar: Offset? = null
        var dragEventPeriodMillisVar: Long? = null
        performMouseGesture {
            startVar = currentPosition
            dragEventPeriodMillisVar = this.eventPeriodMillis
        }
        val start = startVar!!
        val dragEventPeriodMillis = dragEventPeriodMillisVar!!

        // How many steps will we take in durationMillis?
        // At least 1, and a number that will bring as as close to eventPeriod as possible
        val steps = max(1, (durationMillis / dragEventPeriodMillis.toFloat()).roundToInt())

        var previousTime = 0L
        for (step in 1..steps) {
            val progress = step / steps.toFloat()
            val nextTime = lerp(0, stop = durationMillis, fraction = progress)
            val nextPosition = lerp(start, position, nextTime / durationMillis.toFloat())
            performMouseGesture {
                moveTo(nextPosition, delayMillis = nextTime - previousTime)
            }
            previousTime = nextTime
        }
    }

    protected fun mouseDragBy(
        delta: Offset,
        durationMillis: Long = 100L
    ) {
        var startVar: Offset? = null
        performMouseGesture {
            startVar = currentPosition
        }
        val start = startVar!!
        touchDragTo(start + delta, durationMillis)
    }
}
