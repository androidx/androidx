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

@file:JvmName("RawQueries")

package androidx.work.analytics.impl.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.work.analytics.WorkMetricsQuery

/**
 * Converts a [WorkMetricsQuery] to a raw [SupportSQLiteQuery].
 *
 * @return a [SupportSQLiteQuery] instance
 */
internal fun WorkMetricsQuery.toRawQuery(): SupportSQLiteQuery {
    val arguments = mutableListOf<Any>()
    val builder = StringBuilder("SELECT * FROM WorkMetricsSpec")
    var conjunction = " WHERE"

    if (workIds.isNotEmpty()) {
        builder.append("$conjunction work_spec_id IN (")
        bindings(builder, workIds.size)
        builder.append(")")
        for (id in workIds) {
            arguments.add(id.toString())
        }
        conjunction = " AND"
    }

    if (states.isNotEmpty()) {
        builder.append("$conjunction state IN (")
        bindings(builder, states.size)
        builder.append(")")
        for (state in states) {
            arguments.add(state.name)
        }
        conjunction = " AND"
    }

    if (workerClassNames.isNotEmpty()) {
        builder.append("$conjunction worker_class_name IN (")
        bindings(builder, workerClassNames.size)
        builder.append(")")
        arguments.addAll(workerClassNames)
        conjunction = " AND"
    }

    if (tags.isNotEmpty()) {
        builder.append("$conjunction EXISTS (")
        builder.append(
            "SELECT 1 FROM WorkMetricsTag WHERE " +
                "WorkMetricsTag.work_spec_id = WorkMetricsSpec.work_spec_id AND " +
                "WorkMetricsTag.generation = WorkMetricsSpec.generation AND " +
                "WorkMetricsTag.period_count = WorkMetricsSpec.period_count AND " +
                "WorkMetricsTag.tag IN ("
        )
        bindings(builder, tags.size)
        builder.append("))")
        arguments.addAll(tags)
        conjunction = " AND"
    }

    if (beginTimeMillis > 0L) {
        builder.append("$conjunction enqueue_time_ms >= ?")
        arguments.add(beginTimeMillis)
        conjunction = " AND"
    }

    if (endTimeMillis < Long.MAX_VALUE) {
        builder.append("$conjunction enqueue_time_ms <= ?")
        arguments.add(endTimeMillis)
    }

    builder.append(" ORDER BY enqueue_time_ms ASC;")
    return SimpleSQLiteQuery(builder.toString(), arguments.toTypedArray())
}

private fun bindings(builder: StringBuilder, count: Int) {
    if (count <= 0) return
    for (i in 0 until count) {
        if (i > 0) {
            builder.append(",")
        }
        builder.append("?")
    }
}
