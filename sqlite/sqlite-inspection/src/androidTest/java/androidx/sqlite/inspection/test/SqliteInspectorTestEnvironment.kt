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

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.inspection.InspectorEnvironment
import androidx.inspection.testing.InspectorTester
import androidx.sqlite.inspection.SqliteInspectorFactory
import androidx.sqlite.inspection.SqliteInspectorProtocol
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabaseOpenedEvent
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event
import androidx.sqlite.inspection.SqliteInspectorProtocol.Response
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

private const val SQLITE_INSPECTOR_ID = "androidx.sqlite.inspection"

class SqliteInspectorTestEnvironment(
    val factoryOverride: SqliteInspectorFactory? = null
) : ExternalResource() {
    private lateinit var inspectorTester: InspectorTester
    private lateinit var environment: FakeInspectorEnvironment

    override fun before() {
        environment = FakeInspectorEnvironment()
        inspectorTester = runBlocking {
            InspectorTester(
                inspectorId = SQLITE_INSPECTOR_ID,
                environment = environment,
                factoryOverride = factoryOverride
            )
        }
    }

    override fun after() {
        inspectorTester.dispose()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun assertNoQueuedEvents() {
        assertThat(inspectorTester.channel.isEmpty).isTrue()
    }

    suspend fun sendCommand(command: Command): Response {
        inspectorTester.sendCommand(command.toByteArray())
            .let { responseBytes ->
                assertThat(responseBytes).isNotEmpty()
                return Response.parseFrom(responseBytes)
            }
    }

    suspend fun receiveEvent(): Event {
        inspectorTester.channel.receive().let { responseBytes ->
            assertThat(responseBytes).isNotEmpty()
            return Event.parseFrom(responseBytes)
        }
    }

    fun registerAlreadyOpenDatabases(databases: List<SQLiteDatabase>) {
        environment.registerInstancesToFind(databases)
    }

    fun registerApplication(application: Application) {
        environment.registerInstancesToFind(listOf(application))
    }

    fun consumeRegisteredHooks(): List<Hook> =
        environment.consumeRegisteredHooks()

    /** Assumes an event with the relevant database will be fired. */
    suspend fun awaitDatabaseOpenedEvent(databasePath: String): DatabaseOpenedEvent {
        while (true) {
            val event = receiveEvent().databaseOpened
            if (event.path == databasePath) {
                return event
            }
        }
    }
}

suspend fun SqliteInspectorTestEnvironment.issueQuery(
    databaseId: Int,
    command: String,
    queryParams: List<String?>? = null
): SqliteInspectorProtocol.QueryResponse =
    sendCommand(
        MessageFactory.createQueryCommand(
            databaseId,
            command,
            queryParams
        )
    ).query

suspend fun SqliteInspectorTestEnvironment.inspectDatabase(
    databaseInstance: SQLiteDatabase
): Int {
    registerAlreadyOpenDatabases(listOf(databaseInstance))
    sendCommand(MessageFactory.createTrackDatabasesCommand())
    return awaitDatabaseOpenedEvent(databaseInstance.displayName).databaseId
}

/**
 * Fake inspector environment with the following behaviour:
 * - [findInstances] returns pre-registered values from [registerInstancesToFind].
 * - [registerEntryHook] and [registerExitHook] record the calls which can later be
 * retrieved in [consumeRegisteredHooks].
 */
private class FakeInspectorEnvironment : InspectorEnvironment {
    private val instancesToFind = mutableListOf<Any>()
    private val registeredHooks = mutableListOf<Hook>()

    fun registerInstancesToFind(instances: List<Any>) {
        instancesToFind.addAll(instances)
    }

    /**
     *  Returns instances pre-registered in [registerInstancesToFind].
     *  By design crashes in case of the wrong setup - indicating an issue with test code.
     */
    @Suppress("UNCHECKED_CAST")
    // TODO: implement actual findInstances behaviour
    override fun <T : Any?> findInstances(clazz: Class<T>): MutableList<T> =
        instancesToFind.filter { clazz.isInstance(it) }.map { it as T }.toMutableList()

    override fun registerEntryHook(
        originClass: Class<*>,
        originMethod: String,
        entryHook: InspectorEnvironment.EntryHook
    ) {
        // TODO: implement actual registerEntryHook behaviour
        registeredHooks.add(Hook.EntryHook(originClass, originMethod, entryHook))
    }

    override fun <T : Any?> registerExitHook(
        originClass: Class<*>,
        originMethod: String,
        exitHook: InspectorEnvironment.ExitHook<T>
    ) {
        // TODO: implement actual registerExitHook behaviour
        registeredHooks.add(Hook.ExitHook(originClass, originMethod, exitHook))
    }

    fun consumeRegisteredHooks(): List<Hook> =
        registeredHooks.toList().also {
            registeredHooks.clear()
        }
}

sealed class Hook(val originClass: Class<*>, val originMethod: String) {
    class ExitHook(
        originClass: Class<*>,
        originMethod: String,
        val exitHook: InspectorEnvironment.ExitHook<*>
    ) : Hook(originClass, originMethod)

    class EntryHook(
        originClass: Class<*>,
        originMethod: String,
        @Suppress("unused") val entryHook: InspectorEnvironment.EntryHook
    ) : Hook(originClass, originMethod)
}

val Hook.asEntryHook get() = (this as Hook.EntryHook).entryHook
val Hook.asExitHook get() = (this as Hook.ExitHook).exitHook
