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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.box
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.LambdaSpec
import androidx.room.ext.RoomMemberNames.DB_UTIL_PERFORM_BLOCKING
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.ext.isNotVoid
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.InsertOrUpsertMethodAdapter
import androidx.room.vo.ShortcutQueryParameter

/** Binder that knows how to write instant (blocking) insert or upsert methods. */
class InstantInsertOrUpsertMethodBinder(adapter: InsertOrUpsertMethodAdapter?) :
    InsertOrUpsertMethodBinder(adapter) {

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        if (adapter == null) {
            return
        }
        val connectionVar = scope.getTmpVar("_connection")
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = DB_UTIL_PERFORM_BLOCKING,
                argFormat = listOf("%N", "%L", "%L"),
                args = listOf(dbProperty, /* isReadOnly= */ false, /* inTransaction= */ true),
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                            parameterName = connectionVar,
                            returnTypeName = adapter.returnType.asTypeName().box(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            adapter.generateMethodBody(
                                scope = scope,
                                parameters = parameters,
                                adapters = adapters,
                                connectionVar = connectionVar
                            )
                        }
                    }
            )
        val returnPrefix =
            when (scope.language) {
                CodeLanguage.JAVA ->
                    if (adapter.returnType.isNotVoid()) {
                        "return "
                    } else {
                        ""
                    }
                CodeLanguage.KOTLIN -> "return "
            }
        scope.builder.add("$returnPrefix%L", performBlock)
    }

    override fun convertAndReturnCompat(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        adapter?.generateMethodBodyCompat(
            parameters = parameters,
            adapters = adapters,
            dbProperty = dbProperty,
            scope = scope
        )
    }

    override fun isMigratedToDriver() = true
}
