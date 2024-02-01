/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.solver.query.result

import androidx.room.solver.CodeGenScope

/**
 * Gets a Cursor and converts it into the return type of a method annotated with @Query.
 */
abstract class QueryResultAdapter(val rowAdapters: List<RowAdapter>) {

    val mappings: List<QueryMappedRowAdapter.Mapping>
        get() = rowAdapters.filterIsInstance<QueryMappedRowAdapter>().map { it.mapping }

    abstract fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope)

    // Indicates whether the cursor should be copied before converting.
    // This is important for performance reasons if the Cursor will be traverse more than once.
    fun shouldCopyCursor(): Boolean =
        rowAdapters.filterIsInstance<PojoRowAdapter>().any { it.relationCollectors.isNotEmpty() }

    // Gets a list of additionally accessed table names in sub queries done by the adapter
    // (e.g. does done to satisfy @Relation fields).
    fun accessedTableNames(): List<String> =
        rowAdapters.filterIsInstance<PojoRowAdapter>().flatMap { it.relationTableNames() }

    // TODO(b/319660042): Remove once migration to driver API is done.
    open fun isMigratedToDriver(): Boolean = false
}
