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
import androidx.inspection.Connection
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import androidx.inspection.testing.InspectorTester
import androidx.room.Database
import androidx.room.Entity
import androidx.room.InvalidationTracker
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RoomInvalidationHookTest {
    private lateinit var db: TestDatabase
    private val inspectionExecutors =
        Pair(Executors.newSingleThreadExecutor(), Executors.newSingleThreadScheduledExecutor())

    @Before
    fun initDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestDatabase::class.java
        ).setQueryExecutor {
            it.run()
        }.setTransactionExecutor {
            it.run()
        }.build()
    }

    @After
    fun closeDb() {
        listOf(inspectionExecutors.first, inspectionExecutors.second)
            .forEach { inspectionExecutor ->
                inspectionExecutor.shutdown()
                assertWithMessage("inspector should not have any leaking tasks")
                    .that(inspectionExecutor.awaitTermination(10, TimeUnit.SECONDS))
                    .isTrue()
                db.close()
            }
    }

    /**
     * A full integration test where we send a query via the inspector and assert that an
     * invalidation observer on the Room side is invoked.
     */
    @Test
    fun invalidationHook() = runBlocking<Unit> {
        val testEnv = TestInspectorEnvironment(
            roomDatabase = db,
            sqliteDb = db.getSqliteDb()
        )
        val tester = InspectorTester(
            inspectorId = "test",
            environment = testEnv,
            factoryOverride = object : InspectorFactory<SqliteInspector>("test") {
                override fun createInspector(
                    connection: Connection,
                    environment: InspectorEnvironment
                ): SqliteInspector {
                    return SqliteInspector(
                        connection,
                        environment,
                        inspectionExecutors.first,
                        inspectionExecutors.second
                    )
                }
            }
        )
        val invalidatedTables = CompletableDeferred<List<String>>()
        db.invalidationTracker.addObserver(object : InvalidationTracker.Observer("TestEntity") {
            override fun onInvalidated(tables: MutableSet<String>) {
                invalidatedTables.complete(tables.toList())
            }
        })
        val startTrackingCommand = SqliteInspectorProtocol.Command.newBuilder().setTrackDatabases(
            SqliteInspectorProtocol.TrackDatabasesCommand.getDefaultInstance()
        ).build()
        tester.sendCommand(startTrackingCommand.toByteArray())
        // no invalidation yet
        assertWithMessage("test sanity. no invalidation should happen yet")
            .that(invalidatedTables.isActive)
            .isTrue()
        // send a write query
        val insertQuery = """INSERT INTO TestEntity VALUES(1, "foo")"""
        val insertCommand = SqliteInspectorProtocol.Command.newBuilder().setQuery(
            SqliteInspectorProtocol.QueryCommand.newBuilder()
                .setDatabaseId(1)
                .setQuery(insertQuery)
                .build()
        ).build()
        val responseBytes = tester.sendCommand(insertCommand.toByteArray())
        val response = SqliteInspectorProtocol.Response.parseFrom(responseBytes)
        assertWithMessage("test sanity, insert query should succeed")
            .that(response.hasErrorOccurred())
            .isFalse()

        assertWithMessage("writing into db should trigger the table observer")
            .that(invalidatedTables.await())
            .containsExactly("TestEntity")
    }
}

/**
 * extract the framework sqlite database instance from a room database via reflection.
 */
private fun RoomDatabase.getSqliteDb(): SQLiteDatabase {
    val supportDb = this.openHelper.writableDatabase
    // this runs with defaults so we can extract db from it until inspection supports support
    // instances directly
    return supportDb::class.java.getDeclaredField("mDelegate").let {
        it.isAccessible = true
        it.get(supportDb)
    } as SQLiteDatabase
}

@Suppress("UNCHECKED_CAST")
class TestInspectorEnvironment(
    private val roomDatabase: RoomDatabase,
    private val sqliteDb: SQLiteDatabase
) : InspectorEnvironment {
    override fun registerEntryHook(
        originClass: Class<*>,
        originMethod: String,
        entryHook: InspectorEnvironment.EntryHook
    ) {
        // no-op
    }

    override fun <T : Any?> findInstances(clazz: Class<T>): List<T> {
        if (clazz.isAssignableFrom(InvalidationTracker::class.java)) {
            return listOf(roomDatabase.invalidationTracker as T)
        } else if (clazz.isAssignableFrom(SQLiteDatabase::class.java)) {
            return listOf(sqliteDb as T)
        }
        return emptyList()
    }

    override fun <T : Any?> registerExitHook(
        originClass: Class<*>,
        originMethod: String,
        exitHook: InspectorEnvironment.ExitHook<T>
    ) {
        // no-op
    }
}

@Database(
    exportSchema = false,
    entities = [TestEntity::class],
    version = 1
)
abstract class TestDatabase : RoomDatabase()

@Entity
data class TestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val value: String
)
