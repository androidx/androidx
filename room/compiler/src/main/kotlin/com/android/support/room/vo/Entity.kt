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

package com.android.support.room.vo

import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

// TODO make data class when move to kotlin 1.1
class Entity(element: TypeElement, val tableName: String, type: DeclaredType,
             fields: List<Field>, decomposedFields: List<DecomposedField>,
             val indices : List<Index>)
    : Pojo(element, type, fields, decomposedFields) {
    val primaryKeys by lazy {
        fields.filter { it.primaryKey }
    }

    val createTableQuery by lazy {
        val definitions = fields.map { it.databaseDefinition } +
                createPrimaryKeyDefinition()
        "CREATE TABLE IF NOT EXISTS `$tableName` (${definitions.joinToString(", ")})"
    }

    val createIndexQueries by lazy {
        indices.map {
            it.createQuery(tableName)
        }
    }

    private fun createPrimaryKeyDefinition(): String {
        val keys = fields
                .filter { it.primaryKey }
                .map { "`${it.columnName}`" }
                .joinToString(", ")
        return "PRIMARY KEY($keys)"
    }
}
