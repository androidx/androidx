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

package androidx.room.solver.shortcut.binderprovider

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isVoidObject
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.RoomGuavaMemberNames.GUAVA_ROOM_CREATE_LISTENABLE_FUTURE
import androidx.room.ext.RoomGuavaTypeNames.GUAVA_ROOM_MARKER
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.LambdaDeleteOrUpdateMethodBinder

/** Provider for Guava ListenableFuture binders. */
class GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(val context: Context) :
    DeleteOrUpdateMethodBinderProvider {

    private val hasGuavaRoom by lazy {
        context.processingEnv.getElementsFromPackage(GUAVA_ROOM_MARKER.packageName).isNotEmpty()
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 &&
            declared.rawType.asTypeName() == GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE

    override fun provide(declared: XType): DeleteOrUpdateMethodBinder {
        if (!hasGuavaRoom) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT)
        }

        val typeArg = declared.typeArguments.first()
        if (typeArg.isVoidObject() && typeArg.nullability == XNullability.NONNULL) {
            context.logger.e(ProcessorErrors.NONNULL_VOID)
        }

        return LambdaDeleteOrUpdateMethodBinder(
            typeArg = typeArg,
            functionName = GUAVA_ROOM_CREATE_LISTENABLE_FUTURE,
            adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(typeArg)
        )
    }
}
