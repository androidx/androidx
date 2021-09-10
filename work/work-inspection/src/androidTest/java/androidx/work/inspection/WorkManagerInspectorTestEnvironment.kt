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

package androidx.work.inspection

import android.app.Application
import androidx.inspection.ArtTooling
import androidx.inspection.testing.DefaultTestInspectorEnvironment
import androidx.inspection.testing.InspectorTester
import androidx.inspection.testing.TestInspectorExecutors
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.inspection.WorkManagerInspectorProtocol.Command
import androidx.work.inspection.WorkManagerInspectorProtocol.Event
import androidx.work.inspection.WorkManagerInspectorProtocol.Response
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

private const val WORK_MANAGER_INSPECTOR_ID = "androidx.work.inspection"

class WorkManagerInspectorTestEnvironment : ExternalResource() {
    private lateinit var inspectorTester: InspectorTester
    private lateinit var artTooling: FakeArtTooling
    private lateinit var application: InspectorApp
    private val job = Job()
    lateinit var workManager: WorkManager
        private set

    override fun before() {
        artTooling = FakeArtTooling()
        application = InstrumentationRegistry
            .getInstrumentation()
            .context
            .applicationContext as InspectorApp

        workManager = WorkManager.getInstance(application)

        registerApplication(application)
        inspectorTester = runBlocking {
            InspectorTester(
                inspectorId = WORK_MANAGER_INSPECTOR_ID,
                environment = DefaultTestInspectorEnvironment(
                    testInspectorExecutors = TestInspectorExecutors(job),
                    artTooling = artTooling
                )
            )
        }
    }

    override fun after() {
        runBlocking {
            // first let's stop inspector
            application.executor.runAllCommands()
            inspectorTester.dispose()
            job.cancelAndJoin()
            // then clear workmanager. Reverse order
            // will trigger unnecessary events and work in inspector
            workManager.cancelAllWork().await()
            workManager.pruneWork().await()
        }
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

    suspend fun receiveEvent() = receiveFilteredEvent { true }

    suspend fun receiveFilteredEvent(predicate: (Event) -> Boolean): Event {
        while (true) {
            inspectorTester.channel.receive().let { responseBytes ->
                assertThat(responseBytes).isNotEmpty()
                val event = Event.parseFrom(responseBytes)
                if (predicate(event)) {
                    return event
                }
            }
        }
    }

    private fun registerApplication(application: Application) {
        artTooling.registerInstancesToFind(listOf(application))
    }

    fun consumeRegisteredHooks(): List<Hook> =
        artTooling.consumeRegisteredHooks()
}

/**
 * Fake inspector environment with the following behaviour:
 * - [findInstances] returns pre-registered values from [registerInstancesToFind].
 * - [registerEntryHook] and [registerExitHook] record the calls which can later be
 * retrieved in [consumeRegisteredHooks].
 */
private class FakeArtTooling : ArtTooling {
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
    override fun <T : Any?> findInstances(clazz: Class<T>): List<T> =
        instancesToFind.filter { clazz.isInstance(it) }.map { it as T }.toList()

    override fun <T : Any?> registerExitHook(
        originClass: Class<*>,
        originMethod: String,
        exitHook: ArtTooling.ExitHook<T>
    ) {
    }

    override fun registerEntryHook(
        originClass: Class<*>,
        originMethod: String,
        entryHook: ArtTooling.EntryHook
    ) {
        // TODO: implement actual registerEntryHook behaviour
        registeredHooks.add(Hook.EntryHook(originClass, originMethod, entryHook))
    }

    fun consumeRegisteredHooks(): List<Hook> =
        registeredHooks.toList().also {
            registeredHooks.clear()
        }
}

sealed class Hook(val originClass: Class<*>, val originMethod: String) {
    class EntryHook(
        originClass: Class<*>,
        originMethod: String,
        val entryHook: ArtTooling.EntryHook
    ) : Hook(originClass, originMethod)
}

val Hook.asEntryHook get() = (this as Hook.EntryHook).entryHook
