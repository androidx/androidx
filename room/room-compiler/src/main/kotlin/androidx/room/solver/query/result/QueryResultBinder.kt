/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.solver.CodeGenScope

/**
 * Connects the query, db and the ResultAdapter.
 *
 * <p>
 * The default implementation is InstantResultBinder. If the query is deferred rather than executed
 * directly, such alternative implementations can be implement using this interface (e.g. LiveData,
 * Rx, caching etc)
 */
abstract class QueryResultBinder(val adapter: QueryResultAdapter?) {
    /**
     * receives the sql, bind args and adapter and generates the code that runs the query and
     * returns the result.
     */
    abstract fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean, // false if query is provided by the user
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    )

    // TODO(b/319660042): Remove once migration to driver API is done.
    open fun isMigratedToDriver(): Boolean = false

    /**
     * Receives the SQL and a function to bind args into a statement, it must then generate the code
     * that steps on the query, reads its columns and returns the result.
     */
    open fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        error("Result binder has not been migrated to use driver API.")
    }
}
