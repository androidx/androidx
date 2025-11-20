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

package androidx.ink.authoring.testing

import android.graphics.PointF
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiTouchInputBuilderTest {

    @Test
    fun oneStylusWithHistory() {
        val expectedActions =
            listOf(
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP,
            )
        val expectedPointerCounts = listOf(1, 1, 1, 1, 1, 1)
        val expectedToolTypes =
            listOf(
                MotionEvent.TOOL_TYPE_STYLUS,
                MotionEvent.TOOL_TYPE_STYLUS,
                MotionEvent.TOOL_TYPE_STYLUS,
                MotionEvent.TOOL_TYPE_STYLUS,
                MotionEvent.TOOL_TYPE_STYLUS,
                MotionEvent.TOOL_TYPE_STYLUS,
            )
        val expectedTimes =
            listOf(
                listOf(1000L),
                listOf(1010L, 1020L),
                listOf(1030L, 1040L),
                listOf(1050L, 1060L),
                listOf(1070L, 1080L),
                // 1090L is not present since the ACTION_UP event does not contain history values.
                listOf(1100L),
            )
        val expectedPositions =
            listOf(
                listOf(PointF(0F, 0F)),
                listOf(PointF(50F, 50F), PointF(100F, 100F)),
                listOf(PointF(150F, 150F), PointF(200F, 200F)),
                listOf(PointF(250F, 250F), PointF(300F, 300F)),
                listOf(PointF(350F, 350F), PointF(400F, 400F)),
                listOf(PointF(400F, 400F)),
            )
        val expectedPressures =
            listOf(
                listOf(0.05F),
                listOf(0.1F, 0.15F),
                listOf(0.2F, 0.25F),
                listOf(0.3F, 0.35F),
                listOf(0.4F, 0.45F),
                listOf(0.45F),
            )
        val expectedOrientations =
            listOf(
                listOf(0.01F),
                listOf(0.11F, 0.21F),
                listOf(0.31F, 0.41F),
                listOf(0.51F, 0.61F),
                listOf(0.71F, 0.81F),
                listOf(0.81F),
            )
        val expectedTilts =
            listOf(
                listOf(0.07F),
                listOf(0.12F, 0.17F),
                listOf(0.22F, 0.27F),
                listOf(0.32F, 0.37F),
                listOf(0.42F, 0.47F),
                listOf(0.47F),
            )

        val actualActions = mutableListOf<Int>()
        val actualPointerCounts = mutableListOf<Int>()
        val actualToolTypes = mutableListOf<Int>()
        val actualTimes = mutableListOf<MutableList<Long>>()
        val actualPositions = mutableListOf<MutableList<PointF>>()
        val actualPressures = mutableListOf<MutableList<Float>>()
        val actualOrientations = mutableListOf<MutableList<Float>>()
        val actualTilts = mutableListOf<MutableList<Float>>()

        MultiTouchInputBuilder(
                pointerCount = 1,
                toolTypes = intArrayOf(MotionEvent.TOOL_TYPE_STYLUS),
                historyIncrements = 2,
            )
            .runGestureWith {
                actualActions.add(it.action)
                actualPointerCounts.add(it.pointerCount)
                actualToolTypes.add(it.getToolType(0))

                val times = mutableListOf<Long>().also(actualTimes::add)
                val positions = mutableListOf<PointF>().also(actualPositions::add)
                val pressures = mutableListOf<Float>().also(actualPressures::add)
                val orientations = mutableListOf<Float>().also(actualOrientations::add)
                val tilts = mutableListOf<Float>().also(actualTilts::add)
                for (h in 0 until it.historySize) {
                    times.add(it.getHistoricalEventTime(h))
                    positions.add(PointF(it.getHistoricalX(h), it.getHistoricalY(h)))
                    pressures.add(it.getHistoricalPressure(h))
                    orientations.add(it.getHistoricalOrientation(h))
                    tilts.add(it.getHistoricalAxisValue(MotionEvent.AXIS_TILT, h))
                }
                times.add(it.eventTime)
                positions.add(PointF(it.x, it.y))
                pressures.add(it.pressure)
                orientations.add(it.orientation)
                tilts.add(it.getAxisValue(MotionEvent.AXIS_TILT))
            }

        assertThat(actualActions).containsExactlyElementsIn(expectedActions)
        assertThat(actualPointerCounts).containsExactlyElementsIn(expectedPointerCounts)
        assertThat(actualToolTypes).containsExactlyElementsIn(expectedToolTypes)
        assertThat(actualTimes).containsExactlyElementsIn(expectedTimes)
        assertThat(actualPositions).containsExactlyElementsIn(expectedPositions)
        assertThat(actualPressures)
            .comparingElementsUsing(floatListFuzzyEqual)
            .containsExactlyElementsIn(expectedPressures)
        assertThat(actualOrientations)
            .comparingElementsUsing(floatListFuzzyEqual)
            .containsExactlyElementsIn(expectedOrientations)
        assertThat(actualTilts)
            .comparingElementsUsing(floatListFuzzyEqual)
            .containsExactlyElementsIn(expectedTilts)
    }

    @Test
    fun pinchOut() {
        val expectedActionsAndPointerIds =
            listOf(
                MotionEvent.ACTION_DOWN to 9000,
                MotionEvent.ACTION_POINTER_DOWN to 9001,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_POINTER_UP to 9001,
                MotionEvent.ACTION_UP to 9000,
            )
        val expectedPointerCounts = listOf(1, 2, 2, 2, 2, 2, 2, 1)
        val expectedTimes = listOf(1000L, 1000L, 1010L, 1020L, 1030L, 1040L, 1050L, 1050L)
        // Per pointer values: pointer ID 9000 goes down first and up last so has 2 extra events.
        val expectedToolTypesForPointerIds =
            mapOf(
                9000 to List(8) { MotionEvent.TOOL_TYPE_FINGER },
                9001 to
                    listOf(
                        null,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        null,
                    ),
            )
        val expectedPositionsForPointerIds =
            mapOf(
                9000 to
                    listOf(
                        PointF(300F, 500F),
                        PointF(300F, 500F),
                        PointF(275F, 500F),
                        PointF(250F, 500F),
                        PointF(225F, 500F),
                        PointF(200F, 500F),
                        PointF(200F, 500F),
                        PointF(200F, 500F),
                    ),
                9001 to
                    listOf(
                        null,
                        PointF(500F, 500F),
                        PointF(525F, 500F),
                        PointF(550F, 500F),
                        PointF(575F, 500F),
                        PointF(600F, 500F),
                        PointF(600F, 500F),
                        null,
                    ),
            )

        val actualActionsAndPointerIds = mutableListOf<Pair<Int, Int?>>()
        val actualPointerCounts = mutableListOf<Int>()
        val actualTimes = mutableListOf<Long>()
        val actualToolTypesForPointerIds = mutableMapOf<Int, MutableList<Int?>>()
        val actualPositionsForPointerIds = mutableMapOf<Int, MutableList<PointF?>>()
        MultiTouchInputBuilder.pinchOutWithFactor(400F, 500F).runGestureWith {
            actualActionsAndPointerIds.add(
                Pair(
                    it.actionMasked,
                    if (it.actionMasked == MotionEvent.ACTION_MOVE) null
                    else it.getPointerId(it.actionIndex),
                )
            )
            actualPointerCounts.add(it.pointerCount)
            actualTimes.add(it.eventTime)

            val seenPointerIds = mutableSetOf<Int>()
            for (pointerIndex in 0 until it.pointerCount) {
                val pointerId = it.getPointerId(pointerIndex)
                seenPointerIds.add(pointerId)
                actualToolTypesForPointerIds
                    .calculateIfAbsent(pointerId) { mutableListOf() }
                    .add(it.getToolType(pointerIndex))
                actualPositionsForPointerIds
                    .calculateIfAbsent(pointerId) { mutableListOf() }
                    .add(PointF(it.getX(pointerIndex), it.getY(pointerIndex)))
            }
            // Fill in per-pointer values for pointers not seen in this event with null.
            for (pointerId in 9000..9001) {
                if (!seenPointerIds.contains(pointerId)) {
                    actualToolTypesForPointerIds
                        .calculateIfAbsent(pointerId) { mutableListOf() }
                        .add(null)
                    actualPositionsForPointerIds
                        .calculateIfAbsent(pointerId) { mutableListOf() }
                        .add(null)
                }
            }
        }

        assertThat(actualActionsAndPointerIds)
            .containsExactlyElementsIn(expectedActionsAndPointerIds)
        assertThat(actualPointerCounts).containsExactlyElementsIn(expectedPointerCounts)
        assertThat(actualTimes).containsExactlyElementsIn(expectedTimes)
        assertThat(actualToolTypesForPointerIds)
            .containsExactlyEntriesIn(expectedToolTypesForPointerIds)
        assertThat(actualPositionsForPointerIds)
            .containsExactlyEntriesIn(expectedPositionsForPointerIds)
    }

    @Test
    fun pinchIn() {
        val expectedActionsAndPointerIds =
            listOf(
                MotionEvent.ACTION_DOWN to 9000,
                MotionEvent.ACTION_POINTER_DOWN to 9001,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_POINTER_UP to 9001,
                MotionEvent.ACTION_UP to 9000,
            )
        val expectedPointerCounts = listOf(1, 2, 2, 2, 2, 2, 2, 1)
        val expectedTimes = listOf(1000L, 1000L, 1010L, 1020L, 1030L, 1040L, 1050L, 1050L)
        // Per pointer values: pointer ID 9000 goes down first and up last so has 2 extra events.
        val expectedToolTypesForPointerIds =
            mapOf(
                9000 to List(8) { MotionEvent.TOOL_TYPE_FINGER },
                9001 to
                    listOf(
                        null,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        null,
                    ),
            )
        val expectedPositionsForPointerIds =
            mapOf(
                9000 to
                    listOf(
                        PointF(300F, 500F),
                        PointF(300F, 500F),
                        PointF(312.5F, 500F),
                        PointF(325F, 500F),
                        PointF(337.5F, 500F),
                        PointF(350F, 500F),
                        PointF(350F, 500F),
                        PointF(350F, 500F),
                    ),
                9001 to
                    listOf(
                        null,
                        PointF(500F, 500F),
                        PointF(487.5F, 500F),
                        PointF(475F, 500F),
                        PointF(462.5F, 500F),
                        PointF(450F, 500F),
                        PointF(450F, 500F),
                        null,
                    ),
            )

        val actualActionsAndPointerIds = mutableListOf<Pair<Int, Int?>>()
        val actualPointerCounts = mutableListOf<Int>()
        val actualTimes = mutableListOf<Long>()
        val actualToolTypesForPointerIds = mutableMapOf<Int, MutableList<Int?>>()
        val actualPositionsForPointerIds = mutableMapOf<Int, MutableList<PointF?>>()
        MultiTouchInputBuilder.pinchInWithFactor(400F, 500F).runGestureWith {
            actualActionsAndPointerIds.add(
                Pair(
                    it.actionMasked,
                    if (it.actionMasked == MotionEvent.ACTION_MOVE) null
                    else it.getPointerId(it.actionIndex),
                )
            )
            actualPointerCounts.add(it.pointerCount)
            actualTimes.add(it.eventTime)

            val seenPointerIds = mutableSetOf<Int>()
            for (pointerIndex in 0 until it.pointerCount) {
                val pointerId = it.getPointerId(pointerIndex)
                seenPointerIds.add(pointerId)
                actualToolTypesForPointerIds
                    .calculateIfAbsent(pointerId) { mutableListOf() }
                    .add(it.getToolType(pointerIndex))
                actualPositionsForPointerIds
                    .calculateIfAbsent(pointerId) { mutableListOf() }
                    .add(PointF(it.getX(pointerIndex), it.getY(pointerIndex)))
            }
            // Fill in per-pointer values for pointers not seen in this event with null.
            for (pointerId in 9000..9001) {
                if (!seenPointerIds.contains(pointerId)) {
                    actualToolTypesForPointerIds
                        .calculateIfAbsent(pointerId) { mutableListOf() }
                        .add(null)
                    actualPositionsForPointerIds
                        .calculateIfAbsent(pointerId) { mutableListOf() }
                        .add(null)
                }
            }
        }

        assertThat(actualActionsAndPointerIds)
            .containsExactlyElementsIn(expectedActionsAndPointerIds)
        assertThat(actualPointerCounts).containsExactlyElementsIn(expectedPointerCounts)
        assertThat(actualTimes).containsExactlyElementsIn(expectedTimes)
        assertThat(actualToolTypesForPointerIds)
            .containsExactlyEntriesIn(expectedToolTypesForPointerIds)
        assertThat(actualPositionsForPointerIds)
            .containsExactlyEntriesIn(expectedPositionsForPointerIds)
    }

    @Test
    fun rotate90DegreesClockwise() {
        val expectedActionsAndPointerIds =
            listOf(
                MotionEvent.ACTION_DOWN to 9000,
                MotionEvent.ACTION_POINTER_DOWN to 9001,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_MOVE to null,
                MotionEvent.ACTION_POINTER_UP to 9001,
                MotionEvent.ACTION_UP to 9000,
            )
        val expectedPointerCounts = listOf(1, 2, 2, 2, 2, 2, 2, 1)
        val expectedTimes = listOf(1000L, 1000L, 1010L, 1020L, 1030L, 1040L, 1050L, 1050L)
        // Per pointer values: pointer ID 9000 goes down first and up last so has 2 extra events.
        val expectedToolTypesForPointerIds =
            mapOf(
                9000 to List(8) { MotionEvent.TOOL_TYPE_FINGER },
                9001 to
                    listOf(
                        null,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        MotionEvent.TOOL_TYPE_FINGER,
                        null,
                    ),
            )
        val expectedPositionsForPointerIds =
            mapOf(
                9000 to
                    listOf(
                        PointF(350F, 450F),
                        PointF(350F, 450F),
                        PointF(375F, 450F),
                        PointF(400F, 450F),
                        PointF(425F, 450F),
                        PointF(450F, 450F),
                        PointF(450F, 450F),
                        PointF(450F, 450F),
                    ),
                9001 to
                    listOf(
                        null,
                        PointF(450F, 550F),
                        PointF(425F, 550F),
                        PointF(400F, 550F),
                        PointF(375F, 550F),
                        PointF(350F, 550F),
                        PointF(350F, 550F),
                        null,
                    ),
            )

        val actualActionsAndPointerIds = mutableListOf<Pair<Int, Int?>>()
        val actualPointerCounts = mutableListOf<Int>()
        val actualTimes = mutableListOf<Long>()
        val actualToolTypesForPointerIds = mutableMapOf<Int, MutableList<Int?>>()
        val actualPositionsForPointerIds = mutableMapOf<Int, MutableList<PointF?>>()
        MultiTouchInputBuilder.rotate90DegreesClockwise(400F, 500F).runGestureWith {
            actualActionsAndPointerIds.add(
                Pair(
                    it.actionMasked,
                    if (it.actionMasked == MotionEvent.ACTION_MOVE) null
                    else it.getPointerId(it.actionIndex),
                )
            )
            actualPointerCounts.add(it.pointerCount)
            actualTimes.add(it.eventTime)

            val seenPointerIds = mutableSetOf<Int>()
            for (pointerIndex in 0 until it.pointerCount) {
                val pointerId = it.getPointerId(pointerIndex)
                seenPointerIds.add(pointerId)
                actualToolTypesForPointerIds
                    .calculateIfAbsent(pointerId) { mutableListOf() }
                    .add(it.getToolType(pointerIndex))
                actualPositionsForPointerIds
                    .calculateIfAbsent(pointerId) { mutableListOf() }
                    .add(PointF(it.getX(pointerIndex), it.getY(pointerIndex)))
            }
            // Fill in per-pointer values for pointers not seen in this event with null.
            for (pointerId in 9000..9001) {
                if (!seenPointerIds.contains(pointerId)) {
                    actualToolTypesForPointerIds
                        .calculateIfAbsent(pointerId) { mutableListOf() }
                        .add(null)
                    actualPositionsForPointerIds
                        .calculateIfAbsent(pointerId) { mutableListOf() }
                        .add(null)
                }
            }
        }

        assertThat(actualActionsAndPointerIds)
            .containsExactlyElementsIn(expectedActionsAndPointerIds)
        assertThat(actualPointerCounts).containsExactlyElementsIn(expectedPointerCounts)
        assertThat(actualTimes).containsExactlyElementsIn(expectedTimes)
        assertThat(actualToolTypesForPointerIds)
            .containsExactlyEntriesIn(expectedToolTypesForPointerIds)
        assertThat(actualPositionsForPointerIds)
            .containsExactlyEntriesIn(expectedPositionsForPointerIds)
    }

    private val floatListFuzzyEqual: Correspondence<List<Float>, List<Float>> =
        Correspondence.from(
            { actualList: List<Float>?, expectedList: List<Float>? ->
                if (expectedList == null || actualList == null)
                    return@from actualList == expectedList
                actualList
                    .zip(expectedList) { actual, expected ->
                        Correspondence.tolerance(0.001).compare(actual, expected)
                    }
                    .all { it }
            },
            "is approximately equal to",
        )
}

/** Like [MutableMap.computeIfAbsent], but available on all API levels. */
private fun <K, V> MutableMap<K, V>.calculateIfAbsent(key: K, mappingFunction: (K) -> V): V {
    return get(key) ?: mappingFunction(key).also { put(key, it) }
}
