/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room3.solver.query.result

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.ext.RoomTypeNames.STATEMENT_UTIL
import androidx.room3.ext.capitalize
import androidx.room3.ext.stripNonJava
import androidx.room3.parser.ParsedQuery
import androidx.room3.processor.ProcessorErrors
import androidx.room3.solver.CodeGenScope
import androidx.room3.verifier.QueryResultInfo
import androidx.room3.vo.ColumnIndexVar
import java.util.Locale

/** Creates the index variables to retrieve columns from a cursor for a [DataClassRowAdapter]. */
class DataClassIndexAdapter(
    private val mapping: DataClassRowAdapter.DataClassMapping,
    private val info: QueryResultInfo?,
    private val query: ParsedQuery?,
) : IndexAdapter {

    private lateinit var columnIndexVars: List<ColumnIndexVar>

    override fun onStatementReady(stmtVarName: String, scope: CodeGenScope) {
        columnIndexVars =
            mapping.matchedFields.map {
                val indexVar =
                    scope.getTmpVar("_columnIndexOf${it.name.stripNonJava().capitalize(Locale.US)}")
                if (info != null && query != null && query.hasTopStarProjection == false) {
                    // When result info is available and query does not have a top-level star
                    // projection we can generate column to field index since the column result
                    // order
                    // is deterministic.
                    val infoIndex =
                        info.columns.indexOfFirst { columnInfo -> columnInfo.name == it.columnName }
                    check(infoIndex != -1) {
                        "Result column index not found for field '$it' with column name " +
                            "'${it.columnName}'. Query: ${query.original}. Please file a bug at " +
                            ProcessorErrors.ISSUE_TRACKER_LINK
                    }
                    scope.builder.addLocalVal(indexVar, XTypeName.PRIMITIVE_INT, "%L", infoIndex)
                } else {
                    val indexMethod =
                        if (info == null) {
                            "getColumnIndex"
                        } else {
                            "getColumnIndexOrThrow"
                        }
                    val packageMember = STATEMENT_UTIL.packageMember(indexMethod)
                    scope.builder.addLocalVariable(
                        name = indexVar,
                        typeName = XTypeName.PRIMITIVE_INT,
                        assignExpr =
                            XCodeBlock.of("%M(%L, %S)", packageMember, stmtVarName, it.columnName),
                    )
                }
                ColumnIndexVar(it.columnName, indexVar)
            }
    }

    override fun getIndexVars() = columnIndexVars
}
