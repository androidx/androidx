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

package androidx.benchmark.perfetto

/**
 * Convenience for constructing a RowResult for given column values.
 *
 * Useful when asserting expected query results in tests.
 */
@ExperimentalPerfettoTraceProcessorApi
fun rowOf(vararg pairs: Pair<String, Any?>): Row {
    return Row(pairs.toMap())
}

/**
 * A Map<String, Any?> that maps column name to value in a row result from a [QueryResultIterator].
 *
 * Provides convenience methods for converting to internal base types - `String`, `Long`, `Double`,
 * and `ByteArray`.
 *
 * ```
 * session.query("SELECT name,ts,dur FROM slice WHERE name LIKE \"activityStart\"").forEach {
 *     callback(it.string("name"), it.long("ts"), it.long("dur")
 *     // or, used as a map:
 *     //callback(it["name"] as String, it["ts"] as Long, it["dur"] as Long)
 * }
 * ```
 *
 * Nullable variants of each convenience method are also provided.
 */
@ExperimentalPerfettoTraceProcessorApi
class Row(private val map: Map<String, Any?>) : Map<String, Any?> by map {
    fun string(columnName: String): String = map[columnName] as String

    fun long(columnName: String): Long = map[columnName] as Long

    fun double(columnName: String): Double = map[columnName] as Double

    fun bytes(columnName: String): ByteArray = map[columnName] as ByteArray

    fun nullableString(columnName: String): String? = map[columnName] as String?

    @Suppress("AutoBoxing") // primitives are already internally boxed
    fun nullableLong(columnName: String): Long? = map[columnName] as Long?

    @Suppress("AutoBoxing") // primitives are already internally boxed
    fun nullableDouble(columnName: String): Double? = map[columnName] as Double?

    fun nullableBytes(columnName: String): ByteArray? = map[columnName] as ByteArray?

    override fun hashCode(): Int = map.hashCode()

    override fun toString(): String = map.toString()

    override fun equals(other: Any?): Boolean = map == other
}
