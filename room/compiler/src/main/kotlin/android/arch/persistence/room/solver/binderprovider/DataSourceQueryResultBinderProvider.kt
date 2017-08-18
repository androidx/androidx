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
import android.arch.persistence.room.solver.QueryResultBinderProvider
import android.arch.persistence.room.solver.query.result.CountedDataSourceQueryResultBinder
import android.arch.persistence.room.solver.query.result.ListQueryResultAdapter
import android.arch.persistence.room.solver.query.result.QueryResultBinder
import com.google.common.annotations.VisibleForTesting
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

class DataSourceQueryResultBinderProvider(val context: Context) : QueryResultBinderProvider {
    private val countedDataSourceTypeMirror: TypeMirror? by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(PagingTypeNames.COUNTED_DATA_SOURCE.toString())?.asType()
    }

    override fun provide(declared: DeclaredType, query: ParsedQuery): QueryResultBinder {
        val typeArg = declared.typeArguments.first()
        val listAdapter = context.typeAdapterStore.findRowAdapter(typeArg, query)?.let {
            ListQueryResultAdapter(it)
        }
        return CountedDataSourceQueryResultBinder(listAdapter, query.tables.map { it.name })
    }

    override fun matches(declared: DeclaredType): Boolean =
            declared.typeArguments.size == 1 && isCountedDataSource(declared)

    private fun isCountedDataSource(declared: DeclaredType): Boolean {
        if (countedDataSourceTypeMirror == null) {
            return false
        }
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        return context.processingEnv.typeUtils.isAssignable(countedDataSourceTypeMirror
                , erasure)
    }


}