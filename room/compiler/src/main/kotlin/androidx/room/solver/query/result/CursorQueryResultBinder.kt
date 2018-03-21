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
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec

class CursorQueryResultBinder : QueryResultBinder(NO_OP_RESULT_ADAPTER) {
    override fun convertAndReturn(roomSQLiteQueryVar: String,
                                  canReleaseQuery: Boolean,
                                  dbField: FieldSpec,
                                  inTransaction: Boolean,
                                  scope: CodeGenScope) {
        val builder = scope.builder()
        val transactionWrapper = if (inTransaction) {
            builder.transactionWrapper(dbField)
        } else {
            null
        }
        transactionWrapper?.beginTransactionWithControlFlow()
        val resultName = scope.getTmpVar("_tmpResult")
        builder.addStatement("final $T $L = $N.query($L)", AndroidTypeNames.CURSOR, resultName,
                dbField, roomSQLiteQueryVar)
        transactionWrapper?.commitTransaction()
        builder.addStatement("return $L", resultName)
        transactionWrapper?.endTransactionWithControlFlow()
    }

    companion object {
        private val NO_OP_RESULT_ADAPTER = object : QueryResultAdapter(null) {
            override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
            }
        }
    }
}
