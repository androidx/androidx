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

import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.XType
import androidx.room.processor.Context
import androidx.room.solver.RxType
import androidx.room.solver.shortcut.binder.CallableInsertMethodBinder.Companion.createInsertBinder
import androidx.room.solver.shortcut.binder.InsertMethodBinder
import androidx.room.vo.ShortcutQueryParameter

/**
 * Provider for Rx Callable binders.
 */
open class RxCallableInsertMethodBinderProvider internal constructor(
    val context: Context,
    private val rxType: RxType
) : InsertMethodBinderProvider {

    /**
     * [Single] and [Maybe] are generics but [Completable] is not so each implementation of this
     * class needs to define how to extract the type argument.
     */
    open fun extractTypeArg(declared: XType): XType = declared.typeArguments.first()

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: XType): Boolean {
        return declared.rawType.typeName == rxType.className
    }

    override fun provide(
        declared: XType,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder {
        val typeArg = extractTypeArg(declared)
        val adapter = context.typeAdapterStore.findInsertAdapter(typeArg, params)
        return createInsertBinder(typeArg, adapter) { callableImpl, _ ->
            addStatement("return $T.fromCallable($L)", rxType.className, callableImpl)
        }
    }

    companion object {
        fun getAll(context: Context) = listOf(
            RxCallableInsertMethodBinderProvider(context, RxType.RX2_SINGLE),
            RxCallableInsertMethodBinderProvider(context, RxType.RX2_MAYBE),
            RxCompletableInsertMethodBinderProvider(context, RxType.RX2_COMPLETABLE),
            RxCallableInsertMethodBinderProvider(context, RxType.RX3_SINGLE),
            RxCallableInsertMethodBinderProvider(context, RxType.RX3_MAYBE),
            RxCompletableInsertMethodBinderProvider(context, RxType.RX3_COMPLETABLE)
        )
    }
}

private class RxCompletableInsertMethodBinderProvider(
    context: Context,
    rxType: RxType
) : RxCallableInsertMethodBinderProvider(context, rxType) {

    private val completableType: XRawType? by lazy {
        context.processingEnv.findType(rxType.className)?.rawType
    }

    /**
     * Since Completable is not a generic, the supported return type should be Void.
     * Like this, the generated Callable.call method will return Void.
     */
    override fun extractTypeArg(declared: XType): XType =
        context.COMMON_TYPES.VOID

    override fun matches(declared: XType): Boolean = isCompletable(declared)

    private fun isCompletable(declared: XType): Boolean {
        if (completableType == null) {
            return false
        }
        return declared.rawType.isAssignableFrom(completableType!!)
    }
}
