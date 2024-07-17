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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.AndroidTypeNames.CURSOR
import androidx.room.ext.RoomMemberNames.DB_UTIL_QUERY
import androidx.room.solver.CodeGenScope

/**
 * Base class for query result binders that observe the database. It includes common functionality
 * like creating a finalizer to release the query or creating the actual adapter call code.
 */
abstract class BaseObservableQueryResultBinder(adapter: QueryResultAdapter?) :
    QueryResultBinder(adapter) {

    protected fun XTypeSpec.Builder.createFinalizeMethod(roomSQLiteQueryVar: String) {
        addFunction(
            XFunSpec.builder(
                    language = language,
                    name = "finalize",
                    visibility = VisibilityModifier.PROTECTED,
                    // To 'override' finalize in Kotlin one does not use the 'override' keyword, but
                    // in
                    // Java the @Override is needed
                    isOverride = language == CodeLanguage.JAVA
                )
                .apply { addStatement("%L.release()", roomSQLiteQueryVar) }
                .build()
        )
    }

    protected fun createRunQueryAndReturnStatements(
        builder: XCodeBlock.Builder,
        roomSQLiteQueryVar: String,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope,
        cancellationSignalVar: String
    ) {
        val transactionWrapper =
            if (inTransaction) {
                builder.transactionWrapper(dbProperty.name)
            } else {
                null
            }
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        transactionWrapper?.beginTransactionWithControlFlow()
        builder.apply {
            addLocalVariable(
                name = cursorVar,
                typeName = CURSOR,
                assignExpr =
                    XCodeBlock.of(
                        language = language,
                        format = "%M(%N, %L, %L, %L)",
                        DB_UTIL_QUERY,
                        dbProperty,
                        roomSQLiteQueryVar,
                        if (shouldCopyCursor) "true" else "false",
                        cancellationSignalVar
                    )
            )
            beginControlFlow("try").apply {
                val adapterScope = scope.fork()
                adapter?.convert(outVar, cursorVar, adapterScope)
                add(adapterScope.generate())
                transactionWrapper?.commitTransaction()
                addStatement("return %L", outVar)
            }
            nextControlFlow("finally").apply { addStatement("%L.close()", cursorVar) }
            endControlFlow()
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }
}
