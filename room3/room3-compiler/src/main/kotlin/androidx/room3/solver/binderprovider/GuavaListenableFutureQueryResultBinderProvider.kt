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

package androidx.room3.solver.binderprovider

import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.isVoidObject
import androidx.room3.ext.GuavaUtilConcurrentTypeNames
import androidx.room3.ext.RoomGuavaTypeNames
import androidx.room3.parser.ParsedQuery
import androidx.room3.processor.Context
import androidx.room3.processor.ProcessorErrors
import androidx.room3.solver.QueryResultBinderProvider
import androidx.room3.solver.TypeAdapterExtras
import androidx.room3.solver.query.result.GuavaListenableFutureQueryResultBinder
import androidx.room3.solver.query.result.QueryResultBinder

@Suppress("FunctionName")
fun GuavaListenableFutureQueryResultBinderProvider(context: Context): QueryResultBinderProvider =
    GuavaListenableFutureQueryResultBinderProviderImpl(context = context)
        .requireArtifact(
            context = context,
            requiredType = RoomGuavaTypeNames.GUAVA_ROOM_MARKER,
            missingArtifactErrorMsg = ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT,
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
        extras: TypeAdapterExtras,
    ): QueryResultBinder {
        // Use the type T inside ListenableFuture<T> as the type to adapt and to pass into
        // the binder.
        val adapter =
            context.typeAdapterStore.findQueryResultAdapter(
                declared.typeArguments.first(),
                query,
                extras,
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
