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

package androidx.appfunctions.internal.serializableproxies

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import kotlin.test.Test

@RequiresApi(Build.VERSION_CODES.O)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AppFunctionLocalDateTimeTest {

    @Test
    fun toLocalDateTime() {
        val year = 2000
        val month = 3
        val dayOfMonth = 27
        val hour = 12
        val minute = 30
        val second = 40
        val nanoOfSecond = 20
        val appFunctionLocalDateTime =
            AppFunctionLocalDateTime(
                year = year,
                month = month,
                dayOfMonth = dayOfMonth,
                hour = hour,
                minute = minute,
                second = second,
                nanoOfSecond = nanoOfSecond,
            )

        val resultLocalDateTime = appFunctionLocalDateTime.toLocalDateTime()

        assertThat(resultLocalDateTime.year).isEqualTo(year)
        assertThat(resultLocalDateTime.monthValue).isEqualTo(month)
        assertThat(resultLocalDateTime.dayOfMonth).isEqualTo(dayOfMonth)
        assertThat(resultLocalDateTime.hour).isEqualTo(hour)
        assertThat(resultLocalDateTime.minute).isEqualTo(minute)
        assertThat(resultLocalDateTime.second).isEqualTo(second)
        assertThat(resultLocalDateTime.nano).isEqualTo(nanoOfSecond)
    }

    @Test
    fun fromLocalDateTime() {
        val year = 2000
        val month = 3
        val dayOfMonth = 27
        val hour = 12
        val minute = 30
        val second = 40
        val nanoOfSecond = 20
        val localDateTime =
            LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)

        val appFunctionLocalDateTime = AppFunctionLocalDateTime.fromLocalDateTime(localDateTime)

        assertThat(appFunctionLocalDateTime.year).isEqualTo(year)
        assertThat(appFunctionLocalDateTime.month).isEqualTo(month)
        assertThat(appFunctionLocalDateTime.dayOfMonth).isEqualTo(dayOfMonth)
        assertThat(appFunctionLocalDateTime.hour).isEqualTo(hour)
        assertThat(appFunctionLocalDateTime.minute).isEqualTo(minute)
        assertThat(appFunctionLocalDateTime.second).isEqualTo(second)
        assertThat(appFunctionLocalDateTime.nanoOfSecond).isEqualTo(nanoOfSecond)
    }
}
