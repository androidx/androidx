/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.connect.client.records

import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_BIKING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_CALISTHENICS
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_INT_TO_STRING_MAP
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_STRENGTH_TRAINING
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_STRING_TO_INT_MAP
import androidx.health.connect.client.units.meters
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.reflect.typeOf
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSessionRecordTest {

    @Test
    fun validRecord_equals() {
        assertThat(
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                title = "title",
                notes = "notes",
                segments = listOf(
                    ExerciseSegment(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1235L),
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
                    )
                ),
                laps = listOf(
                    ExerciseLap(
                        startTime = Instant.ofEpochMilli(1235L),
                        endTime = Instant.ofEpochMilli(1236L),
                        length = 10.meters,
                    )
                ),
                route = ExerciseRoute(
                    route = listOf(
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(1234L),
                            latitude = 34.5,
                            longitude = -34.5,
                            horizontalAccuracy = 0.4.meters,
                            verticalAccuracy = 1.3.meters,
                            altitude = 23.4.meters
                        )
                    )
                ),
                hasRoute = true,
            )
        )
            .isEqualTo(
                ExerciseSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    exerciseType = EXERCISE_TYPE_BIKING,
                    title = "title",
                    notes = "notes",
                    segments = listOf(
                        ExerciseSegment(
                            startTime = Instant.ofEpochMilli(1234L),
                            endTime = Instant.ofEpochMilli(1235L),
                            segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
                        )
                    ),
                    laps = listOf(
                        ExerciseLap(
                            startTime = Instant.ofEpochMilli(1235L),
                            endTime = Instant.ofEpochMilli(1236L),
                            length = 10.meters,
                        )
                    ),
                    route = ExerciseRoute(
                        route = listOf(
                            ExerciseRoute.Location(
                                time = Instant.ofEpochMilli(1234L),
                                latitude = 34.5,
                                longitude = -34.5,
                                horizontalAccuracy = 0.4.meters,
                                verticalAccuracy = 1.3.meters,
                                altitude = 23.4.meters
                            )
                        )
                    ),
                    hasRoute = true,
                )
            )
    }

    @Test
    fun invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1234L),
                endZoneOffset = null,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                title = "title",
                notes = "notes",
            )
        }
    }

    @Test
    fun allExerciseTypeEnums_hasMapping() {
        val allEnums =
            ExerciseSessionRecord.Companion::class
                .members
                .asSequence()
                .filter { it -> it.name.startsWith("EXERCISE_TYPE") }
                .filter { it -> it.returnType == typeOf<Int>() }
                .map { it -> it.call(ExerciseSessionRecord.Companion) }
                .toHashSet()

        assertThat(EXERCISE_TYPE_STRING_TO_INT_MAP.values.toSet())
            .containsExactlyElementsIn(allEnums)
        assertThat(EXERCISE_TYPE_INT_TO_STRING_MAP.keys).containsExactlyElementsIn(allEnums)
    }

    @Test
    fun legacyTypesMapToRightValues() {
        assertThat(EXERCISE_TYPE_INT_TO_STRING_MAP[EXERCISE_TYPE_STRENGTH_TRAINING])
            .isEqualTo("strength_training")

        assertThat(EXERCISE_TYPE_INT_TO_STRING_MAP[EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING])
            .isEqualTo("high_intensity_interval_training")

        assertThat(EXERCISE_TYPE_INT_TO_STRING_MAP[EXERCISE_TYPE_CALISTHENICS])
            .isEqualTo("calisthenics")
    }

    @Test
    fun record_segmentOutOfRange_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                segments = listOf(
                    ExerciseSegment(
                        startTime = Instant.ofEpochMilli(1233L),
                        endTime = Instant.ofEpochMilli(1235L),
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
                    )
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                segments = listOf(
                    ExerciseSegment(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
                    )
                )
            )
        }
    }

    @Test
    fun record_lapOutOfRange_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                laps = listOf(
                    ExerciseLap(
                        startTime = Instant.ofEpochMilli(1233L),
                        endTime = Instant.ofEpochMilli(1235L),
                    )
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                laps = listOf(
                    ExerciseLap(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                    )
                )
            )
        }
    }

    @Test
    fun record_routeOutOfRange_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                route = ExerciseRoute(
                    route = listOf(
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(1233L),
                            latitude = 34.5,
                            longitude = -34.5
                        )
                    )
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1235L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                route = ExerciseRoute(
                    route = listOf(
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(1235L),
                            latitude = 34.5,
                            longitude = -34.5
                        )
                    )
                )
            )
        }
    }

    @Test
    fun record_segmentsOverlap_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                segments = listOf(
                    ExerciseSegment(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
                    ),
                    ExerciseSegment(
                        startTime = Instant.ofEpochMilli(1235L),
                        endTime = Instant.ofEpochMilli(1236L),
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING
                    ),
                )
            )
        }
    }

    @Test
    fun record_lapsOverlap_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                laps = listOf(
                    ExerciseLap(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                    ),
                    ExerciseLap(
                        startTime = Instant.ofEpochMilli(1235L),
                        endTime = Instant.ofEpochMilli(1236L),
                    ),
                )
            )
        }
    }

    @Test
    fun segmentTypeNotCompatibleWithSession_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                segments = listOf(
                    ExerciseSegment(
                        startTime = Instant.ofEpochMilli(1234L),
                        endTime = Instant.ofEpochMilli(1236L),
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK
                    ),
                )
            )
        }
    }

    @Test
    fun exerciseRouteSet_hasRouteSetToFalse_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                route = ExerciseRoute(
                    route = listOf(
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(1235L),
                            latitude = 34.5,
                            longitude = -34.5
                        )
                    )
                ),
                hasRoute = false,
            )
        }
    }

    @Test
    fun secondConstructor_hasRouteSetCorrectly() {
        assertThat(
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
                route = ExerciseRoute(
                    route = listOf(
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(1235L),
                            latitude = 34.5,
                            longitude = -34.5
                        )
                    )
                ),
            ).hasRoute
        ).isTrue()
        assertThat(
            ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                exerciseType = EXERCISE_TYPE_BIKING,
            ).hasRoute
        ).isFalse()
    }
}
