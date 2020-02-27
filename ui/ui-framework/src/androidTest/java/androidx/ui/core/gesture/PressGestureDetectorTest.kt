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
package androidx.ui.core.gesture

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Layout
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(JUnit4::class)
class PressGestureDetectorTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var onPress: ((PxPosition) -> Unit)
    private lateinit var onRelease: (() -> Unit)
    private lateinit var onCancel: (() -> Unit)
    private lateinit var view: View
    private lateinit var theActivity: Activity

    @Before
    fun setup() {
        val activity = activityTestRule.activity
        theActivity = activity
        assertTrue(activity.hasFocusLatch.await(5, TimeUnit.SECONDS))

        onPress = spy { }
        onCancel = spy { }
        onRelease = spy { }
    }

    // Verifies behavior for scenarios when onPress should not be called.

    @Test
    fun ui_disabledDown_onPressNotCalled() {
        compose(false)

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
        }

        verify(onPress, never()).invoke(any())
    }

    // Verifies behavior for scenarios when onPress should be called.

    @Test
    fun ui_down_onPressCalledOnce() {
        compose()

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
        }

        verify(onPress).invoke(any())
    }

    // Verifies behavior for scenarios when onRelease should not be called.

    @Test
    fun ui_disabledDownUp_onReleaseNotCalled() {
        compose(false)

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val up = MotionEvent(
            10,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(up)
        }

        verify(onRelease, never()).invoke()
    }

    // Verifies behavior for scenarios when onRelease should be called.

    @Test
    fun ui_downUp_onReleaseCalledOnce() {
        compose(false)

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val up = MotionEvent(
            10,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(up)
        }

        verify(onRelease, never()).invoke()
    }

    // Verify when onCancel should be called.

    @Test
    fun ui_downCancel_onCancelCalled() {
        compose()

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        val cancel = MotionEvent(
            10,
            MotionEvent.ACTION_CANCEL,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f))
        )
        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(cancel)
        }

        verify(onCancel).invoke()
    }

    private fun compose(enabled: Boolean? = null) {
        val setupLatch = CountDownLatch(2)
        activityTestRule.runOnUiThreadIR {
            theActivity.setContent {
                if (enabled != null) {
                    PressGestureDetector(onPress, onRelease, onCancel, enabled = enabled) {
                        content {
                            setupLatch.countDown()
                        }
                    }
                } else {
                    PressGestureDetector(onPress, onRelease, onCancel) {
                        content {
                            setupLatch.countDown()
                        }
                    }
                }
            }

            view = theActivity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }
        assertTrue(setupLatch.await(1000, TimeUnit.SECONDS))
    }

    @Composable
    fun content(onLayout: () -> Unit) {
        Layout(
            measureBlock = { _, _, _ ->
                layout(100.ipx, 100.ipx) {
                    onLayout()
                }
            }, children = emptyContent()
        )
    }
}