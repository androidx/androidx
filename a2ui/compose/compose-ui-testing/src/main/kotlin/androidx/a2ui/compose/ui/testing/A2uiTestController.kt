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

package androidx.a2ui.compose.ui.testing

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiUserAction

/**
 * Creates a [A2uiTestController] that can be used to drive tests.
 *
 * @param catalog The [A2uiCatalog] under test.
 * @param theme Simulated theme overrides (e.g., `primaryColor`) from the agent.
 * @param initialComponents A list of [A2uiComponentPayload]s used to initialize the UI hierarchy.
 * @param initialData The initial data tree injected into the data model.
 * @param componentStubs A List of component stubs to override or append to the catalog.
 * @return An unstarted [A2uiTestController].
 *
 * TODO(b/527415570): add samples, including the stubbing APIs.
 */
public fun A2uiTestController(
    catalog: A2uiCatalog,
    theme: Map<String, Any?> = emptyMap(),
    initialComponents: List<A2uiComponentPayload> = emptyList(),
    initialData: Map<String, Any?> = emptyMap(),
    componentStubs: List<A2uiComponentStub> = emptyList(),
): A2uiTestController =
    A2uiTestControllerImpl(
        catalog = catalog,
        theme = theme,
        initialComponents = initialComponents,
        initialData = initialData,
        componentStubs = componentStubs,
    )

/**
 * A controller for orchestrating A2UI component and surface tests.
 *
 * It provides access to the underlying [A2uiSurfaceModel] for rendering, tracks outbound actions
 * dispatched by the UI, and exposes synchronous methods to simulate incoming protocol messages
 * (data and component updates) from an agent.
 */
public interface A2uiTestController {

    /**
     * The [A2uiSurfaceModel] instance managed by this controller.
     *
     * @throws IllegalStateException If accessed before calling [start].
     */
    public val surface: A2uiSurfaceModel

    /**
     * A sequentially ordered record of all [A2uiUserAction]s dispatched by components during the
     * test. This includes both local client-side function calls and server-bound events.
     *
     * To inspect only the events queued for network transmission to the agent, use
     * [outboundEvents]. Use [clearDispatchedActions] to clear this record.
     */
    public val dispatchedActions: List<A2uiUserAction>

    /**
     * A sequentially ordered record of all [A2uiClientEventMessage]s queued for transmission to the
     * server (agent).
     *
     * Local function calls are not included in this list. Use [clearOutboundEvents] to clear this
     * record.
     */
    public val outboundEvents: List<A2uiClientEventMessage>

    /**
     * A sequentially ordered record of all [A2uiClientErrorMessage]s queued for transmission to the
     * server (agent), useful for asserting self-correction feedback loops.
     *
     * Use [clearOutboundErrors] to clear this record.
     */
    public val outboundErrors: List<A2uiClientErrorMessage>

    /**
     * Starts the controller's background processing loops, initializes the surface with the
     * configured theme, initial data, and initial components, and waits for the reactive state to
     * settle.
     *
     * This is expected to be called in a block passed to `runComposeUiTest`.
     *
     * @return The fully initialized [A2uiSurfaceModel] ready to be mounted in a UI.
     */
    public suspend fun start(): A2uiSurfaceModel

    /**
     * Suspends test execution until the coroutine scheduler processes all pending background data
     * layer tasks and evaluator actions.
     *
     * Note: This settles the A2UI data model and component registry layers. Typically, calling
     * `ComposeUiTest.waitForIdle()` afterward is still needed to allow the Compose UI tree to
     * recompose and reflect the new states.
     *
     * This is expected to be called in a block passed to `runComposeUiTest`.
     */
    public suspend fun waitForIdle()

    /**
     * Simulates the agent sending a data model update to the test surface.
     *
     * @param path The JSON pointer path to update.
     * @param value The new value to place at the specified path.
     */
    public fun updateData(path: String, value: Any?)

    /**
     * Simulates the agent pushing a structural component update to the surface.
     *
     * @param id The unique identifier of the component.
     * @param type The string type identifier of the component.
     * @param properties The property map representing the component's configuration.
     */
    public fun updateComponent(id: String, type: String, properties: Map<String, Any?>)

    /**
     * Simulates the agent pushing an incremental property update to an already existing component
     * on the surface, reusing its previously recorded type.
     *
     * @param id The unique identifier of the component.
     * @param properties The property map representing the component's configuration.
     * @throws IllegalStateException If no type has been recorded for `id` via `initialComponents`
     *   or a prior call to [updateComponent].
     */
    public fun updateComponent(id: String, properties: Map<String, Any?>)

    /**
     * Simulates an agent hallucination or validation failure for a given component.
     *
     * @param id The unique identifier of the component to fail.
     * @param exception The [A2uiException] simulating the error.
     */
    public fun failComponent(id: String, exception: A2uiException)

    /**
     * Reads a value synchronously from the underlying test data model.
     *
     * @param path The JSON pointer path to read.
     * @return The raw data model value, or `null` if the path does not exist.
     */
    public fun getRawData(path: String): Any?

    /** Clears the history of [dispatchedActions]. */
    public fun clearDispatchedActions()

    /** Clears the history of [outboundEvents]. */
    public fun clearOutboundEvents()

    /** Clears the history of [outboundErrors]. */
    public fun clearOutboundErrors()
}

/**
 * Reads a value from the underlying data model at the specified JSON pointer path and casts it to
 * the requested type [T].
 *
 * @param path The JSON pointer path to read (e.g., `"/user/name"`).
 * @return The data model value cast to [T], or `null` if the path does not exist.
 * @throws ClassCastException if the value at [path] cannot be cast to [T].
 */
public inline fun <reified T> A2uiTestController.getData(path: String): T? {
    val value = getRawData(path) ?: return null
    if (value !is T) {
        throw ClassCastException(
            "Cannot cast value '$value' (${value::class.simpleName}) at path '$path' to ${T::class.simpleName}."
        )
    }
    return value
}
