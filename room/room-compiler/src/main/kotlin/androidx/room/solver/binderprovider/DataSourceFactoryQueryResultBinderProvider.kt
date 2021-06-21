/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.solver.binderprovider

import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.XType
import androidx.room.ext.PagingTypeNames
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.query.result.DataSourceFactoryQueryResultBinder
import androidx.room.solver.query.result.ListQueryResultAdapter
import androidx.room.solver.query.result.PositionalDataSourceQueryResultBinder
import androidx.room.solver.query.result.QueryResultBinder

class DataSourceFactoryQueryResultBinderProvider(val context: Context) : QueryResultBinderProvider {
    private val dataSourceFactoryType: XRawType? by lazy {
        context.processingEnv.findType(PagingTypeNames.DATA_SOURCE_FACTORY)?.rawType
    }

    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        if (query.tables.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        val typeArg = declared.typeArguments[1]
        val adapter = context.typeAdapterStore.findRowAdapter(typeArg, query)?.let {
            ListQueryResultAdapter(typeArg, it)
        }

        val tableNames = (
            (adapter?.accessedTableNames() ?: emptyList()) +
                query.tables.map { it.name }
            ).toSet()
        val countedBinder = PositionalDataSourceQueryResultBinder(
            listAdapter = adapter,
            tableNames = tableNames,
            forPaging3 = false
        )
        return DataSourceFactoryQueryResultBinder(countedBinder)
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 2 && isLivePagedList(declared)

    private fun isLivePagedList(declared: XType): Boolean {
        if (dataSourceFactoryType == null) {
            return false
        }
        // we don't want to return paged list unless explicitly requested
        return declared.rawType.isAssignableFrom(dataSourceFactoryType!!)
    }
}
