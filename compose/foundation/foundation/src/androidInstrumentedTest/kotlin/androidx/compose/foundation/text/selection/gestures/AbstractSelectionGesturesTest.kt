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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.contextmenu.internal.ProvidePlatformTextContextMenuToolbar
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.selection.HandlePressedScope
import androidx.compose.foundation.text.selection.gestures.util.FakeHapticFeedback
import androidx.compose.foundation.text.selection.gestures.util.mouseDragNodeBy
import androidx.compose.foundation.text.selection.gestures.util.mouseDragNodeTo
import androidx.compose.foundation.text.selection.gestures.util.touchDragNodeBy
import androidx.compose.foundation.text.selection.gestures.util.touchDragNodeTo
import androidx.compose.foundation.text.selection.withHandlePressed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import org.junit.Before
import org.junit.Rule

const val RtlChar = "\u05D1"

internal abstract class AbstractSelectionGesturesTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    protected abstract val pointerAreaTag: String

    protected open var textDirection: ResolvedTextDirection = ResolvedTextDirection.Ltr

    protected val hapticFeedback = FakeHapticFeedback()
    protected val fontFamily = TEST_FONT_FAMILY

    // small enough to fit in narrow screen in pre-submit,
    // big enough that pointer movement can target a single char on center
    protected val fontSize = 15.sp
    protected val density = Density(1f)

    protected lateinit var textToolbar: TextToolbar
    protected var spyTextActionModeCallback: SpyTextActionModeCallback? = null

    @Composable abstract fun Content()

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun setup() {
        rule.setTextFieldTestContent {
            if (ComposeFoundationFlags.isNewContextMenuEnabled) {
                val spyTextActionModeCallback =
                    SpyTextActionModeCallback().also { spyTextActionModeCallback = it }

                ProvidePlatformTextContextMenuToolbar(
                    callbackInjector = { spyTextActionModeCallback.apply { delegate = it } }
                ) {
                    InnerContent()
                }
            } else {
                InnerContent()
            }
        }
    }

    @Composable
    private fun InnerContent() {
        textToolbar = LocalTextToolbar.current
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalViewConfiguration provides
                TestViewConfiguration(
                    minimumTouchTargetSize = DpSize.Zero,
                    touchSlop = 0.1f, // less than 1, not too close to 0
                ),
            LocalHapticFeedback provides hapticFeedback,
        ) {
            Box(modifier = Modifier.padding(32.dp).fillMaxSize().wrapContentSize()) { Content() }
        }
    }

    protected val bounds
        get() = rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().size.toSize().toRect()

    private val left
        get() = bounds.left + 1f

    private val top
        get() = bounds.top + 1f

    private val right
        get() = bounds.right - 1f

    private val bottom
        get() = bounds.bottom - 1f

    private val start
        get() = textDirection.take(ltr = left, rtl = right)

    private val end
        get() = textDirection.take(ltr = right, rtl = left)

    protected val topStart
        get() = Offset(start, top)

    protected val centerStart
        get() = Offset(start, center.y)

    protected val bottomStart
        get() = Offset(start, bottom)

    protected val center
        get() = bounds.center

    protected val topEnd
        get() = Offset(end, top)

    protected val centerEnd
        get() = Offset(end, center.y)

    protected val bottomEnd
        get() = Offset(end, bottom)

    protected enum class VerticalDirection {
        UP,
        DOWN,
        CENTER,
    }

    protected enum class HorizontalDirection {
        START,
        END,
        CENTER,
    }

    // nudge 2f since we start 1f inwards from the edges and want to ensure we move over them if
    // we nudge outwards again
    protected fun Offset.nudge(
        xDirection: HorizontalDirection = HorizontalDirection.CENTER,
        yDirection: VerticalDirection = VerticalDirection.CENTER,
    ): Offset = Offset(x = x.adjustHorizontal(xDirection, 2f), y = y.adjustVertical(yDirection, 2f))

    private fun Float.adjustVertical(direction: VerticalDirection, diff: Float): Float =
        this +
            diff *
                when (direction) {
                    VerticalDirection.UP -> -1f
                    VerticalDirection.CENTER -> 0f
                    VerticalDirection.DOWN -> 1f
                }

    private fun Float.adjustHorizontal(direction: HorizontalDirection, diff: Float): Float =
        this +
            diff *
                when (direction) {
                    HorizontalDirection.START -> textDirection.take(ltr = -1f, rtl = 1f)
                    HorizontalDirection.CENTER -> 0f
                    HorizontalDirection.END -> textDirection.take(ltr = 1f, rtl = -1f)
                }

    private fun <T> ResolvedTextDirection.take(ltr: T, rtl: T): T =
        when (this) {
            ResolvedTextDirection.Ltr -> ltr
            ResolvedTextDirection.Rtl -> rtl
        }

    // TODO(b/281584353) When touch mode can be changed globally,
    //  this should change to a single tap outside of the bounds.
    internal fun TouchInjectionScope.enterTouchMode() {
        swipe(start = bounds.center, end = bounds.bottomCenter + Offset(0f, 10f))
    }

    // TODO(b/281584353) When touch mode can be changed globally,
    //  this should change to a mouse movement outside of the bounds.
    internal fun enterMouseMode() {
        mouseDragTo(bounds.centerLeft, durationMillis = 50)
        mouseDragTo(bounds.bottomRight, durationMillis = 50)
        mouseDragTo(bounds.center, durationMillis = 50)
    }

    protected fun performTouchGesture(block: TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag(pointerAreaTag).performTouchInput(block)
        rule.waitForIdle()
    }

    protected fun performMouseGesture(block: MouseInjectionScope.() -> Unit) {
        rule.onNodeWithTag(pointerAreaTag).performMouseInput(block)
    }

    protected fun withHandlePressed(handle: Handle, block: HandlePressedScope.() -> Unit) {
        rule.withHandlePressed(handle, block)
        rule.waitForIdle()
    }

    protected fun touchDragTo(position: Offset, durationMillis: Long = 200L) {
        rule.onNodeWithTag(pointerAreaTag).touchDragNodeTo(position, durationMillis)
        rule.waitForIdle()
    }

    protected fun touchDragBy(delta: Offset, durationMillis: Long = 100L) {
        rule.onNodeWithTag(pointerAreaTag).touchDragNodeBy(delta, durationMillis)
        rule.waitForIdle()
    }

    protected fun mouseDragTo(position: Offset, durationMillis: Long = 200L) {
        rule.onNodeWithTag(pointerAreaTag).mouseDragNodeTo(position, durationMillis)
        rule.waitForIdle()
    }

    protected fun mouseDragBy(delta: Offset, durationMillis: Long = 100L) {
        rule.onNodeWithTag(pointerAreaTag).mouseDragNodeBy(delta, durationMillis)
        rule.waitForIdle()
    }
}
