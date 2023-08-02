/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.impl.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.work.WorkQuery
import androidx.work.impl.model.WorkTypeConverters.stateToInt

/**
 * Converts a [WorkQuery] to a raw [SupportSQLiteQuery].
 *
 * @return a [SupportSQLiteQuery] instance
 */
fun WorkQuery.toRawQuery(): SupportSQLiteQuery {
    val arguments = mutableListOf<Any>()
    val builder = StringBuilder("SELECT * FROM workspec")
    var conjunction = " WHERE"
    if (states.isNotEmpty()) {
        val stateIds = states.map { stateToInt(it!!) }
        builder.append("$conjunction state IN (")
        bindings(builder, stateIds.size)
        builder.append(")")
        arguments.addAll(stateIds)
        conjunction = " AND"
    }
    if (ids.isNotEmpty()) {
        val workSpecIds = ids.map { it.toString() }
        builder.append("$conjunction id IN (")
        bindings(builder, ids.size)
        builder.append(")")
        arguments.addAll(workSpecIds)
        conjunction = " AND"
    }
    if (tags.isNotEmpty()) {
        builder.append("$conjunction id IN (SELECT work_spec_id FROM worktag WHERE tag IN (")
        bindings(builder, tags.size)
        builder.append("))")
        arguments.addAll(tags)
        conjunction = " AND"
    }
    if (uniqueWorkNames.isNotEmpty()) {
        builder.append("$conjunction id IN (SELECT work_spec_id FROM workname WHERE name IN (")
        bindings(builder, uniqueWorkNames.size)
        builder.append("))")
        arguments.addAll(uniqueWorkNames)
    }
    builder.append(";")
    return SimpleSQLiteQuery(builder.toString(), arguments.toTypedArray())
}

private fun bindings(builder: StringBuilder, count: Int) {
    if (count <= 0) {
        return
    }
    builder.append((List(count) { "?" }.joinToString(",")))
}