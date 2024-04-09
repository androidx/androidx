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

package androidx.room.solver.shortcut.binder

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.processing.XType
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.vo.ShortcutQueryParameter

/**
 * Binder for deferred delete and update methods.
 *
 * This binder will create a Callable implementation that delegates to the
 * [DeleteOrUpdateMethodAdapter]. Usage of the Callable impl is then delegate to the [addStmntBlock]
 * function.
 */
class CallableDeleteOrUpdateMethodBinder private constructor(
    val typeArg: XType,
    val addStmntBlock: XCodeBlock.Builder.(callableImpl: XTypeSpec, dbField: XPropertySpec) -> Unit,
    adapter: DeleteOrUpdateMethodAdapter?
) : DeleteOrUpdateMethodBinder(adapter) {

    companion object {
        fun createDeleteOrUpdateBinder(
            typeArg: XType,
            adapter: DeleteOrUpdateMethodAdapter?,
            addCodeBlock: XCodeBlock.Builder.(
                callableImpl: XTypeSpec,
                dbField: XPropertySpec
            ) -> Unit
        ) = CallableDeleteOrUpdateMethodBinder(typeArg, addCodeBlock, adapter)
    }

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, XTypeSpec>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
      convertAndReturnCompat(parameters, adapters, dbProperty, scope)
    }

    override fun convertAndReturnCompat(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, XTypeSpec>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val callableImpl = CallableTypeSpecBuilder(scope.language, typeArg.asTypeName()) {
            adapter?.generateMethodBodyCompat(
                parameters = parameters,
                adapters = adapters,
                dbProperty = dbProperty,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()

        scope.builder.apply {
            addStmntBlock(callableImpl, dbProperty)
        }
    }
}
