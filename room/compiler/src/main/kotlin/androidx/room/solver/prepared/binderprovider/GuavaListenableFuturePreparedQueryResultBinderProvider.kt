/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.solver.prepared.binderprovider

import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.prepared.binder.CallablePreparedQueryResultBinder.Companion.createPreparedBinder
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder
import javax.lang.model.type.DeclaredType

class GuavaListenableFuturePreparedQueryResultBinderProvider(val context: Context) :
    PreparedQueryResultBinderProvider {

    private val hasGuavaRoom by lazy {
        context.processingEnv.elementUtils
            .getTypeElement(RoomGuavaTypeNames.GUAVA_ROOM.toString()) != null
    }

    override fun matches(declared: DeclaredType): Boolean =
        declared.typeArguments.size == 1 &&
                context.processingEnv.typeUtils.erasure(declared).typeName() ==
                GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE

    override fun provide(declared: DeclaredType, query: ParsedQuery): PreparedQueryResultBinder {
        if (!hasGuavaRoom) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT)
        }
        val typeArg = declared.typeArguments.first()
        return createPreparedBinder(
            returnType = typeArg,
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(typeArg, query)
        ) { callableImpl, dbField ->
            addStatement(
                "return $T.createListenableFuture($N, $L, $L)",
                RoomGuavaTypeNames.GUAVA_ROOM,
                dbField,
                "true", // inTransaction
                callableImpl
            )
        }
    }
}