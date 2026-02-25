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

package androidx.room3.solver.types

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XType
import androidx.room3.solver.CodeGenScope

/**
 * Internal compiler representation of a DAO return type adapter.
 *
 * This class models the conversion logic defined by a developer provided function annotated with
 * [DaoReturnTypeConverter]. It is used by Room's compiler to generate the necessary code that wraps
 * the execution of a DAO method.
 *
 * This specific abstract class is intended for converters that can be represented by a **single
 * expression** (e.g., `return Foo(executeAndConvert.invoke())`).
 *
 * @param to The target type of the conversion, which is the custom return type specified by the DAO
 *   method (e.g., `Foo<MyEntity>`).
 */
abstract class DaoReturnTypeConverter(val to: XType) {
    abstract val isSuspend: Boolean

    // A value of `-1` indicates that the row adapter does not have a type argument.
    abstract val rowAdapterTypeArgPosition: Int
    abstract val hasNullableLambdaReturnType: Boolean
    abstract val requiredFunctionParamTypes: List<XType>

    /**
     * Returns a [XCodeBlock] that will compute the converted [to] value.
     *
     * This code block must represent a single statement that takes the result of the original DAO
     * operation (passed via the `returnTypeArgName`) and converts it into the final return type.
     *
     * @param returnTypeArgName The name of the variable holding the result of the original DAO
     *   operation (the `T` from `suspend () -> T`).
     * @param scope The [CodeGenScope] for generating code.
     * @return A [XCodeBlock] containing the single conversion statement.
     */
    abstract fun buildStatement(returnTypeArgName: XTypeName, scope: CodeGenScope): XCodeBlock
}
