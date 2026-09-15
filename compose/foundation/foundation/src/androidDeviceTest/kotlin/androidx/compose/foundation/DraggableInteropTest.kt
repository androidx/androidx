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

package androidx.compose.foundation

import android.os.SystemClock
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.absoluteValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Note that this scenario is only exercised once both
 * [ComposeFoundationFlags.isDraggableVelocityTrackerFixEnabled] and
 * [androidx.compose.ui.ComposeUiFlags.isTriggerMoveEventsWhenLocationHasNotChangedEnabled] are
 * enabled: the former selects the velocity tracking path under test, and the latter makes Compose
 * dispatch the move event that follows the translation, whose raw coordinates are unchanged and
 * which would otherwise be dropped before the velocity tracker could see the jump.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DraggableInteropTest {
    @get:Rule val activityRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun draggable_velocityIsCorrect_whenComposeViewTranslates() {
        var dragVelocity = 1000f
        var root: FrameLayout? = null
        var view: ComposeView? = null
        activityRule.activityRule.scenario.onActivity { activity ->
            root = FrameLayout(activity)
            activity.setContentView(root)

            view = ComposeView(activity)
            root!!.addView(view)

            view!!.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .draggable(
                            state = rememberDraggableState {},
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> dragVelocity = velocity },
                        )
                )
            }
        }
        activityRule.waitForIdle()

        val downTime = SystemClock.uptimeMillis()
        var time = downTime

        // Events are dispatched to the parent, so their coordinates stay in the parent's (fixed)
        // space and the View hierarchy applies the ComposeView's translation for us. Dispatching
        // straight to the ComposeView and hand-adjusting y instead would leave rawY inconsistent
        // with y, and AndroidComposeView derives its window position from rawY - so the
        // translation would cancel itself out and this test could never fail.
        //
        // Events are also 10 ms apart: the VelocityTracker discards everything before a gap larger
        // than 40 ms, so a coarser cadence would leave it with too few samples to compute a
        // velocity at all, and the assertion below would hold no matter what.
        fun dispatch(action: Int, y: Float) {
            activityRule.runOnIdle {
                root!!.dispatchTouchEvent(MotionEvent.obtain(downTime, time, action, 50f, y, 0))
            }
            time += 10
        }

        fun dispatchMove(y: Float) = dispatch(MotionEvent.ACTION_MOVE, y)

        // 1. Initial touch
        dispatch(MotionEvent.ACTION_DOWN, 100f)

        // drag past touch slop (reach 160f)
        dispatchMove(120f)
        dispatchMove(140f)
        dispatchMove(160f)

        // finger stays completely still, long enough for the initial motion to fall out of the
        // tracker's 100 ms horizon and bring the velocity to 0
        repeat(15) { dispatchMove(160f) }

        // 2. Translate the ComposeView up by 50px.
        // The finger doesn't move, but its Y coordinate local to the ComposeView jumps by +50.
        activityRule.runOnIdle { view!!.translationY = -50f }

        // 3. Keep the finger at the exact same physical spot
        dispatchMove(160f)

        // 4. Release
        dispatch(MotionEvent.ACTION_UP, 160f)

        activityRule.waitForIdle()

        // The finger was stationary for the whole second half of the gesture, so the fling velocity
        // must be ~0. Without the fix, the 50px jump in local coordinates caused by translating the
        // view is mistaken for pointer movement and reported as a large velocity instead.
        assertThat(dragVelocity.absoluteValue).isLessThan(100f)
    }
}
