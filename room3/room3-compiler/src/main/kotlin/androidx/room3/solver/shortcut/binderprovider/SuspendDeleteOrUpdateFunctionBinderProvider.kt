/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.solver.shortcut.binderprovider

import androidx.room3.compiler.processing.XType
import androidx.room3.processor.Context
import androidx.room3.processor.ContinuationParamName
import androidx.room3.solver.TypeAdapterExtras
import androidx.room3.solver.shortcut.binder.CoroutineDeleteOrUpdateFunctionBinder
import androidx.room3.solver.shortcut.binder.DeleteOrUpdateFunctionBinder

/**
 * This binder provider is the equivalent of the [InstantDeleteOrUpdateFunctionBinderProvider] for
 * suspending functions, i.e. the 'default provider' for them.
 */
class SuspendDeleteOrUpdateFunctionBinderProvider(private val context: Context) :
    DeleteOrUpdateFunctionBinderProvider {

    override fun matches(declared: XType) = true

    override fun provide(declared: XType, extras: TypeAdapterExtras): DeleteOrUpdateFunctionBinder {
        val continuationName =
            checkNotNull(extras.getData(ContinuationParamName::class)?.paramName) {
                "Continuation parameter name not found in TypeAdapterExtras."
            }
        return CoroutineDeleteOrUpdateFunctionBinder(
            typeArg = declared,
            adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(declared),
            continuationParamName = continuationName,
        )
    }
}
