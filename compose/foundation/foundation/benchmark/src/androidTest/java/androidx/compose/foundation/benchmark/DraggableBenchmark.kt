/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.foundation.benchmark

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.benchmark.lazy.MotionEventHelper
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DraggableBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun performPointerInputDrag() {
        benchmarkRule.runBenchmarkFor({ DraggablePerformScrollTestCase() }) {
            lateinit var case: DraggablePerformScrollTestCase
            lateinit var rootView: View
            benchmarkRule.runOnUiThread {
                doFramesUntilNoChangesPending()
                case = getTestCase()
                rootView = getHostView()
            }
            val motionEventHelper = MotionEventHelper(rootView)
            var overallDrag = 0f
            var expectedDragged = case.draggedDelta
            val viewCenter = Offset(rootView.measuredWidth / 2f, rootView.measuredHeight / 2f)
            benchmarkRule.measureRepeatedOnUiThread {
                motionEventHelper.sendEvent(MotionEvent.ACTION_DOWN, viewCenter)
                motionEventHelper.sendEvent(
                    MotionEvent.ACTION_MOVE,
                    Offset(0f, case.touchSlop + DragDelta),
                )
                motionEventHelper.sendEvent(MotionEvent.ACTION_UP, Offset.Zero)
                expectedDragged += DragDelta
                overallDrag += DragDelta
                assertThat(case.draggedDelta).isEqualTo(expectedDragged)
            }

            assertThat(case.draggedDelta).isEqualTo(overallDrag)
        }
    }
}

private class DraggablePerformScrollTestCase : ComposeTestCase {
    var draggedDelta = 0f
        private set

    val draggableState = DraggableState { delta -> draggedDelta += delta }

    var touchSlop: Float = 0f
        private set

    @Composable
    override fun Content() {
        touchSlop = LocalViewConfiguration.current.touchSlop
        Box(Modifier.fillMaxSize().draggable(draggableState, Orientation.Vertical))
    }
}

private const val DragDelta = 100
