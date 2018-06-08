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

package androidx.room.vo

import androidx.room.RoomMasterTable
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.SchemaBundle
import com.squareup.javapoet.ClassName
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

/**
 * Holds information about a class annotated with Database.
 */
data class Database(val element: TypeElement,
                    val type: TypeMirror,
                    val entities: List<Entity>,
                    val daoMethods: List<DaoMethod>,
                    val version: Int,
                    val exportSchema: Boolean,
                    val enableForeignKeys: Boolean) {
    val typeName: ClassName by lazy { ClassName.get(element) }

    private val implClassName by lazy {
        "${typeName.simpleNames().joinToString("_")}_Impl"
    }

    val implTypeName: ClassName by lazy {
        ClassName.get(typeName.packageName(), implClassName)
    }

    val bundle by lazy {
        DatabaseBundle(version, identityHash, entities.map(Entity::toBundle),
                listOf(RoomMasterTable.CREATE_QUERY,
                        RoomMasterTable.createInsertQuery(identityHash)))
    }

    /**
     * Create a has that identifies this database definition so that at runtime we can check to
     * ensure developer didn't forget to update the version.
     */
    val identityHash: String by lazy {
        val idKey = SchemaIdentityKey()
        idKey.appendSorted(entities)
        idKey.hash()
    }

    val legacyIdentityHash: String by lazy {
        val entityDescriptions = entities
                .sortedBy { it.tableName }
                .map { it.createTableQuery }
        val indexDescriptions = entities
                .flatMap { entity ->
                    entity.indices.map { index ->
                        index.createQuery(entity.tableName)
                    }
                }
        val input = (entityDescriptions + indexDescriptions).joinToString("¯\\_(ツ)_/¯")
        DigestUtils.md5Hex(input)
    }

    fun exportSchema(file: File) {
        val schemaBundle = SchemaBundle(SchemaBundle.LATEST_FORMAT, bundle)
        if (file.exists()) {
            val existing = file.inputStream().use {
                SchemaBundle.deserialize(it)
            }
            if (existing.isSchemaEqual(schemaBundle)) {
                return
            }
        }
        SchemaBundle.serialize(schemaBundle, file)
    }
}
