/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.persistence.room.writer

import android.arch.persistence.room.ext.AndroidTypeNames
import android.arch.persistence.room.ext.L
import android.arch.persistence.room.ext.N
import android.arch.persistence.room.ext.S
import android.arch.persistence.room.ext.T
import android.arch.persistence.room.solver.CodeGenScope
import android.arch.persistence.room.vo.RelationCollector
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import stripNonJava
import javax.lang.model.element.Modifier

/**
 * Writes the method that fetches the relations of a POJO and assigns them into the given map.
 */
class RelationCollectorMethodWriter(val collector: RelationCollector)
    : ClassWriter.SharedMethodSpec(
        "fetchRelationship${collector.relation.entity.tableName.stripNonJava()}" +
                "As${collector.relation.pojoTypeName.toString().stripNonJava()}") {
    companion object {
        val KEY_SET_VARIABLE = "__mapKeySet"
    }
    override fun getUniqueKey(): String {
        val relation = collector.relation
        return "RelationCollectorMethodWriter" +
                "-${collector.mapTypeName}" +
                "-${relation.entity.typeName}" +
                "-${relation.entityField.columnName}" +
                "-${relation.pojoTypeName}" +
                "-${relation.createLoadAllSql()}"
    }

    override fun prepare(writer: ClassWriter, builder: MethodSpec.Builder) {
        val scope = CodeGenScope(writer)
        val relation = collector.relation

        val param = ParameterSpec.builder(collector.mapTypeName, "_map")
                .addModifiers(Modifier.FINAL)
                .build()
        val sqlQueryVar = scope.getTmpVar("_sql")
        val keySetVar = KEY_SET_VARIABLE

        val cursorVar = "_cursor"
        val itemKeyIndexVar = "_itemKeyIndex"
        val stmtVar = scope.getTmpVar("_stmt")
        scope.builder().apply {

            val keySetType = ParameterizedTypeName.get(
                    ClassName.get(Set::class.java), collector.keyTypeName
            )
            addStatement("final $T $L = $N.keySet()", keySetType, keySetVar, param)
            beginControlFlow("if ($L.isEmpty())", keySetVar).apply {
                addStatement("return")
            }
            endControlFlow()
            collector.queryWriter.prepareReadAndBind(sqlQueryVar, stmtVar, scope)

            addStatement("final $T $L = $N.query($L)", AndroidTypeNames.CURSOR, cursorVar,
                    DaoWriter.dbField, stmtVar)

            beginControlFlow("try").apply {
                addStatement("final $T $L = $L.getColumnIndex($S)",
                        TypeName.INT, itemKeyIndexVar, cursorVar, relation.entityField.columnName)

                beginControlFlow("if ($L == -1)", itemKeyIndexVar).apply {
                    addStatement("return")
                }
                endControlFlow()

                collector.rowAdapter.onCursorReady(cursorVar, scope)
                val tmpVarName = scope.getTmpVar("_item")
                beginControlFlow("while($L.moveToNext())", cursorVar).apply {
                    // read key from the cursor
                    collector.readKey(
                            cursorVarName = cursorVar,
                            indexVar = itemKeyIndexVar,
                            scope = scope
                    ) { keyVar ->
                        val collectionVar = scope.getTmpVar("_tmpCollection")
                        addStatement("$T $L = $N.get($L)", collector.collectionTypeName,
                                collectionVar, param, keyVar)
                        beginControlFlow("if ($L != null)", collectionVar).apply {
                            addStatement("final $T $L", relation.pojoTypeName, tmpVarName)
                            collector.rowAdapter.convert(tmpVarName, cursorVar, scope)
                            addStatement("$L.add($L)", collectionVar, tmpVarName)
                        }
                        endControlFlow()
                    }
                }
                endControlFlow()
                collector.rowAdapter.onCursorFinished()?.invoke(scope)
            }
            nextControlFlow("finally").apply {
                addStatement("$L.close()", cursorVar)
            }
            endControlFlow()
        }
        builder.apply {
            addModifiers(Modifier.PRIVATE)
            addParameter(param)
            returns(TypeName.VOID)
            addCode(scope.builder().build())
        }
    }
}
