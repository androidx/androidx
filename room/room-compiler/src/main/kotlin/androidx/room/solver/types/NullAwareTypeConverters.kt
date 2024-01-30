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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.ExceptionTypeNames
import androidx.room.solver.CodeGenScope

/**
 * A type converter that checks if the input is null and returns null instead of calling the
 * [delegate].
 */
class NullSafeTypeConverter(
    val delegate: TypeConverter
) : TypeConverter(
    from = delegate.from.makeNullable(),
    to = delegate.to.makeNullable(),
    cost = delegate.cost + Cost.NULL_SAFE
) {
    init {
        check(delegate.from.nullability == XNullability.NONNULL) {
            "NullableWrapper can ony be used if the input type is non-nullable"
        }
    }

    override fun doConvert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            beginControlFlow("if (%L == null)", inputVarName).apply {
                addStatement("%L = null", outputVarName)
            }
            nextControlFlow("else").apply {
                delegate.convert(inputVarName, outputVarName, scope)
            }
            endControlFlow()
        }
    }
}

/**
 * A [TypeConverter] that checks the value is `non-null` and throws if it is null.
 */
class RequireNotNullTypeConverter(
    from: XType,
) : TypeConverter(
    from = from,
    to = from.makeNonNullable(),
    cost = Cost.REQUIRE_NOT_NULL
) {
    init {
        check(from.nullability != XNullability.NONNULL) {
            "No reason to null check a non-null input"
        }
    }

    override fun doConvert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            beginControlFlow("if (%L == null)", inputVarName).apply {
                addIllegalStateException()
            }
            nextControlFlow("else").apply {
                addStatement("%L = %L", outputVarName, inputVarName)
            }
            endControlFlow()
        }
    }

    override fun doConvert(inputVarName: String, scope: CodeGenScope): String {
        scope.builder.apply {
            beginControlFlow("if (%L == null)", inputVarName).apply {
                addIllegalStateException()
            }
            endControlFlow()
        }
        return inputVarName
    }

    private fun XCodeBlock.Builder.addIllegalStateException() {
        val typeName = from.asTypeName().copy(nullable = false).toString(language)
        val message = "Expected NON-NULL '$typeName', but it was NULL."
        when (language) {
            CodeLanguage.JAVA -> {
                addStatement(
                    "throw %L",
                    XCodeBlock.ofNewInstance(
                        language,
                        ExceptionTypeNames.ILLEGAL_STATE_EXCEPTION,
                        "%S",
                        message
                    )
                )
            }
            CodeLanguage.KOTLIN -> {
                addStatement("error(%S)", message)
            }
        }
    }
}
