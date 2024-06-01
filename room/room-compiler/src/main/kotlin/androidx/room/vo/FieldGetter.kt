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

package androidx.room.vo

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.capitalize
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.StatementValueBinder
import java.util.Locale

data class FieldGetter(
    val fieldName: String,
    val jvmName: String,
    val type: XType,
    val callType: CallType,
) {
    fun writeGet(ownerVar: String, outVar: String, builder: XCodeBlock.Builder) {
        builder.addLocalVariable(
            name = outVar,
            typeName = type.asTypeName(),
            assignExpr = getterExpression(ownerVar, builder.language)
        )
    }

    fun writeGetToStatement(
        ownerVar: String,
        stmtParamVar: String,
        indexVar: String,
        binder: StatementValueBinder,
        scope: CodeGenScope
    ) {
        val varExpr = getterExpression(ownerVar, scope.language)
        // A temporary local val is needed in Kotlin whenever the getter method returns nullable or
        // the field / property is nullable such that a smart cast can be properly performed. Even
        // if the field / property are immutable (val), we still use a local val in case the
        // property is declared in another module, which would make the smart cast impossible.
        if (scope.language == CodeLanguage.KOTLIN && type.nullability != XNullability.NONNULL) {
            val tmpField = scope.getTmpVar("_tmp${fieldName.capitalize(Locale.US)}")
            scope.builder.addLocalVariable(
                name = tmpField,
                typeName = type.asTypeName(),
                assignExpr = varExpr
            )
            binder.bindToStmt(stmtParamVar, indexVar, tmpField, scope)
        } else {
            binder.bindToStmt(stmtParamVar, indexVar, varExpr.toString(), scope)
        }
    }

    private fun getterExpression(ownerVar: String, codeLanguage: CodeLanguage): XCodeBlock {
        return when (codeLanguage) {
            CodeLanguage.JAVA ->
                when (callType) {
                    CallType.FIELD -> "%L.%L"
                    CallType.METHOD,
                    CallType.SYNTHETIC_METHOD -> "%L.%L()"
                    CallType.CONSTRUCTOR -> error("Getters should never be of type 'constructor'!")
                }.let { expr -> XCodeBlock.of(codeLanguage, expr, ownerVar, jvmName) }
            CodeLanguage.KOTLIN ->
                when (callType) {
                    CallType.FIELD,
                    CallType.SYNTHETIC_METHOD ->
                        XCodeBlock.of(codeLanguage, "%L.%L", ownerVar, fieldName)
                    CallType.METHOD -> XCodeBlock.of(codeLanguage, "%L.%L()", ownerVar, jvmName)
                    CallType.CONSTRUCTOR -> error("Getters should never be of type 'constructor'!")
                }
        }
    }
}
