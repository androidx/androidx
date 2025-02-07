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
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.processing.XType
import androidx.room.ext.capitalize
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.StatementValueReader
import java.util.Locale

data class PropertySetter(
    val propertyName: String,
    val jvmName: String,
    val type: XType,
    val callType: CallType
) {
    fun writeSet(ownerVar: String, inVar: String, builder: XCodeBlock.Builder) {
        if (callType == CallType.CONSTRUCTOR) {
            return
        }
        builder.applyTo { language ->
            when (language) {
                CodeLanguage.JAVA -> {
                    val stmt =
                        when (callType) {
                            CallType.PROPERTY -> "%L.%L = %L"
                            CallType.FUNCTION,
                            CallType.SYNTHETIC_FUNCTION -> "%L.%L(%L)"
                            else -> error("Unknown call type: $callType")
                        }
                    addStatement(stmt, ownerVar, jvmName, inVar)
                }
                CodeLanguage.KOTLIN -> addStatement("%L.%L = %L", ownerVar, propertyName, inVar)
            }
        }
    }

    fun writeSetFromStatement(
        ownerVar: String,
        stmtVar: String,
        indexVar: String,
        reader: StatementValueReader,
        scope: CodeGenScope
    ) {
        when (scope.language) {
            CodeLanguage.JAVA ->
                when (callType) {
                    CallType.PROPERTY -> {
                        val outPropertyName = "$ownerVar.$jvmName"
                        reader.readFromStatement(outPropertyName, stmtVar, indexVar, scope)
                    }
                    CallType.FUNCTION,
                    CallType.SYNTHETIC_FUNCTION -> {
                        val tmpProperty =
                            scope.getTmpVar("_tmp${propertyName.capitalize(Locale.US)}")
                        scope.builder.apply {
                            addLocalVariable(tmpProperty, type.asTypeName())
                            reader.readFromStatement(tmpProperty, stmtVar, indexVar, scope)
                            addStatement("%L.%L(%L)", ownerVar, jvmName, tmpProperty)
                        }
                    }
                    CallType.CONSTRUCTOR -> {
                        // no code, property is set via constructor
                    }
                }
            CodeLanguage.KOTLIN ->
                when (callType) {
                    CallType.PROPERTY,
                    CallType.SYNTHETIC_FUNCTION -> {
                        val outPropertyName = "$ownerVar.$propertyName"
                        reader.readFromStatement(outPropertyName, stmtVar, indexVar, scope)
                    }
                    CallType.FUNCTION -> {
                        val tmpProperty =
                            scope.getTmpVar("_tmp${propertyName.capitalize(Locale.US)}")
                        scope.builder.apply {
                            addLocalVariable(tmpProperty, type.asTypeName())
                            reader.readFromStatement(tmpProperty, stmtVar, indexVar, scope)
                            addStatement("%L.%L(%L)", ownerVar, jvmName, tmpProperty)
                        }
                    }
                    CallType.CONSTRUCTOR -> {
                        // no code, property is set via constructor
                    }
                }
        }
    }
}
