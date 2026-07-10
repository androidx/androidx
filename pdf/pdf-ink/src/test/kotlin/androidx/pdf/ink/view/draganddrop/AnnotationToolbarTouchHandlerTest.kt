/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.ink.view.draganddrop

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AnnotationToolbarTouchHandlerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var dummyToolbar: View
    private lateinit var parentContainer: FrameLayout // Acts as the parent

    @Mock private lateinit var dragListener: ToolbarDragListener

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    @Before
    fun setup() {
        // Initialize @Mock annotations
        MockitoAnnotations.openMocks(this)

        parentContainer = spy(FrameLayout(context))

        dummyToolbar = View(context, null)

        parentContainer.addView(dummyToolbar)
    }

    @Test
    fun test_onTouchEvent_onInteractiveChild_ignoresDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { true }
        handler.setOnDragListener(dragListener)
        val event = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)

        val result = handler.onTouchEvent(event)

        assertFalse(result)
        verify(dragListener, never()).onDragStart(any())
    }

    @Test
    fun test_touchHandler_longPress_startDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        handler.setOnDragListener(dragListener)
        val event = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)

        handler.onTouchEvent(event)

        // fast-forward time
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val shadowParent = shadowOf(parentContainer)
        // Check the actual state of the view
        assertTrue(shadowParent.disallowInterceptTouchEvent)

        verify(dragListener).onDragStart(any())
    }

    @Test
    fun test_touchHandler_continuous_actionMove_doesNotStartDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        handler.setOnDragListener(dragListener)

        val downEvent = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        val moveEvent = obtainEvent(MotionEvent.ACTION_MOVE, 0f, (touchSlop + 10).toFloat())

        handler.onTouchEvent(downEvent)
        handler.onTouchEvent(moveEvent)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(dragListener, never()).onDragStart(any())
    }

    @Test
    fun test_touchHandler_actionUp_endsDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        handler.setOnDragListener(dragListener)
        val downEvent = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        val upEvent = obtainEvent(MotionEvent.ACTION_UP, 0f, 0f)

        handler.onTouchEvent(downEvent)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks() // Trigger Drag
        handler.onTouchEvent(upEvent)

        verify(dragListener).onDragEnd()

        val shadowParent = shadowOf(parentContainer)
        // Assert parent's disallowInterceptTouchEvent state
        assertFalse(shadowParent.disallowInterceptTouchEvent)
    }

    @Test
    fun test_onInterceptTouchEvent_actionUp_endsDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        handler.setOnDragListener(dragListener)
        val downEvent = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        val upEvent = obtainEvent(MotionEvent.ACTION_UP, 0f, 0f)

        handler.onInterceptTouchEvent(downEvent)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks() // Trigger Drag
        handler.onInterceptTouchEvent(upEvent)

        verify(dragListener).onDragEnd()

        val shadowParent = shadowOf(parentContainer)
        // Assert parent's disallowInterceptTouchEvent state
        assertFalse(shadowParent.disallowInterceptTouchEvent)
    }

    @Test
    fun test_onInterceptTouchEvent_actionCancel_endsDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        handler.setOnDragListener(dragListener)
        val downEvent = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        val cancelEvent = obtainEvent(MotionEvent.ACTION_CANCEL, 0f, 0f)

        handler.onInterceptTouchEvent(downEvent)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks() // Trigger Drag
        handler.onInterceptTouchEvent(cancelEvent)

        verify(dragListener).onDragEnd()

        val shadowParent = shadowOf(parentContainer)
        // Assert parent's disallowInterceptTouchEvent state
        assertFalse(shadowParent.disallowInterceptTouchEvent)
    }

    @Test
    fun test_onInterceptTouchEvent_actionUp_WhileNotDragging() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        handler.setOnDragListener(dragListener)
        val upEvent = obtainEvent(MotionEvent.ACTION_UP, 0f, 0f)
        // pass up event when not dragging
        handler.onInterceptTouchEvent(upEvent)
        // verify no interactions with onDrag event API
        verify(dragListener, never()).onDragEnd()
    }

    @Test
    fun test_onTouchEvent_actionUp_WhileNotDragging() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        handler.setOnDragListener(dragListener)
        val upEvent = obtainEvent(MotionEvent.ACTION_UP, 0f, 0f)
        // pass up event when not dragging
        handler.onTouchEvent(upEvent)
        // verify no interactions with onDrag event API
        verify(dragListener, never()).onDragEnd()
    }

    private fun obtainEvent(action: Int, x: Float, y: Float): MotionEvent {
        return MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            action,
            x,
            y,
            0,
        )
    }
}
