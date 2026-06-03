/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.analytics.impl.model

import androidx.room.TypeConverter
import androidx.work.analytics.WorkMetricsInfo

internal class WorkMetricsInfoTypeConverters {
    @TypeConverter fun stateToString(state: WorkMetricsInfo.State): String = state.name

    @TypeConverter
    fun stringToState(value: String): WorkMetricsInfo.State = WorkMetricsInfo.State.valueOf(value)

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter fun stringListToString(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun stringToIntMap(value: String): Map<Int, Int> {
        if (value.isEmpty()) return emptyMap()
        return value.split(";").associate {
            val parts = it.split(":")
            parts[0].toInt() to parts[1].toInt()
        }
    }

    @TypeConverter
    fun intMapToString(map: Map<Int, Int>): String =
        map.entries.joinToString(";") { "${it.key}:${it.value}" }
}
