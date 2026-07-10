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

package androidx.room3.solver.shortcut.binder

import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.codegen.XTypeSpec
import androidx.room3.compiler.processing.XType
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.shortcut.result.DeleteOrUpdateFunctionAdapter
import androidx.room3.solver.types.DaoReturnTypeConverter
import androidx.room3.vo.ShortcutQueryParameter

class DaoConverterDeleteOrUpdateFunctionBinder(
    val typeArg: XType,
    override val adapter: DeleteOrUpdateFunctionAdapter?,
    converter: DaoReturnTypeConverter,
) : BaseDaoConverterShortcutBinder(converter), DeleteOrUpdateFunctionBinder {
    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, XTypeSpec>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope,
    ) {
        if (adapter == null) {
            return
        }
        convertAndReturnShortcut(typeArg = typeArg, dbProperty = dbProperty, scope = scope) {
            innerScope,
            connectionVar ->
            adapter.generateFunctionBody(
                scope = innerScope,
                parameters = parameters,
                adapters = adapters,
                connectionVar = connectionVar,
            )
        }
    }
}
