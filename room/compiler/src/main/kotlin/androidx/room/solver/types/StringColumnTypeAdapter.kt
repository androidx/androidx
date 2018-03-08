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

package androidx.room.solver.types

import androidx.room.ext.L
import androidx.room.parser.SQLTypeAffinity.TEXT
import androidx.room.solver.CodeGenScope
import javax.annotation.processing.ProcessingEnvironment

class StringColumnTypeAdapter(processingEnvironment: ProcessingEnvironment)
    : ColumnTypeAdapter((processingEnvironment.elementUtils.getTypeElement(
        String::class.java.canonicalName)).asType(), TEXT) {
    override fun readFromCursor(outVarName: String, cursorVarName: String, indexVarName: String,
                                scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $L.getString($L)", outVarName, cursorVarName, indexVarName)
    }

    override fun bindToStmt(stmtName: String, indexVarName: String, valueVarName: String,
                            scope: CodeGenScope) {
        scope.builder().apply {
            beginControlFlow("if ($L == null)", valueVarName)
                    .addStatement("$L.bindNull($L)", stmtName, indexVarName)
            nextControlFlow("else")
                    .addStatement("$L.bindString($L, $L)", stmtName, indexVarName, valueVarName)
            endControlFlow()
        }
    }
}
