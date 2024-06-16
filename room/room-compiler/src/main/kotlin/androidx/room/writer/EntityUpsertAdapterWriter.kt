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

package androidx.room.writer

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.ext.RoomTypeNames
import androidx.room.vo.Pojo
import androidx.room.vo.ShortcutEntity

class EntityUpsertAdapterWriter private constructor(val tableName: String, val pojo: Pojo) {
    companion object {
        fun create(entity: ShortcutEntity): EntityUpsertAdapterWriter {
            return EntityUpsertAdapterWriter(tableName = entity.tableName, pojo = entity.pojo)
        }
    }

    fun createConcrete(
        entity: ShortcutEntity,
        typeWriter: TypeWriter,
        dbProperty: XPropertySpec,
        useDriverApi: Boolean
    ): XCodeBlock {
        val upsertAdapter =
            if (useDriverApi) {
                    RoomTypeNames.UPSERT_ADAPTER
                } else {
                    RoomTypeNames.UPSERT_ADAPTER_COMPAT
                }
                .parametrizedBy(pojo.typeName)
        val insertHelper =
            EntityInsertAdapterWriter.create(entity, "")
                .createAnonymous(typeWriter, dbProperty, useDriverApi)
        val updateHelper =
            EntityUpdateAdapterWriter.create(entity, "")
                .createAnonymous(typeWriter, dbProperty.name, useDriverApi)
        return XCodeBlock.ofNewInstance(
            language = typeWriter.codeLanguage,
            typeName = upsertAdapter,
            argsFormat = "%L, %L",
            args = arrayOf(insertHelper, updateHelper)
        )
    }
}
