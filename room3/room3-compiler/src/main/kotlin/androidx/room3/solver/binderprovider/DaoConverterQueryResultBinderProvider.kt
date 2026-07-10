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

import androidx.room3.OperationType
import androidx.room3.compiler.processing.XType
import androidx.room3.parser.ParsedQuery
import androidx.room3.processor.Context
import androidx.room3.solver.QueryResultBinderProvider
import androidx.room3.solver.TypeAdapterExtras
import androidx.room3.solver.query.result.DaoConverterQueryResultBinder
import androidx.room3.solver.query.result.QueryResultBinder
import androidx.room3.solver.types.DaoReturnTypeConverter

class DaoConverterQueryResultBinderProvider(
    context: Context,
    val returnTypeConverter: DaoReturnTypeConverter,
) : QueryResultBinderProvider, BaseDaoConverterBinderProvider(context, returnTypeConverter) {

    override fun matches(declared: XType): Boolean = matchConverter(declared, OperationType.READ)

    override fun provide(
        declared: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras,
    ): QueryResultBinder {
        val initialTypeArg = extractTypeArg(declared)

        val typeArg = executeAndReturnLambda.adjustToResultAdapterType(initialTypeArg)
        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query, extras)
        val tableNames =
            ((adapter?.accessedTableNames() ?: emptyList()) + query.tables.map { it.name }).toSet()

        return DaoConverterQueryResultBinder(
            typeArg = typeArg,
            tableNames = tableNames,
            adapter = adapter,
            converter = returnTypeConverter,
        )
    }
}
