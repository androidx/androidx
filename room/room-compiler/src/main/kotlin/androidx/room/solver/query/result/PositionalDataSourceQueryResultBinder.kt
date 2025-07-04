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
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.CommonTypeNames.LIST
import androidx.room.ext.RoomMemberNames.DB_UTIL_SUPPORT_DB_TO_CONNECTION
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.ext.SQLiteDriverTypeNames.CONNECTION
import androidx.room.solver.CodeGenScope

/** Used by Paging2 pipeline */
class PositionalDataSourceQueryResultBinder(
    val listAdapter: ListQueryResultAdapter?,
    val tableNames: Set<String>,
) : QueryResultBinder(listAdapter) {
    val itemTypeName: XTypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.asTypeName() ?: XTypeName.ANY_OBJECT
    val typeName: XTypeName = RoomTypeNames.LIMIT_OFFSET_DATA_SOURCE.parametrizedBy(itemTypeName)

    override val usesCompatQueryWriter = true

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope,
    ) {
        // first comma for table names comes from the string since it might be empty in which case
        // we don't need a comma. If list is empty, this prevents generating bad code (it is still
        // an error to have empty list but that is already reported while item is processed)
        val tableNamesList = tableNames.joinToString("") { ", \"$it\"" }
        val spec =
            XTypeSpec.anonymousClassBuilder(
                    "%N, %L, %L, %L%L",
                    dbProperty,
                    sqlQueryVar,
                    inTransaction,
                    true,
                    tableNamesList,
                )
                .apply {
                    superclass(typeName)
                    addConvertRowsMethod(scope)
                }
                .build()
        val rowAdapter = listAdapter?.rowAdapters?.first()
        if (rowAdapter is DataClassRowAdapter && rowAdapter.relationCollectors.isNotEmpty()) {
            // @Relation use found, initialize a connection.
            val connectionVar = scope.getTmpVar("_connection")
            scope.builder.applyTo { language ->
                val assignExprFormat =
                    when (language) {
                        CodeLanguage.JAVA -> "%M(%L.getOpenHelper().getWritableDatabase())"
                        CodeLanguage.KOTLIN -> "%M(%L.openHelper.writableDatabase)"
                    }
                addLocalVal(
                    name = connectionVar,
                    typeName = CONNECTION,
                    assignExprFormat = assignExprFormat,
                    DB_UTIL_SUPPORT_DB_TO_CONNECTION,
                    dbProperty.name,
                )
            }
        }
        scope.builder.addStatement("return %L", spec)
    }

    private fun XTypeSpec.Builder.addConvertRowsMethod(scope: CodeGenScope) {
        addFunction(
            XFunSpec.builder(
                    name = "convertRows",
                    visibility = VisibilityModifier.PROTECTED,
                    isOverride = true,
                )
                .apply {
                    returns(LIST.parametrizedBy(itemTypeName))
                    val cursorParamName = "statement"
                    addParameter(cursorParamName, SQLiteDriverTypeNames.STATEMENT)
                    val resultVar = scope.getTmpVar("_res")
                    val rowsScope = scope.fork()
                    listAdapter?.convert(resultVar, cursorParamName, rowsScope)
                    addCode(rowsScope.generate())
                    addStatement("return %L", resultVar)
                }
                .build()
        )
    }
}
