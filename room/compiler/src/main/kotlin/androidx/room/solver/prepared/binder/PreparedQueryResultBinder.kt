/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.solver.prepared.binder

import androidx.room.solver.CodeGenScope
import androidx.room.solver.prepared.result.PreparedQueryResultAdapter
import com.squareup.javapoet.FieldSpec

/**
 * Connects a prepared query (INSERT, DELETE or UPDATE), db and ResultAdapter.
 *
 * Default implementation is [InstantPreparedQueryResultBinder]. If the query is deferred rather
 * than executed directly then alternative implementations can be implement using this interface
 * (e.g. Rx, ListenableFuture).
 */
abstract class PreparedQueryResultBinder(val adapter: PreparedQueryResultAdapter?) {
    /**
     * Receives a function that will prepare the query in a given scope to then generate the code
     * that runs the query and returns the result.
     */
    abstract fun executeAndReturn(
        prepareQueryStmtBlock: CodeGenScope.() -> String,
        preparedStmtField: String?, // null when the query is not shared
        dbField: FieldSpec,
        scope: CodeGenScope
    )
}