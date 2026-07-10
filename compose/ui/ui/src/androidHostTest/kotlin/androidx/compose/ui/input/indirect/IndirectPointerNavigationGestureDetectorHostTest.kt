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

package androidx.compose.ui.input.indirect

import android.app.Activity
import android.os.Looper
import android.util.StringBuilderPrinter
import android.view.InputDevice.SOURCE_TOUCH_NAVIGATION
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.platform.IndirectPointerNavigationGestureDetector
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 29)
class IndirectPointerNavigationGestureDetectorHostTest {

    @Test
    fun dispose_clearsPendingMessagesInGestureDetector() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val detector = IndirectPointerNavigationGestureDetector(activity) {}
        detector.primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X

        // Send a down event to trigger a scheduled message (e.g. SHOW_PRESS or LONG_PRESS)
        val downTime = System.currentTimeMillis()
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(100f, 100f))
        detector.onIndirectPointerEvent(downEvent, isConsumed = false)

        // Verify that the message is scheduled by dumping the main looper's message queue.
        val sbBefore = java.lang.StringBuilder()
        val printerBefore = StringBuilderPrinter(sbBefore)
        Looper.getMainLooper().dump(printerBefore, "")
        val dumpBefore = sbBefore.toString()
        assertTrue(
            "Expected to find GestureDetector\$GestureHandler in the looper before dispose. Dump: $dumpBefore",
            dumpBefore.contains("android.view.GestureDetector\$GestureHandler"),
        )

        // Now dispose the detector. This should cancel/remove the message.
        detector.dispose()

        // Verify that the message is gone from the looper.
        val sbAfter = java.lang.StringBuilder()
        val printerAfter = StringBuilderPrinter(sbAfter)
        Looper.getMainLooper().dump(printerAfter, "")
        val dumpAfter = sbAfter.toString()
        assertTrue(
            "Expected NOT to find GestureDetector\$GestureHandler in the looper after dispose. Dump: $dumpAfter",
            !dumpAfter.contains("android.view.GestureDetector\$GestureHandler"),
        )
    }

    private fun createDownIndirectPointerEvent(
        downTime: Long,
        position: Offset,
        uptimeMillis: Long = downTime,
        previousUptimeMillis: Long = downTime,
        previousPosition: Offset = position,
        previousPressed: Boolean = false,
    ): IndirectPointerEvent =
        IndirectPointerEvent(
            changes =
                listOf(
                    IndirectPointerInputChange(
                        id = PointerId(0L),
                        uptimeMillis = uptimeMillis,
                        position = position,
                        pressed = true,
                        pressure = 1.0f,
                        previousUptimeMillis = previousUptimeMillis,
                        previousPosition = previousPosition,
                        previousPressed = previousPressed,
                    )
                ),
            type = IndirectPointerEventType.Press,
            primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            motionEvent =
                obtainIndirectMotionEvent(
                    downTime = downTime,
                    eventTime = uptimeMillis,
                    action = MotionEvent.ACTION_DOWN,
                    coordinates = position,
                ),
        )

    private fun obtainIndirectMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        coordinates: Offset,
    ): MotionEvent {
        return MotionEvent.obtain(
                /* downTime = */ downTime,
                /* eventTime = */ eventTime,
                /* action = */ action,
                /* x = */ coordinates.x,
                /* y = */ coordinates.y,
                /* metaState = */ 0,
            )
            .apply { source = SOURCE_TOUCH_NAVIGATION }
    }
}
