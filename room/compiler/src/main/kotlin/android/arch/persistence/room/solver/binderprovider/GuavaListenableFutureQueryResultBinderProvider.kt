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

package android.arch.persistence.room.solver.binderprovider

import android.arch.persistence.room.ext.GuavaUtilConcurrentTypeNames
import android.arch.persistence.room.ext.RoomGuavaTypeNames
import android.arch.persistence.room.ext.typeName
import android.arch.persistence.room.parser.ParsedQuery
import android.arch.persistence.room.processor.Context
import android.arch.persistence.room.processor.ProcessorErrors
import android.arch.persistence.room.solver.QueryResultBinderProvider
import android.arch.persistence.room.solver.query.result.QueryResultBinder
import android.arch.persistence.room.solver.query.result.GuavaListenableFutureQueryResultBinder
import javax.lang.model.type.DeclaredType

class GuavaListenableFutureQueryResultBinderProvider(val context: Context)
    : QueryResultBinderProvider {

    private val hasGuavaRoom by lazy {
        context.processingEnv.elementUtils
                .getTypeElement(RoomGuavaTypeNames.GUAVA_ROOM.toString()) != null
    }

    /**
     * Returns the {@link GuavaListenableFutureQueryResultBinder} instance for the input type, if
     * possible.
     *
     * <p>Emits a compiler error if the Guava Room extension library is not linked.
     */
    override fun provide(declared: DeclaredType, query: ParsedQuery): QueryResultBinder {
        if (!hasGuavaRoom) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT)
        }

        // Use the type T inside ListenableFuture<T> as the type to adapt and to pass into
        // the binder.
        val adapter = context.typeAdapterStore.findQueryResultAdapter(
                declared.typeArguments.first(), query)
        return GuavaListenableFutureQueryResultBinder(
                declared.typeArguments.first(), adapter)
    }

    /**
     * Returns true iff the input {@code declared} type is ListenableFuture<T>.
     */
    override fun matches(declared: DeclaredType): Boolean =
        declared.typeArguments.size == 1 &&
                context.processingEnv.typeUtils.erasure(declared).typeName() ==
                        GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE
}
