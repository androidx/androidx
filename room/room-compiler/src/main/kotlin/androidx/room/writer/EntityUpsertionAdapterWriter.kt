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

import androidx.room.ext.L
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.T
import androidx.room.vo.Pojo
import androidx.room.vo.ShortcutEntity
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName

// TODO b/240736981 need to implement the writer's test
class EntityUpsertionAdapterWriter private constructor(
    val tableName: String,
    val pojo: Pojo
) {
    companion object {
        fun create(entity: ShortcutEntity): EntityUpsertionAdapterWriter {
            return EntityUpsertionAdapterWriter(
                tableName = entity.tableName,
                pojo = entity.pojo
            )
        }
    }

    fun createConcrete(
        entity: ShortcutEntity,
        classWriter: ClassWriter,
        dbParam: String
    ): CodeBlock {
        val upsertionAdapter = ParameterizedTypeName.get(
            RoomTypeNames.UPSERTION_ADAPTER, pojo.typeName
        )
        val insertionHelper = EntityInsertionAdapterWriter.create(entity, "")
            .createAnonymous(classWriter, dbParam)
        val updateHelper = EntityUpdateAdapterWriter.create(entity, "")
            .createAnonymous(classWriter, dbParam)
        return CodeBlock.builder().add("new $T($L, $L)",
            upsertionAdapter, insertionHelper, updateHelper
        ).build()
    }
}