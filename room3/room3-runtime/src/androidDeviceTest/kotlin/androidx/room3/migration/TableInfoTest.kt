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
package androidx.room3.migration

import androidx.kruth.assertThat
import androidx.room3.util.TableInfo
import androidx.room3.util.TableInfo.Column.Companion.defaultValueEquals
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TableInfoTest {
    @Test
    fun readSimple() = runTest {
        openDatabase("CREATE TABLE foo (id INTEGER PRIMARY KEY AUTOINCREMENT," + "name TEXT)")
            .use { connection ->
                val dbInfo = TableInfo.read(connection, "foo")
                val expectedInfo =
                    TableInfo(
                        "foo",
                        arrayOf(
                                TableInfo.Column(
                                    "id",
                                    "INTEGER",
                                    false,
                                    1,
                                    null,
                                    TableInfo.CREATED_FROM_ENTITY,
                                ),
                                TableInfo.Column(
                                    "name",
                                    "TEXT",
                                    false,
                                    0,
                                    null,
                                    TableInfo.CREATED_FROM_ENTITY,
                                ),
                            )
                            .associateBy { it.name },
                        emptySet(),
                    )
                assertThat(dbInfo).isEqualTo(expectedInfo)
            }
    }

    @Test
    fun readSimple_toStringCheck() = runTest {
        openDatabase("CREATE TABLE foo (id INTEGER PRIMARY KEY AUTOINCREMENT," + "name TEXT)")
            .use { connection ->
                val dbInfo = TableInfo.read(connection, "foo")
                assertThat(dbInfo.toString())
                    .contains(
                        """
                        TableInfo {
                            name = 'foo',
                            columns = {    
                                Column {
                                   name = 'id',
                                   type = 'INTEGER',
                                   affinity = '3',
                                   notNull = 'false',
                                   primaryKeyPosition = '1',
                                   defaultValue = 'undefined'
                                },
                                Column {
                                   name = 'name',
                                   type = 'TEXT',
                                   affinity = '2',
                                   notNull = 'false',
                                   primaryKeyPosition = '0',
                                   defaultValue = 'undefined'
                                }
                            },
                            foreignKeys = { }
                            indices = { }
                        }
                        """
                            .trimIndent()
                    )
            }
    }

    @Test
    fun multiplePrimaryKeys() = runTest {
        openDatabase("CREATE TABLE foo (id INTEGER," + "name TEXT, PRIMARY KEY(name, id))").use {
            connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val expectedInfo =
                TableInfo(
                    "foo",
                    arrayOf(
                            TableInfo.Column(
                                "id",
                                "INTEGER",
                                false,
                                2,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            ),
                            TableInfo.Column(
                                "name",
                                "TEXT",
                                false,
                                1,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            ),
                        )
                        .associateBy { it.name },
                    emptySet(),
                )
            assertThat(dbInfo).isEqualTo(expectedInfo)
        }
    }

    @Test
    fun alteredTable() = runTest {
        openDatabase("CREATE TABLE foo (id INTEGER," + "name TEXT, PRIMARY KEY(name))").use {
            connection ->
            connection.execSQL("ALTER TABLE foo ADD COLUMN added REAL;")
            val dbInfo = TableInfo.read(connection, "foo")
            val expectedInfo =
                TableInfo(
                    "foo",
                    arrayOf(
                            TableInfo.Column(
                                "id",
                                "INTEGER",
                                false,
                                0,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            ),
                            TableInfo.Column(
                                "name",
                                "TEXT",
                                false,
                                1,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            ),
                            TableInfo.Column(
                                "added",
                                "REAL",
                                false,
                                0,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            ),
                        )
                        .associateBy { it.name },
                    emptySet(),
                )
            assertThat(dbInfo).isEqualTo(expectedInfo)
        }
    }

    @Test
    fun nonNull() = runTest {
        openDatabase("CREATE TABLE foo (name TEXT NOT NULL)").use { connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val expectedInfo =
                TableInfo(
                    "foo",
                    arrayOf(
                            TableInfo.Column(
                                "name",
                                "TEXT",
                                true,
                                0,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            )
                        )
                        .associateBy { it.name },
                    emptySet(),
                )
            assertThat(dbInfo).isEqualTo(expectedInfo)
        }
    }

    @Test
    fun defaultValue() = runTest {
        openDatabase("CREATE TABLE foo (name TEXT DEFAULT blah)").use { connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val expectedInfo =
                TableInfo(
                    "foo",
                    arrayOf(
                            TableInfo.Column(
                                "name",
                                "TEXT",
                                false,
                                0,
                                "blah",
                                TableInfo.CREATED_FROM_ENTITY,
                            )
                        )
                        .associateBy { it.name },
                    emptySet(),
                )
            assertThat(dbInfo).isEqualTo(expectedInfo)
        }
    }

    @Test
    fun defaultValue_missing() = runTest {
        openDatabase("CREATE TABLE foo (name TEXT)").use { connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val expectedInfo =
                TableInfo(
                    "foo",
                    arrayOf(
                            TableInfo.Column(
                                "name",
                                "TEXT",
                                false,
                                0,
                                "blah",
                                TableInfo.CREATED_FROM_ENTITY,
                            )
                        )
                        .associateBy { it.name },
                    emptySet(),
                )
            assertThat(dbInfo).isNotEqualTo(expectedInfo)
        }
    }

    @Test
    fun defaultValue_missing_should_print_undefined() = runTest {
        openDatabase("CREATE TABLE foo (name TEXT)").use { connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val columnInfo = dbInfo.columns["name"]
            assertThat(columnInfo.toString().trimIndent())
                .isEqualTo(
                    """
                    Column {
                       name = 'name',
                       type = 'TEXT',
                       affinity = '2',
                       notNull = 'false',
                       primaryKeyPosition = '0',
                       defaultValue = 'undefined'
                    }
                    """
                        .trimIndent()
                )
        }
    }

    @Test
    fun defaultValue_null_should_print_null() = runTest {
        openDatabase("CREATE TABLE foo (name TEXT DEFAULT null)").use { connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val columnInfo = dbInfo.columns["name"]
            assertThat(columnInfo.toString().trimIndent())
                .isEqualTo(
                    """
                    Column {
                       name = 'name',
                       type = 'TEXT',
                       affinity = '2',
                       notNull = 'false',
                       primaryKeyPosition = '0',
                       defaultValue = 'null'
                    }
                    """
                        .trimIndent()
                )
        }
    }

    @Test
    fun defaultValue_unaccounted() = runTest {
        openDatabase("CREATE TABLE foo (name TEXT DEFAULT blah)").use { connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val expectedInfo =
                TableInfo(
                    "foo",
                    arrayOf(
                            TableInfo.Column(
                                "name",
                                "TEXT",
                                false,
                                0,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            )
                        )
                        .associateBy { it.name },
                    emptySet(),
                )
            assertThat(dbInfo).isEqualTo(expectedInfo)
        }
    }

    @Test
    fun columnInfo_defaultValue_equality() {
        var column1: TableInfo.Column
        var column2: TableInfo.Column
        column1 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY)
        column2 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY)
        assertThat(column1).isEqualTo(column2)
        column1 = TableInfo.Column("name", "TEXT", false, 0, "", TableInfo.CREATED_FROM_ENTITY)
        column2 =
            TableInfo.Column("name", "TEXT", false, 0, "different", TableInfo.CREATED_FROM_ENTITY)
        assertThat(column1).isNotEqualTo(column2)
        column1 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY)
        column2 =
            TableInfo.Column("name", "TEXT", false, 0, "different", TableInfo.CREATED_FROM_ENTITY)
        assertThat(column1).isNotEqualTo(column2)
        column1 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_DATABASE)
        column2 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_DATABASE)
        assertThat(column1).isEqualTo(column2)
        column1 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_DATABASE)
        column2 =
            TableInfo.Column("name", "TEXT", false, 0, "different", TableInfo.CREATED_FROM_DATABASE)
        assertThat(column1).isNotEqualTo(column2)
        column1 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_UNKNOWN)
        column2 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_DATABASE)
        assertThat(column1).isEqualTo(column2)
        column1 = TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_UNKNOWN)
        column2 =
            TableInfo.Column("name", "TEXT", false, 0, "different", TableInfo.CREATED_FROM_DATABASE)
        assertThat(column1).isEqualTo(column2)
    }

    @Test
    fun foreignKey() = runTest {
        openDatabase(
                "CREATE TABLE foo (name TEXT)",
                "CREATE TABLE bar(barName TEXT, FOREIGN KEY(barName) REFERENCES foo(name))",
            )
            .use { connection ->
                val dbInfo = TableInfo.read(connection, "bar")
                assertThat(dbInfo.foreignKeys).hasSize(1)
                val foreignKey = dbInfo.foreignKeys.iterator().next()
                assertThat(foreignKey.columnNames).containsExactly("barName")
                assertThat(foreignKey.referenceColumnNames).containsExactly("name")
                assertThat(foreignKey.onDelete).isEqualTo("NO ACTION")
                assertThat(foreignKey.onUpdate).isEqualTo("NO ACTION")
                assertThat(foreignKey.referenceTable).isEqualTo("foo")
            }
    }

    @Test
    fun multipleForeignKeys() = runTest {
        openDatabase(
                "CREATE TABLE foo (name TEXT, lastName TEXT)",
                "CREATE TABLE foo2 (name TEXT, lastName TEXT)",
                "CREATE TABLE bar(barName TEXT, barLastName TEXT, " +
                    " FOREIGN KEY(barName) REFERENCES foo(name) ON UPDATE SET NULL," +
                    " FOREIGN KEY(barLastName) REFERENCES foo2(lastName) ON DELETE CASCADE)",
            )
            .use { connection ->
                val dbInfo = TableInfo.read(connection, "bar")
                assertThat(dbInfo.foreignKeys).hasSize(2)
                val expected =
                    setOf(
                        TableInfo.ForeignKey(
                            "foo2", // table
                            "CASCADE", // on delete
                            "NO ACTION", // on update
                            listOf("barLastName"), // my
                            listOf("lastName"),
                        ), // ref
                        TableInfo.ForeignKey(
                            "foo", // table
                            "NO ACTION", // on delete
                            "SET NULL", // on update
                            listOf("barName"), // mine
                            listOf("name"),
                        ),
                    )
                assertThat(dbInfo.foreignKeys).isEqualTo(expected)
            }
    }

    @Test
    fun compositeForeignKey() = runTest {
        openDatabase(
                "CREATE TABLE foo (name TEXT, lastName TEXT)",
                "CREATE TABLE bar(barName TEXT, barLastName TEXT, " +
                    " FOREIGN KEY(barName, barLastName) REFERENCES foo(name, lastName)" +
                    " ON UPDATE cascade ON DELETE RESTRICT)",
            )
            .use { connection ->
                val dbInfo = TableInfo.read(connection, "bar")
                assertThat(dbInfo.foreignKeys).hasSize(1)
                val expected =
                    TableInfo.ForeignKey(
                        "foo", // table
                        "RESTRICT", // on delete
                        "CASCADE", // on update
                        listOf("barName", "barLastName"), // my columns
                        listOf("name", "lastName"),
                    ) // ref columns
                assertThat(dbInfo.foreignKeys.iterator().next()).isEqualTo(expected)
            }
    }

    @Test
    fun caseInsensitiveTypeName() = runTest {
        openDatabase("CREATE TABLE foo (n integer)").use { connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            val expectedInfo =
                TableInfo(
                    "foo",
                    arrayOf(
                            TableInfo.Column(
                                "n",
                                "INTEGER",
                                false,
                                0,
                                null,
                                TableInfo.CREATED_FROM_ENTITY,
                            )
                        )
                        .associateBy { it.name },
                    emptySet(),
                )
            assertThat(dbInfo).isEqualTo(expectedInfo)
        }
    }

    @Test
    fun readIndices() = runTest {
        openDatabase(
                "CREATE TABLE foo (n INTEGER, indexed TEXT, unique_indexed TEXT," +
                    "a INTEGER, b INTEGER);",
                "CREATE INDEX foo_indexed ON foo(indexed);",
                "CREATE UNIQUE INDEX foo_unique_indexed ON foo(unique_indexed COLLATE NOCASE" +
                    " DESC);",
                "CREATE INDEX " +
                    TableInfo.Index.DEFAULT_PREFIX +
                    "foo_composite_indexed" +
                    " ON foo(a, b);",
            )
            .use { connection ->
                val dbInfo = TableInfo.read(connection, "foo")
                val expectedInfo =
                    TableInfo(
                        "foo",
                        arrayOf(
                                TableInfo.Column(
                                    "n",
                                    "INTEGER",
                                    false,
                                    0,
                                    null,
                                    TableInfo.CREATED_FROM_ENTITY,
                                ),
                                TableInfo.Column(
                                    "indexed",
                                    "TEXT",
                                    false,
                                    0,
                                    null,
                                    TableInfo.CREATED_FROM_ENTITY,
                                ),
                                TableInfo.Column(
                                    "unique_indexed",
                                    "TEXT",
                                    false,
                                    0,
                                    null,
                                    TableInfo.CREATED_FROM_ENTITY,
                                ),
                                TableInfo.Column(
                                    "a",
                                    "INTEGER",
                                    false,
                                    0,
                                    null,
                                    TableInfo.CREATED_FROM_ENTITY,
                                ),
                                TableInfo.Column(
                                    "b",
                                    "INTEGER",
                                    false,
                                    0,
                                    null,
                                    TableInfo.CREATED_FROM_ENTITY,
                                ),
                            )
                            .associateBy { it.name },
                        emptySet(),
                        setOf(
                            TableInfo.Index(
                                "index_foo_blahblah",
                                false,
                                listOf("a", "b"),
                                emptyList(),
                            ),
                            TableInfo.Index(
                                "foo_unique_indexed",
                                true,
                                listOf("unique_indexed"),
                                listOf("DESC"),
                            ),
                            TableInfo.Index("foo_indexed", false, listOf("indexed"), listOf("ASC")),
                        ),
                    )
                assertThat(dbInfo).isEqualTo(expectedInfo)
            }
    }

    @Test
    fun compatColumnTypes() = runTest {
        // see:https://www.sqlite.org/datatype3.html 3.1
        val testCases =
            listOf(
                "TINYINT" to "integer",
                "VARCHAR" to "text",
                "DOUBLE" to "real",
                "BOOLEAN" to "numeric",
                "FLOATING POINT" to "integer",
            )
        for ((first, second) in testCases) {
            openDatabase(
                    "CREATE TABLE foo (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name " +
                        first +
                        ")"
                )
                .use { connection ->
                    val info = TableInfo.read(connection, "foo")
                    assertThat(info)
                        .isEqualTo(
                            TableInfo(
                                "foo",
                                arrayOf(
                                        TableInfo.Column(
                                            "id",
                                            "INTEGER",
                                            false,
                                            1,
                                            null,
                                            TableInfo.CREATED_FROM_ENTITY,
                                        ),
                                        TableInfo.Column(
                                            "name",
                                            second,
                                            false,
                                            0,
                                            null,
                                            TableInfo.CREATED_FROM_ENTITY,
                                        ),
                                    )
                                    .associateBy { it.name },
                                emptySet(),
                            )
                        )
                }
        }
    }

    @Test
    fun testSurroundingParenthesis() = runTest {
        openDatabase("CREATE TABLE foo (name INTEGER NOT NULL DEFAULT ((0) + (1 + 2)))").use {
            connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            assertThat("((0) + (1 + 2))").isNotEqualTo(dbInfo.columns["name"]!!.defaultValue)
            assertThat(defaultValueEquals("((0) + (1 + 2))", dbInfo.columns["name"]!!.defaultValue))
                .isTrue()
        }
    }

    @Test
    fun testDoubleSurroundingParenthesis() = runTest {
        openDatabase("CREATE TABLE foo (name INTEGER NOT NULL DEFAULT (((0) + (1 + 2))))").use {
            connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            assertThat("(((0) + (1 + 2)))").isNotEqualTo(dbInfo.columns["name"]!!.defaultValue)
            assertThat(
                    defaultValueEquals("(((0) + (1 + 2)))", dbInfo.columns["name"]!!.defaultValue)
                )
                .isTrue()
        }
    }

    @Test
    fun testMultipleParenthesisWithSurrounding() = runTest {
        openDatabase(
                "CREATE TABLE foo (name INTEGER NOT NULL DEFAULT (((3 + 5) + (2 + 1)) + (1 + 2)))"
            )
            .use { connection ->
                val dbInfo = TableInfo.read(connection, "foo")
                assertThat("(((3 + 5) + (2 + 1)) + (1 + 2))")
                    .isNotEqualTo(dbInfo.columns["name"]!!.defaultValue)
                assertThat(
                        defaultValueEquals(
                            "(((3 + 5) + (2 + 1)) + (1 + 2))",
                            dbInfo.columns["name"]!!.defaultValue,
                        )
                    )
                    .isTrue()
            }
    }

    @Test
    fun testSurroundingParenthesisWithSpacesBefore() = runTest {
        openDatabase("CREATE TABLE foo (name INTEGER NOT NULL DEFAULT (    (0) + (1 + 2)))").use {
            connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            assertThat("(    (0) + (1 + 2))").isNotEqualTo(dbInfo.columns["name"]!!.defaultValue)
            assertThat(
                    defaultValueEquals("(    (0) + (1 + 2))", dbInfo.columns["name"]!!.defaultValue)
                )
                .isTrue()
        }
    }

    @Test
    fun testSurroundingParenthesisWithSpacesAfter() = runTest {
        openDatabase("CREATE TABLE foo (name INTEGER NOT NULL DEFAULT ((0) + (1 + 2)    ))").use {
            connection ->
            val dbInfo = TableInfo.read(connection, "foo")
            assertThat("((0) + (1 + 2)    )").isNotEqualTo(dbInfo.columns["name"]!!.defaultValue)
            assertThat(
                    defaultValueEquals("((0) + (1 + 2)    )", dbInfo.columns["name"]!!.defaultValue)
                )
                .isTrue()
        }
    }

    private fun openDatabase(vararg createQueries: String) =
        AndroidSQLiteDriver().open(":memory:").also { connection ->
            createQueries.forEach { connection.execSQL(it) }
        }
}
