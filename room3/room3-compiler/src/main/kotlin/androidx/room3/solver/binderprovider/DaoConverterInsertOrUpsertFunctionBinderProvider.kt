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

package androidx.room3.solver.binderprovider

import androidx.room3.OperationType
import androidx.room3.compiler.processing.XType
import androidx.room3.processor.Context
import androidx.room3.solver.shortcut.binder.DaoConverterInsertOrUpsertFunctionBinder
import androidx.room3.solver.shortcut.binder.InsertOrUpsertFunctionBinder
import androidx.room3.solver.shortcut.binderprovider.InsertOrUpsertFunctionBinderProvider
import androidx.room3.solver.types.DaoReturnTypeConverter
import androidx.room3.vo.ShortcutQueryParameter

class DaoConverterInsertOrUpsertFunctionQueryResultBinderProvider(
    context: Context,
    returnTypeConverter: DaoReturnTypeConverter,
) :
    BaseDaoConverterBinderProvider(context, returnTypeConverter),
    InsertOrUpsertFunctionBinderProvider {
    override fun matches(declared: XType): Boolean = matchConverter(declared, OperationType.WRITE)

    override fun provide(
        declared: XType,
        params: List<ShortcutQueryParameter>,
        forUpsert: Boolean,
    ): InsertOrUpsertFunctionBinder {
        val typeArg = extractTypeArg(declared)
        val adapter =
            if (forUpsert) {
                context.typeAdapterStore.findUpsertAdapter(typeArg, params)
            } else {
                context.typeAdapterStore.findInsertAdapter(typeArg, params)
            }

        return DaoConverterInsertOrUpsertFunctionBinder(
            typeArg = typeArg,
            adapter = adapter,
            converter = converter,
        )
    }
}
