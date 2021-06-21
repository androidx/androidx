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

import android.database.AbstractCursor
import android.database.Cursor
import android.database.CursorWrapper
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import androidx.inspection.ArtTooling
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabasePossiblyChangedEvent
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event.OneOfCase.DATABASE_POSSIBLY_CHANGED
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class InvalidationTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @get:Rule
    val temporaryFolder = TemporaryFolder(getInstrumentation().context.cacheDir)

    @Test
    @FlakyTest(bugId = 159202455)
    fun test_exec_hook_methods() = test_simple_hook_methods(
        listOf(
            "execute()V",
            "executeInsert()J",
            "executeUpdateDelete()I"
        ).map { it to SQLiteStatement::class.java }
    )

    @Test
    fun test_end_transaction_hook_method() =
        test_simple_hook_methods(listOf("endTransaction()V" to SQLiteDatabase::class.java))

    private fun test_simple_hook_methods(expectedHooks: List<Pair<String, Class<*>>>) =
        runBlocking {
            // Starting to track databases makes the inspector register hooks
            testEnvironment.sendCommand(MessageFactory.createTrackDatabasesCommand())

            // Verification of hooks registration and triggering the DatabasePossiblyChangedEvent
            testEnvironment.consumeRegisteredHooks().let { hooks ->
                expectedHooks.forEach { (method, clazz) ->
                    val hook = hooks.filter { hook ->
                        hook.originMethod == method &&
                            hook.originClass == clazz
                    }
                    assertThat(hook).hasSize(1)

                    testEnvironment.assertNoQueuedEvents()
                    hook.first().asExitHook.onExit(null)
                    testEnvironment.receiveEvent().let { event ->
                        assertThat(event.oneOfCase == DATABASE_POSSIBLY_CHANGED)
                        assertThat(event.databasePossiblyChanged).isEqualTo(
                            DatabasePossiblyChangedEvent.getDefaultInstance()
                        )
                    }
                    testEnvironment.assertNoQueuedEvents()
                }
            }
        }

    @Test
    @FlakyTest // TODO: deflake
    @Ignore
    fun test_throttling(): Unit = runBlocking {
        // Starting to track databases makes the inspector register hooks
        testEnvironment.sendCommand(MessageFactory.createTrackDatabasesCommand())

        // Any hook that triggers invalidation
        val hook = testEnvironment.consumeRegisteredHooks()
            .first { it.originMethod == "executeInsert()J" }
            .asExitHook

        testEnvironment.assertNoQueuedEvents()

        // First invalidation triggering event
        hook.onExit(null)
        val event1 = testEnvironment.receiveEvent()

        // Shortly followed by many invalidation triggering events
        repeat(50) { hook.onExit(null) }
        val event2 = testEnvironment.receiveEvent()

        // Event validation
        listOf(event1, event2).forEach {
            assertThat(it.oneOfCase).isEqualTo(DATABASE_POSSIBLY_CHANGED)
        }

        // Only two invalidation events received
        testEnvironment.assertNoQueuedEvents()
    }

    @Test
    fun test_cursor_methods(): Unit = runBlocking {
        // Starting to track databases makes the inspector register hooks
        testEnvironment.sendCommand(MessageFactory.createTrackDatabasesCommand())

        // Hook method signatures
        val rawQueryMethodSignature = "rawQueryWithFactory(" +
            "Landroid/database/sqlite/SQLiteDatabase\$CursorFactory;" +
            "Ljava/lang/String;" +
            "[Ljava/lang/String;" +
            "Ljava/lang/String;" +
            "Landroid/os/CancellationSignal;" +
            ")Landroid/database/Cursor;"
        val closeMethodSignature = "close()V"

        val hooks: List<Hook> = testEnvironment.consumeRegisteredHooks()

        // Check for hooks being registered
        val hooksByClass = hooks.groupBy { it.originClass }
        val rawQueryHooks = hooksByClass[SQLiteDatabase::class.java]!!.filter {
            it.originMethod == rawQueryMethodSignature
        }.map { it::class }

        assertThat(rawQueryHooks).containsExactly(Hook.EntryHook::class, Hook.ExitHook::class)
        val hook = hooksByClass[SQLiteCursor::class.java]!!.single()
        assertThat(hook).isInstanceOf(Hook.EntryHook::class.java)
        assertThat(hook.originMethod).isEqualTo(closeMethodSignature)
        // Check for hook behaviour
        fun wrap(cursor: Cursor): Cursor = object : CursorWrapper(cursor) {}
        fun noOp(c: Cursor): Cursor = c
        listOf(::wrap, ::noOp).forEach { wrap ->
            listOf(
                "insert into t1 values (1)" to true,
                "select * from sqlite_master" to false
            ).forEach { (query, shouldCauseInvalidation) ->
                testEnvironment.assertNoQueuedEvents()

                val cursor = cursorForQuery(query)
                hooks.entryHookFor(rawQueryMethodSignature).onEntry(null, listOf(null, query))
                hooks.exitHookFor(rawQueryMethodSignature).onExit(wrap(wrap(cursor)))
                hooks.entryHookFor(closeMethodSignature).onEntry(cursor, emptyList())

                if (shouldCauseInvalidation) {
                    testEnvironment.receiveEvent()
                }
                testEnvironment.assertNoQueuedEvents()
            }
        }

        // no crash for unknown cursor class
        hooks.entryHookFor(rawQueryMethodSignature).onEntry(null, listOf(null, "select * from t1"))
        hooks.exitHookFor(rawQueryMethodSignature).onExit(object : AbstractCursor() {
            override fun getLong(column: Int): Long = 0
            override fun getCount(): Int = 0
            override fun getColumnNames(): Array<String> = emptyArray()
            override fun getShort(column: Int): Short = 0
            override fun getFloat(column: Int): Float = 0f
            override fun getDouble(column: Int): Double = 0.0
            override fun isNull(column: Int): Boolean = false
            override fun getInt(column: Int): Int = 0
            override fun getString(column: Int): String = ""
        })

        Unit
    }

    private fun cursorForQuery(query: String): SQLiteCursor {
        val db = Database("ignored", Table("t1", Column("c1", "int")))
            .createInstance(temporaryFolder)
        val cursor = db.rawQuery(query, null)
        val context = ApplicationProvider.getApplicationContext() as android.content.Context
        context.deleteDatabase(db.path)
        return cursor as SQLiteCursor
    }

    private fun List<Hook>.entryHookFor(m: String): ArtTooling.EntryHook =
        this.first { it.originMethod == m && it is Hook.EntryHook }.asEntryHook

    @Suppress("UNCHECKED_CAST")
    private fun List<Hook>.exitHookFor(m: String): ArtTooling.ExitHook<Any> =
        this.first { it.originMethod == m && it is Hook.ExitHook }
            .asExitHook as ArtTooling.ExitHook<Any>
}
