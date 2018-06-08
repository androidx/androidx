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

package androidx.room.vo

import androidx.room.migration.bundle.BundleUtil
import androidx.room.migration.bundle.IndexBundle

/**
 * Represents a processed index.
 */
data class Index(val name: String, val unique: Boolean, val fields: List<Field>) :
        HasSchemaIdentity {
    companion object {
        // should match the value in TableInfo.Index.DEFAULT_PREFIX
        const val DEFAULT_PREFIX = "index_"
    }

    override fun getIdKey(): String {
        return "$unique-$name-${fields.joinToString(",") { it.columnName }}"
    }

    fun createQuery(tableName: String): String {
        val uniqueSQL = if (unique) {
            "UNIQUE"
        } else {
            ""
        }
        return """
            CREATE $uniqueSQL INDEX `$name`
            ON `$tableName` (${fields.map { it.columnName }.joinToString(", ") { "`$it`" }})
            """.trimIndent().replace("\n", " ")
    }

    val columnNames by lazy { fields.map { it.columnName } }

    fun toBundle(): IndexBundle = IndexBundle(name, unique, fields.map { it.columnName },
            createQuery(BundleUtil.TABLE_NAME_PLACEHOLDER))
}
