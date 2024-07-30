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
import androidx.room.solver.ObservableQueryResultBinderProvider
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.RxType
import androidx.room.solver.TypeAdapterExtras
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.query.result.RxLambdaQueryResultBinder

class RxLambdaQueryResultBinderProvider
private constructor(val context: Context, private val rxType: RxType) : QueryResultBinderProvider {
    override fun provide(
        declared: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras
    ): QueryResultBinder {
        // Add info that the type mirror is hardcoded into a Kotlin Nullable for Callable
        extras.putData(
            ObservableQueryResultBinderProvider.OriginalTypeArg::class,
            ObservableQueryResultBinderProvider.OriginalTypeArg(declared)
        )
        val typeArg = extractTypeArg(declared)
        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query, extras)
        return RxLambdaQueryResultBinder(rxType, typeArg, adapter)
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: XType): Boolean {
        return declared.rawType.asTypeName() == rxType.className
    }

    private fun extractTypeArg(declared: XType): XType {
        // Maybe is always expected to be built with a Callable containing a nullable type argument
        return if (rxType.canBeNull || rxType.isSingle()) {
            declared.typeArguments.first().makeNullable()
        } else {
            declared.typeArguments.first()
        }
    }

    companion object {
        fun getAll(context: Context) =
            listOf(RxType.RX2_SINGLE, RxType.RX2_MAYBE, RxType.RX3_SINGLE, RxType.RX3_MAYBE).map {
                RxLambdaQueryResultBinderProvider(context, it)
                    .requireArtifact(
                        context = context,
                        requiredType = it.version.rxMarkerClassName,
                        missingArtifactErrorMsg = it.version.missingArtifactMessage
                    )
            }
    }
}
