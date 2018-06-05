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

import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.ObservableQueryResultBinderProvider
import androidx.room.solver.query.result.QueryResultAdapter
import androidx.room.solver.query.result.QueryResultBinder
import androidx.room.solver.query.result.RxQueryResultBinder
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

sealed class RxQueryResultBinderProvider(
    context: Context,
    private val rxType: RxQueryResultBinder.RxType
) : ObservableQueryResultBinderProvider(context) {
    private val typeMirror: TypeMirror? by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(rxType.className.toString())?.asType()
    }
    private val hasRxJava2Artifact by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(RoomRxJava2TypeNames.RX_ROOM.toString()) != null
    }

    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()

    override fun create(
        typeArg: TypeMirror,
        resultAdapter: QueryResultAdapter?,
        tableNames: Set<String>
    ): QueryResultBinder {
        return RxQueryResultBinder(
                rxType = rxType,
                typeArg = typeArg,
                queryTableNames = tableNames,
                adapter = resultAdapter
        )
    }

    override fun matches(declared: DeclaredType): Boolean =
            declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: DeclaredType): Boolean {
        if (typeMirror == null) {
            return false
        }
        val erasure = context.processingEnv.typeUtils.erasure(declared)
        val match = context.processingEnv.typeUtils.isAssignable(typeMirror, erasure)
        if (match && !hasRxJava2Artifact) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_RXJAVA2_ARTIFACT)
        }
        return match
    }
}

class RxFlowableQueryResultBinderProvider(context: Context)
    : RxQueryResultBinderProvider(context, RxQueryResultBinder.RxType.FLOWABLE)

class RxObservableQueryResultBinderProvider(context: Context)
    : RxQueryResultBinderProvider(context, RxQueryResultBinder.RxType.OBSERVABLE)