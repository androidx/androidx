/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XRawType
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.CommonTypeNames.LIST
import androidx.room3.parser.ParsedQuery
import androidx.room3.processor.Context
import androidx.room3.processor.ProcessorErrors
import androidx.room3.solver.QueryResultBinderProvider
import androidx.room3.solver.TypeAdapterExtras
import androidx.room3.solver.query.result.ListQueryResultAdapter
import androidx.room3.solver.query.result.MultiTypePagingSourceQueryResultBinder
import androidx.room3.solver.query.result.QueryResultBinder

class MultiTypedPagingSourceQueryResultBinderProvider(
    private val context: Context,
    private val roomPagingClassName: XClassName,
    private val pagingSourceTypeName: XClassName,
) : QueryResultBinderProvider {

    private val pagingSourceType: XRawType? by lazy {
        context.processingEnv.findType(pagingSourceTypeName.canonicalName)?.rawType
    }

    private val roomPagingSourceTypeElement by lazy {
        context.processingEnv.requireTypeElement(roomPagingClassName)
    }

    private val convertExecutableElement by lazy {
        roomPagingSourceTypeElement.getDeclaredMethods().first {
            it.isSuspendFunction() && it.name == "convertRows"
        }
    }

    override fun provide(
        declared: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras,
    ): QueryResultBinder {
        if (query.tables.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        val typeArg = declared.typeArguments.last()
        val listAdapter =
            context.typeAdapterStore.findRowAdapter(typeArg, query)?.let {
                ListQueryResultAdapter(typeArg, it)
            }
        val tableNames =
            ((listAdapter?.accessedTableNames() ?: emptyList()) + query.tables.map { it.name })
                .toSet()

        val convertRowsOverrideInfo =
            ConvertRowsOverrideInfo(
                function = convertExecutableElement,
                continuationParamName = convertExecutableElement.parameters.last().name,
                owner = context.processingEnv.getDeclaredType(roomPagingSourceTypeElement, typeArg),
                returnTypeName = LIST.parametrizedBy(typeArg.asTypeName()),
            )
        return MultiTypePagingSourceQueryResultBinder(
            listAdapter = listAdapter,
            tableNames = tableNames,
            className = roomPagingClassName,
            convertRowsOverrideInfo = convertRowsOverrideInfo,
        )
    }

    override fun matches(declared: XType): Boolean {
        val collectionTypeRaw =
            context.processingEnv.requireType(CommonTypeNames.COLLECTION).rawType

        if (pagingSourceType == null) {
            return false
        }

        if (declared.typeArguments.isEmpty()) {
            return false
        }

        if (!pagingSourceType!!.isAssignableFrom(declared)) {
            return false
        }

        val boxedIntType = context.processingEnv.requireType(XTypeName.BOXED_INT)
        if (!declared.typeArguments.first().isSameType(boxedIntType)) {
            context.logger.e(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_TYPE)
        }

        if (collectionTypeRaw.isAssignableFrom(declared.typeArguments.last().rawType)) {
            context.logger.e(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
        }

        return true
    }
}

/**
 * Data class used to store necessary info when generating the suspending `convertRows` function.
 */
class ConvertRowsOverrideInfo(
    val continuationParamName: String,
    val function: XMethodElement,
    val owner: XType,
    val returnTypeName: XTypeName,
)
