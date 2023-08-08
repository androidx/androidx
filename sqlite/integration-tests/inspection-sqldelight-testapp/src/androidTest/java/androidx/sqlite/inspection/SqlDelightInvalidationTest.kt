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

package androidx.sqlite.inspection

import android.database.sqlite.SQLiteDatabase
import androidx.inspection.ArtTooling
import androidx.inspection.testing.DefaultTestInspectorEnvironment
import androidx.inspection.testing.InspectorTester
import androidx.inspection.testing.TestInspectorExecutors
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryCommand
import androidx.sqlite.inspection.SqliteInspectorProtocol.TrackDatabasesCommand
import androidx.sqlite.inspection.sqldeligttestapp.Database
import androidx.sqlite.inspection.test.TestEntity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@FlowPreview
class SqlDelightInvalidationTest {

    lateinit var driver: SqlDriver
    lateinit var openedDb: SupportSQLiteDatabase

    @Before
    fun setup() {
        driver = AndroidSqliteDriver(
            schema = Database.Schema,
            context = ApplicationProvider.getApplicationContext(),
            callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    openedDb = db
                    super.onCreate(db)
                }
            }
        )
    }

    @Test
    fun test() {
        runBlocking {
            val dao = Database(driver).testEntityQueries
            dao.insertOrReplace("one")
            val sqliteDb = openedDb.getSqliteDb()
            val query = dao.selectAll()
            val job = this.coroutineContext[Job]!!
            val tester = InspectorTester(
                inspectorId = "androidx.sqlite.inspection",
                environment =
                    DefaultTestInspectorEnvironment(
                        TestInspectorExecutors(job),
                        TestArtTooling(sqliteDb, listOf(query))
                    )
            )
            val updates = query.asFlow().mapToList().take(2).produceIn(this)

            val firstExpected = TestEntity.Impl(1, "one")
            val secondExpected = TestEntity.Impl(2, "foo")
            assertThat(updates.receive()).isEqualTo(listOf(firstExpected))

            val startTrackingCommand = Command.newBuilder().setTrackDatabases(
                TrackDatabasesCommand.getDefaultInstance()
            ).build()

            tester.sendCommand(startTrackingCommand.toByteArray())

            val insertQuery = """INSERT INTO TestEntity VALUES(2, "foo")"""
            val insertCommand = Command.newBuilder().setQuery(
                QueryCommand.newBuilder()
                    .setDatabaseId(1)
                    .setQuery(insertQuery)
                    .build()
            ).build()
            val responseBytes = tester.sendCommand(insertCommand.toByteArray())
            val response = SqliteInspectorProtocol.Response.parseFrom(responseBytes)
            assertWithMessage("test sanity, insert query should succeed")
                .that(response.hasErrorOccurred())
                .isFalse()

            assertThat(updates.receive()).isEqualTo(
                listOf(
                    firstExpected,
                    secondExpected
                )
            )
        }
    }

    @After
    fun tearDown() {
        driver.close()
    }
}

/**
 * extract the framework sqlite database instance from a room database via reflection.
 */
private fun SupportSQLiteDatabase.getSqliteDb(): SQLiteDatabase {
    // this runs with defaults so we can extract db from it until inspection supports support
    // instances directly
    return this::class.java.getDeclaredField("mDelegate").let {
        it.isAccessible = true
        it.get(this)
    } as SQLiteDatabase
}

@Suppress("UNCHECKED_CAST")
class TestArtTooling(
    private val sqliteDb: SQLiteDatabase,
    private val queries: List<Query<*>>
) : ArtTooling {
    override fun registerEntryHook(
        originClass: Class<*>,
        originMethod: String,
        entryHook: ArtTooling.EntryHook
    ) {
        // no-op
    }

    override fun <T : Any?> findInstances(clazz: Class<T>): List<T> {
        if (clazz.isAssignableFrom(Query::class.java)) {
            return queries as List<T>
        } else if (clazz.isAssignableFrom(SQLiteDatabase::class.java)) {
            return listOf(sqliteDb as T)
        }
        return emptyList()
    }

    override fun <T : Any?> registerExitHook(
        originClass: Class<*>,
        originMethod: String,
        exitHook: ArtTooling.ExitHook<T>
    ) {
        // no-op
    }
}
