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

package androidx.pdf.view.annotation.draganddrop

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28])
class AnnotationToolbarTouchHandlerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var dummyToolbar: View

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    @Before
    fun setup() {
        dummyToolbar = spy(View(context, null))
    }

    @Test
    fun test_onTouchEvent_onInteractiveChild_ignoresDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { true }
        val event = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)

        val result = handler.onTouchEvent(event)

        assertFalse(result)
        verify(dummyToolbar, never()).startDragAndDrop(anyOrNull(), any(), anyOrNull(), any())
    }

    @Test
    fun test_touchHandler_longPress_startDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }
        val event = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)

        handler.onTouchEvent(event)

        // fast-forward time
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(dummyToolbar).startDragAndDrop(anyOrNull(), any(), anyOrNull(), any())
    }

    @Test
    fun test_touchHandler_continuous_actionMove_doesNotStartDrag() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }

        val downEvent = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        val moveEvent = obtainEvent(MotionEvent.ACTION_MOVE, 0f, (touchSlop + 10).toFloat())

        handler.onTouchEvent(downEvent)
        handler.onTouchEvent(moveEvent)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(dummyToolbar, never()).startDragAndDrop(anyOrNull(), any(), anyOrNull(), any())
    }

    @Test
    fun test_touchHandler_afterScroll_canStartDragOnNextGesture() {
        val handler = AnnotationToolbarTouchHandler(dummyToolbar) { false }

        // 1. First gesture: down then move beyond touchSlop to trigger onScroll
        val down1 = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        val move1 = obtainEvent(MotionEvent.ACTION_MOVE, 0f, (touchSlop + 10).toFloat())
        val up1 = obtainEvent(MotionEvent.ACTION_UP, 0f, (touchSlop + 10).toFloat())

        handler.onTouchEvent(down1)
        handler.onTouchEvent(move1)
        handler.onTouchEvent(up1)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(dummyToolbar, never()).startDragAndDrop(anyOrNull(), any(), anyOrNull(), any())

        // 2. Second gesture: down and long press without moving
        val down2 = obtainEvent(MotionEvent.ACTION_DOWN, 0f, 0f)
        handler.onTouchEvent(down2)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(dummyToolbar).startDragAndDrop(anyOrNull(), any(), anyOrNull(), any())
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
