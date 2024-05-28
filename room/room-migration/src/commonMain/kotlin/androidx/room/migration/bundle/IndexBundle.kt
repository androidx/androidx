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

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo
import androidx.room.Index
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Data class that holds the schema information about a table [androidx.room.Index] */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IndexBundle(
    @SerialName("name") val name: String,
    @SerialName("unique") val isUnique: Boolean,
    @SerialName("columnNames") val columnNames: List<String>? = null,
    @SerialName("orders") val orders: List<String>? = null,
    @SerialName("createSql") val createSql: String
) : SchemaEquality<IndexBundle> {
    companion object {
        // should match Index.kt
        const val DEFAULT_PREFIX: String = "index_"
    }

    fun create(tableName: String): String {
        return replaceTableName(createSql, tableName)
    }

    /** Gets the CREATE INDEX SQL query that uses the given table name. */
    fun getCreateSql(tableName: String): String {
        return replaceTableName(createSql, tableName)
    }

    override fun isSchemaEqual(other: IndexBundle): Boolean {
        if (isUnique != other.isUnique) return false
        if (name.startsWith(DEFAULT_PREFIX)) {
            if (!other.name.startsWith(DEFAULT_PREFIX)) {
                return false
            }
        } else if (other.name.startsWith(DEFAULT_PREFIX)) {
            return false
        } else if (name != other.name) {
            return false
        }

        // order matters
        if (columnNames?.let { columnNames != other.columnNames } ?: (other.columnNames != null)) {
            return false
        }

        // order matters and null orders is considered equal to all ASC orders, to be backward
        // compatible with schemas where orders are not present in the schema file
        val columnsSize = columnNames?.size ?: 0
        val orders =
            if (orders.isNullOrEmpty()) {
                List(columnsSize) { Index.Order.ASC.name }
            } else {
                orders
            }
        val otherOrders =
            if (other.orders.isNullOrEmpty()) {
                List(columnsSize) { Index.Order.ASC.name }
            } else {
                other.orders
            }

        if (orders != otherOrders) return false
        return true
    }
}
