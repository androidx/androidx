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

package androidx.compose.material3

import com.google.common.truth.Truth
import org.junit.Test

class EqualityOfCalendarModelsTest {

    // Copy of Android CalendarModelTest.equalModelsOutput
    // with KotlinxDatetimeCalendarModel and different time zones.
    // Ensures that models have the same implementation
    @Test
    fun equalModelsOutput() {
        // Note: This test ignores the parameters and just runs a few equality tests for the output.
        // It will execute twice, but that should to tolerable :)
        val newModel = KotlinxDatetimeCalendarModel()
        val legacyModel = LegacyCalendarModelImpl()

        val defaultTZ = getTimeZone()

        listOf("GMT-12", "GMT-5", "GMT+5", "GMT+12").forEach {

            setTimeZone(it)

            val date = newModel.getCanonicalDate(January2022Millis) // 1/1/2022
            val legacyDate = legacyModel.getCanonicalDate(January2022Millis)
            val month = newModel.getMonth(date)
            val legacyMonth = legacyModel.getMonth(date)

            Truth.assertThat(newModel.today).isEqualTo(legacyModel.today)
            Truth.assertThat(month).isEqualTo(legacyMonth)
            Truth.assertThat(newModel.getDateInputFormat())
                .isEqualTo(legacyModel.getDateInputFormat())
            Truth.assertThat(newModel.plusMonths(month, 3))
                .isEqualTo(legacyModel.plusMonths(month, 3))
            Truth.assertThat(date).isEqualTo(legacyDate)
            Truth.assertThat(newModel.getDayOfWeek(date)).isEqualTo(legacyModel.getDayOfWeek(date))
            if (supportsDateSkeleton) {
                Truth.assertThat(newModel.formatWithSkeleton(date, "MMM d, yyyy")).isEqualTo(
                    legacyModel.formatWithSkeleton(
                        date,
                        "MMM d, yyyy"
                    )
                )
                Truth.assertThat(newModel.formatWithSkeleton(month, "MMM yyyy")).isEqualTo(
                    legacyModel.formatWithSkeleton(
                        month,
                        "MMM yyyy"
                    )
                )
            }
        }

        setTimeZone(defaultTZ)
    }
}