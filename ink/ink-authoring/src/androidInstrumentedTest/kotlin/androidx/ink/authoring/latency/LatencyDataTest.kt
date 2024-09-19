/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.latency

import android.view.MotionEvent
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLatencyDataApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class LatencyDataTest {
    @Test
    fun setters_setFieldsDirectly() {
        val strokeId = InProgressStrokeId()
        val latencyData =
            LatencyData().apply {
                eventAction = LatencyData.EventAction.PREDICTED_MOVE
                strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                this.strokeId = strokeId
                batchSize = 5
                batchIndex = 3
                osDetectsEvent = -123456L
                strokesViewGetsAction = 0L
                strokesViewFinishesDrawCalls = 123L
                estimatedPixelPresentationTime = 456L
                canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 789L
                hwuiInProgressStrokesRenderHelperData.finishesDrawCalls = 1011L
            }

        with(latencyData) {
            assertThat(eventAction).isEqualTo(LatencyData.EventAction.PREDICTED_MOVE)
            assertThat(strokeAction).isEqualTo(LatencyData.StrokeAction.PREDICTED_ADD)
            assertThat(this.strokeId).isEqualTo(strokeId)
            assertThat(this.strokeId.hashCode()).isEqualTo(strokeId.hashCode())
            assertThat(batchSize).isEqualTo(5)
            assertThat(batchIndex).isEqualTo(3)
            assertThat(osDetectsEvent).isEqualTo(-123456L)
            assertThat(strokesViewGetsAction).isEqualTo(0L)
            assertThat(strokesViewFinishesDrawCalls).isEqualTo(123L)
            assertThat(estimatedPixelPresentationTime).isEqualTo(456L)
            assertThat(canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls).isEqualTo(789L)
            assertThat(hwuiInProgressStrokesRenderHelperData.finishesDrawCalls).isEqualTo(1011L)
        }
    }

    @Test
    fun reset_resetsAllFields() {
        val strokeId = InProgressStrokeId()
        val latencyData =
            LatencyData().apply {
                eventAction = LatencyData.EventAction.PREDICTED_MOVE
                strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                this.strokeId = strokeId
                batchSize = 5
                batchIndex = 3
                osDetectsEvent = -123456L
                strokesViewGetsAction = 0L
                strokesViewFinishesDrawCalls = 123L
                estimatedPixelPresentationTime = 456L
                canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 789L
                hwuiInProgressStrokesRenderHelperData.finishesDrawCalls = 1011L
            }
        latencyData.reset()

        with(latencyData) {
            assertThat(eventAction).isEqualTo(LatencyData.EventAction.UNKNOWN)
            assertThat(strokeAction).isEqualTo(LatencyData.StrokeAction.UNKNOWN)
            assertThat(this.strokeId).isEqualTo(LatencyData.UNKNOWN_STROKE_ID)
            assertThat(this.strokeId.hashCode()).isEqualTo(LatencyData.UNKNOWN_STROKE_ID.hashCode())
            assertThat(batchSize).isEqualTo(Int.MIN_VALUE)
            assertThat(batchIndex).isEqualTo(Int.MIN_VALUE)
            assertThat(osDetectsEvent).isEqualTo(Long.MIN_VALUE)
            assertThat(strokesViewGetsAction).isEqualTo(Long.MIN_VALUE)
            assertThat(strokesViewFinishesDrawCalls).isEqualTo(Long.MIN_VALUE)
            assertThat(estimatedPixelPresentationTime).isEqualTo(Long.MIN_VALUE)
            assertThat(canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls)
                .isEqualTo(Long.MIN_VALUE)
            assertThat(hwuiInProgressStrokesRenderHelperData.finishesDrawCalls)
                .isEqualTo(Long.MIN_VALUE)
        }
    }

    @Test
    fun toString_showsAllFields() {
        val latencyData =
            LatencyData().apply {
                eventAction = LatencyData.EventAction.PREDICTED_MOVE
                strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                strokeId = InProgressStrokeId()
                batchSize = 555
                batchIndex = 333
                osDetectsEvent = -123456L
                strokesViewGetsAction = 0L
                strokesViewFinishesDrawCalls = 123L
                estimatedPixelPresentationTime = 456L
                canvasFrontBufferStrokesRenderHelperData.finishesDrawCalls = 789L
                hwuiInProgressStrokesRenderHelperData.finishesDrawCalls = 1011L
            }

        val str = latencyData.toString()
        assertThat(str).contains("PREDICTED_ADD")
        assertThat(str).contains("PREDICTED_MOVE")
        assertThat(str).contains("InProgressStrokeId")
        assertThat(str).contains("555")
        assertThat(str).contains("333")
        assertThat(str).contains("-123456")
        assertThat(str).contains("0")
        assertThat(str).contains("123")
        assertThat(str).contains("456")
        assertThat(str).contains("789")
        assertThat(str).contains("1011")
    }

    @Test
    fun eventActionFromMotionEvent_mapsToEventAction() {
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
                )
            )
            .isEqualTo(LatencyData.EventAction.DOWN)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_POINTER_DOWN, 0f, 0f, 0)
                )
            )
            .isEqualTo(LatencyData.EventAction.DOWN)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0)
                )
            )
            .isEqualTo(LatencyData.EventAction.MOVE)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
                )
            )
            .isEqualTo(LatencyData.EventAction.UP)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_POINTER_UP, 0f, 0f, 0)
                )
            )
            .isEqualTo(LatencyData.EventAction.UP)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
                )
            )
            .isEqualTo(LatencyData.EventAction.CANCEL)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 0f, 0f, 0)
                )
            )
            .isEqualTo(LatencyData.EventAction.UNKNOWN)
    }

    fun eventActionFromMotionEvent_predictedOnlyAppliesToMove() {
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0),
                    predicted = true,
                )
            )
            .isEqualTo(LatencyData.EventAction.PREDICTED_MOVE)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0),
                    predicted = true,
                )
            )
            .isEqualTo(LatencyData.EventAction.DOWN)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0),
                    predicted = true,
                )
            )
            .isEqualTo(LatencyData.EventAction.UP)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0),
                    predicted = true,
                )
            )
            .isEqualTo(LatencyData.EventAction.CANCEL)
        assertThat(
                LatencyData.EventAction.fromMotionEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 0f, 0f, 0),
                    predicted = true,
                )
            )
            .isEqualTo(LatencyData.EventAction.UNKNOWN)
    }

    @Test
    fun eventActionToString_isAccurate() {
        assertThat(LatencyData.EventAction.UNKNOWN.toString()).endsWith("UNKNOWN")
        assertThat(LatencyData.EventAction.DOWN.toString()).endsWith("DOWN")
        assertThat(LatencyData.EventAction.MOVE.toString()).endsWith("MOVE")
        assertThat(LatencyData.EventAction.PREDICTED_MOVE.toString()).endsWith("PREDICTED_MOVE")
        assertThat(LatencyData.EventAction.UP.toString()).endsWith("UP")
        assertThat(LatencyData.EventAction.CANCEL.toString()).endsWith("CANCEL")
    }

    @Test
    fun eventActionEquals_isAccurate() {
        assertThat(LatencyData.EventAction.DOWN).isEqualTo(LatencyData.EventAction.DOWN)
        assertThat(LatencyData.EventAction.MOVE)
            .isNotEqualTo(LatencyData.EventAction.PREDICTED_MOVE)
        assertThat(LatencyData.EventAction.UNKNOWN).isNotEqualTo(0)
    }

    @Test
    fun strokeActionToString_isAccurate() {
        assertThat(LatencyData.StrokeAction.UNKNOWN.toString()).endsWith("UNKNOWN")
        assertThat(LatencyData.StrokeAction.START.toString()).endsWith("START")
        assertThat(LatencyData.StrokeAction.ADD.toString()).endsWith("ADD")
        assertThat(LatencyData.StrokeAction.PREDICTED_ADD.toString()).endsWith("PREDICTED_ADD")
        assertThat(LatencyData.StrokeAction.FINISH.toString()).endsWith("FINISH")
        assertThat(LatencyData.StrokeAction.CANCEL.toString()).endsWith("CANCEL")
    }

    @Test
    fun strokeActionEquals_isAccurate() {
        assertThat(LatencyData.StrokeAction.START).isEqualTo(LatencyData.StrokeAction.START)
        assertThat(LatencyData.StrokeAction.ADD)
            .isNotEqualTo(LatencyData.StrokeAction.PREDICTED_ADD)
        assertThat(LatencyData.StrokeAction.UNKNOWN).isNotEqualTo(0)
    }
}
