/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.engine.processor

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.model.A2uiCoreSurfaceGroupModel
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiFunctionCallAction
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class A2uiCoreSurfaceActorTest {

    private companion object {
        const val SURFACE_ID = "surf-test"
        const val CATALOG_ID = "catalog-test"
    }

    private val catalogs = listOf(TestCatalog(CATALOG_ID))
    private val surfaceGroup = A2uiCoreSurfaceGroupModel()

    // Use a replay buffer in tests to easily inspect emitted items via replayCache
    private val outboundEvents =
        MutableSharedFlow<A2uiClientToServerMessage>(
            replay = 64,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    // =========================================================================
    // Core Queue & Lifecycle Tests
    // =========================================================================

    @Test
    fun enqueue_whenChannelOpen_tracksPendingMessagesAndReturnsTrue() = runTest {
        val actor = createActor()

        val result = actor.enqueue(A2uiEngineExternalMessage(A2uiDeleteSurfaceMessage(SURFACE_ID)))

        assertThat(result).isTrue()
        assertThat(actor.isPendingMessagesEmpty).isFalse()
    }

    @Test
    fun enqueue_whenChannelClosed_rollsBackPendingCountAndReturnsFalse() = runTest {
        val actor = createActor()
        actor.closeChannel()

        val result = actor.enqueue(A2uiEngineExternalMessage(A2uiDeleteSurfaceMessage(SURFACE_ID)))

        assertThat(result).isFalse()
        assertThat(actor.isPendingMessagesEmpty).isTrue()
    }

    @Test
    fun runProcessingLoop_mismatchedSurfaceId_throwsIllegalArgumentExceptionAndCrashes() = runTest {
        val actor = createActor()
        val job = launch { assertFailsWith<IllegalArgumentException> { actor.runProcessingLoop() } }

        actor.enqueue(
            A2uiEngineExternalMessage(A2uiCreateSurfaceMessage("wrong-surface-id", CATALOG_ID))
        )
        advanceUntilIdle()

        job.cancel()
    }

    // =========================================================================
    // Message Handlers: Create, Component, Data Model
    // =========================================================================
    @Test
    fun runProcessingLoop_surfaceCreationMessage_createsSurfaceSuccessfully() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }
        val message =
            A2uiCreateSurfaceMessage(
                surfaceId = SURFACE_ID,
                catalogId = CATALOG_ID,
                theme = mapOf("color" to "blue"),
                shouldSendDataModel = true,
            )

        actor.enqueue(A2uiEngineExternalMessage(message))
        advanceUntilIdle()

        val surface = surfaceGroup.getSurface(SURFACE_ID)
        assertThat(surface).isNotNull()
        assertThat(surface!!.id).isEqualTo(SURFACE_ID)
        assertThat(surface.catalog.id).isEqualTo(CATALOG_ID)
        assertThat(surface.theme).isEqualTo(mapOf("color" to "blue"))
        assertThat(surface.shouldSendDataModel).isTrue()
        assertThat(actor.isPendingMessagesEmpty).isTrue()

        job.cancel()
    }

    @Test
    fun runProcessingLoop_surfaceCreationMessageWithUnknownCatalog_emitsA2uiException() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }

        actor.enqueue(
            A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, "non-existent-catalog"))
        )
        advanceUntilIdle()

        val collectedErrors = outboundEvents.replayCache
        assertThat(collectedErrors).hasSize(1)
        val error = collectedErrors[0] as A2uiClientErrorMessage
        assertThat(error.code).isEqualTo("RUNTIME_ERROR")
        assertThat(error.surfaceId).isEqualTo(SURFACE_ID)
        assertThat(error.message).contains("Catalog with ID 'non-existent-catalog' not found")

        job.cancel()
    }

    @Test
    fun runProcessingLoop_surfaceCreationMessageForExistingSurface_emitsA2uiException() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        advanceUntilIdle()

        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        advanceUntilIdle()

        val collectedErrors = outboundEvents.replayCache
        assertThat(collectedErrors).hasSize(1)
        val error = collectedErrors[0] as A2uiClientErrorMessage
        assertThat(error.code).isEqualTo("RUNTIME_ERROR")
        assertThat(error.surfaceId).isEqualTo(SURFACE_ID)
        assertThat(error.message).contains("Surface '$SURFACE_ID' already exists")

        job.cancel()
    }

    @Test
    fun runProcessingLoop_componentUpdatesMessage_appliesToSurface() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        advanceUntilIdle()

        val registry =
            surfaceGroup.getSurface(SURFACE_ID)!!.componentRegistry as TestComponentRegistry
        val payload = A2uiComponentPayload("btn-1", "button", mapOf("text" to "Click"))

        actor.enqueue(
            A2uiEngineExternalMessage(A2uiUpdateComponentsMessage(SURFACE_ID, listOf(payload)))
        )
        advanceUntilIdle()

        assertThat(registry.updates).containsExactly(payload)
        job.cancel()
    }

    @Test
    fun runProcessingLoop_componentUpdatesMessageForUnknownSurface_emitsA2uiException() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }

        actor.enqueue(
            A2uiEngineExternalMessage(A2uiUpdateComponentsMessage(SURFACE_ID, emptyList()))
        )
        advanceUntilIdle()

        val collectedErrors = outboundEvents.replayCache
        assertThat(collectedErrors).hasSize(1)
        val error = collectedErrors[0] as A2uiClientErrorMessage
        assertThat(error.code).isEqualTo("RUNTIME_ERROR")
        assertThat(error.message).contains("Surface 'surf-test' not found")

        job.cancel()
    }

    @Test
    fun runProcessingLoop_dataModelUpdatesMessage_appliesToSurface() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        advanceUntilIdle()
        val dataModel = surfaceGroup.getSurface(SURFACE_ID)!!.dataModel as TestDataModel

        actor.enqueue(
            A2uiEngineExternalMessage(A2uiUpdateDataModelMessage(SURFACE_ID, "/volume", 15))
        )
        advanceUntilIdle()

        assertThat(dataModel.updates).containsEntry("/volume", 15)
        job.cancel()
    }

    @Test
    fun runProcessingLoop_dataModelUpdatesMessageForUnknownSurface_emitsA2uiException() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }

        actor.enqueue(
            A2uiEngineExternalMessage(A2uiUpdateDataModelMessage(SURFACE_ID, "/test", "val"))
        )
        advanceUntilIdle()

        val collectedErrors = outboundEvents.replayCache
        assertThat(collectedErrors).hasSize(1)
        val error = collectedErrors[0] as A2uiClientErrorMessage
        assertThat(error.code).isEqualTo("RUNTIME_ERROR")
        assertThat(error.message).contains("Surface 'surf-test' not found")

        job.cancel()
    }

    // =========================================================================
    // Actions and Interceptors
    // =========================================================================

    @Test
    fun runProcessingLoop_actionMessageWithoutInterceptor_propagatesToOutboundEvents() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        val actionMessage =
            A2uiEngineActionMessage(
                surfaceId = SURFACE_ID,
                action =
                    A2uiEventAction(
                        surfaceId = SURFACE_ID,
                        componentId = "btn-1",
                        timestamp = 123L,
                        eventName = "click",
                        context = mapOf("x" to 10),
                    ),
            )

        actor.enqueue(actionMessage)
        advanceUntilIdle()

        val collectedEvents = outboundEvents.replayCache
        assertThat(collectedEvents).hasSize(1)
        val eventMessage = collectedEvents[0] as A2uiClientEventMessage
        assertThat(eventMessage.componentId).isEqualTo("btn-1")
        assertThat(eventMessage.type).isEqualTo("click")
        assertThat(eventMessage.context).isEqualTo(mapOf("x" to 10))

        job.cancel()
    }

    @Test
    fun runProcessingLoop_actionMessageWithInterceptorConsumingNull_doesNotPropagate() = runTest {
        val interceptor = A2uiActionInterceptor { action ->
            if (action.componentId == "btn-1") null else action
        }
        val actor = createActor(interceptors = listOf(interceptor))
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))

        actor.enqueue(
            A2uiEngineActionMessage(
                surfaceId = SURFACE_ID,
                action =
                    A2uiEventAction(
                        surfaceId = SURFACE_ID,
                        componentId = "btn-1",
                        timestamp = 123L,
                        eventName = "click",
                        context = emptyMap(),
                    ),
            )
        )
        actor.enqueue(
            A2uiEngineActionMessage(
                surfaceId = SURFACE_ID,
                action =
                    A2uiEventAction(
                        surfaceId = SURFACE_ID,
                        componentId = "btn-2",
                        timestamp = 123L,
                        eventName = "click",
                        context = emptyMap(),
                    ),
            )
        )
        advanceUntilIdle()

        val collectedEvents = outboundEvents.replayCache
        assertThat(collectedEvents).hasSize(1)
        val eventMessage = collectedEvents[0] as A2uiClientEventMessage
        assertThat(eventMessage.componentId).isEqualTo("btn-2")

        job.cancel()
    }

    @Test
    fun runProcessingLoop_actionMessageWithMultipleInterceptors_propagatesFinalTransformed() =
        runTest {
            val interceptor1 = A2uiActionInterceptor { action ->
                val serverAction = action as A2uiEventAction
                A2uiEventAction(
                    surfaceId = serverAction.surfaceId,
                    componentId = serverAction.componentId,
                    timestamp = serverAction.timestamp,
                    eventName = serverAction.eventName,
                    context = mapOf("i1" to true),
                )
            }
            val interceptor2 = A2uiActionInterceptor { action ->
                val serverAction = action as A2uiEventAction
                val newContext = serverAction.context.toMutableMap()
                newContext["i2"] = true
                A2uiEventAction(
                    surfaceId = serverAction.surfaceId,
                    componentId = serverAction.componentId,
                    timestamp = serverAction.timestamp,
                    eventName = serverAction.eventName,
                    context = newContext,
                )
            }
            val actor = createActor(interceptors = listOf(interceptor1, interceptor2))
            val job = launch { actor.runProcessingLoop() }
            actor.enqueue(
                A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID))
            )

            actor.enqueue(
                A2uiEngineActionMessage(
                    surfaceId = SURFACE_ID,
                    action =
                        A2uiEventAction(
                            surfaceId = SURFACE_ID,
                            componentId = "btn-1",
                            timestamp = 123L,
                            eventName = "click",
                            context = emptyMap(),
                        ),
                )
            )
            advanceUntilIdle()

            val collectedEvents = outboundEvents.replayCache
            assertThat(collectedEvents).hasSize(1)
            val eventMessage = collectedEvents[0] as A2uiClientEventMessage
            assertThat(eventMessage.componentId).isEqualTo("btn-1")
            assertThat(eventMessage.context).isEqualTo(mapOf("i1" to true, "i2" to true))

            job.cancel()
        }

    @Test
    fun runProcessingLoop_functionCallAction_isExecutedButNotEmittedToServer() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))

        actor.enqueue(
            A2uiEngineActionMessage(
                surfaceId = SURFACE_ID,
                action =
                    A2uiFunctionCallAction(
                        surfaceId = SURFACE_ID,
                        componentId = "btn-1",
                        timestamp = 123L,
                        functionName = "test_func",
                        args = mapOf("arg" to "val"),
                    ),
            )
        )
        advanceUntilIdle()

        assertThat(outboundEvents.replayCache).isEmpty()
        job.cancel()
    }

    // =========================================================================
    // Error Handling & Unhandled Exceptions
    // =========================================================================

    @Test
    fun runProcessingLoop_errorMessage_propagatesToOutboundEvents() = runTest {
        val actor = createActor()
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        val testError =
            A2uiClientErrorMessage(code = "TEST", surfaceId = SURFACE_ID, message = "msg")

        actor.enqueue(A2uiEngineErrorMessage(SURFACE_ID, testError))
        advanceUntilIdle()

        val collectedEvents = outboundEvents.replayCache
        assertThat(collectedEvents).hasSize(1)
        assertThat(collectedEvents[0]).isEqualTo(testError)

        job.cancel()
    }

    @Test
    fun runProcessingLoop_unexpectedJvmException_crashesCoroutine() = runTest {
        val badDataModelFactory = { throw RuntimeException("Simulated JVM crash!") }
        val actor = createActor(dataModelFactoryOverride = badDataModelFactory)

        val job = launch { assertFailsWith<RuntimeException> { actor.runProcessingLoop() } }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        advanceUntilIdle()
        job.cancel()
    }

    // =========================================================================
    // Deletion, Ghost Taps, and Loopback
    // =========================================================================

    @Test
    fun runProcessingLoop_deleteSurfaceMessage_ignoresSubsequentActionsButProcessesErrors() =
        runTest {
            val actor = createActor(onActorIdleAndDeleted = { false })
            val job = launch { actor.runProcessingLoop() }
            actor.enqueue(
                A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID))
            )

            actor.enqueue(A2uiEngineExternalMessage(A2uiDeleteSurfaceMessage(SURFACE_ID)))
            actor.enqueue(
                A2uiEngineActionMessage(
                    surfaceId = SURFACE_ID,
                    action =
                        A2uiEventAction(
                            surfaceId = SURFACE_ID,
                            componentId = "btn-1",
                            timestamp = 123L,
                            eventName = "click",
                            context = emptyMap(),
                        ),
                )
            )
            val testError =
                A2uiClientErrorMessage(code = "TEST", surfaceId = SURFACE_ID, message = "crash")
            actor.enqueue(A2uiEngineErrorMessage(SURFACE_ID, testError))
            advanceUntilIdle()

            val collectedEvents = outboundEvents.replayCache
            assertThat(collectedEvents).hasSize(1)
            assertThat(collectedEvents[0]).isEqualTo(testError) // Only the error made it through

            job.cancel()
        }

    @Test
    fun runProcessingLoop_whenDeleted_eagerDrainAndCleanup() = runTest {
        var supervisorCalled = false
        val actor =
            createActor(
                onActorIdleAndDeleted = { deletingActor ->
                    supervisorCalled = true
                    deletingActor.closeChannel()
                    true
                }
            )
        val job = launch { actor.runProcessingLoop() }
        actor.enqueue(A2uiEngineExternalMessage(A2uiCreateSurfaceMessage(SURFACE_ID, CATALOG_ID)))
        advanceUntilIdle()

        actor.enqueue(A2uiEngineExternalMessage(A2uiDeleteSurfaceMessage(SURFACE_ID)))
        actor.enqueue(
            A2uiEngineExternalMessage(A2uiUpdateComponentsMessage(SURFACE_ID, emptyList()))
        )
        advanceUntilIdle()

        assertThat(surfaceGroup.getSurface(SURFACE_ID)).isNull()
        assertThat(supervisorCalled).isTrue()
        assertThat(actor.isPendingMessagesEmpty).isTrue()

        val collectedEvents = outboundEvents.replayCache
        assertThat(collectedEvents).hasSize(1)
        val error = collectedEvents[0] as A2uiClientErrorMessage
        assertThat(error.code).isEqualTo("RUNTIME_ERROR")
        assertThat(error.message).contains("Surface 'surf-test' not found")

        job.cancel()
    }

    private fun createActor(
        interceptors: List<A2uiActionInterceptor> = emptyList(),
        onActorIdleAndDeleted: (A2uiCoreSurfaceActor) -> Boolean = { true },
        dataModelFactoryOverride: (() -> A2uiCoreDataModel)? = null,
    ): A2uiCoreSurfaceActor {
        var actorReference: A2uiCoreSurfaceActor? = null
        val actor =
            A2uiCoreSurfaceActor(
                surfaceId = SURFACE_ID,
                catalogs = catalogs,
                surfaceGroup = surfaceGroup,
                dataModelFactory = dataModelFactoryOverride ?: { TestDataModel() },
                componentRegistryFactory = { TestComponentRegistry() },
                actionInterceptors = interceptors,
                outboundEvents = outboundEvents,
                onActorIdleAndDeleted = onActorIdleAndDeleted,
                onDispatchMessageToProcessor = { actorReference?.enqueue(it) },
            )
        actorReference = actor
        return actor
    }

    private class TestCatalog(override val id: String) : A2uiCoreCatalog {
        override val componentDefinitions =
            listOf(
                object : A2uiCoreComponentDefinition {
                    override val name = "button"
                    override val description = "A test button"
                    override val propertySchema = A2uiObjectSchema()
                }
            )
        override val functions =
            listOf(
                object : A2uiFunction {
                    override val definition =
                        object : androidx.a2ui.model.catalog.A2uiFunctionDefinition {
                            override val name = "test_func"
                            override val description = "A test function"
                            override val argumentSchema = A2uiObjectSchema()
                            override val returnType =
                                androidx.a2ui.model.catalog.A2uiFunctionReturnType.VOID
                        }

                    override fun execute(
                        args: Map<String, Any>,
                        executionContext: androidx.a2ui.model.protocol.A2uiExecutionContext,
                    ): Any? {
                        return null
                    }
                }
            )
        override val themeSchema: A2uiSchema? = null

        override fun getComponentDefinition(name: String) =
            componentDefinitions.find { it.name == name }

        override fun getFunction(name: String) = functions.find { it.definition.name == name }
    }

    private class TestDataModel : A2uiCoreDataModel {
        val updates = mutableMapOf<String, Any?>()

        override fun update(path: A2uiDataPath, value: Any?) {
            updates[path.normalizedPath] = value
        }

        override fun get(path: A2uiDataPath): Any? = updates[path.normalizedPath]

        override fun close() {}
    }

    private class TestComponentRegistry : A2uiCoreComponentRegistry {
        val updates = mutableListOf<A2uiComponentPayload>()

        override fun update(components: List<A2uiComponentPayload>) {
            updates.addAll(components)
        }

        override fun reportError(id: String, exception: A2uiException) {}

        override fun close() {}
    }
}
