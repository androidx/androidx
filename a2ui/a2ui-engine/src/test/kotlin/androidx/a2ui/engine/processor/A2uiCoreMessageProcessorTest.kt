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
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class A2uiCoreMessageProcessorTest {

    private companion object {
        const val CATALOG_ID = "test-catalog"
        const val CATALOG_ID_2 = "test-catalog-2"
        const val SURFACE_A = "surface-A"
        const val SURFACE_B = "surface-B"
    }

    private val catalogs = listOf(TestCatalog(CATALOG_ID), TestCatalog(CATALOG_ID_2))
    private val actionInterceptors = mutableListOf<A2uiActionInterceptor>()
    private val registriesCreated = mutableListOf<TestComponentRegistry>()

    @Test
    fun clientCapabilities_resolvesCatalogIdsCorrectly() {
        val processor = createProcessor()
        val capabilities = processor.clientCapabilities
        assertThat(capabilities.supportedCatalogIds)
            .containsExactly(CATALOG_ID, CATALOG_ID_2)
            .inOrder()
    }

    @Test
    fun processMessage_whenSurfaceIsNew_createsNewActor() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }

        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        advanceUntilIdle()

        assertThat(registriesCreated).hasSize(1)

        collectJob.cancel()
    }

    @Test
    fun processMessage_whenSurfaceIsActive_routesToExistingActor() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        advanceUntilIdle()
        val payload = A2uiComponentPayload("btn1", "button", emptyMap())

        processor.processMessage(A2uiUpdateComponentsMessage(SURFACE_A, listOf(payload)))
        advanceUntilIdle()

        val registry = registriesCreated.first()
        assertThat(registriesCreated).hasSize(1)
        assertThat(registry.updates).containsExactly(payload)

        collectJob.cancel()
    }

    @Test
    fun processMessage_whenSurfaceIdsDiffer_routesToDifferentActors() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }

        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_B, CATALOG_ID_2))
        advanceUntilIdle()

        assertThat(registriesCreated).hasSize(2)

        collectJob.cancel()
    }

    @Test
    fun activeSurfaces_initially_emitsEmptyList() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        val activeSurfacesList = mutableListOf<List<A2uiSurfaceModel>>()
        val surfacesJob = launch { processor.activeSurfaces.toList(activeSurfacesList) }

        advanceUntilIdle()

        assertThat(activeSurfacesList.last()).isEmpty()

        collectJob.cancel()
        surfacesJob.cancel()
    }

    @Test
    fun activeSurfaces_whenSurfaceCreated_emitsNewSurface() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        val activeSurfacesList = mutableListOf<List<A2uiSurfaceModel>>()
        val surfacesJob = launch { processor.activeSurfaces.toList(activeSurfacesList) }

        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        advanceUntilIdle()

        assertThat(activeSurfacesList.last().map { it.id }).containsExactly(SURFACE_A)

        collectJob.cancel()
        surfacesJob.cancel()
    }

    @Test
    fun activeSurfaces_whenSurfaceDeleted_removesSurface() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        val activeSurfacesList = mutableListOf<List<A2uiSurfaceModel>>()
        val surfacesJob = launch { processor.activeSurfaces.toList(activeSurfacesList) }
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_B, CATALOG_ID_2))
        advanceUntilIdle()

        processor.processMessage(A2uiDeleteSurfaceMessage(SURFACE_A))
        advanceUntilIdle()

        assertThat(activeSurfacesList.last().map { it.id }).containsExactly(SURFACE_B)

        collectJob.cancel()
        surfacesJob.cancel()
    }

    @Test
    fun processMessage_withActionInterceptors_injectedCorrectly() = runTest {
        var intercepted = false
        actionInterceptors.add(
            A2uiActionInterceptor { _ ->
                intercepted = true
                null
            }
        )
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        val collectedOutbound = mutableListOf<A2uiClientToServerMessage>()
        val outboundJob = launch { processor.outboundEvents.toList(collectedOutbound) }
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))

        processor.processInternalMessage(
            A2uiEngineActionMessage(
                surfaceId = SURFACE_A,
                action =
                    A2uiEventAction(
                        surfaceId = SURFACE_A,
                        componentId = "btn-A",
                        timestamp = 123L,
                        eventName = "click",
                        context = emptyMap(),
                    ),
            )
        )
        advanceUntilIdle()

        assertThat(intercepted).isTrue()
        assertThat(collectedOutbound).isEmpty()

        outboundJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun processMessage_whenSurfaceIsDeletedAndRecreated_revivesActor() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        advanceUntilIdle()

        processor.processMessage(A2uiDeleteSurfaceMessage(SURFACE_A))
        advanceUntilIdle()
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        advanceUntilIdle()

        assertThat(registriesCreated).hasSize(2)

        collectJob.cancel()
    }

    @Test
    fun collectMessages_whenCancellation_tearsDownAllChildActors() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        advanceUntilIdle()

        collectJob.cancel()
        advanceUntilIdle()
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_B, CATALOG_ID_2))
        advanceUntilIdle()

        assertThat(registriesCreated).hasSize(1)
    }

    @Test
    fun collectMessages_whenUnhandledJvmExceptions_crashTheProcessorScope() = runTest {
        val badDataModelFactory = { throw RuntimeException("Simulated JVM crash in DataModel!") }

        val crashingProcessor =
            A2uiCoreMessageProcessor(
                catalogs = catalogs,
                dataModelFactory = badDataModelFactory,
                componentRegistryFactory = { TestComponentRegistry() },
            )

        val collectJob = launch {
            assertFailsWith<RuntimeException> { crashingProcessor.collectMessages() }
        }

        crashingProcessor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        advanceUntilIdle()

        collectJob.cancel()
    }

    @Test
    fun outboundEvents_fromAllActors_routesToGlobalFlow() = runTest {
        val processor = createProcessor()
        val collectJob = launch { processor.collectMessages() }
        val collectedOutbound = mutableListOf<A2uiClientToServerMessage>()
        val outboundJob = launch { processor.outboundEvents.toList(collectedOutbound) }
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_A, CATALOG_ID))
        processor.processMessage(A2uiCreateSurfaceMessage(SURFACE_B, CATALOG_ID_2))
        advanceUntilIdle()
        val testError = A2uiClientErrorMessage("TEST", SURFACE_B, "msg")

        processor.processInternalMessage(
            A2uiEngineActionMessage(
                surfaceId = SURFACE_A,
                action =
                    A2uiEventAction(
                        surfaceId = SURFACE_A,
                        componentId = "btn-A",
                        timestamp = 123L,
                        eventName = "click",
                        context = emptyMap(),
                    ),
            )
        )
        processor.processInternalMessage(A2uiEngineErrorMessage(SURFACE_B, testError))
        advanceUntilIdle()

        assertThat(collectedOutbound).hasSize(2)
        val eventA = collectedOutbound[0] as A2uiClientEventMessage
        assertThat(eventA.surfaceId).isEqualTo(SURFACE_A)
        assertThat(eventA.componentId).isEqualTo("btn-A")
        val eventB = collectedOutbound[1] as A2uiClientErrorMessage
        assertThat(eventB.surfaceId).isEqualTo(SURFACE_B)

        outboundJob.cancel()
        collectJob.cancel()
    }

    private fun createProcessor(): A2uiCoreMessageProcessor {
        return A2uiCoreMessageProcessor(
            catalogs = catalogs,
            dataModelFactory = { TestDataModel() },
            componentRegistryFactory = {
                TestComponentRegistry().also { registriesCreated.add(it) }
            },
            actionInterceptors = actionInterceptors,
        )
    }

    private class TestCatalog(override val id: String) : A2uiCoreCatalog {
        override val components =
            listOf(
                object : A2uiCoreComponentDefinition {
                    override val name = "button"
                    override val description = "A test button"
                    override val propertySchema = A2uiObjectSchema()
                }
            )
        override val functions = emptyList<A2uiFunction>()
        override val themeSchema: A2uiSchema? = null

        override fun getComponent(name: String) = components.find { it.name == name }

        override fun getFunction(name: String) = null
    }

    private class TestDataModel : A2uiCoreDataModel {
        override fun update(path: A2uiDataPath, value: Any?) {}

        override fun get(path: A2uiDataPath): Any? = null

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
