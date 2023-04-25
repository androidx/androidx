/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSegmentTest {
    @Test
    fun validSegment_equals() {
        assertThat(
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5678L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                repetitions = 10,
            )
        ).isEqualTo(
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5678L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                repetitions = 10,
            )
        )
    }

    @Test
    fun invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(1234L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(5678L),
                endTime = Instant.ofEpochMilli(1234L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
            )
        }
    }

    @Test
    fun invalidReps_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5678),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                repetitions = -1,
            )
        }
    }
}