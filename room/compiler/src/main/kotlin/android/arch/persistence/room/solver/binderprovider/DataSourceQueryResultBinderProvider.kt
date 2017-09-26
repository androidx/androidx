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

package android.arch.persistence.room.solver.binderprovider

import android.arch.persistence.room.ext.PagingTypeNames
import android.arch.persistence.room.parser.ParsedQuery
import android.arch.persistence.room.processor.Context
import android.arch.persistence.room.processor.ProcessorErrors
import android.arch.persistence.room.solver.QueryResultBinderProvider
import android.arch.persistence.room.solver.query.result.TiledDataSourceQueryResultBinder
import android.arch.persistence.room.solver.query.result.ListQueryResultAdapter
import android.arch.persistence.room.solver.query.result.QueryResultBinder
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

class DataSourceQueryResultBinderProvider(val context: Context) : QueryResultBinderProvider {
    private val dataSourceTypeMirror: TypeMirror? by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(PagingTypeNames.DATA_SOURCE.toString())?.asType()
    }

    private val tiledDataSourceTypeMirror: TypeMirror? by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(PagingTypeNames.TILED_DATA_SOURCE.toString())?.asType()
    }

    override fun provide(declared: DeclaredType, query: ParsedQuery): QueryResultBinder {
        val typeArg = declared.typeArguments.last()
        val listAdapter = context.typeAdapterStore.findRowAdapter(typeArg, query)?.let {
            ListQueryResultAdapter(it)
        }
        return TiledDataSourceQueryResultBinder(listAdapter, query.tables.map { it.name })
    }

    override fun matches(declared: DeclaredType): Boolean {
        if (dataSourceTypeMirror == null || tiledDataSourceTypeMirror == null) {
            return false
        }
        if (declared.typeArguments.isEmpty()) {
            return false
        }
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        val isDataSource = context.processingEnv.typeUtils
                .isAssignable(erasure, dataSourceTypeMirror)
        if (!isDataSource) {
            return false
        }
        val isTiled = context.processingEnv.typeUtils
                .isAssignable(erasure, tiledDataSourceTypeMirror)
        if (!isTiled) {
            context.logger.e(ProcessorErrors.PAGING_SPECIFY_DATA_SOURCE_TYPE)
        }
        return true
    }
}