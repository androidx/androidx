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

package androidx.room.solver.binderprovider

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isVoidObject
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.QueryResultBinderProvider
import androidx.room.solver.TypeAdapterExtras
import androidx.room.solver.query.result.GuavaListenableFutureQueryResultBinder
import androidx.room.solver.query.result.QueryResultBinder

@Suppress("FunctionName")
fun GuavaListenableFutureQueryResultBinderProvider(context: Context): QueryResultBinderProvider =
    GuavaListenableFutureQueryResultBinderProviderImpl(context = context)
        .requireArtifact(
            context = context,
            requiredType = RoomGuavaTypeNames.GUAVA_ROOM_MARKER,
            missingArtifactErrorMsg = ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT
        )

class GuavaListenableFutureQueryResultBinderProviderImpl(val context: Context) :
    QueryResultBinderProvider {
    /**
     * Returns the {@link GuavaListenableFutureQueryResultBinder} instance for the input type, if
     * possible.
     *
     * <p>Emits a compiler error if the Guava Room extension library is not linked.
     */
    override fun provide(
        declared: XType,
        query: ParsedQuery,
        extras: TypeAdapterExtras
    ): QueryResultBinder {
        // Use the type T inside ListenableFuture<T> as the type to adapt and to pass into
        // the binder.
        val adapter =
            context.typeAdapterStore.findQueryResultAdapter(
                declared.typeArguments.first(),
                query,
                extras
            )
        val typeArg = declared.typeArguments.first()
        if (typeArg.isVoidObject() && typeArg.nullability == XNullability.NONNULL) {
            context.logger.e(ProcessorErrors.NONNULL_VOID)
        }
        return GuavaListenableFutureQueryResultBinder(typeArg, adapter)
    }

    /** Returns true iff the input {@code declared} type is ListenableFuture<T>. */
    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 &&
            declared.rawType.asTypeName() == GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE
}
