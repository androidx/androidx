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

package androidx.room.solver.types

import androidx.room.ext.L
import androidx.room.solver.CodeGenScope
import com.google.auto.common.MoreTypes
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeMirror

/**
 * Adapters for all boxed primitives that has direct cursor mappings.
 */
open class BoxedPrimitiveColumnTypeAdapter(
        boxed: TypeMirror,
        val primitiveAdapter: PrimitiveColumnTypeAdapter
) : ColumnTypeAdapter(boxed, primitiveAdapter.typeAffinity) {
    companion object {
        fun createBoxedPrimitiveAdapters(
                processingEnvironment: ProcessingEnvironment,
                primitiveAdapters: List<PrimitiveColumnTypeAdapter>
        ): List<ColumnTypeAdapter> {

            return primitiveAdapters.map {
                BoxedPrimitiveColumnTypeAdapter(
                        processingEnvironment.typeUtils
                                .boxedClass(MoreTypes.asPrimitiveType(it.out)).asType(),
                        it
                )
            }
        }
    }

    override fun bindToStmt(stmtName: String, indexVarName: String, valueVarName: String,
                            scope: CodeGenScope) {
        scope.builder().apply {
            beginControlFlow("if ($L == null)", valueVarName).apply {
                addStatement("$L.bindNull($L)", stmtName, indexVarName)
            }
            nextControlFlow("else").apply {
                primitiveAdapter.bindToStmt(stmtName, indexVarName, valueVarName, scope)
            }
            endControlFlow()
        }
    }

    override fun readFromCursor(outVarName: String, cursorVarName: String, indexVarName: String,
                                scope: CodeGenScope) {
        scope.builder().apply {
            beginControlFlow("if ($L.isNull($L))", cursorVarName, indexVarName).apply {
                addStatement("$L = null", outVarName)
            }
            nextControlFlow("else").apply {
                primitiveAdapter.readFromCursor(outVarName, cursorVarName, indexVarName, scope)
            }
            endControlFlow()
        }
    }
}
