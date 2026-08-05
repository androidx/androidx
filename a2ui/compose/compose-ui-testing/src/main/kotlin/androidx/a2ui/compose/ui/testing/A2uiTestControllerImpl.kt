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

@file:Suppress("BanConcurrentHashMap")

package androidx.a2ui.compose.ui.testing

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.engine.schema.A2uiCoreSchemaValidator
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiServerToClientMessage
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher

/**
 * Implements [A2uiTestController].
 *
 * @property catalog The [A2uiCatalog] under test.
 * @property theme Simulated theme overrides (e.g., `primaryColor`) from the agent.
 * @property initialComponents A list of [A2uiComponentPayload]s used to initialize the UI
 *   hierarchy.
 * @property initialData The initial data tree injected into the data model.
 * @property componentStubs A List of component stubs to override or append to the catalog.
 */
internal class A2uiTestControllerImpl(
    val catalog: A2uiCatalog,
    val theme: Map<String, Any?> = emptyMap(),
    val initialComponents: List<A2uiComponentPayload> = emptyList(),
    val initialData: Map<String, Any?> = emptyMap(),
    val componentStubs: List<A2uiComponentStub> = emptyList(),
) : A2uiTestController {

    private val _dispatchedActions = CopyOnWriteArrayList<A2uiUserAction>()
    override val dispatchedActions: List<A2uiUserAction>
        get() = _dispatchedActions

    private val _outboundEvents = CopyOnWriteArrayList<A2uiClientEventMessage>()
    override val outboundEvents: List<A2uiClientEventMessage>
        get() = _outboundEvents

    private val _outboundErrors = CopyOnWriteArrayList<A2uiClientErrorMessage>()
    override val outboundErrors: List<A2uiClientErrorMessage>
        get() = _outboundErrors

    override val surface: A2uiCoreSurfaceModel
        get() {
            val surfaces = processor.activeSurfaces.value
            for (i in surfaces.indices) {
                val surface = surfaces[i]
                if (surface.id == TestSurfaceId) {
                    return surface as? A2uiCoreSurfaceModel
                        ?: throw IllegalStateException(
                            "Surface must implement A2uiCoreSurfaceModel."
                        )
                }
            }
            throw IllegalStateException("Surface not created. Ensure start() was called.")
        }

    /** Map of component IDs to stub implementations for ID-scoped test overrides. */
    internal val idStubs: Map<String, A2uiComponentStubImpl>

    /** Map of component IDs to generated synthetic type names for ID-scoped stubs. */
    internal val syntheticTypesById: Map<String, String>

    /** Extended catalog containing base components, type overrides, and synthetic ID stubs. */
    internal val testCatalog: A2uiCatalog

    /** Schema validator for validating payloads against [testCatalog]. */
    private val schemaValidator: A2uiCoreSchemaValidator

    /** Message processor orchestrating protocol messages for the test catalog. */
    private val processor: A2uiMessageProcessor

    /** Action interceptor recording dispatched user actions into [_dispatchedActions]. */
    private val testInterceptor: A2uiActionInterceptor

    /** Guard flag preventing duplicate background processing initialization. */
    private var isProcessingStarted = false

    /** Buffer for server-to-client messages enqueued before [start] is called. */
    private val preStartMessages = mutableListOf<A2uiServerToClientMessage>()

    /**
     * Map of known component IDs to their types, used for [updateComponent] overloads without type.
     */
    private val knownComponentTypes = ConcurrentHashMap<String, String>()

    init {
        val (idStubsMap, typeStubsMap) = partitionStubs(componentStubs)
        idStubs = idStubsMap
        syntheticTypesById = idStubsMap.keys.associateWith { id -> "__stub_$id" }
        for ((id, _) in syntheticTypesById) {
            knownComponentTypes[id] = STUB_TYPE_SENTINEL
        }
        for (i in initialComponents.indices) {
            val component = initialComponents[i]
            knownComponentTypes[component.id] = component.type
        }

        val components = buildTestComponents(catalog, typeStubsMap, idStubs, syntheticTypesById)

        require(catalog is A2uiCoreCatalog) {
            "A2uiTestController requires an A2uiCoreCatalog implementation."
        }
        val coreCatalog = catalog
        testCatalog =
            A2uiCatalog(
                catalogId = catalog.id,
                components = components,
                functions = coreCatalog.functions,
                themeSchema = coreCatalog.themeSchema,
            )
        schemaValidator = A2uiCoreSchemaValidator(testCatalog as A2uiCoreCatalog)

        testInterceptor = A2uiActionInterceptor { request ->
            _dispatchedActions.add(request)
            request
        }

        processor =
            A2uiMessageProcessor(
                catalogs = listOf(testCatalog),
                interceptors = listOf(testInterceptor),
            )
    }

    override suspend fun start(): A2uiSurfaceModel {
        if (isProcessingStarted) return surface
        isProcessingStarted = true

        startProcessing()
        initializeSurface()
        for (i in preStartMessages.indices) {
            processor.processMessage(preStartMessages[i])
        }
        preStartMessages.clear()
        waitForIdle()
        return surface
    }

    override suspend fun waitForIdle() {
        val dispatcher = currentCoroutineContext()[ContinuationInterceptor] as? TestDispatcher
        requireNotNull(dispatcher) {
            "waitForIdle() must be called from within a test coroutine environment (e.g., runComposeUiTest)."
        }
        dispatcher.scheduler.advanceUntilIdle()
    }

    override fun getRawData(path: String): Any? = surface.dataModel[A2uiDataPath(path)]

    override fun updateData(path: String, value: Any?) {
        val message = A2uiUpdateDataModelMessage(TestSurfaceId, path, value)
        if (!isProcessingStarted) {
            preStartMessages.add(message)
        } else {
            processor.processMessage(message)
        }
    }

    override fun updateComponent(id: String, type: String, properties: Map<String, Any?>) {
        val coreCatalog = testCatalog as A2uiCoreCatalog
        val payload = validateAndCreatePayload(id, type, properties, coreCatalog)
        knownComponentTypes[id] = type
        val message = A2uiUpdateComponentsMessage(TestSurfaceId, listOf(payload))
        if (!isProcessingStarted) {
            preStartMessages.add(message)
        } else {
            processor.processMessage(message)
        }
    }

    override fun updateComponent(id: String, properties: Map<String, Any?>) {
        val existingType =
            knownComponentTypes[id]
                ?: throw IllegalStateException(
                    "Cannot update component '$id': no type recorded. Call updateComponent(id, type, properties) first."
                )
        updateComponent(id, existingType, properties)
    }

    override fun failComponent(id: String, exception: A2uiException) {
        surface.dispatchError(exception, id)
    }

    override fun clearDispatchedActions() = _dispatchedActions.clear()

    override fun clearOutboundEvents() = _outboundEvents.clear()

    override fun clearOutboundErrors() = _outboundErrors.clear()

    /** Partitions component stubs into ID-scoped and type-scoped maps after duplicate checking. */
    private fun partitionStubs(
        stubs: List<A2uiComponentStub>
    ): Pair<Map<String, A2uiComponentStubImpl>, MutableMap<String, A2uiComponentStubImpl>> {
        val idStubsMap = mutableMapOf<String, A2uiComponentStubImpl>()
        val typeStubsMap = mutableMapOf<String, A2uiComponentStubImpl>()

        for (i in stubs.indices) {
            when (val stubImpl = stubs[i] as A2uiComponentStubImpl) {
                is IdStubImpl -> {
                    require(!idStubsMap.containsKey(stubImpl.id)) {
                        "Duplicate stub defined for ID: '${stubImpl.id}'."
                    }
                    idStubsMap[stubImpl.id] = stubImpl
                }
                is TypeStubImpl -> {
                    require(!typeStubsMap.containsKey(stubImpl.type)) {
                        "Duplicate stub defined for Type: '${stubImpl.type}'."
                    }
                    typeStubsMap[stubImpl.type] = stubImpl
                }
            }
        }
        return Pair(idStubsMap, typeStubsMap)
    }

    /** Builds merged component list combining base catalog definitions and test stubs. */
    private fun buildTestComponents(
        baseCatalog: A2uiCatalog,
        typeStubs: MutableMap<String, A2uiComponentStubImpl>,
        idStubs: Map<String, A2uiComponentStubImpl>,
        syntheticTypesById: Map<String, String>,
    ): List<A2uiComponent> {
        val components = ArrayList<A2uiComponent>()

        for (i in baseCatalog.components.indices) {
            val originalComponent = baseCatalog.components[i]
            val stub = typeStubs.remove(originalComponent.name)
            components.add(
                if (stub != null) {
                    createStubComponent(
                        name = originalComponent.name,
                        description = originalComponent.description,
                        properties = originalComponent.properties,
                        stubContent = stub,
                    )
                } else {
                    originalComponent
                }
            )
        }

        for ((type, stub) in typeStubs) {
            components.add(
                createStubComponent(
                    name = type,
                    description = "Stub for type $type",
                    properties = emptyList(),
                    stubContent = stub,
                )
            )
        }

        for ((id, stub) in idStubs) {
            val syntheticType = syntheticTypesById.getValue(id)
            components.add(
                createStubComponent(
                    name = syntheticType,
                    description = "Stub for ID $id",
                    properties = emptyList(),
                    stubContent = stub,
                )
            )
        }

        return components
    }

    /** Launches detached background coroutines for event collection and message processing. */
    internal suspend fun startProcessing() {
        val testContext = currentCoroutineContext()

        // Inherit the test dispatcher, but use a new Job to detach the infinite loops from
        // structured concurrency so that the test doesn't hang. Attach a CoroutineExceptionHandler
        // that delegates any uncaught crash inside the processing loops to the test context's
        // exception handler so it registers as a hard test failure.
        val exceptionHandler = CoroutineExceptionHandler { ctx, throwable ->
            testContext[CoroutineExceptionHandler]?.handleException(ctx, throwable)
                ?: throw throwable
        }
        val backgroundScope = CoroutineScope(testContext.minusKey(Job) + Job() + exceptionHandler)

        // Subscribe eagerly using UNDISPATCHED
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            processor.outboundEvents.collect { message ->
                when (message) {
                    is A2uiClientErrorMessage -> _outboundErrors.add(message)
                    is A2uiClientEventMessage -> _outboundEvents.add(message)
                }
            }
        }
        backgroundScope.launch { processor.collectMessages() }

        // Hook into the test's lifecycle to terminate the background scope when the parent Job
        // finished (e.g., the runComposeUiTest block finishes).
        testContext.job.invokeOnCompletion { backgroundScope.cancel() }
    }

    /** Sends initial messages for surface creation, theme, data model, and components. */
    internal fun initializeSurface() {
        processor.processMessage(
            A2uiCreateSurfaceMessage(
                surfaceId = TestSurfaceId,
                catalogId = catalog.id,
                theme = theme,
            )
        )

        if (initialData.isNotEmpty()) {
            processor.processMessage(
                A2uiUpdateDataModelMessage(
                    surfaceId = TestSurfaceId,
                    path = "/",
                    value = initialData,
                )
            )
        }

        val payloads = ArrayList<A2uiComponentPayload>(idStubs.size + initialComponents.size)
        val coreCatalog = testCatalog as A2uiCoreCatalog

        for ((id, _) in idStubs) {
            payloads.add(validateAndCreatePayload(id, STUB_TYPE_SENTINEL, emptyMap(), coreCatalog))
        }

        for (i in initialComponents.indices) {
            val component = initialComponents[i]
            payloads.add(
                validateAndCreatePayload(
                    component.id,
                    component.type,
                    component.properties,
                    coreCatalog,
                )
            )
        }

        if (payloads.isNotEmpty()) {
            processor.processMessage(
                A2uiUpdateComponentsMessage(surfaceId = TestSurfaceId, components = payloads)
            )
        }
    }

    /** Validates properties against the catalog schema and creates an [A2uiComponentPayload]. */
    private fun validateAndCreatePayload(
        id: String,
        type: String,
        properties: Map<String, Any?>,
        coreCatalog: A2uiCoreCatalog,
    ): A2uiComponentPayload {
        val actualType =
            if (id in syntheticTypesById) {
                if (type != STUB_TYPE_SENTINEL) {
                    throw IllegalArgumentException(
                        "Component ID '$id' is registered as an ID stub. Do not specify a component type when updating or initializing an ID stub; use updateComponent(id, properties) or A2uiComponentPayload(id, properties) instead."
                    )
                }
                syntheticTypesById.getValue(id)
            } else {
                if (type == STUB_TYPE_SENTINEL) {
                    throw IllegalArgumentException(
                        "A2uiComponentPayload(id, properties) without a type can only be used for ID stubs created via A2uiComponentStub.withId(). ID '$id' is not registered as an ID stub."
                    )
                }
                type
            }

        val componentDefinition =
            coreCatalog.componentDefinitions[actualType]
                ?: throw A2uiRuntimeException(
                    "Component type '$actualType' is not registered in the test catalog."
                )

        schemaValidator.validateSchema(
            properties,
            componentDefinition.propertySchema,
            basePath = "/components/$id",
        )

        return A2uiComponentPayload(id, actualType, properties)
    }
}

/** Creates an [A2uiComponent] wrapper delegating readiness and content to a test stub. */
private fun createStubComponent(
    name: String,
    description: String,
    properties: List<A2uiProperty<*>>,
    stubContent: A2uiComponentStubImpl,
): A2uiComponent =
    object : A2uiComponent {
        override val name = name
        override val description = description
        override val properties = properties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
            stubContent.isReady(this, properties)

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            stubContent.content(this, properties, modifier)
        }
    }

/** Default surface identifier used for the test surface managed by this controller. */
internal const val TestSurfaceId = "TestSurface"
