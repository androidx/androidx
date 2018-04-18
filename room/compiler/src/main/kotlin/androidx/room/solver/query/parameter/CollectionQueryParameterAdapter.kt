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

package androidx.room.solver.query.parameter

import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.StatementValueBinder
import com.squareup.javapoet.TypeName

/**
 * Binds Collection<T> (e.g. List<T>) into String[] query args.
 */
class CollectionQueryParameterAdapter(val bindAdapter: StatementValueBinder)
            : QueryParameterAdapter(true) {
    override fun bindToStmt(inputVarName: String, stmtVarName: String, startIndexVarName: String,
                            scope: CodeGenScope) {
        scope.builder().apply {
            val itrVar = scope.getTmpVar("_item")
            beginControlFlow("for ($T $L : $L)", bindAdapter.typeMirror().typeName(), itrVar,
                    inputVarName).apply {
                        bindAdapter.bindToStmt(stmtVarName, startIndexVarName, itrVar, scope)
                        addStatement("$L ++", startIndexVarName)
                    }
            endControlFlow()
        }
    }

    override fun getArgCount(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder()
                .addStatement("final $T $L = $L.size()", TypeName.INT, outputVarName, inputVarName)
    }
}
