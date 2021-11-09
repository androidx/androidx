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

package androidx.viewpager2.integration.testapp.test.util

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.test.espresso.InjectEventSecurityException
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.viewpager2.integration.testapp.test.util.SwipeAction.Direction.BACKWARD
import androidx.viewpager2.integration.testapp.test.util.SwipeAction.Direction.FORWARD
import androidx.viewpager2.widget.ViewPager2
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.Matcher

/**
 * ViewAction that issues a swipe gesture on a [ViewPager2] to move that ViewPager2 to the next
 * page, taking orientation and layout direction into account.
 */
fun swipeNext(): ViewAction {
    return SwipeAction(FORWARD)
}

/**
 * ViewAction that issues a swipe gesture on a [ViewPager2] to move that ViewPager2 to the previous
 * page, taking orientation and layout direction into account.
 */
fun swipePrevious(): ViewAction {
    return SwipeAction(BACKWARD)
}

/**
 * ViewAction that repetitively injects motion events until it succeeds. Useful as a black box
 * approach to dealing with window animations that happen when the test activity is launched, and
 * the test is trying to inject motion events before the activity is settled on the screen.
 */
fun waitForInjectMotionEvents(): ViewAction {
    return WaitForInjectMotionEventsAction()
}

private class SwipeAction(val direction: Direction) : ViewAction {
    enum class Direction {
        FORWARD,
        BACKWARD
    }

    override fun getDescription(): String = "Swiping $direction"

    override fun getConstraints(): Matcher<View> =
        allOf(
            anyOf(
                isAssignableFrom(ViewPager2::class.java),
                isDescendantOfA(isAssignableFrom(ViewPager2::class.java))
            ),
            isDisplayingAtLeast(90)
        )

    override fun perform(uiController: UiController, view: View) {
        val vp = if (view is ViewPager2) {
            view
        } else {
            var parent = view.parent
            while (parent !is ViewPager2 && parent != null) {
                parent = parent.parent
            }
            parent as ViewPager2
        }
        val isForward = direction == FORWARD
        val swipeAction: ViewAction
        if (vp.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
            swipeAction = if (isForward == vp.isRtl()) swipeRight() else swipeLeft()
        } else {
            swipeAction = if (isForward) swipeUp() else swipeDown()
        }
        swipeAction.perform(uiController, view)
    }

    private fun ViewPager2.isRtl(): Boolean {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
    }
}

private class WaitForInjectMotionEventsAction : ViewAction {
    override fun getDescription(): String {
        return "Inject MotionEvents with UiController until the injection stops failing"
    }

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isDisplayed()
    }

    override fun perform(uiController: UiController?, view: View?) {
        if (uiController == null || view == null) {
            throw PerformException.Builder()
                .withActionDescription("waiting for injection of MotionEvents")
                .withViewDescription("$view")
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
