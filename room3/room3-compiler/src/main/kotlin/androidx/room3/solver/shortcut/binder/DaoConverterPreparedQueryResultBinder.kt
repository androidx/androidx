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

package androidx.room3.solver.shortcut.binder

import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.SQLiteDriverMemberNames
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.prepared.binder.PreparedQueryResultBinder
import androidx.room3.solver.prepared.result.PreparedQueryResultAdapter
import androidx.room3.solver.types.DaoReturnTypeConverter

class DaoConverterPreparedQueryResultBinder(
    val typeArg: XType,
    override val adapter: PreparedQueryResultAdapter?,
    converter: DaoReturnTypeConverter,
) : BaseDaoConverterShortcutBinder(converter), PreparedQueryResultBinder {
    override fun executeAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        returnTypeName: XTypeName,
        scope: CodeGenScope,
    ) {
        convertAndReturnShortcut(typeArg = typeArg, dbProperty = dbProperty, scope = scope) {
            innerScope,
            connectionVar ->
            val statementVar = innerScope.getTmpVar("_stmt")
            innerScope.builder.apply {
                addLocalVal(
                    statementVar,
                    SQLiteDriverTypeNames.STATEMENT,
                    "%L.%M(%L)",
                    connectionVar,
                    SQLiteDriverMemberNames.CONNECTION_PREPARE,
                    sqlQueryVar,
                )
                beginControlFlow("try")
                bindStatement(innerScope, statementVar)
                adapter?.executeAndReturn(connectionVar, statementVar, innerScope)
                nextControlFlow("finally")
                addStatement("%L.close()", statementVar)
                endControlFlow()
            }
        }
    }
}
