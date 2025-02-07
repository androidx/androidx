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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.compat.XConverters.toString
import androidx.room.compiler.processing.XType
import androidx.room.ext.ArrayLiteral
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames.STATEMENT_UTIL
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.Entity
import androidx.room.vo.columnNames
import androidx.room.writer.EntityStatementConverterWriter

class EntityRowAdapter(val entity: Entity, out: XType) : QueryMappedRowAdapter(out) {
    override val mapping = EntityMapping(entity)

    private lateinit var functionSpec: XFunSpec

    private var stmtDelegateVarName: String? = null

    private val indexAdapter =
        object : IndexAdapter {

            private var indexVars: List<ColumnIndexVar>? = null

            override fun onStatementReady(stmtVarName: String, scope: CodeGenScope) {
                indexVars =
                    entity.columnNames.map { columnName ->
                        val packageMember = STATEMENT_UTIL.packageMember("getColumnIndex")
                        ColumnIndexVar(
                            column = columnName,
                            indexVar =
                                XCodeBlock.of("%M(%L, %S)", packageMember, stmtVarName, columnName)
                                    // indexVar expects a string, and that depends on the language.
                                    // We should change the function signature to accept XCodeBlock.
                                    .toString(scope.language)
                        )
                    }
            }

            override fun getIndexVars() = indexVars ?: emptyList()
        }

    override fun onStatementReady(
        stmtVarName: String,
        scope: CodeGenScope,
        indices: List<ColumnIndexVar>
    ) {
        // Check if given indices are the default ones, i.e. onStatementReady() was called without
        // an indices argument and these are the default parameter ones, which means a wrapped
        // statement is not needed since the generated entity statement converter has access to the
        // original statement.
        if (indices.isNotEmpty() && indices != indexAdapter.getIndexVars()) {
            // Due to entity converter code being shared and using getColumnIndex() we can't
            // generate code that uses the mapping directly. Instead we create a wrapped statement
            // that is solely used in the shared converter function and whose getColumnIndex() is
            // overridden to return the resolved column index.
            stmtDelegateVarName = scope.getTmpVar("_wrappedStmt")
            val entityColumnNamesParam =
                ArrayLiteral(CommonTypeNames.STRING, *entity.columnNames.toTypedArray())
            val entityColumnIndicesParam =
                ArrayLiteral(XTypeName.PRIMITIVE_INT, *indices.map { it.indexVar }.toTypedArray())
            val wrapperTypeName = SQLiteDriverTypeNames.STATEMENT
            val packageMember = STATEMENT_UTIL.packageMember("wrapMappedColumns")
            scope.builder.addLocalVariable(
                checkNotNull(stmtDelegateVarName),
                wrapperTypeName,
                assignExpr =
                    XCodeBlock.of(
                        "%M(%L, %L, %L)",
                        packageMember,
                        stmtVarName,
                        entityColumnNamesParam,
                        entityColumnIndicesParam
                    )
            )
        }
        functionSpec =
            scope.writer.getOrCreateFunction(EntityStatementConverterWriter(entity = entity))
    }

    override fun convert(outVarName: String, stmtVarName: String, scope: CodeGenScope) {
        scope.builder.addStatement(
            "%L = %N(%L)",
            outVarName,
            functionSpec,
            stmtDelegateVarName ?: stmtVarName
        )
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class EntityMapping(val entity: Entity) : Mapping() {
        override val usedColumns: List<String> = entity.columnNames
    }
}
