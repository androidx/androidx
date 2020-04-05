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

import android.database.sqlite.SQLiteStatement
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabasePossiblyChangedEvent
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event.OneOfCase.DATABASE_POSSIBLY_CHANGED
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

private val sqliteStatementInvalidationTriggeringMethods =
    listOf("execute()V", "executeInsert()J", "executeUpdateDelete()I")

@LargeTest
@RunWith(AndroidJUnit4::class)
class InvalidationTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @get:Rule
    val temporaryFolder = TemporaryFolder(getInstrumentation().context.cacheDir)

    @Test
    fun test_exec_methods(): Unit = runBlocking {
        // Starting to track databases makes the inspector register hooks
        testEnvironment.sendCommand(MessageFactory.createTrackDatabasesCommand())

        // Verification of hooks being registered and triggering the DatabasePossiblyChangedEvent
        testEnvironment.consumeRegisteredHooks().let { hooks ->
            sqliteStatementInvalidationTriggeringMethods
                .forEach { method ->
                    val hook = hooks.filter { hook ->
                        hook.originMethod == method &&
                                hook.originClass == SQLiteStatement::class.java
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
    fun test_throttling(): Unit = runBlocking {
        // Starting to track databases makes the inspector register hooks
        testEnvironment.sendCommand(MessageFactory.createTrackDatabasesCommand())

        // Any hook that triggers invalidation
        val hook = testEnvironment.consumeRegisteredHooks()
            .first { it.originMethod == sqliteStatementInvalidationTriggeringMethods.first() }
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
}
