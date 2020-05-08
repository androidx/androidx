/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection.test

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.inspection.test.MessageFactory.createGetSchemaCommand
import androidx.sqlite.inspection.test.MessageFactory.createTrackDatabasesCommand
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class GetSchemaTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @get:Rule
    val temporaryFolder = TemporaryFolder(getInstrumentation().context.cacheDir)

    @Test
    fun test_get_schema_complex_tables() {
        test_get_schema(
            listOf(
                Database(
                    "db1",
                    Table(
                        "table1",
                        Column("t", "TEXT"),
                        Column("nu", "NUMERIC"),
                        Column("i", "INTEGER"),
                        Column("r", "REAL"),
                        Column("b", "BLOB")
                    ),
                    Table(
                        "table2",
                        Column("id", "INTEGER"),
                        Column("name", "TEXT")

                    ),
                    Table(
                        "table3a",
                        Column("c1", "INT"),
                        Column("c2", "INT", primaryKey = 1)
                    ),
                    Table(
                        "table3b", // compound-primary-key
                        Column("c1", "INT", primaryKey = 2),
                        Column("c2", "INT", primaryKey = 1)
                    ),
                    Table(
                        "table4", // compound-primary-key, two unique columns
                        Column("c1", "INT", primaryKey = 1),
                        Column("c2", "INT", primaryKey = 2, isUnique = true),
                        Column("c3", "INT", isUnique = true)
                    ),
                    Table(
                        "table5", // mix: unique, primary key, notNull
                        Column("c1", "INT", isNotNull = true),
                        Column("c2", "INT", primaryKey = 1, isUnique = true),
                        Column("c3", "INT", isUnique = true, isNotNull = true)
                    ),
                    Table(
                        "table6", // compound-unique-constraint-indices in [onDatabaseCreated]
                        Column("c1", "INT"),
                        Column("c2uuu", "INT", isUnique = true),
                        Column("c3", "INT"),
                        Column("c4u", "INT", isUnique = true)
                    )
                )
            ),
            onDatabaseCreated = { db ->
                // compound-unique-constraint-indices
                listOf(
                    "create index index6_12 on 'table6' ('c1', 'c2uuu')",
                    "create index index6_23 on 'table6' ('c2uuu', 'c3')"
                ).forEach { query ->
                    db.execSQL(query, emptyArray())
                }

                // sanity check: verifies if the above index adding operations succeeded
                val indexCountTable6 =
                    db.rawQuery("select count(*) from pragma_index_list('table6')", null).let {
                        it.moveToNext()
                        val count = it.getString(0)
                        it.close()
                        count
                    }

                assertThat(indexCountTable6).isEqualTo("4")
            }
        )
    }

    @Test
    fun test_get_schema_multiple_databases() {
        test_get_schema(
            listOf(
                Database("db3", Table("t3", Column("c3", "BLOB"))),
                Database("db2", Table("t2", Column("c2", "TEXT"))),
                Database("db1", Table("t1", Column("c1", "TEXT")))
            )
        )
    }

    @Test
    fun test_get_schema_views() {
        val c1 = Column("c1", "INT")
        val c2 = Column("c2", "INT")
        val c3 = Column("c3", "INT")
        test_get_schema(
            listOf(
                Database(
                    "db1",
                    Table("t1", c1, c2),
                    Table("t2", c1, c2, c3),
                    Table(
                        "v1", listOf(c1, c2), isView = true,
                        viewQuery = "select t1.c1, t2.c2 from t1 inner join t2 on t1.c1 = t2.c2"
                    )
                )
            )
        )
    }

    @Test
    fun test_get_schema_auto_increment() = runBlocking {
        val databaseId = testEnvironment.inspectDatabase(
            Database("db1").createInstance(temporaryFolder).also {
                it.execSQL("CREATE TABLE t1 (c2 INTEGER PRIMARY KEY AUTOINCREMENT)")
                it.execSQL("INSERT INTO t1 VALUES(3)")
            })
        testEnvironment.sendCommand(createGetSchemaCommand(databaseId)).let { response ->
            val tableNames = response.getSchema.tablesList.map { it.name }
            assertThat(tableNames).isEqualTo(listOf("t1"))
        }
    }

    @Test
    fun test_get_schema_wrong_database_id() = runBlocking {
        val databaseId = 123456789
        testEnvironment.sendCommand(createGetSchemaCommand(databaseId)).let { response ->
            assertThat(response.hasErrorOccurred()).isEqualTo(true)
            val error = response.errorOccurred.content
            assertThat(error.message).contains(
                "Unable to perform an operation on database (id=$databaseId).")
            assertThat(error.message).contains("The database may have already been closed.")
            assertThat(error.recoverability.isRecoverable).isEqualTo(true)
        }
    }

    private fun test_get_schema(
        alreadyOpenDatabases: List<Database>,
        onDatabaseCreated: (SQLiteDatabase) -> Unit = {}
    ) =
        runBlocking {
        assertThat(alreadyOpenDatabases).isNotEmpty() // sanity check

        testEnvironment.registerAlreadyOpenDatabases(alreadyOpenDatabases.map {
            it.createInstance(temporaryFolder).also { db -> onDatabaseCreated(db) }
        })
        testEnvironment.sendCommand(createTrackDatabasesCommand())
        val databaseConnections =
            alreadyOpenDatabases.indices.map { testEnvironment.receiveEvent().databaseOpened }

        val schemas =
            databaseConnections
                .sortedBy { it.path }
                .map {
                    testEnvironment.sendCommand(createGetSchemaCommand(it.databaseId)).getSchema
                }

        alreadyOpenDatabases
            .sortedBy { it.name }
            .zipSameSize(schemas)
            .forEach { (expectedSchema, actualSchema) ->
                val expectedTables = expectedSchema.tables.sortedBy { it.name }
                val actualTables = actualSchema.tablesList.sortedBy { it.name }

                expectedTables
                    .zipSameSize(actualTables)
                    .forEach { (expectedTable, actualTable) ->
                        assertThat(actualTable.name).isEqualTo(expectedTable.name)
                        assertThat(actualTable.isView).isEqualTo(expectedTable.isView)

                        val expectedColumns = expectedTable.columns.sortedBy { it.name }
                        val actualColumns = actualTable.columnsList.sortedBy { it.name }

                        expectedColumns
                            .adjustForSinglePrimaryKey()
                            .zipSameSize(actualColumns)
                            .forEach { (expectedColumn, actualColumnProto) ->
                                val actualColumn = Column(
                                    name = actualColumnProto.name,
                                    type = actualColumnProto.type,
                                    primaryKey = actualColumnProto.primaryKey,
                                    isNotNull = actualColumnProto.isNotNull,
                                    isUnique = actualColumnProto.isUnique
                                )
                                assertThat(actualColumn).isEqualTo(expectedColumn)
                            }
                    }
            }
    }

    // The sole primary key in a table is by definition unique
    private fun List<Column>.adjustForSinglePrimaryKey(): List<Column> =
        if (this.count { it.isPrimaryKey } > 1) this
        else this.map {
            if (it.isPrimaryKey) it.copy(isUnique = true)
            else it
        }

    /** Same as [List.zip] but ensures both lists are the same size. */
    private fun <A, B> List<A>.zipSameSize(other: List<B>): List<Pair<A, B>> {
        assertThat(this.size).isEqualTo(other.size)
        return this.zip(other)
    }
}
