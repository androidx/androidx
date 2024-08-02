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

package androidx.room.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class QueryInterceptorTest {
    private val testCoroutineScope = TestScope()
    private lateinit var database: QueryInterceptorTestDatabase
    private val queryAndArgs = CopyOnWriteArrayList<Pair<String, ArrayList<Any?>>>()

    @Entity(tableName = "queryInterceptorTestDatabase")
    data class QueryInterceptorEntity(@PrimaryKey val id: String, val description: String?)

    @Dao
    interface QueryInterceptorDao {
        @Query("DELETE FROM queryInterceptorTestDatabase WHERE id=:id") fun delete(id: String)

        @Insert fun insert(item: QueryInterceptorEntity)

        @Update fun update(vararg item: QueryInterceptorEntity)
    }

    @Database(version = 1, entities = [QueryInterceptorEntity::class], exportSchema = false)
    abstract class QueryInterceptorTestDatabase : RoomDatabase() {
        abstract fun queryInterceptorDao(): QueryInterceptorDao
    }

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    QueryInterceptorTestDatabase::class.java
                )
                .setQueryCoroutineContext(testCoroutineScope.coroutineContext)
                .setQueryCallback(testCoroutineScope.coroutineContext) { sqlQuery, bindArgs ->
                    val argTrace = ArrayList<Any?>()
                    argTrace.addAll(bindArgs)
                    queryAndArgs.add(Pair(sqlQuery, argTrace))
                }
                .build()
    }

    @After
    fun tearDown() {
        database.close()
        testCoroutineScope.cancel()
    }

    @Test
    fun testInsert() {
        database
            .queryInterceptorDao()
            .insert(QueryInterceptorEntity("Insert", "Inserted a placeholder query"))

        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            listOf("Insert", "Inserted a placeholder query")
        )
        assertTransactionQueries()
    }

    @Test
    fun testDelete() {
        database.queryInterceptorDao().delete("Insert")
        assertQueryLogged("DELETE FROM queryInterceptorTestDatabase WHERE id=?", listOf("Insert"))
        assertTransactionQueries()
    }

    @Test
    fun testUpdate() {
        database
            .queryInterceptorDao()
            .insert(QueryInterceptorEntity("Insert", "Inserted a placeholder query"))
        database
            .queryInterceptorDao()
            .update(QueryInterceptorEntity("Insert", "Updated the placeholder query"))

        assertQueryLogged(
            "UPDATE OR ABORT `queryInterceptorTestDatabase` SET `id` " +
                "= ?,`description` = ? " +
                "WHERE `id` = ?",
            listOf("Insert", "Updated the placeholder query", "Insert")
        )
        assertTransactionQueries()
    }

    @Test
    fun testCompileStatement() {
        assertEquals(queryAndArgs.size, 0)
        database
            .queryInterceptorDao()
            .insert(QueryInterceptorEntity("Insert", "Inserted a placeholder query"))
        database.openHelper.writableDatabase
            .compileStatement("DELETE FROM queryInterceptorTestDatabase WHERE id=?")
            .execute()
        assertQueryLogged("DELETE FROM queryInterceptorTestDatabase WHERE id=?", emptyList())
    }

    @Test
    fun testLoggingSupportSQLiteQuery() {
        database.openHelper.writableDatabase.query(
            SimpleSQLiteQuery(
                "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                    "VALUES (?,?)",
                arrayOf("3", "Description")
            )
        )
        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            listOf("3", "Description")
        )
    }

    @Test
    fun testExecSQLWithBindArgs() {
        database.openHelper.writableDatabase.execSQL(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            arrayOf("3", "Description")
        )
        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            listOf("3", "Description")
        )
    }

    @Test
    fun testNullBindArgument() {
        database.openHelper.writableDatabase.query(
            SimpleSQLiteQuery(
                "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                    "VALUES (?,?)",
                arrayOf("ID", null)
            )
        )
        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`," +
                "`description`) VALUES (?,?)",
            listOf("ID", null)
        )
    }

    @Test
    fun testNullBindArgumentCompileStatement() {
        val sql =
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)"
        val statement = database.openHelper.writableDatabase.compileStatement(sql)
        statement.bindString(1, "ID")
        statement.bindNull(2)
        statement.execute()

        testCoroutineScope.testScheduler.advanceUntilIdle()

        val filteredQueries = queryAndArgs.filter { (query, _) -> query == sql }

        assertThat(filteredQueries).hasSize(1)
        assertThat(filteredQueries[0].second).hasSize(2)
        assertThat(filteredQueries[0].second[0]).isEqualTo("ID")
        assertThat(filteredQueries[0].second[1]).isEqualTo(null)
    }

    @Test
    fun testCallbackCalledOnceAfterCloseAndReOpen() {
        val dbBuilder =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    QueryInterceptorTestDatabase::class.java
                )
                .setQueryCoroutineContext(testCoroutineScope.coroutineContext)
                .setQueryCallback(testCoroutineScope.coroutineContext) { sqlQuery, bindArgs ->
                    val argTrace = ArrayList<Any?>()
                    argTrace.addAll(bindArgs)
                    queryAndArgs.add(Pair(sqlQuery, argTrace))
                }

        dbBuilder.build().close()

        database = dbBuilder.build()

        database
            .queryInterceptorDao()
            .insert(QueryInterceptorEntity("Insert", "Inserted a placeholder query"))

        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            listOf("Insert", "Inserted a placeholder query")
        )
        assertTransactionQueries()
    }

    private fun assertQueryLogged(query: String, expectedArgs: List<String?>) {
        testCoroutineScope.testScheduler.advanceUntilIdle()
        val filteredQueries = queryAndArgs.filter { it.first == query }
        assertThat(filteredQueries).hasSize(1)
        assertThat(expectedArgs).containsExactlyElementsIn(filteredQueries[0].second)
    }

    private fun assertTransactionQueries() {
        testCoroutineScope.testScheduler.advanceUntilIdle()
        val queries = queryAndArgs.map { it.first }
        assertThat(queries).contains("BEGIN IMMEDIATE TRANSACTION")
        assertThat(queries).contains("TRANSACTION SUCCESSFUL")
        assertThat(queries).contains("END TRANSACTION")
    }
}
