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

import androidx.arch.core.executor.testing.CountingTaskExecutorRule
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
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@SmallTest
@RunWith(AndroidJUnit4::class)
class QueryInterceptorTest {
    @Rule
    @JvmField
    val countingTaskExecutorRule = CountingTaskExecutorRule()
    lateinit var mDatabase: QueryInterceptorTestDatabase
    var queryAndArgs = CopyOnWriteArrayList<Pair<String, ArrayList<Any>>>()

    @Entity(tableName = "queryInterceptorTestDatabase")
    data class QueryInterceptorEntity(@PrimaryKey val id: String, val description: String)

    @Dao
    interface QueryInterceptorDao {
        @Query("DELETE FROM queryInterceptorTestDatabase WHERE id=:id")
        fun delete(id: String)

        @Insert
        fun insert(item: QueryInterceptorEntity)

        @Update
        fun update(vararg item: QueryInterceptorEntity)
    }

    @Database(
        version = 1,
        entities = [
            QueryInterceptorEntity::class
        ],
        exportSchema = false
    )
    abstract class QueryInterceptorTestDatabase : RoomDatabase() {
        abstract fun queryInterceptorDao(): QueryInterceptorDao
    }

    @Before
    fun setUp() {
        mDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QueryInterceptorTestDatabase::class.java
        ).setQueryCallback(
            RoomDatabase.QueryCallback { sqlQuery, bindArgs ->
                val argTrace = ArrayList<Any>()
                argTrace.addAll(bindArgs)
                queryAndArgs.add(Pair(sqlQuery, argTrace))
            },
            MoreExecutors.directExecutor()
        ).build()
    }

    @After
    fun tearDown() {
        mDatabase.close()
    }

    @Test
    fun testInsert() {
        mDatabase.queryInterceptorDao().insert(
            QueryInterceptorEntity("Insert", "Inserted a placeholder query")
        )

        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            listOf("Insert", "Inserted a placeholder query")
        )
        assertTransactionQueries()
    }

    @Test
    fun testDelete() {
        mDatabase.queryInterceptorDao().delete("Insert")
        assertQueryLogged(
            "DELETE FROM queryInterceptorTestDatabase WHERE id=?",
            listOf("Insert")
        )
        assertTransactionQueries()
    }

    @Test
    fun testUpdate() {
        mDatabase.queryInterceptorDao().insert(
            QueryInterceptorEntity("Insert", "Inserted a placeholder query")
        )
        mDatabase.queryInterceptorDao().update(
            QueryInterceptorEntity("Insert", "Updated the placeholder query")
        )

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
        mDatabase.queryInterceptorDao().insert(
            QueryInterceptorEntity("Insert", "Inserted a placeholder query")
        )
        mDatabase.openHelper.writableDatabase.compileStatement(
            "DELETE FROM queryInterceptorTestDatabase WHERE id=?"
        ).execute()
        assertQueryLogged("DELETE FROM queryInterceptorTestDatabase WHERE id=?", emptyList())
    }

    @Test
    fun testLoggingSupportSQLiteQuery() {
        mDatabase.openHelper.writableDatabase.query(
            SimpleSQLiteQuery(
                "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                    "VALUES (?,?)",
                arrayOf<Any>("3", "Description")
            )
        )
        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            listOf("3", "Description")
        )
    }

    @Test
    fun testNullBindArgument() {
        mDatabase.openHelper.writableDatabase.query(
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
    fun testCallbackCalledOnceAfterCloseAndReOpen() {
        val dbBuilder = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QueryInterceptorTestDatabase::class.java
        ).setQueryCallback(
            RoomDatabase.QueryCallback { sqlQuery, bindArgs ->
                val argTrace = ArrayList<Any>()
                argTrace.addAll(bindArgs)
                queryAndArgs.add(Pair(sqlQuery, argTrace))
            },
            MoreExecutors.directExecutor()
        )

        dbBuilder.build().close()

        mDatabase = dbBuilder.build()

        mDatabase.queryInterceptorDao().insert(
            QueryInterceptorEntity("Insert", "Inserted a placeholder query")
        )

        assertQueryLogged(
            "INSERT OR ABORT INTO `queryInterceptorTestDatabase` (`id`,`description`) " +
                "VALUES (?,?)",
            listOf("Insert", "Inserted a placeholder query")
        )
        assertTransactionQueries()
    }

    private fun assertQueryLogged(
        query: String,
        expectedArgs: List<String?>
    ) {
        val filteredQueries = queryAndArgs.filter {
            it.first == query
        }
        assertThat(filteredQueries).hasSize(1)
        assertThat(expectedArgs).containsExactlyElementsIn(filteredQueries[0].second)
    }

    private fun assertTransactionQueries() {
        assertNotNull(
            queryAndArgs.any {
                it.equals("BEGIN TRANSACTION")
            }
        )
        assertNotNull(
            queryAndArgs.any {
                it.equals("TRANSACTION SUCCESSFUL")
            }
        )
        assertNotNull(
            queryAndArgs.any {
                it.equals("END TRANSACTION")
            }
        )
    }
}
