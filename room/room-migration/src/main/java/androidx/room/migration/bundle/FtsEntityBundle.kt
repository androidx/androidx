/*
 * Copyright 2018 The Android Open Source Project
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
 * Data class that holds the schema information about an {@link Fts3 FTS3} or {@link Fts4 FTS4}
 * entity.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FtsEntityBundle(
    tableName: String,
    createSql: String,
    fields: List<FieldBundle>,
    primaryKey: PrimaryKeyBundle,
    @field:SerializedName("ftsVersion")
    public open val ftsVersion: String,
    @field:SerializedName("ftsOptions")
    public open val ftsOptions: FtsOptionsBundle,
    @SerializedName("contentSyncTriggers")
    public open val contentSyncSqlTriggers: List<String>
) : EntityBundle(
    tableName,
    createSql,
    fields,
    primaryKey,
    emptyList(),
    emptyList()
) {
    // Used by GSON
    @Deprecated("Marked deprecated to avoid usage in the codebase")
    @SuppressWarnings("unused")
    private constructor() : this(
        "",
        "",
        emptyList(),
        PrimaryKeyBundle(false, emptyList()),
        "",
        FtsOptionsBundle("", emptyList(), "", "", "", emptyList(), emptyList(), ""),
        emptyList()
    )

    private val SHADOW_TABLE_NAME_SUFFIXES = listOf(
        "_content",
        "_segdir",
        "_segments",
        "_stat",
        "_docsize"
    )

    /**
     * @return Creates the list of SQL queries that are necessary to create this entity.
     */
   override fun buildCreateQueries(): Collection<String> {
        return buildList {
            add(createTable())
            addAll(contentSyncSqlTriggers)
        }
    }

    override fun isSchemaEqual(other: EntityBundle): Boolean {
        val isSuperSchemaEqual = super.isSchemaEqual(other)
        return if (other is FtsEntityBundle) {
            isSuperSchemaEqual && ftsVersion == other.ftsVersion &&
                checkSchemaEquality(ftsOptions, other.ftsOptions)
        } else {
            isSuperSchemaEqual
        }
    }

    /**
     * Gets the list of shadow table names corresponding to the FTS virtual table.
     * @return the list of names.
     */
    public open val shadowTableNames: List<String> by lazy {
        val currentTable = this@FtsEntityBundle.tableName
        buildList {
            SHADOW_TABLE_NAME_SUFFIXES.forEach { suffix ->
                add(currentTable + suffix)
            }
        }
    }
}
