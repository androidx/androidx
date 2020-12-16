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

package androidx.room.solver.binderprovider

import androidx.room.compiler.processing.XType
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.query.result.CoroutineFlowResultBinder
import androidx.room.solver.query.result.QueryResultBinder

@Suppress("FunctionName")
fun CoroutineFlowResultBinderProvider(context: Context): QueryResultBinderProvider =
    CoroutineFlowResultBinderProviderImpl(
        context
    ).requireArtifact(
        context = context,
        requiredType = RoomCoroutinesTypeNames.COROUTINES_ROOM,
        missingArtifactErrorMsg = ProcessorErrors.MISSING_ROOM_COROUTINE_ARTIFACT
    )

private class CoroutineFlowResultBinderProviderImpl(
    val context: Context
) : QueryResultBinderProvider {
    companion object {
        val CHANNEL_TYPE_NAMES = listOf(
            KotlinTypeNames.CHANNEL,
            KotlinTypeNames.SEND_CHANNEL,
            KotlinTypeNames.RECEIVE_CHANNEL
        )
    }

    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        val typeArg = declared.typeArguments.first()
        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query)
        val tableNames = (
            (adapter?.accessedTableNames() ?: emptyList()) +
                query.tables.map { it.name }
            ).toSet()
        if (tableNames.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        return CoroutineFlowResultBinder(typeArg, tableNames, adapter)
    }

    override fun matches(declared: XType): Boolean {
        if (declared.typeArguments.size != 1) {
            return false
        }
        val typeName = declared.rawType.typeName
        if (typeName in CHANNEL_TYPE_NAMES) {
            context.logger.e(ProcessorErrors.invalidChannelType(typeName.toString()))
            return false
        }
        return typeName == KotlinTypeNames.FLOW
    }
}