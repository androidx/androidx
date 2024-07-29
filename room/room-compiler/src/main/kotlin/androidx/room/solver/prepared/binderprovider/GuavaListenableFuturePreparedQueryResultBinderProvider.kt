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

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isVoidObject
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.RoomGuavaMemberNames.GUAVA_ROOM_CREATE_LISTENABLE_FUTURE
import androidx.room.ext.RoomGuavaTypeNames.GUAVA_ROOM_MARKER
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.prepared.binder.CallablePreparedQueryResultBinder.Companion.createPreparedBinder
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder

class GuavaListenableFuturePreparedQueryResultBinderProvider(val context: Context) :
    PreparedQueryResultBinderProvider {

    private val hasGuavaRoom by lazy {
        context.processingEnv.findTypeElement(GUAVA_ROOM_MARKER.canonicalName) != null
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 &&
            declared.rawType.asTypeName() == GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE

    override fun provide(declared: XType, query: ParsedQuery): PreparedQueryResultBinder {
        if (!hasGuavaRoom) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT)
        }
        val typeArg = declared.typeArguments.first()
        if (typeArg.isVoidObject() && typeArg.nullability == XNullability.NONNULL) {
            context.logger.e(ProcessorErrors.NONNULL_VOID)
        }

        return createPreparedBinder(
            returnType = typeArg,
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(typeArg, query)
        ) { callableImpl, dbField ->
            addStatement(
                "return %M(%N, %L, %L)",
                GUAVA_ROOM_CREATE_LISTENABLE_FUTURE,
                dbField,
                "true", // inTransaction
                callableImpl
            )
        }
    }
}
