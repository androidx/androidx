/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.room.writer

import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.solver.CodeGenScope

/** Creates anonymous classes for RoomTypeNames#SHARED_SQLITE_STMT. */
class PreparedStatementWriter(val queryWriter: QueryWriter) {
    fun createAnonymous(typeWriter: TypeWriter, dbProperty: XPropertySpec): XTypeSpec {
        val scope = CodeGenScope(typeWriter)
        return XTypeSpec.anonymousClassBuilder(scope.language, "%N", dbProperty)
            .apply {
                superclass(RoomTypeNames.SHARED_SQLITE_STMT)
                addFunction(
                    XFunSpec.builder(
                            language = scope.language,
                            name = "createQuery",
                            visibility = VisibilityModifier.PUBLIC,
                            isOverride = true
                        )
                        .apply {
                            returns(CommonTypeNames.STRING)
                            val queryName = scope.getTmpVar("_query")
                            val queryGenScope = scope.fork()
                            queryWriter.prepareQuery(queryName, queryGenScope)
                            addCode(queryGenScope.generate())
                            addStatement("return %L", queryName)
                        }
                        .build()
                )
            }
            .build()
    }
}
