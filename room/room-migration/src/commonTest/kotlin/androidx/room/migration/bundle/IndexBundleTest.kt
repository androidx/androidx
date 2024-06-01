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

class IndexBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffName_notEqual() {
        val bundle =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = "index3",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffGenericName_equal() {
        val bundle =
            IndexBundle(
                name = IndexBundle.DEFAULT_PREFIX + "x",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = IndexBundle.DEFAULT_PREFIX + "y",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffUnique_notEqual() {
        val bundle =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = "index1",
                isUnique = true,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffColumns_notEqual() {
        val bundle =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col2", "col1"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffSql_equal() {
        val bundle =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql22"
            )
        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffSort_notEqual() {
        val bundle =
            IndexBundle("index1", false, listOf("col1", "col2"), listOf("ASC", "DESC"), "sql")
        val other =
            IndexBundle("index1", false, listOf("col1", "col2"), listOf("DESC", "ASC"), "sql")
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_sortNullVsAllAsc_isEqual() {
        val bundle =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = null,
                createSql = "sql"
            )
        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_sortEmptyVsAllAsc_isEqual() {
        val bundle =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = listOf("ASC", "ASC"),
                createSql = "sql"
            )
        val other =
            IndexBundle(
                name = "index1",
                isUnique = false,
                columnNames = listOf("col1", "col2"),
                orders = emptyList(),
                createSql = "sql"
            )
        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }
}
