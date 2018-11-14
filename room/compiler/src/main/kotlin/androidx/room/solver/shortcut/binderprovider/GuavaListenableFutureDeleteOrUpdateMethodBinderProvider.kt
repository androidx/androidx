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

import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.typeName
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import androidx.room.solver.shortcut.binder.GuavaListenableFutureDeleteOrUpdateMethodBinder
import javax.lang.model.type.DeclaredType

/**
 * Provider for Guava ListenableFuture binders.
 */
class GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(
    val context: Context
) : DeleteOrUpdateMethodBinderProvider {

    private val hasGuavaRoom by lazy {
        context.processingEnv.elementUtils
            .getTypeElement(RoomGuavaTypeNames.GUAVA_ROOM.toString()) != null
    }

    override fun matches(declared: DeclaredType): Boolean =
        declared.typeArguments.size == 1 &&
                context.processingEnv.typeUtils.erasure(declared).typeName() ==
                GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE

    override fun provide(declared: DeclaredType): DeleteOrUpdateMethodBinder {
        if (!hasGuavaRoom) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT)
        }

        val typeArg = declared.typeArguments.first()
        val adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(typeArg)
        return GuavaListenableFutureDeleteOrUpdateMethodBinder(typeArg, adapter)
    }
}