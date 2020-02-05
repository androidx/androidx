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

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Layout
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import androidx.ui.unit.IntPx
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// TODO(shepshapard): Test that all events related to scaling are consumed.

@LargeTest
@RunWith(JUnit4::class)
class ScaleGestureDetectorTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var scaleObserver: ScaleObserver
    private lateinit var touchSlop: IntPx
    private lateinit var view: View

    @Before
    fun setup() {
        scaleObserver = spy(MyScaleObserver())

        val activity = activityTestRule.activity
        assertTrue(activity.hasFocusLatch.await(5, TimeUnit.SECONDS))

        val setupLatch = CountDownLatch(2)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                touchSlop = with(DensityAmbient.current) { TouchSlop.toIntPx() }
                ScaleGestureDetector(scaleObserver) {
                    Layout(
                        measureBlock = { _, _ ->
                            layout(touchSlop * 4, touchSlop * 4) {
                                setupLatch.countDown()
                            }
                        }, children = {}
                    )
                }
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }
        assertTrue(setupLatch.await(1000, TimeUnit.SECONDS))
    }

    @Test
    fun ui_pointerMovementWithinTouchSlop_noCallbacksCalled() {

        val touchSlopFloat = touchSlop.value.toFloat()

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlopFloat, 50f))
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(PointerProperties(0), PointerProperties(1)),
            arrayOf(
                PointerCoords(touchSlopFloat, 50f),
                PointerCoords(touchSlopFloat * 3, 50f)
            )
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlopFloat * 0, 50f),
                PointerCoords(touchSlopFloat * 4, 50f)
            )
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlopFloat * 0, 50f),
                PointerCoords(touchSlopFloat * 4, 50f)
            )
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
        }

        verifyNoMoreInteractions(scaleObserver)
    }

    @Test
    fun ui_pointerMovementBeyondTouchSlop_correctCallbacksInOrder() {

        val touchSlopFloat = touchSlop.value.toFloat()

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlopFloat, 50f))
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(PointerProperties(0), PointerProperties(1)),
            arrayOf(
                PointerCoords(touchSlopFloat, 50f),
                PointerCoords(touchSlopFloat * 3, 50f)
            )
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(-touchSlopFloat * 2, 50f),
                PointerCoords(touchSlopFloat * 6, 50f)
            )
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(-touchSlopFloat * 2, 50f),
                PointerCoords(touchSlopFloat * 6, 50f)
            )
        )
        val up2 = MotionEvent(
            30,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlopFloat * 6, 50f)
            )
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
            view.dispatchTouchEvent(up2)
        }

        scaleObserver.inOrder {
            verify().onStart()
            verify().onScale(4f)
            verify().onStop()
        }
    }

    // Verify when onCancel should be called.

    @Test
    fun ui_downMoveBeyondSlopCancel_correctCallbacksInOrder() {

        val touchSlopFloat = touchSlop.value.toFloat()

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlopFloat, 50f))
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(PointerProperties(0), PointerProperties(1)),
            arrayOf(
                PointerCoords(touchSlopFloat, 50f),
                PointerCoords(touchSlopFloat * 3, 50f)
            )
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(-touchSlopFloat * 2, 50f),
                PointerCoords(touchSlopFloat * 6, 50f)
            )
        )
        val cancel = MotionEvent(
            30,
            MotionEvent.ACTION_CANCEL,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(-touchSlopFloat * 2, 50f),
                PointerCoords(touchSlopFloat * 6, 50f)
            )
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(cancel)
        }

        scaleObserver.inOrder {
            verify().onStart()
            verify().onScale(4f)
            verify().onCancel()
        }
    }
}

@Suppress("RedundantOverride")
open class MyScaleObserver : ScaleObserver {
    override fun onStart() {
        super.onStart()
    }

    override fun onScale(scaleFactor: Float) {}

    override fun onStop() {
        super.onStop()
    }
}
