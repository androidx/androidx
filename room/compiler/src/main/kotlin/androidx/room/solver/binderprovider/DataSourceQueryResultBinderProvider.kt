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

import androidx.room.ext.PagingTypeNames
import androidx.room.ext.findTypeMirror
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.query.result.ListQueryResultAdapter
import androidx.room.solver.query.result.PositionalDataSourceQueryResultBinder
import androidx.room.solver.query.result.QueryResultBinder
import erasure
import isAssignableFrom
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

class DataSourceQueryResultBinderProvider(val context: Context) : QueryResultBinderProvider {
    private val dataSourceTypeMirror: TypeMirror? by lazy {
        context.processingEnv.findTypeMirror(PagingTypeNames.DATA_SOURCE)
    }

    private val positionalDataSourceTypeMirror: TypeMirror? by lazy {
        context.processingEnv.findTypeMirror(PagingTypeNames.POSITIONAL_DATA_SOURCE)
    }

    override fun provide(declared: DeclaredType, query: ParsedQuery): QueryResultBinder {
        if (query.tables.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        val typeArg = declared.typeArguments.last()
        val listAdapter = context.typeAdapterStore.findRowAdapter(typeArg, query)?.let {
            ListQueryResultAdapter(it)
        }
        val tableNames = ((listAdapter?.accessedTableNames() ?: emptyList()) +
                query.tables.map { it.name }).toSet()
        return PositionalDataSourceQueryResultBinder(listAdapter, tableNames)
    }

    override fun matches(declared: DeclaredType): Boolean {
        if (dataSourceTypeMirror == null || positionalDataSourceTypeMirror == null) {
            return false
        }
        if (declared.typeArguments.isEmpty()) {
            return false
        }
        val typeUtils = context.processingEnv.typeUtils
        val erasure = declared.erasure(typeUtils)
        val isDataSource = dataSourceTypeMirror!!.isAssignableFrom(
            typeUtils,
            erasure
        )
        if (!isDataSource) {
            return false
        }
        val isPositional = positionalDataSourceTypeMirror!!.isAssignableFrom(
            typeUtils, erasure
        )
        if (!isPositional) {
            context.logger.e(ProcessorErrors.PAGING_SPECIFY_DATA_SOURCE_TYPE)
        }
        return true
    }
}
