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
import androidx.room.compiler.codegen.buildCodeBlock
import androidx.room.compiler.codegen.compat.XConverters.toString
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.capitalize
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.StatementValueBinder
import java.util.Locale

data class PropertyGetter(
    val propertyName: String,
    val jvmName: String,
    val type: XType,
    val callType: CallType,
) {
    fun writeGet(ownerVar: String, outVar: String, builder: XCodeBlock.Builder) {
        builder.addLocalVariable(
            name = outVar,
            typeName = type.asTypeName(),
            assignExpr = getterExpression(ownerVar)
        )
    }

    fun writeGetToStatement(
        ownerVar: String,
        stmtParamVar: String,
        indexVar: String,
        binder: StatementValueBinder,
        scope: CodeGenScope
    ) {
        val varExpr = getterExpression(ownerVar)
        // A temporary local val is needed in Kotlin whenever the getter function returns nullable
        // or
        // the property is nullable such that a smart cast can be properly performed. Even
        // if the property are immutable (val), we still use a local val in case the
        // property is declared in another module, which would make the smart cast impossible.
        if (scope.language == CodeLanguage.KOTLIN && type.nullability != XNullability.NONNULL) {
            val tmpProperty = scope.getTmpVar("_tmp${propertyName.capitalize(Locale.US)}")
            scope.builder.addLocalVariable(
                name = tmpProperty,
                typeName = type.asTypeName(),
                assignExpr = varExpr
            )
            binder.bindToStmt(stmtParamVar, indexVar, tmpProperty, scope)
        } else {
            binder.bindToStmt(
                stmtParamVar,
                indexVar,
                // This function expects a String, which depends on the language. We should change
                // the function signature to accept an XCodeBlock instead.
                varExpr.toString(scope.language),
                scope
            )
        }
    }

    private fun getterExpression(ownerVar: String) = buildCodeBlock { language ->
        when (language) {
            CodeLanguage.JAVA ->
                when (callType) {
                    CallType.PROPERTY -> add("%L.%L", ownerVar, jvmName)
                    CallType.FUNCTION,
                    CallType.SYNTHETIC_FUNCTION -> add("%L.%L()", ownerVar, jvmName)
                    CallType.CONSTRUCTOR -> error("Getters should never be of type 'constructor'!")
                }
            CodeLanguage.KOTLIN ->
                when (callType) {
                    CallType.PROPERTY,
                    CallType.SYNTHETIC_FUNCTION -> add("%L.%L", ownerVar, propertyName)
                    CallType.FUNCTION -> add("%L.%L()", ownerVar, jvmName)
                    CallType.CONSTRUCTOR -> error("Getters should never be of type 'constructor'!")
                }
        }
    }
}
