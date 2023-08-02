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

package androidx.room.solver.prepared.binder

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.processing.XType
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.solver.CodeGenScope
import androidx.room.solver.prepared.result.PreparedQueryResultAdapter

/**
 * Binder for deferred queries.
 *
 * This binder will create a Callable implementation that delegates to the
 * [PreparedQueryResultAdapter]. Usage of the Callable impl is then delegate to the [addStmntBlock]
 * function.
 */
class CallablePreparedQueryResultBinder private constructor(
    val returnType: XType,
    val addStmntBlock: XCodeBlock.Builder.(
        callableImpl: XTypeSpec,
        dbProperty: XPropertySpec
    ) -> Unit,
    adapter: PreparedQueryResultAdapter?
) : PreparedQueryResultBinder(adapter) {

    companion object {
        fun createPreparedBinder(
            returnType: XType,
            adapter: PreparedQueryResultAdapter?,
            addCodeBlock: XCodeBlock.Builder.(
                callableImpl: XTypeSpec,
                dbProperty: XPropertySpec
            ) -> Unit
        ) = CallablePreparedQueryResultBinder(returnType, addCodeBlock, adapter)
    }

    override fun executeAndReturn(
        prepareQueryStmtBlock: CodeGenScope.() -> String,
        preparedStmtProperty: XPropertySpec?,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        val binderScope = scope.fork()
        val callableImpl = CallableTypeSpecBuilder(scope.language, returnType.asTypeName()) {
            adapter?.executeAndReturn(
                binderScope.prepareQueryStmtBlock(),
                preparedStmtProperty,
                dbProperty,
                binderScope
            )
            addCode(binderScope.generate())
        }.build()

        scope.builder.apply {
            addStmntBlock(callableImpl, dbProperty)
        }
    }
}
