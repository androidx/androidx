/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope

/** A [TypeConverter] that has only 1 statement (e.g. foo ? bar : baz). */
abstract class SingleStatementTypeConverter(from: XType, to: XType) : TypeConverter(from, to) {
    final override fun doConvert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder.addStatement("%L = %L", outputVarName, buildStatement(inputVarName, scope))
    }

    final override fun doConvert(inputVarName: String, scope: CodeGenScope): String {
        val outputVarName = scope.getTmpVar()
        scope.builder.addLocalVariable(
            name = outputVarName,
            typeName = to.asTypeName(),
            assignExpr = buildStatement(inputVarName, scope)
        )
        return outputVarName
    }

    /** Returns a [XCodeBlock] that will compute the [to] value. */
    abstract fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock
}
