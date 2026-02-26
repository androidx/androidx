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

class DaoReturnTypeQueryResultBinderProvider(
    val context: Context,
    val returnTypeConverter: DaoReturnTypeConverter,
) : QueryResultBinderProvider {
    val executeAndReturnLambda = returnTypeConverter.executeAndReturnLambda

    override fun matches(declared: XType): Boolean {
        if (!declared.rawType.isAssignableFrom(returnTypeConverter.to.rawType)) {
            return false
        }

        val convertFunctionReturnTypeArgs = returnTypeConverter.to.typeArguments
        val daoFunctionReturnTypeArgs = declared.typeArguments

        if (convertFunctionReturnTypeArgs.size != daoFunctionReturnTypeArgs.size) {
            return false
        }

        val allTypeArgsExceptRowAdapterPositionMatch =
            daoFunctionReturnTypeArgs.indices.all { pos ->
                pos == executeAndReturnLambda.rowAdapterTypeArgPosition ||
                    convertFunctionReturnTypeArgs[pos].isAssignableFrom(
                        daoFunctionReturnTypeArgs[pos]
                    )
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

        val initialTypeArg =
            if (declared.typeArguments.isEmpty()) {
                    declared
                } else {
                    declared.typeArguments[executeAndReturnLambda.rowAdapterTypeArgPosition]
                }
                .let {
                    if (executeAndReturnLambda.hasNullableReturnType && !isCollectionOrOptional(it))
                        it.makeNullable()
                    else it
                }
        if (initialTypeArg.isVoidObject() && initialTypeArg.nullability == XNullability.NONNULL) {
            context.logger.e(ProcessorErrors.NONNULL_VOID)
        }

        val typeArg = executeAndReturnLambda.adjustToResultAdapterType(initialTypeArg)
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
