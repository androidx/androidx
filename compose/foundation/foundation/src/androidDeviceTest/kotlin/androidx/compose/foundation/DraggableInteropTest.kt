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
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DraggableInteropTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    @Test
    fun draggable_velocityIsCorrect_whenComposeViewTranslates() {
        var dragVelocity = 1000f
        var view: ComposeView? = null
        activityRule.activityRule.scenario.onActivity { activity ->
            val root = FrameLayout(activity)
            activity.setContentView(root)

            view = ComposeView(activity)
            root.addView(view)

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

        fun dispatchMove(y: Float, timeDelta: Long = 10) {
            time += timeDelta
            activityRule.runOnIdle {
                view!!.dispatchTouchEvent(
                    MotionEvent.obtain(downTime, time, MotionEvent.ACTION_MOVE, 50f, y, 0)
                )
            }
        }

        // 1. Initial touch
        activityRule.runOnIdle {
            view!!.dispatchTouchEvent(
                MotionEvent.obtain(downTime, time, MotionEvent.ACTION_DOWN, 50f, 100f, 0)
            )
        }

        // slowly drag past touch slop (reach 160f)
        dispatchMove(120f, 100)
        dispatchMove(140f, 100)
        dispatchMove(160f, 100)

        // finger stays completely still for 200 ms to bring velocity to 0
        dispatchMove(160f, 100)
        dispatchMove(160f, 100)

        // 2. Translate view up by 50px physically
        // This means the local Y coordinate of the stationary finger becomes 160 + 50 = 210f
        activityRule.runOnIdle { view!!.translationY = -50f }

        // 3. Dispatch move at same physical spot but new local spot for 10 ms (local Y = 210f)
        dispatchMove(210f, 10)

        // 4. Release
        activityRule.runOnIdle {
            time += 10
            view!!.dispatchTouchEvent(
                MotionEvent.obtain(downTime, time, MotionEvent.ACTION_UP, 50f, 210f, 0)
            )
        }

        activityRule.waitForIdle()

        // Without the fix, the jump from 150f to 200f creates a 50px delta over 10ms -> 5000px/s
        // velocity
        // With the fix, rootOffset = -50 is added, canceling out the 50px local jump, leaving ~0
        // velocity.
        assertThat(dragVelocity.absoluteValue).isLessThan(100f)
    }
}
