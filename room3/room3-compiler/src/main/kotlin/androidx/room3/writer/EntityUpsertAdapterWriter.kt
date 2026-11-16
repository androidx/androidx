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

package androidx.room3.writer

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.ext.RoomTypeNames
import androidx.room3.vo.DataClass
import androidx.room3.vo.ShortcutEntity
import androidx.room3.vo.columnNames

class EntityUpsertAdapterWriter
private constructor(val tableName: String, val dataClass: DataClass) {
    companion object {
        fun create(entity: ShortcutEntity): EntityUpsertAdapterWriter {
            return EntityUpsertAdapterWriter(
                tableName = entity.tableName,
                dataClass = entity.dataClass,
            )
        }
    }

    fun createConcrete(entity: ShortcutEntity, typeWriter: TypeWriter): XCodeBlock {
        val upsertAdapter = RoomTypeNames.UPSERT_ADAPTER.parametrizedBy(dataClass.typeName)
        val insertWriter = EntityInsertAdapterWriter.create(entity, "")
        val insertHelper = insertWriter.createAnonymous(typeWriter)
        val updateHelper = EntityUpdateAdapterWriter.create(entity, "").createAnonymous(typeWriter)
        val pkColumnNames = entity.primaryKey.columnNames
        // Upsert is not supported on virtual tables and needs primary key columns to conflict on.
        val upsertQuery =
            if (entity.isVirtualTable || pkColumnNames.isEmpty()) {
                null
            } else {
                val pkCols = pkColumnNames.joinToString(",") { "`$it`" }
                val updateCols =
                    (dataClass.columnNames - pkColumnNames.toSet()).joinToString(",") {
                        "`$it` = excluded.`$it`"
                    }
                val action = if (updateCols.isEmpty()) "NOTHING" else "UPDATE SET $updateCols"
                "${insertWriter.createInsertQuery()} ON CONFLICT ($pkCols) DO $action"
            }
        return XCodeBlock.ofNewInstance(
            typeName = upsertAdapter,
            argsFormat = "%L, %L, %S",
            args = arrayOf(insertHelper, updateHelper, upsertQuery),
        )
    }
}
