/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.vo

import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.solver.types.DaoReturnTypeConverter.OptionalParam

/** Holds info on the `convert()` function of a [DaoReturnTypeConverter]. */
data class CustomDaoReturnTypeConverter(
    val to: XType,
    val operationTypes: List<OperationType>,
    val enclosingClass: XTypeElement,
    val isEnclosingClassKotlinObject: Boolean,
    val function: XMethodElement,
    val requiredParameters: List<OptionalParam>,
    val isProvidedConverter: Boolean,
    val executeAndReturnLambda: ExecuteAndReturnLambda,
) {
    val className: XClassName by lazy { enclosingClass.asClassName() }
    val isStatic by lazy { function.isStatic() }

    fun getFunctionName(lang: CodeLanguage) =
        when (lang) {
            CodeLanguage.JAVA -> function.jvmName
            CodeLanguage.KOTLIN -> function.name
        }
}

/**
 * Holds info on the `executeAndReturn()` functional / lambda parameter of a
 * [DaoReturnTypeConverter]'s `convert()` function.
 */
data class ExecuteAndReturnLambda(
    val returnType: XType,
    val hasNullableReturnType: Boolean,
    val hasRawQueryParam: Boolean,
    val rowAdapterTypeArgPosition: Int = -1,
    val adjustToResultAdapterType: (XType) -> XType,
)
