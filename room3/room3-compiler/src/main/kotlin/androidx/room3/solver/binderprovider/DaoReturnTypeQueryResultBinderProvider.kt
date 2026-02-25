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

package androidx.room3.solver.binderprovider

import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.isVoidObject
import androidx.room3.ext.isCollection
import androidx.room3.parser.ParsedQuery
import androidx.room3.processor.Context
import androidx.room3.processor.ProcessorErrors
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITH_A_TYPE_PARAM_SHOULD_HAVE_RETURN_TYPE_WITH_ONLY_ONE_GENERIC_ARG
import androidx.room3.solver.QueryResultBinderProvider
import androidx.room3.solver.TypeAdapterExtras
import androidx.room3.solver.query.result.DaoReturnTypeQueryResultBinder
import androidx.room3.solver.query.result.QueryResultBinder
import androidx.room3.solver.types.DaoReturnTypeConverter
import com.google.common.base.Optional
import kotlin.collections.plus
import kotlin.collections.toSet

class DaoReturnTypeQueryResultBinderProvider(
    val context: Context,
    val returnTypeConverter: DaoReturnTypeConverter,
) : QueryResultBinderProvider {

    override fun matches(declared: XType): Boolean {
        if (!declared.rawType.isAssignableFrom(returnTypeConverter.to.rawType)) {
            return false
        }

        val converterReturnTypeArgs = returnTypeConverter.to.typeArguments.toMutableList()
        val daoFunctionReturnTypeArgs = declared.typeArguments
        if (converterReturnTypeArgs.size != daoFunctionReturnTypeArgs.size) {
            return false
        } else if (returnTypeConverter.rowAdapterTypeArgPosition >= converterReturnTypeArgs.size) {
            return false
        } else {
            // Remove the element at row adapter type arg position
            converterReturnTypeArgs.removeAt(returnTypeConverter.rowAdapterTypeArgPosition)
        }

        val allTypeArgsExceptRowAdapterPositionMatch =
            converterReturnTypeArgs.zip(daoFunctionReturnTypeArgs).all { (converterType, daoType) ->
                converterType.isAssignableFrom(daoType)
            }

        context.checker.check(
            predicate = allTypeArgsExceptRowAdapterPositionMatch,
            element = returnTypeConverter.to.typeElement!!,
            DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITH_A_TYPE_PARAM_SHOULD_HAVE_RETURN_TYPE_WITH_ONLY_ONE_GENERIC_ARG,
        )

        return allTypeArgsExceptRowAdapterPositionMatch
    }

    override fun provide(
        declared: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras,
    ): QueryResultBinder {
        fun isCollectionOrOptional(type: XType): Boolean {
            return type.isCollection() ||
                type.isTypeOf(Optional::class) ||
                type.isTypeOf(Map::class)
        }

        val typeArg =
            if (declared.typeArguments.isEmpty()) {
                    declared
                } else {
                    declared.typeArguments[returnTypeConverter.rowAdapterTypeArgPosition]
                }
                .let {
                    if (
                        returnTypeConverter.hasNullableLambdaReturnType &&
                            !isCollectionOrOptional(it)
                    )
                        it.makeNullable()
                    else it
                }
        if (typeArg.isVoidObject() && typeArg.nullability == XNullability.NONNULL) {
            context.logger.e(ProcessorErrors.NONNULL_VOID)
        }

        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query, extras)
        val tableNames =
            ((adapter?.accessedTableNames() ?: emptyList()) + query.tables.map { it.name }).toSet()

        return DaoReturnTypeQueryResultBinder(
            typeArg = typeArg,
            tableNames = tableNames,
            adapter = adapter,
            converter = returnTypeConverter,
        )
    }
}
