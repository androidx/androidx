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
import androidx.room.compiler.processing.XType
import androidx.room.ext.capitalize
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.CursorValueReader
import java.util.Locale

data class FieldSetter(
    val fieldName: String,
    val jvmName: String,
    val type: XType,
    val callType: CallType
) {
    fun writeSet(ownerVar: String, inVar: String, builder: XCodeBlock.Builder) {
        if (callType == CallType.CONSTRUCTOR) {
            return
        }
        when (builder.language) {
            CodeLanguage.JAVA -> {
                val stmt =
                    when (callType) {
                        CallType.FIELD -> "%L.%L = %L"
                        CallType.METHOD,
                        CallType.SYNTHETIC_METHOD -> "%L.%L(%L)"
                        else -> error("Unknown call type: $callType")
                    }
                builder.addStatement(stmt, ownerVar, jvmName, inVar)
            }
            CodeLanguage.KOTLIN -> {
                builder.addStatement("%L.%L = %L", ownerVar, fieldName, inVar)
            }
        }
    }

    fun writeSetFromCursor(
        ownerVar: String,
        cursorVar: String,
        indexVar: String,
        reader: CursorValueReader,
        scope: CodeGenScope
    ) {
        when (scope.language) {
            CodeLanguage.JAVA ->
                when (callType) {
                    CallType.FIELD -> {
                        val outFieldName = "$ownerVar.$jvmName"
                        reader.readFromCursor(outFieldName, cursorVar, indexVar, scope)
                    }
                    CallType.METHOD,
                    CallType.SYNTHETIC_METHOD -> {
                        val tmpField = scope.getTmpVar("_tmp${fieldName.capitalize(Locale.US)}")
                        scope.builder.apply {
                            addLocalVariable(tmpField, type.asTypeName())
                            reader.readFromCursor(tmpField, cursorVar, indexVar, scope)
                            addStatement("%L.%L(%L)", ownerVar, jvmName, tmpField)
                        }
                    }
                    CallType.CONSTRUCTOR -> {
                        // no code, field is set via constructor
                    }
                }
            CodeLanguage.KOTLIN ->
                when (callType) {
                    CallType.FIELD,
                    CallType.SYNTHETIC_METHOD -> {
                        val outFieldName = "$ownerVar.$fieldName"
                        reader.readFromCursor(outFieldName, cursorVar, indexVar, scope)
                    }
                    CallType.METHOD -> {
                        val tmpField = scope.getTmpVar("_tmp${fieldName.capitalize(Locale.US)}")
                        scope.builder.apply {
                            addLocalVariable(tmpField, type.asTypeName())
                            reader.readFromCursor(tmpField, cursorVar, indexVar, scope)
                            addStatement("%L.%L(%L)", ownerVar, jvmName, tmpField)
                        }
                    }
                    CallType.CONSTRUCTOR -> {
                        // no code, field is set via constructor
                    }
                }
        }
    }
}
