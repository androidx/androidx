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
data class Index(val name: String, val unique: Boolean, override val fields: Fields) :
    HasSchemaIdentity, HasFields {
    companion object {
        // should match the value in TableInfo.Index.DEFAULT_PREFIX
        const val DEFAULT_PREFIX = "index_"
    }

    constructor(name: String, unique: Boolean, fields: List<Field>) :
        this(name, unique, Fields(fields))

    override fun getIdKey() = "$unique-$name-${columnNames.joinToString(",")}"

    fun createQuery(tableName: String): String {
        val indexSQL = if (unique) {
            "UNIQUE INDEX"
        } else {
            "INDEX"
        }
        return """
            CREATE $indexSQL IF NOT EXISTS `$name`
            ON `$tableName` (${columnNames.joinToString(", ") { "`$it`" }})
        """.trimIndent().replace("\n", " ")
    }

    fun toBundle(): IndexBundle = IndexBundle(
        name, unique, columnNames,
        createQuery(BundleUtil.TABLE_NAME_PLACEHOLDER)
    )
}
