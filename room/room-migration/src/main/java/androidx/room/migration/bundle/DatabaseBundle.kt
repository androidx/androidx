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
import com.google.gson.annotations.SerializedName

/**
 * Data class that holds the schema information for a
 * [androidx.room.Database].
 *
 * @constructor Creates a new database
 * @property version Version
 * @property identityHash Identity hash
 * @property entities List of entities
 * @property views List of views
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class DatabaseBundle(
    @field:SerializedName("version")
    public open val version: Int,
    @field:SerializedName("identityHash")
    public open val identityHash: String,
    @field:SerializedName("entities")
    public open val entities: List<EntityBundle>,
    @field:SerializedName("views")
    public open val views: List<DatabaseViewBundle>,
    @field:SerializedName("setupQueries")
    private val setupQueries: List<String>,
) : SchemaEquality<DatabaseBundle> {

    // Used by GSON
    @Deprecated("Marked deprecated to avoid usage in the codebase")
    @SuppressWarnings("unused")
    public constructor() : this(0, "", emptyList(), emptyList(), emptyList())

    @delegate:Transient
    public open val entitiesByTableName: Map<String, EntityBundle> by lazy {
        entities.associateBy { it.tableName }
    }

    @delegate:Transient
    public val viewsByName: Map<String, DatabaseViewBundle> by lazy {
        views.associateBy { it.viewName }
    }

    /**
     * @return List of SQL queries to build this database from scratch.
     */
    public open fun buildCreateQueries(): List<String> {
        return buildList {
            entities.sortedWith(FtsEntityCreateComparator()).forEach { entityBundle ->
                addAll(entityBundle.buildCreateQueries())
            }
            views.forEach { viewBundle ->
                add(viewBundle.createView())
            }
            addAll(setupQueries)
        }
    }

    @Override
    override fun isSchemaEqual(other: DatabaseBundle): Boolean {
        return checkSchemaEquality(entitiesByTableName, other.entitiesByTableName) &&
            checkSchemaEquality(viewsByName, other.viewsByName)
    }

    // Comparator to sort FTS entities after their declared external content entity so that the
    // content entity table gets created first.
    public class FtsEntityCreateComparator : Comparator<EntityBundle> {
        override fun compare(firstEntity: EntityBundle, secondEntity: EntityBundle): Int {
            if (firstEntity is FtsEntityBundle) {
                val contentTable = firstEntity.ftsOptions.contentTable
                if (contentTable == secondEntity.tableName) {
                    return 1
                }
            } else if (secondEntity is FtsEntityBundle) {
                val contentTable = secondEntity.ftsOptions.contentTable
                if (contentTable == firstEntity.tableName) {
                    return -1
                }
            }
            return 0
        }
    }
}
