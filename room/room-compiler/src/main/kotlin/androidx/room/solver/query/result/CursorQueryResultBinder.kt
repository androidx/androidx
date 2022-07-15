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

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

class CursorQueryResultBinder : QueryResultBinder(NO_OP_RESULT_ADAPTER) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        sectionsVar: String?,
        tempTableVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val builder = scope.builder()
        val transactionWrapper = if (inTransaction) {
            builder.transactionWrapper(dbField)
        } else {
            null
        }
        transactionWrapper?.beginTransactionWithControlFlow()
        val resultName = scope.getTmpVar("_tmpResult")
        val cursorVar = scope.getTmpVar("_cursor")

        scope.builder().apply {
            if (sectionsVar != null) {
                addStatement("$T $L = null", RoomTypeNames.ROOM_SQL_QUERY, roomSQLiteQueryVar)
            }
            addStatement("$T $L = null", AndroidTypeNames.CURSOR, cursorVar)

            if (sectionsVar != null) {
                val pairVar = scope.getTmpVar("_resultPair")
                addStatement(
                    "final $T $L = $T.prepareQuery($N, $L, $S, $L, true)",
                    ParameterizedTypeName.get(
                        ClassName.get(Pair::class.java),
                        RoomTypeNames.ROOM_SQL_QUERY,
                        TypeName.BOOLEAN.box()
                    ),
                    pairVar,
                    RoomTypeNames.QUERY_UTIL, dbField, inTransaction, tempTableVar, sectionsVar
                )
                addStatement("$L = $L.getFirst()", roomSQLiteQueryVar, pairVar)
            }

            addStatement(
                "final $T $L = $N.query($L)", AndroidTypeNames.CURSOR, resultName,
                dbField, roomSQLiteQueryVar
            )
            transactionWrapper?.commitTransaction()
            addStatement("return $L", resultName)
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }

    companion object {
        private val NO_OP_RESULT_ADAPTER = object : QueryResultAdapter(emptyList()) {
            override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
            }
        }
    }
}
