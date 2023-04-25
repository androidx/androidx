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

import androidx.health.connect.client.units.Length
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test

class ExerciseRouteTest {

    @Test
    fun validLocation_equals() {
        assertThat(
                ExerciseRoute.Location(
                    time = Instant.ofEpochMilli(1234L),
                    latitude = 34.5,
                    longitude = -34.5,
                    horizontalAccuracy = Length.meters(0.4),
                    verticalAccuracy = Length.meters(1.3),
                    altitude = Length.meters(23.4)
                )
            )
            .isEqualTo(
                ExerciseRoute.Location(
                    time = Instant.ofEpochMilli(1234L),
                    latitude = 34.5,
                    longitude = -34.5,
                    horizontalAccuracy = Length.meters(0.4),
                    verticalAccuracy = Length.meters(1.3),
                    altitude = Length.meters(23.4)
                )
            )
    }

    @Test
    fun invalidLatitudeAndLongitude_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseRoute.Location(
                time = Instant.ofEpochMilli(1234L),
                latitude = -91.0,
                longitude = -34.5,
                horizontalAccuracy = Length.meters(0.4),
                verticalAccuracy = Length.meters(1.3),
                altitude = Length.meters(23.4)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ExerciseRoute.Location(
                time = Instant.ofEpochMilli(1234L),
                latitude = 34.5,
                longitude = 189.5,
                horizontalAccuracy = Length.meters(0.4),
                verticalAccuracy = Length.meters(1.3),
                altitude = Length.meters(23.4)
            )
        }
    }

    @Test
    fun emptyRoute() {
        assertThat(ExerciseRoute(listOf())).isEqualTo(ExerciseRoute(listOf()))
    }

    @Test
    fun nonEmptyRoute() {
        val location1 =
            ExerciseRoute.Location(
                time = Instant.ofEpochMilli(1234L),
                latitude = 34.5,
                longitude = -34.5,
                horizontalAccuracy = Length.meters(0.4),
                verticalAccuracy = Length.meters(1.3),
                altitude = Length.meters(23.4)
            )
        val location2 =
            ExerciseRoute.Location(
                time = Instant.ofEpochMilli(2345L),
                latitude = 34.8,
                longitude = -34.8,
            )
        assertThat(ExerciseRoute(listOf(location1, location2)))
            .isEqualTo(ExerciseRoute(listOf(location1, location2)))
    }
}
