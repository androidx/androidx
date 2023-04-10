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

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(JUnit4::class)
class EntityBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle = EntityBundle("foo", "sq",
                listOf(createFieldBundle("foo"), createFieldBundle("bar")),
            PrimaryKeyBundle(false, listOf("foo")),
        listOf(createIndexBundle("foo")),
        listOf(createForeignKeyBundle("bar", "foo")))

        val other = EntityBundle("foo", "sq",
            listOf(createFieldBundle("foo"), createFieldBundle("bar")),
            PrimaryKeyBundle(false, listOf("foo")),
        listOf(createIndexBundle("foo")),
        listOf(createForeignKeyBundle("bar", "foo")))

        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_reorderedFields_equal() {
        val bundle = EntityBundle("foo", "sq",
            listOf(createFieldBundle("foo"), createFieldBundle("bar")),
            PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
                emptyList())

        val other = EntityBundle("foo", "sq",
            listOf(createFieldBundle("bar"), createFieldBundle("foo")),
            PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
                emptyList())

        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffFields_notEqual() {
        val bundle = EntityBundle("foo", "sq",
            listOf(createFieldBundle("foo"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
                emptyList())

        val other = EntityBundle("foo", "sq",
            listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
            PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
                emptyList())

        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_reorderedForeignKeys_equal() {
        val bundle = EntityBundle("foo", "sq",
                emptyList(),
                PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
            listOf(createForeignKeyBundle("x", "y"),
                        createForeignKeyBundle("bar", "foo")))

        val other = EntityBundle("foo", "sq",
                emptyList(),
                PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
            listOf(createForeignKeyBundle("bar", "foo"),
                        createForeignKeyBundle("x", "y")))
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffForeignKeys_notEqual() {
        val bundle = EntityBundle("foo", "sq",
                emptyList(),
                PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
            listOf(createForeignKeyBundle("bar", "foo")))

        val other = EntityBundle("foo", "sq",
                emptyList(),
            PrimaryKeyBundle(false, listOf("foo")),
                emptyList(),
            listOf(createForeignKeyBundle("bar2", "foo")))

        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_reorderedIndices_equal() {
        val bundle = EntityBundle("foo", "sq",
                emptyList(),
                PrimaryKeyBundle(false, listOf("foo")),
            listOf(createIndexBundle("foo"), createIndexBundle("baz")),
                emptyList())

        val other = EntityBundle("foo", "sq",
                emptyList(),
                PrimaryKeyBundle(false, listOf("foo")),
            listOf(createIndexBundle("baz"), createIndexBundle("foo")),
                emptyList())

        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffIndices_notEqual() {
        val bundle = EntityBundle("foo", "sq",
                emptyList(),
                PrimaryKeyBundle(false, listOf("foo")),
            listOf(createIndexBundle("foo")),
                emptyList())

        val other = EntityBundle("foo", "sq",
                emptyList(),
                PrimaryKeyBundle(false, listOf("foo")),
            listOf(createIndexBundle("foo2")),
                emptyList())

        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    private fun createFieldBundle(name: String): FieldBundle {
        return FieldBundle("foo", name, "text", false, null)
    }

    private fun createIndexBundle(colName: String): IndexBundle {
        return IndexBundle(
            "ind_$colName", false,
            listOf(colName), emptyList(), "create")
    }

    private fun createForeignKeyBundle(targetTable: String, column: String): ForeignKeyBundle {
        return ForeignKeyBundle(targetTable, "CASCADE", "CASCADE",
            listOf(column), listOf(column))
    }
}
