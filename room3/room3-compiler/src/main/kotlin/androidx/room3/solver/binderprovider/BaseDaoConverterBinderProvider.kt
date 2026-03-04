/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.solver.binderprovider

import androidx.room3.OperationType
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.isVoidObject
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.isCollection
import androidx.room3.processor.Context
import androidx.room3.processor.ProcessorErrors
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_TYPE_PARAM_MISMATCH
import androidx.room3.solver.types.DaoReturnTypeConverter
import com.google.common.base.Optional

abstract class BaseDaoConverterBinderProvider(
    val context: Context,
    val converter: DaoReturnTypeConverter,
) {
    val executeAndReturnLambda = converter.executeAndReturnLambda

    fun matchConverter(declared: XType, operationType: OperationType): Boolean {
        // Check if the converter supports the operation type of this provider
        if (operationType !in converter.operationTypes) {
            return false
        }

        if (!declared.rawType.isAssignableFrom(converter.to.rawType)) {
            return false
        }

        val convertFunctionReturnTypeArgs = converter.to.typeArguments
        val daoFunctionReturnTypeArgs = declared.typeArguments

        // For side effect operation types (e.g. Rx's Completable, Coroutine's Job, etc).
        if (convertFunctionReturnTypeArgs.isEmpty()) {
            return true
        }
        if (convertFunctionReturnTypeArgs.size != daoFunctionReturnTypeArgs.size) {
            return false
        }

        val allTypeArgsExceptRowAdapterPositionMatch =
            daoFunctionReturnTypeArgs.indices.all { pos ->
                if (pos == executeAndReturnLambda.rowAdapterTypeArgPosition) {
                    true
                } else {
                    convertFunctionReturnTypeArgs[pos].isAssignableFrom(
                        daoFunctionReturnTypeArgs[pos]
                    )
                }
            }

        context.checker.check(
            predicate = allTypeArgsExceptRowAdapterPositionMatch,
            element = converter.to.typeElement!!,
            DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_TYPE_PARAM_MISMATCH,
        )

        return allTypeArgsExceptRowAdapterPositionMatch
    }

    fun extractTypeArg(declared: XType): XType {
        if (converter.to.typeArguments.isEmpty()) {
            return context.processingEnv.requireType(KotlinTypeNames.UNIT)
        }

        val initialTypeArg =
            if (declared.typeArguments.isEmpty()) {
                declared
            } else {
                declared.typeArguments[executeAndReturnLambda.rowAdapterTypeArgPosition]
            }

        val finalTypeArg =
            initialTypeArg.let {
                if (executeAndReturnLambda.hasNullableReturnType && !isCollectionOrOptional(it)) {
                    it.makeNullable()
                } else {
                    it
                }
            }

        if (finalTypeArg.isVoidObject() && finalTypeArg.nullability == XNullability.NONNULL) {
            context.logger.e(ProcessorErrors.NONNULL_VOID)
        }
        return finalTypeArg
    }

    private fun isCollectionOrOptional(type: XType): Boolean {
        return type.isCollection() || type.isTypeOf(Optional::class) || type.isTypeOf(Map::class)
    }
}
