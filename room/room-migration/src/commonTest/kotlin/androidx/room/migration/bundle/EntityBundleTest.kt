/*
 * Copyright 2017 The Android Open Source Project
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

class EntityBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = listOf(createFieldBundle("foo"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = listOf(createIndexBundle("foo")),
                foreignKeys = listOf(createForeignKeyBundle("bar", "foo"))
            )

        val other =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = listOf(createFieldBundle("foo"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = listOf(createIndexBundle("foo")),
                foreignKeys = listOf(createForeignKeyBundle("bar", "foo"))
            )

        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_reorderedFields_equal() {
        val bundle =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = listOf(createFieldBundle("foo"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )

        val other =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = listOf(createFieldBundle("bar"), createFieldBundle("foo")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )

        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffFields_notEqual() {
        val bundle =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = listOf(createFieldBundle("foo"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )

        val other =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys = emptyList()
            )

        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_reorderedForeignKeys_equal() {
        val bundle =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys =
                    listOf(createForeignKeyBundle("x", "y"), createForeignKeyBundle("bar", "foo"))
            )

        val other =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys =
                    listOf(createForeignKeyBundle("bar", "foo"), createForeignKeyBundle("x", "y"))
            )

        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffForeignKeys_notEqual() {
        val bundle =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys = listOf(createForeignKeyBundle("bar", "foo"))
            )

        val other =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = emptyList(),
                foreignKeys = listOf(createForeignKeyBundle("bar2", "foo"))
            )

        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_reorderedIndices_equal() {
        val bundle =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = listOf(createIndexBundle("foo"), createIndexBundle("baz")),
                foreignKeys = emptyList()
            )

        val other =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = listOf(createIndexBundle("baz"), createIndexBundle("foo")),
                foreignKeys = emptyList()
            )

        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffIndices_notEqual() {
        val bundle =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = listOf(createIndexBundle("foo")),
                foreignKeys = emptyList()
            )

        val other =
            EntityBundle(
                tableName = "foo",
                createSql = "sq",
                fields = emptyList(),
                primaryKey = PrimaryKeyBundle(false, listOf("foo")),
                indices = listOf(createIndexBundle("foo2")),
                foreignKeys = emptyList()
            )

        assertThat(bundle.isSchemaEqual(other)).isFalse()
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

    private fun createIndexBundle(colName: String): IndexBundle {
        return IndexBundle(
            name = "ind_$colName",
            isUnique = false,
            columnNames = listOf(colName),
            orders = emptyList(),
            createSql = "create"
        )
    }

    private fun createForeignKeyBundle(targetTable: String, column: String): ForeignKeyBundle {
        return ForeignKeyBundle(
            table = targetTable,
            onDelete = "CASCADE",
            onUpdate = "CASCADE",
            columns = listOf(column),
            referencedColumns = listOf(column)
        )
    }
}
