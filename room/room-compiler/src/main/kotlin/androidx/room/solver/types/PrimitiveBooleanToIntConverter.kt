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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.solver.CodeGenScope

/** int to boolean adapter. */
object PrimitiveBooleanToIntConverter {
    fun create(processingEnvironment: XProcessingEnv): List<TypeConverter> {
        val tBoolean = processingEnvironment.requireType(XTypeName.PRIMITIVE_BOOLEAN)
        val tInt = processingEnvironment.requireType(XTypeName.PRIMITIVE_INT)
        return listOf(
            object : SingleStatementTypeConverter(tBoolean, tInt) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return when (scope.language) {
                        CodeLanguage.JAVA ->
                            XCodeBlock.of(scope.language, "%L ? 1 : 0", inputVarName)
                        CodeLanguage.KOTLIN ->
                            XCodeBlock.of(scope.language, "if (%L) 1 else 0", inputVarName)
                    }
                }
            },
            object : SingleStatementTypeConverter(tInt, tBoolean) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.of(scope.language, "%L != 0", inputVarName)
                }
            }
        )
    }
}
