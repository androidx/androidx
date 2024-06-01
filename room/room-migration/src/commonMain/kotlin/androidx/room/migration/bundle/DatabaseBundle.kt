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
import androidx.room.migration.bundle.SchemaEqualityUtil.checkSchemaEquality
import androidx.room.migration.bundle.SchemaEqualityUtil.filterValuesInstance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Data class that holds the schema information for a [androidx.room.Database]. */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DatabaseBundle(
    @SerialName("version") val version: Int,
    @SerialName("identityHash") val identityHash: String,
    @SerialName("entities") val entities: List<BaseEntityBundle>,
    @SerialName("views") val views: List<DatabaseViewBundle> = emptyList(),
    @SerialName("setupQueries") private val setupQueries: List<String>,
) : SchemaEquality<DatabaseBundle> {

    val entitiesByTableName: Map<String, BaseEntityBundle> by lazy {
        entities.associateBy { it.tableName }
    }

    val viewsByName: Map<String, DatabaseViewBundle> by lazy { views.associateBy { it.viewName } }

    /** Builds the list of SQL queries to build this database from scratch. */
    fun buildCreateQueries(): List<String> {
        return buildList {
            entities.sortedWith(FtsEntityCreateComparator()).forEach { entityBundle ->
                addAll(entityBundle.buildCreateQueries())
            }
            views.forEach { viewBundle -> add(viewBundle.createView()) }
            addAll(setupQueries)
        }
    }

    override fun isSchemaEqual(other: DatabaseBundle): Boolean {
        return checkSchemaEquality(
            entitiesByTableName.filterValuesInstance<String, EntityBundle>(),
            other.entitiesByTableName.filterValuesInstance<String, EntityBundle>()
        ) &&
            checkSchemaEquality(
                entitiesByTableName.filterValuesInstance<String, FtsEntityBundle>(),
                other.entitiesByTableName.filterValuesInstance<String, FtsEntityBundle>()
            ) &&
            checkSchemaEquality(viewsByName, other.viewsByName)
    }

    // Comparator to sort FTS entities after their declared external content entity so that the
    // content entity table gets created first.
    private class FtsEntityCreateComparator : Comparator<BaseEntityBundle> {
        override fun compare(a: BaseEntityBundle, b: BaseEntityBundle): Int {
            if (a is FtsEntityBundle) {
                val contentTable = a.ftsOptions.contentTable
                if (contentTable == b.tableName) {
                    return 1
                }
            } else if (b is FtsEntityBundle) {
                val contentTable = b.ftsOptions.contentTable
                if (contentTable == a.tableName) {
                    return -1
                }
            }
            return 0
        }
    }
}
