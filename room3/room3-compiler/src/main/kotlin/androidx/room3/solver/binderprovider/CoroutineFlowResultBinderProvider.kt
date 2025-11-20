/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.room3.compiler.processing.XType
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.parser.ParsedQuery
import androidx.room3.processor.Context
import androidx.room3.processor.ProcessorErrors
import androidx.room3.solver.QueryResultBinderProvider
import androidx.room3.solver.TypeAdapterExtras
import androidx.room3.solver.query.result.CoroutineFlowResultBinder
import androidx.room3.solver.query.result.QueryResultBinder

class CoroutineFlowResultBinderProvider(val context: Context) : QueryResultBinderProvider {
    companion object {
        val CHANNEL_TYPE_NAMES =
            listOf(
                KotlinTypeNames.CHANNEL,
                KotlinTypeNames.SEND_CHANNEL,
                KotlinTypeNames.RECEIVE_CHANNEL,
            )
    }

    override fun provide(
        declared: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras,
    ): QueryResultBinder {
        val typeArg = declared.typeArguments.first()
        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query, extras)
        val tableNames =
            ((adapter?.accessedTableNames() ?: emptyList()) + query.tables.map { it.name }).toSet()
        if (tableNames.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        return CoroutineFlowResultBinder(typeArg, tableNames, adapter)
    }

    override fun matches(declared: XType): Boolean {
        if (declared.typeArguments.size != 1) {
            return false
        }
        val typeName = declared.rawType.asTypeName()
        if (typeName in CHANNEL_TYPE_NAMES) {
            context.logger.e(
                ProcessorErrors.invalidChannelType(typeName.toString(context.codeLanguage))
            )
            return false
        }
        return typeName == KotlinTypeNames.FLOW
    }
}
