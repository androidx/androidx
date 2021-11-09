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

import androidx.room.compiler.processing.XType
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.T
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.shortcut.binder.CallableInsertMethodBinder.Companion.createInsertBinder
import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.vo.ShortcutQueryParameter

/**
 * Provider for Guava ListenableFuture binders.
 */
class GuavaListenableFutureInsertMethodBinderProvider(
    private val context: Context
) : InsertMethodBinderProvider {

    private val hasGuavaRoom by lazy {
        context.processingEnv.findTypeElement(RoomGuavaTypeNames.GUAVA_ROOM) != null
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 &&
            declared.rawType.typeName == GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE

    override fun provide(
        declared: XType,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder {
        if (!hasGuavaRoom) {
            context.logger.e(ProcessorErrors.MISSING_ROOM_GUAVA_ARTIFACT)
        }

        val typeArg = declared.typeArguments.first()
        val adapter = context.typeAdapterStore.findInsertAdapter(typeArg, params)
        return createInsertBinder(typeArg, adapter) { callableImpl, dbField ->
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