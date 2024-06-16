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

import androidx.kruth.assertThat
import kotlin.test.Test

class DatabaseBundleTest {

    @Test
    fun buildCreateQueries_noFts() {
        val entity1 =
            EntityBundle(
                tableName = "e1",
                createSql = "sq1",
                fields = listOf(createFieldBundle("foo1"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo1")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )
        val entity2 =
            EntityBundle(
                tableName = "e2",
                createSql = "sq2",
                fields = listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo2")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )
        val bundle =
            DatabaseBundle(
                version = 1,
                identityHash = "hash",
                entities = listOf(entity1, entity2),
                views = emptyList(),
                setupQueries = emptyList()
            )

        assertThat(bundle.buildCreateQueries()).containsExactly("sq1", "sq2")
    }

    @Test
    fun buildCreateQueries_withFts() {
        val entity1 =
            EntityBundle(
                tableName = "e1",
                createSql = "sq1",
                fields = listOf(createFieldBundle("foo1"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo1")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )
        val entity2 =
            FtsEntityBundle(
                tableName = "e2",
                createSql = "sq2",
                fields = listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo2")),
                ftsVersion = "FTS4",
                ftsOptions = createFtsOptionsBundle(""),
                contentSyncSqlTriggers = emptyList()
            )
        val entity3 =
            EntityBundle(
                tableName = "e3",
                createSql = "sq3",
                fields = listOf(createFieldBundle("foo3"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo3")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )
        val bundle =
            DatabaseBundle(
                version = 1,
                identityHash = "hash",
                entities = listOf(entity1, entity2, entity3),
                views = emptyList(),
                setupQueries = emptyList()
            )

        assertThat(bundle.buildCreateQueries()).containsExactly("sq1", "sq2", "sq3")
    }

    @Test
    fun buildCreateQueries_withExternalContentFts() {
        val entity1 =
            EntityBundle(
                tableName = "e1",
                createSql = "sq1",
                fields = listOf(createFieldBundle("foo1"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo1")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )
        val entity2 =
            FtsEntityBundle(
                tableName = "e2",
                createSql = "sq2",
                fields = listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo2")),
                ftsVersion = "FTS4",
                ftsOptions = createFtsOptionsBundle("e3"),
                contentSyncSqlTriggers = listOf("e2_trig")
            )
        val entity3 =
            EntityBundle(
                tableName = "e3",
                createSql = "sq3",
                fields = listOf(createFieldBundle("foo3"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo3")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )
        val bundle =
            DatabaseBundle(
                version = 1,
                identityHash = "hash",
                entities = listOf(entity1, entity2, entity3),
                views = emptyList(),
                setupQueries = emptyList()
            )

        assertThat(bundle.buildCreateQueries()).containsExactly("sq1", "sq3", "sq2", "e2_trig")
    }

    @Test
    fun schemaEquality_missingView_notEqual() {
        val entity =
            EntityBundle(
                tableName = "e",
                createSql = "sq",
                fields = listOf(createFieldBundle("foo"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )
        val view = DatabaseViewBundle(viewName = "bar", createSql = "sq")
        val bundle1 =
            DatabaseBundle(
                version = 1,
                identityHash = "hash",
                entities = listOf(entity),
                views = emptyList(),
                setupQueries = emptyList()
            )
        val bundle2 =
            DatabaseBundle(
                version = 1,
                identityHash = "hash",
                entities = listOf(entity),
                views = listOf(view),
                setupQueries = emptyList()
            )
        assertThat(bundle1.isSchemaEqual(bundle2)).isFalse()
    }

    private fun createFieldBundle(name: String): FieldBundle {
        return FieldBundle(
            fieldPath = "foo",
            columnName = name,
            affinity = "text",
            isNonNull = false,
            defaultValue = null
        )
    }

    private fun createFtsOptionsBundle(contentTableName: String): FtsOptionsBundle {
        return FtsOptionsBundle(
            tokenizer = "",
            tokenizerArgs = emptyList(),
            contentTable = contentTableName,
            languageIdColumnName = "",
            matchInfo = "",
            notIndexedColumns = emptyList(),
            prefixSizes = emptyList(),
            preferredOrder = ""
        )
    }
}
