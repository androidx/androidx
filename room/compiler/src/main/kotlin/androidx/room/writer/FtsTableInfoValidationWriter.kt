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

package androidx.room.writer

import androidx.room.ext.CommonTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.vo.FtsEntity
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import stripNonJava

class FtsTableInfoValidationWriter(val entity: FtsEntity) : ValidationWriter() {
    override fun write(dbParam: ParameterSpec, scope: CountingCodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize()
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder().apply {
            val columnListVar = scope.getTmpVar("_columns$suffix")
            val columnListType = ParameterizedTypeName.get(HashSet::class.typeName(),
                    CommonTypeNames.STRING)

            addStatement("final $T $L = new $T($L)", columnListType, columnListVar,
                    columnListType, entity.fields.size)
            entity.nonHiddenFields.forEach {
                addStatement("$L.add($S)", columnListVar, it.columnName)
            }

            addStatement("final $T $L = new $T($S, $L, $S)",
                    RoomTypeNames.FTS_TABLE_INFO, expectedInfoVar, RoomTypeNames.FTS_TABLE_INFO,
                    entity.tableName, columnListVar, entity.createTableQuery)

            val existingVar = scope.getTmpVar("_existing$suffix")
            addStatement("final $T $L = $T.read($N, $S)",
                    RoomTypeNames.FTS_TABLE_INFO, existingVar, RoomTypeNames.FTS_TABLE_INFO,
                    dbParam, entity.tableName)

            beginControlFlow("if (!$L.equals($L))", expectedInfoVar, existingVar).apply {
                addStatement("return new $T(false, $S + $L + $S + $L)",
                        RoomTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                        "${entity.tableName}(${entity.element.qualifiedName}).\n Expected:\n",
                        expectedInfoVar, "\n Found:\n", existingVar)
            }
            endControlFlow()
        }
    }
}