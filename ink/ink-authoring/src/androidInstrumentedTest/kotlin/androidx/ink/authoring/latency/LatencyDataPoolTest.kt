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

import android.os.Build
import android.view.MotionEvent
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLatencyDataApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class LatencyDataPoolTest {
    @Test
    fun obtain_getsDifferentInstancesEachTime() {
        val pool = LatencyDataPool(2)

        val data1 = pool.obtain()
        val data2 = pool.obtain()

        assertThat(data1).isNotSameInstanceAs(data2)
    }

    @Test
    fun obtain_canGetMoreInstancesThanWerePreallocated() {
        val pool = LatencyDataPool(2)

        val data1 = pool.obtain()
        val data2 = pool.obtain()
        val data3 = pool.obtain()

        assertThat(data3).isNotSameInstanceAs(data1)
        assertThat(data3).isNotSameInstanceAs(data2)
    }

    @Test
    fun recycle_resetsContents() {
        val pool = LatencyDataPool(2)

        val data1 = pool.obtain().apply { osDetectsEvent = 123L }
        val data2 = pool.obtain().apply { osDetectsEvent = 456L }
        val data3 = pool.obtain().apply { osDetectsEvent = 789L }

        pool.recycle(data1)
        pool.recycle(data2)
        pool.recycle(data3)

        assertThat(data1.osDetectsEvent).isEqualTo(Long.MIN_VALUE)
        assertThat(data2.osDetectsEvent).isEqualTo(Long.MIN_VALUE)
        assertThat(data3.osDetectsEvent).isEqualTo(Long.MIN_VALUE)
    }

    @Test
    fun recycle_reusesInstancesWhenObtainedAgain() {
        val pool = LatencyDataPool(2)

        val data1 = pool.obtain()
        val data2 = pool.obtain()
        val data3 = pool.obtain()

        pool.recycle(data1)
        pool.recycle(data2)
        pool.recycle(data3)

        val data4 = pool.obtain()
        val data5 = pool.obtain()
        val data6 = pool.obtain()
        val data7 = pool.obtain() // New instance; wasn't already in the pool.

        assertThat(data4).isSameInstanceAs(data1)
        assertThat(data5).isSameInstanceAs(data2)
        assertThat(data6).isSameInstanceAs(data3)
        assertThat(data7).isNotSameInstanceAs(data1)
        assertThat(data7).isNotSameInstanceAs(data2)
        assertThat(data7).isNotSameInstanceAs(data3)
    }

    // The code path for recording `osDetectsEvent` differs depending on SDK level. This test
    // provides
    // coverage for the Pre-U path.
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun obtainLatencyDataForSingleEvent_SdkPreU_setsInitialValues() {
        val pool = LatencyDataPool(2)

        // eventTime is 456 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_UP, 10f, 20f, 0)
        val strokeId = InProgressStrokeId()
        val data =
            pool.obtainLatencyDataForSingleEvent(
                event,
                LatencyData.StrokeAction.FINISH,
                strokeId,
                strokesViewGetsActionTimeNanos = 457_000_000,
            )

        // Before SDK U, the internal representation of the event time is in millis, but LatencyData
        // always uses nanos.
        assertThat(data.eventAction).isEqualTo(LatencyData.EventAction.UP)
        assertThat(data.strokeAction).isEqualTo(LatencyData.StrokeAction.FINISH)
        assertThat(data.strokeId).isEqualTo(strokeId)
        assertThat(data.batchSize).isEqualTo(1)
        assertThat(data.batchIndex).isEqualTo(0)
        assertThat(data.osDetectsEvent).isEqualTo(456_000_000L) // nanoseconds
        assertThat(data.strokesViewGetsAction).isEqualTo(457_000_000L)
    }

    // The code path for recording `osDetectsEvent` differs depending on SDK level. This test
    // provides
    // coverage for the U+ path.
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun obtainLatencyDataForSingleEvent_SdkUPlus_setsInitialValues() {
        val pool = LatencyDataPool(2)

        // eventTime is 456 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_UP, 10f, 20f, 0)
        val strokeId = InProgressStrokeId()
        val data =
            pool.obtainLatencyDataForSingleEvent(
                event,
                LatencyData.StrokeAction.FINISH,
                strokeId,
                strokesViewGetsActionTimeNanos = 457_000_000,
            )

        // In SDK U+, the internal representation of the event time is in nanoseconds, and full
        // precision is exposed in LatencyData. However, `MotionEvent.obtain` only accepts
        // milliseconds,
        // so there's no way to observe this full precision with fake input data.
        assertThat(data.eventAction).isEqualTo(LatencyData.EventAction.UP)
        assertThat(data.strokeAction).isEqualTo(LatencyData.StrokeAction.FINISH)
        assertThat(data.strokeId).isEqualTo(strokeId)
        assertThat(data.batchSize).isEqualTo(1)
        assertThat(data.batchIndex).isEqualTo(0)
        assertThat(data.osDetectsEvent).isEqualTo(456_000_000L) // nanoseconds
        assertThat(data.strokesViewGetsAction).isEqualTo(457_000_000L)
    }

    @Test
    fun obtainLatencyDataForSingleEvent_setsPredictedMove() {
        val pool = LatencyDataPool(2)
        val strokeId = InProgressStrokeId()

        // eventTime is 456 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_MOVE, 10f, 20f, 0)

        val regularData =
            pool.obtainLatencyDataForSingleEvent(
                event,
                LatencyData.StrokeAction.ADD,
                strokeId,
                strokesViewGetsActionTimeNanos = 457_000_000,
            )
        assertThat(regularData.eventAction).isEqualTo(LatencyData.EventAction.MOVE)
        assertThat(regularData.strokeAction).isEqualTo(LatencyData.StrokeAction.ADD)
        assertThat(regularData.strokeId).isEqualTo(strokeId)
        assertThat(regularData.strokeId.hashCode()).isEqualTo(strokeId.hashCode())
        assertThat(regularData.batchSize).isEqualTo(1)
        assertThat(regularData.batchIndex).isEqualTo(0)
        assertThat(regularData.osDetectsEvent).isEqualTo(456_000_000L) // nanoseconds
        assertThat(regularData.strokesViewGetsAction).isEqualTo(457_000_000L)

        val predictedData =
            pool.obtainLatencyDataForSingleEvent(
                event,
                LatencyData.StrokeAction.PREDICTED_ADD,
                strokeId,
                predicted = true,
                strokesViewGetsActionTimeNanos = 457_000_000,
            )
        assertThat(predictedData.eventAction).isEqualTo(LatencyData.EventAction.PREDICTED_MOVE)
        assertThat(predictedData.strokeAction).isEqualTo(LatencyData.StrokeAction.PREDICTED_ADD)
        assertThat(predictedData.strokeId).isEqualTo(strokeId)
        assertThat(predictedData.strokeId.hashCode()).isEqualTo(strokeId.hashCode())
        assertThat(predictedData.batchSize).isEqualTo(1)
        assertThat(predictedData.batchIndex).isEqualTo(0)
        assertThat(predictedData.osDetectsEvent).isEqualTo(456_000_000L) // nanoseconds
        assertThat(predictedData.strokesViewGetsAction).isEqualTo(457_000_000L)
    }

    @Test
    fun obtainLatencyDataForSingleEvent_ignoresHistoricalEvents() {
        val pool = LatencyDataPool(2)

        // eventTimes are 456, 789, and 1011 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        event.addBatch(789, 11f, 21f, 1f, 1f, 0)
        event.addBatch(1011, 12f, 22f, 1f, 1f, 0)
        val strokeId = InProgressStrokeId()
        val data =
            pool.obtainLatencyDataForSingleEvent(
                event,
                LatencyData.StrokeAction.ADD,
                strokeId,
                strokesViewGetsActionTimeNanos = 1012_000_000,
            )

        assertThat(data.eventAction).isEqualTo(LatencyData.EventAction.MOVE)
        assertThat(data.strokeAction).isEqualTo(LatencyData.StrokeAction.ADD)
        assertThat(data.strokeId).isEqualTo(strokeId)
        assertThat(data.strokeId.hashCode()).isEqualTo(strokeId.hashCode())
        assertThat(data.batchSize).isEqualTo(3)
        assertThat(data.batchIndex).isEqualTo(2)
        assertThat(data.osDetectsEvent).isEqualTo(1011_000_000L) // nanoseconds
        assertThat(data.strokesViewGetsAction).isEqualTo(1012_000_000L)
    }

    // The code path for recording `osDetectsEvent` differs depending on SDK level. This test
    // provides
    // coverage for the Pre-U path.
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun obtainLatencyDataForPrimaryAndHistoricalEvents_SdkPreU_makesDataForEveryEventInBatch() {
        val pool = LatencyDataPool(2)

        // eventTimes are 456, 789, and 1011 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        event.addBatch(789, 11f, 21f, 1f, 1f, 0)
        event.addBatch(1011, 12f, 22f, 1f, 1f, 0)
        val strokeId = InProgressStrokeId()

        val datas = ArrayDeque<LatencyData>()
        pool.obtainLatencyDataForPrimaryAndHistoricalEvents(
            event,
            LatencyData.StrokeAction.ADD,
            strokeId,
            strokesViewGetsActionTimeNanos = 1012_000_000,
            predicted = false,
            datas,
        )

        val expectedDatas =
            mutableListOf<LatencyData>(
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.MOVE
                    strokeAction = LatencyData.StrokeAction.ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 0
                    osDetectsEvent = 456_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.MOVE
                    strokeAction = LatencyData.StrokeAction.ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 1
                    osDetectsEvent = 789_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.MOVE
                    strokeAction = LatencyData.StrokeAction.ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 2
                    osDetectsEvent = 1011_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
            )
        assertThat(datas)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(expectedDatas)
            .inOrder()
    }

    // The code path for recording `osDetectsEvent` differs depending on SDK level. This test
    // provides
    // coverage for the U+ path.
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun obtainLatencyDataForPrimaryAndHistoricalEvents_SdkUPlus_makesDataForEveryEventInBatch() {
        val pool = LatencyDataPool(2)

        // eventTimes are 456, 789, and 1011 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        event.addBatch(789, 11f, 21f, 1f, 1f, 0)
        event.addBatch(1011, 12f, 22f, 1f, 1f, 0)
        val strokeId = InProgressStrokeId()

        val datas = ArrayDeque<LatencyData>()
        pool.obtainLatencyDataForPrimaryAndHistoricalEvents(
            event,
            LatencyData.StrokeAction.ADD,
            strokeId,
            strokesViewGetsActionTimeNanos = 1012_000_000,
            predicted = false,
            datas,
        )

        val expectedDatas =
            mutableListOf<LatencyData>(
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.MOVE
                    strokeAction = LatencyData.StrokeAction.ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 0
                    osDetectsEvent = 456_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.MOVE
                    strokeAction = LatencyData.StrokeAction.ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 1
                    osDetectsEvent = 789_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.MOVE
                    strokeAction = LatencyData.StrokeAction.ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 2
                    osDetectsEvent = 1011_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
            )
        assertThat(datas)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(expectedDatas)
            .inOrder()
    }

    @Test
    fun obtainLatencyDataForPrimaryAndHistoricalEvents_makesJustOneForEventWithNoHistory() {
        val pool = LatencyDataPool(2)

        // eventTime is 456 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        val strokeId = InProgressStrokeId()

        val datas = ArrayDeque<LatencyData>()
        pool.obtainLatencyDataForPrimaryAndHistoricalEvents(
            event,
            LatencyData.StrokeAction.ADD,
            strokeId,
            strokesViewGetsActionTimeNanos = 457_000_000,
            predicted = false,
            datas,
        )

        val expectedDatas =
            mutableListOf<LatencyData>(
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.MOVE
                    strokeAction = LatencyData.StrokeAction.ADD
                    this.strokeId = strokeId
                    batchSize = 1
                    batchIndex = 0
                    osDetectsEvent = 456_000_000L // nanoseconds
                    strokesViewGetsAction = 457_000_000L
                }
            )
        assertThat(datas)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(expectedDatas)
            .inOrder()
    }

    @Test
    fun obtainLatencyDataForPrimaryAndHistoricalEvents_setsPredictedMove() {
        val pool = LatencyDataPool(2)

        // eventTimes are 456, 789, and 1011 milliseconds.
        val event = MotionEvent.obtain(123, 456, MotionEvent.ACTION_MOVE, 10f, 20f, 0)
        event.addBatch(789, 11f, 21f, 1f, 1f, 0)
        event.addBatch(1011, 12f, 22f, 1f, 1f, 0)
        val strokeId = InProgressStrokeId()

        val datas = ArrayDeque<LatencyData>()
        pool.obtainLatencyDataForPrimaryAndHistoricalEvents(
            event,
            LatencyData.StrokeAction.PREDICTED_ADD,
            strokeId,
            strokesViewGetsActionTimeNanos = 1012_000_000,
            predicted = true,
            datas,
        )

        val expectedDatas =
            mutableListOf<LatencyData>(
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.PREDICTED_MOVE
                    strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 0
                    osDetectsEvent = 456_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.PREDICTED_MOVE
                    strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 1
                    osDetectsEvent = 789_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
                LatencyData().apply {
                    eventAction = LatencyData.EventAction.PREDICTED_MOVE
                    strokeAction = LatencyData.StrokeAction.PREDICTED_ADD
                    this.strokeId = strokeId
                    batchSize = 3
                    batchIndex = 2
                    osDetectsEvent = 1011_000_000L // nanoseconds
                    strokesViewGetsAction = 1012_000_000L
                },
            )
        assertThat(datas)
            .comparingElementsUsing(latencyDataEqual)
            .containsExactlyElementsIn(expectedDatas)
            .inOrder()
    }
}
