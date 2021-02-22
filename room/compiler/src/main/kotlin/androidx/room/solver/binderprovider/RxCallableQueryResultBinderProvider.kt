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

import androidx.room.compiler.processing.XType
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.RxType
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.query.result.RxCallableQueryResultBinder

class RxCallableQueryResultBinderProvider private constructor(
    val context: Context,
    private val rxType: RxType
) : QueryResultBinderProvider {
    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        val typeArg = declared.typeArguments.first()
        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query)
        return RxCallableQueryResultBinder(rxType, typeArg, adapter)
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: XType): Boolean {
        return declared.rawType.typeName == rxType.className
    }

    companion object {
        fun getAll(context: Context) = listOf(
            RxType.RX2_SINGLE,
            RxType.RX2_MAYBE,
            RxType.RX3_SINGLE,
            RxType.RX3_MAYBE
        ).map {
            RxCallableQueryResultBinderProvider(context, it).requireArtifact(
                context = context,
                requiredType = it.version.rxRoomClassName,
                missingArtifactErrorMsg = it.version.missingArtifactMessage
            )
        }
    }
}
