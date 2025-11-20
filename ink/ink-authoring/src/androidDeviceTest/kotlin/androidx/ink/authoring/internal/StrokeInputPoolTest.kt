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

package androidx.ink.authoring.internal

import android.graphics.Matrix
import android.view.MotionEvent
import androidx.ink.authoring.testing.MultiTouchInputBuilder
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class StrokeInputPoolTest {

    @Test
    fun obtain_withDefaultArgs_shouldGetStrokeInputThatMatchesParameters() {
        val pool = StrokeInputPool()

        val input = pool.obtain(x = 1F, y = 2F, elapsedTimeMillis = 3L)

        assertThat(input.x).isEqualTo(1F)
        assertThat(input.y).isEqualTo(2F)
        assertThat(input.elapsedTimeMillis).isEqualTo(3L)
        assertThat(input.toolType).isEqualTo(InputToolType.UNKNOWN)
        assertThat(input.pressure).isEqualTo(StrokeInput.NO_PRESSURE)
        assertThat(input.tiltRadians).isEqualTo(StrokeInput.NO_TILT)
        assertThat(input.orientationRadians).isEqualTo(StrokeInput.NO_ORIENTATION)
    }

    @Test
    fun obtain_withCustomArgs_shouldGetStrokeInputThatMatchesParameters() {
        val pool = StrokeInputPool()

        val input =
            pool.obtain(
                x = 1F,
                y = 2F,
                elapsedTimeMillis = 3L,
                toolType = InputToolType.STYLUS,
                pressure = 4F,
                orientationRadians = 5F,
                tiltRadians = 6F,
            )

        assertThat(input.x).isEqualTo(1F)
        assertThat(input.y).isEqualTo(2F)
        assertThat(input.elapsedTimeMillis).isEqualTo(3L)
        assertThat(input.toolType).isEqualTo(InputToolType.STYLUS)
        assertThat(input.pressure).isEqualTo(4F)
        assertThat(input.orientationRadians).isEqualTo(5F)
        assertThat(input.tiltRadians).isEqualTo(6F)
    }

    @Test
    fun obtain_whenCalledTwiceWithoutRecycle_shouldReturnDifferentInstances() {
        val pool = StrokeInputPool()

        val input1 = pool.obtain(x = 1F, y = 2F, elapsedTimeMillis = 3L)
        val input2 = pool.obtain(x = 4F, y = 5F, elapsedTimeMillis = 6L)

        assertThat(input2).isNotSameInstanceAs(input1)
        assertThat(input1.x).isEqualTo(1F)
        assertThat(input1.y).isEqualTo(2F)
        assertThat(input1.elapsedTimeMillis).isEqualTo(3L)
        assertThat(input2.x).isEqualTo(4F)
        assertThat(input2.y).isEqualTo(5F)
        assertThat(input2.elapsedTimeMillis).isEqualTo(6L)
    }

    @Test
    fun obtain_afterPreviousRecycle_shouldReceiveSameInstance() {
        // Get a pool with zero pre-allocated instances to be able to verify with instance equality.
        val pool = StrokeInputPool(0)
        val input = pool.obtain(x = 1F, y = 2F, elapsedTimeMillis = 3L)
        pool.recycle(input)

        val anotherInput = pool.obtain(x = 4F, y = 5F, elapsedTimeMillis = 6L)

        assertThat(anotherInput).isSameInstanceAs(input)
        assertThat(input.x).isEqualTo(4F)
        assertThat(input.y).isEqualTo(5F)
        assertThat(input.elapsedTimeMillis).isEqualTo(6L)
    }

    @Test
    fun obtainSingleValueForMotionEvent() {
        val pool = StrokeInputPool()
        val motionEventToStrokeCoordinatesTransform =
            Matrix().apply {
                setScale(3F, 3F)
                postTranslate(10F, 50F)
            }
        val gestureStartTime = 3000L

        val pointerIdsToStrokeInputs = mutableMapOf<Int, MutableList<StrokeInput>>()
        MultiTouchInputBuilder(
                pointerCount = 2,
                toolTypes = intArrayOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_FINGER),
                startOrientation = arrayOf(-Math.PI.toFloat() / 2, null),
                // Tilt is not supported in Robolectric today, so omit it even for the stylus
                // pointer. It
                // will return as a value of 0.0F.
                startTilt = arrayOf(null, null),
                historyIncrements = 3,
                downtime = gestureStartTime,
            )
            .runGestureWith {
                for (pointerIndex in 0 until it.pointerCount) {
                    val pointerId = it.getPointerId(pointerIndex)
                    val pointerInputList =
                        pointerIdsToStrokeInputs.calculateIfAbsent(pointerId) { mutableListOf() }
                    pointerInputList.add(
                        pool.obtainSingleValueForMotionEvent(
                            it,
                            pointerIndex,
                            motionEventToStrokeCoordinatesTransform,
                            gestureStartTime,
                        )
                    )
                }
            }

        assertThat(pointerIdsToStrokeInputs).hasSize(2)
        assertThat(pointerIdsToStrokeInputs.keys).isEqualTo(setOf(9000, 9001))
        assertThat(pointerIdsToStrokeInputs[9000]!!)
            .comparingElementsUsing(strokeInputNearEqual())
            .containsExactly(
                StrokeInput.create(
                    x = 10F,
                    y = 50F,
                    elapsedTimeMillis = 0,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.05F,
                    tiltRadians = 0F,
                    orientationRadians = 0F,
                ),
                StrokeInput.create(
                    x = 10F,
                    y = 50F,
                    elapsedTimeMillis = 0,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.05F,
                    tiltRadians = 0F,
                    orientationRadians = 0F,
                ),
                StrokeInput.create(
                    x = 310F,
                    y = 350F,
                    elapsedTimeMillis = 30,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.15F,
                    tiltRadians = 0F,
                    orientationRadians = 0.2F,
                ),
                StrokeInput.create(
                    x = 610F,
                    y = 650F,
                    elapsedTimeMillis = 60,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.25F,
                    tiltRadians = 0F,
                    orientationRadians = 0.4F,
                ),
                StrokeInput.create(
                    x = 910F,
                    y = 950F,
                    elapsedTimeMillis = 90,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.35F,
                    tiltRadians = 0F,
                    orientationRadians = 0.6F,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 1250F,
                    elapsedTimeMillis = 120,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.45F,
                    tiltRadians = 0F,
                    orientationRadians = 0.8F,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 1250F,
                    elapsedTimeMillis = 150,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.45F,
                    tiltRadians = 0F,
                    orientationRadians = 0.8F,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 1250F,
                    elapsedTimeMillis = 150,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.45F,
                    tiltRadians = 0F,
                    orientationRadians = 0.8F,
                ),
            )

        assertThat(pointerIdsToStrokeInputs[9001]!!)
            .comparingElementsUsing(strokeInputNearEqual())
            .containsExactly(
                StrokeInput.create(
                    x = 310F,
                    y = 50F,
                    elapsedTimeMillis = 0,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 610F,
                    y = 350F,
                    elapsedTimeMillis = 30,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 910F,
                    y = 650F,
                    elapsedTimeMillis = 60,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 950F,
                    elapsedTimeMillis = 90,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1510F,
                    y = 1250F,
                    elapsedTimeMillis = 120,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1510F,
                    y = 1250F,
                    elapsedTimeMillis = 150,
                    toolType = InputToolType.TOUCH,
                ),
            )
    }

    @Test
    fun obtainAllHistoryForMotionEvent() {
        val pool = StrokeInputPool()
        val motionEventToStrokeCoordinatesTransform =
            Matrix().apply {
                setScale(3F, 3F)
                postTranslate(10F, 50F)
            }
        val gestureStartTime = 3000L

        val pointerIdsToStrokeInputs = mutableMapOf<Int, MutableList<StrokeInput>>()
        MultiTouchInputBuilder(
                pointerCount = 2,
                toolTypes = intArrayOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_FINGER),
                // Tilt is not supported in Robolectric today, so omit it even for the stylus
                // pointer. It
                // will return as a value of 0.0F.
                startTilt = arrayOf(null, null),
                startOrientation = arrayOf(-Math.PI.toFloat() / 2, null),
                historyIncrements = 2,
                downtime = gestureStartTime,
            )
            .runGestureWith {
                for (pointerIndex in 0 until it.pointerCount) {
                    val outBatchBuilder = MutableStrokeInputBatch()
                    pool.obtainAllHistoryForMotionEvent(
                        it,
                        pointerIndex,
                        motionEventToStrokeCoordinatesTransform,
                        gestureStartTime,
                        outBatch = outBatchBuilder,
                    )
                    val outBatch = outBatchBuilder.toImmutable()
                    val pointerId = it.getPointerId(pointerIndex)
                    val pointerInputList =
                        pointerIdsToStrokeInputs.calculateIfAbsent(pointerId) { mutableListOf() }
                    for (i in 0 until outBatch.size) {
                        pointerInputList.add(outBatch.get(i))
                    }
                }
            }

        assertThat(pointerIdsToStrokeInputs).hasSize(2)
        assertThat(pointerIdsToStrokeInputs.keys).isEqualTo(setOf(9000, 9001))
        assertThat(pointerIdsToStrokeInputs[9000]!!)
            .comparingElementsUsing(strokeInputNearEqual())
            .containsExactly(
                StrokeInput.create(
                    x = 10F,
                    y = 50F,
                    elapsedTimeMillis = 0,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.05F,
                    tiltRadians = 0F,
                    orientationRadians = 0F,
                ),
                StrokeInput.create(
                    x = 10F,
                    y = 50F,
                    elapsedTimeMillis = 0,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.05F,
                    tiltRadians = 0F,
                    orientationRadians = 0F,
                ),
                StrokeInput.create(
                    x = 160F,
                    y = 200F,
                    elapsedTimeMillis = 10,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.1F,
                    tiltRadians = 0F,
                    orientationRadians = 0.1F,
                ),
                StrokeInput.create(
                    x = 310F,
                    y = 350F,
                    elapsedTimeMillis = 20,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.15F,
                    tiltRadians = 0F,
                    orientationRadians = 0.2F,
                ),
                StrokeInput.create(
                    x = 460F,
                    y = 500F,
                    elapsedTimeMillis = 30,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.2F,
                    tiltRadians = 0F,
                    orientationRadians = 0.3F,
                ),
                StrokeInput.create(
                    x = 610F,
                    y = 650F,
                    elapsedTimeMillis = 40,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.25F,
                    tiltRadians = 0F,
                    orientationRadians = 0.4F,
                ),
                StrokeInput.create(
                    x = 760F,
                    y = 800F,
                    elapsedTimeMillis = 50,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.3F,
                    tiltRadians = 0F,
                    orientationRadians = 0.5F,
                ),
                StrokeInput.create(
                    x = 910F,
                    y = 950F,
                    elapsedTimeMillis = 60,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.35F,
                    tiltRadians = 0F,
                    orientationRadians = 0.6F,
                ),
                StrokeInput.create(
                    x = 1060F,
                    y = 1100F,
                    elapsedTimeMillis = 70,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.4F,
                    tiltRadians = 0F,
                    orientationRadians = 0.7F,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 1250F,
                    elapsedTimeMillis = 80,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.45F,
                    tiltRadians = 0F,
                    orientationRadians = 0.8F,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 1250F,
                    elapsedTimeMillis = 100,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.45F,
                    tiltRadians = 0F,
                    orientationRadians = 0.8F,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 1250F,
                    elapsedTimeMillis = 100,
                    toolType = InputToolType.STYLUS,
                    pressure = 0.45F,
                    tiltRadians = 0F,
                    orientationRadians = 0.8F,
                ),
            )

        assertThat(pointerIdsToStrokeInputs[9001]!!)
            .comparingElementsUsing(strokeInputNearEqual())
            .containsExactly(
                StrokeInput.create(
                    x = 310F,
                    y = 50F,
                    elapsedTimeMillis = 0,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 460F,
                    y = 200F,
                    elapsedTimeMillis = 10,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 610F,
                    y = 350F,
                    elapsedTimeMillis = 20,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 760F,
                    y = 500F,
                    elapsedTimeMillis = 30,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 910F,
                    y = 650F,
                    elapsedTimeMillis = 40,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1060F,
                    y = 800F,
                    elapsedTimeMillis = 50,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1210F,
                    y = 950F,
                    elapsedTimeMillis = 60,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1360F,
                    y = 1100F,
                    elapsedTimeMillis = 70,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1510F,
                    y = 1250F,
                    elapsedTimeMillis = 80,
                    toolType = InputToolType.TOUCH,
                ),
                StrokeInput.create(
                    x = 1510F,
                    y = 1250F,
                    elapsedTimeMillis = 100,
                    toolType = InputToolType.TOUCH,
                ),
            )
    }
}

/** A [Correspondence] for fuzzy matching of a [StrokeInput]. */
private fun strokeInputNearEqual(
    tolerance: Double = 0.001
): Correspondence<StrokeInput, StrokeInput> =
    Correspondence.from(
        { actual: StrokeInput?, expected: StrokeInput? ->
            if (expected == null || actual == null) return@from actual == expected
            val floatTolerance = Correspondence.tolerance(tolerance)
            floatTolerance.compare(actual.x, expected.x) &&
                floatTolerance.compare(actual.y, expected.y) &&
                actual.elapsedTimeMillis == expected.elapsedTimeMillis &&
                actual.toolType == expected.toolType &&
                floatTolerance.compare(actual.pressure, expected.pressure) &&
                floatTolerance.compare(actual.tiltRadians, expected.tiltRadians) &&
                floatTolerance.compare(actual.orientationRadians, expected.orientationRadians)
        },
        "is approximately equal to",
    )

/** Like [MutableMap.computeIfAbsent], but available on all API levels. */
private fun <K, V> MutableMap<K, V>.calculateIfAbsent(key: K, mappingFunction: (K) -> V): V {
    return get(key) ?: mappingFunction(key).also { put(key, it) }
}
