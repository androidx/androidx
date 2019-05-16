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

package androidx.viewpager2.widget.swipe

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.InjectEventSecurityException
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher

class WaitForInjectMotionEventsAction : ViewAction {
    companion object {
        fun waitForInjectMotionEvents(): ViewAction {
            return WaitForInjectMotionEventsAction()
        }
    }

    override fun getDescription(): String {
        return "Inject MotionEvents with UiController until the injection stops failing"
    }

    override fun getConstraints(): Matcher<View> {
        return isDisplayed()
    }

    override fun perform(uiController: UiController?, view: View?) {
        if (uiController == null || view == null) {
            throw PerformException.Builder()
                .withActionDescription("waiting for injection of MotionEvents")
                .withViewDescription(view?.toString() ?: "null")
                .withCause(IllegalStateException("Missing UiController and/or View"))
                .build()
        }

        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        val x = rect.exactCenterX()
        val y = rect.exactCenterY()

        var t: Long
        var event: MotionEvent
        var injectionSucceeded = false

        while (!injectionSucceeded) {
            t = SystemClock.uptimeMillis()
            event = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
            try {
                if (uiController.injectMotionEvent(event)) {
                    injectionSucceeded = true
                }
            } catch (e: InjectEventSecurityException) {
            }
            event.recycle()

            if (injectionSucceeded) {
                event = MotionEvent.obtain(t, t + 10, MotionEvent.ACTION_CANCEL, x, y, 0)
                uiController.injectMotionEvent(event)
                event.recycle()
            } else {
                uiController.loopMainThreadForAtLeast(10)
            }
        }
    }
}