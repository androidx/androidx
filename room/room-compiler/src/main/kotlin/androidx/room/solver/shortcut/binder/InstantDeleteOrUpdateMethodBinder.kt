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
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.box
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.ext.isNotVoid
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import androidx.room.vo.ShortcutQueryParameter

/**
 * Binder that knows how to write instant (blocking) delete and update methods.
 */
class InstantDeleteOrUpdateMethodBinder(
    adapter: DeleteOrUpdateMethodAdapter?
) : DeleteOrUpdateMethodBinder(adapter) {

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, XTypeSpec>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        when (scope.language) {
            CodeLanguage.JAVA -> convertAndReturnJava(
                parameters, adapters, dbProperty, scope
            )
            CodeLanguage.KOTLIN -> convertAndReturnKotlin(
                parameters, adapters, dbProperty, scope
            )
        }
    }

    private fun convertAndReturnJava(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        if (adapter == null) {
            return
        }

        val returnPrefix = if (adapter.returnType.isNotVoid()) { "return " } else { "" }
        val connectionVar = scope.getTmpVar("_connection")
        scope.builder.addStatement(
            "$returnPrefix%M(%N, %L, %L, %L)",
            RoomTypeNames.DB_UTIL.packageMember("performBlocking"),
            dbProperty,
            false, // isReadOnly
            true, // inTransaction
            Function1TypeSpec(
                language = scope.language,
                parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                parameterName = connectionVar,
                returnTypeName = adapter.returnType.asTypeName().box()
            ) {
                val functionScope = scope.fork()
                val functionCode = functionScope.builder.apply {
                    adapter.generateMethodBody(
                        scope = functionScope,
                        parameters = parameters,
                        adapters = adapters,
                        connectionVar = connectionVar
                    )
                }.build()
                this.addCode(functionCode)
            }
        )
    }

    private fun convertAndReturnKotlin(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        if (adapter == null) {
            return
        }
        val connectionVar = scope.getTmpVar("_connection")
        scope.builder.apply {
            beginControlFlow(
                "return %M(%N, %L, %L) { %L ->",
                RoomTypeNames.DB_UTIL.packageMember("performBlocking"),
                dbProperty,
                false, // isReadOnly
                true, // inTransaction
                connectionVar
            ).apply {
                adapter.generateMethodBody(
                    scope = scope,
                    parameters = parameters,
                    adapters = adapters,
                    connectionVar = connectionVar
                )
            }.endControlFlow()
        }
    }

    override fun convertAndReturnCompat(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, XTypeSpec>>,
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
