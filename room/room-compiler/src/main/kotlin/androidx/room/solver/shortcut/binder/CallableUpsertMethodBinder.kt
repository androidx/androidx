/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.InsertOrUpsertMethodAdapter
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

/**
 * Binder for deferred upsert methods.
 *
 * This binder will create a Callable implementation that delegates to the
 * [InsertOrUpsertMethodAdapter]. Usage of the Callable impl is then delegate to the [addStmntBlock]
 * function.
 */
class CallableUpsertMethodBinder(
    val typeArg: XType,
    val addStmntBlock: CodeBlock.Builder.(callableImpl: TypeSpec, dbField: FieldSpec) -> Unit,
    adapter: InsertOrUpsertMethodAdapter?
) : InsertOrUpsertMethodBinder(adapter) {

    companion object {
        fun createUpsertBinder(
            typeArg: XType,
            adapter: InsertOrUpsertMethodAdapter?,
            addCodeBlock: CodeBlock.Builder.(callableImpl: TypeSpec, dbField: FieldSpec) -> Unit
        ) = CallableUpsertMethodBinder(typeArg, addCodeBlock, adapter)
    }

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, Any>>,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName) {
            adapter?.createMethodBody(
                parameters = parameters,
                adapters = adapters,
                dbField = dbField,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()

        scope.builder().apply {
            addStmntBlock(callableImpl, dbField)
        }
    }
}