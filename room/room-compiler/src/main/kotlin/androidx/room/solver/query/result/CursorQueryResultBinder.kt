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

import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.ext.AndroidTypeNames
import androidx.room.solver.CodeGenScope

class CursorQueryResultBinder : QueryResultBinder(NO_OP_RESULT_ADAPTER) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val transactionWrapper =
            if (inTransaction) {
                scope.builder.transactionWrapper(dbProperty.name)
            } else {
                null
            }
        transactionWrapper?.beginTransactionWithControlFlow()
        scope.builder.apply {
            val resultName = scope.getTmpVar("_tmpResult")
            addLocalVal(
                resultName,
                AndroidTypeNames.CURSOR,
                "%N.query(%L)",
                dbProperty,
                roomSQLiteQueryVar
            )
            transactionWrapper?.commitTransaction()
            addStatement("return %L", resultName)
            transactionWrapper?.endTransactionWithControlFlow()
        }
    }

    companion object {
        private val NO_OP_RESULT_ADAPTER =
            object : QueryResultAdapter(emptyList()) {
                override fun convert(
                    outVarName: String,
                    cursorVarName: String,
                    scope: CodeGenScope
                ) {}
            }
    }
}
