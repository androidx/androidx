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

package androidx.a2ui.core.model

import androidx.a2ui.core.catalog.A2uiCoreCatalog
import androidx.a2ui.core.platform.A2uiComponentRegistry
import androidx.a2ui.core.platform.A2uiDataModel
import androidx.a2ui.core.protocol.A2uiClientError
import androidx.a2ui.core.protocol.A2uiComponentPayload
import androidx.a2ui.core.protocol.A2uiDataPath
import androidx.a2ui.core.protocol.A2uiException
import androidx.a2ui.core.protocol.A2uiUserAction

/**
 * The root domain model for a single active surface.
 *
 * It acts as the owner of that surface's [A2uiDataModel] and [A2uiComponentRegistry], managing
 * updates to these registries and propagating user actions and validation/runtime errors.
 *
 * @param id The unique identifier of this surface.
 * @param catalog The catalog used in this surface.
 * @param dataModel The storage model for this surface's data tree.
 * @param componentRegistry The component registry backing this surface.
 * @param onDispatchAction Callback to handle user actions dispatched from this surface.
 * @param onDispatchError Callback to handle errors dispatched from this surface.
 * @param theme An optional map of theme-specific overrides for this surface.
 * @param shouldSendDataModel If true, indicates the data model of this surface should be appended
 *   as metadata to outgoing messages to the server.
 * @param timeProvider Provider that returns the current epoch time in milliseconds.
 */
public class A2uiSurfaceModel(
    public val id: String,
    public val catalog: A2uiCoreCatalog,
    public val dataModel: A2uiDataModel,
    public val componentRegistry: A2uiComponentRegistry,
    private val onDispatchAction: (A2uiUserAction) -> Unit,
    private val onDispatchError: (A2uiClientError) -> Unit,
    public val theme: Map<String, Any?> = emptyMap(),
    @get:JvmName("shouldSendDataModel") public val shouldSendDataModel: Boolean = false,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
) {

    /**
     * Applies a data model update to this surface.
     *
     * @param path The absolute JSON pointer path to update.
     * @param value The new value to store at the path.
     */
    internal fun updateDataModel(path: A2uiDataPath, value: Any?) {
        dataModel.update(path, value)
    }

    /**
     * Updates the registry with a batch of components.
     *
     * @param payloads The list of components to apply.
     *
     * TODO(annabelo): Consider running schema validation here.
     */
    internal fun updateComponents(payloads: List<A2uiComponentPayload>) {
        componentRegistry.update(payloads)
    }

    /**
     * Dispatches a user action from this surface to the registered action handler callback.
     *
     * @param componentId The unique identifier of the component that was interacted with.
     * @param actionDefinition A map containing details about the action (e.g., name, context).
     */
    public fun dispatchAction(componentId: String, actionDefinition: Map<String, Any?>) {
        // TODO(annabelo): Should we fail here?
        val type = (actionDefinition["type"] as? String) ?: "unknown_action"
        @Suppress("UNCHECKED_CAST")
        val context = (actionDefinition["context"] as? Map<String, Any?>) ?: emptyMap()
        val action =
            A2uiUserAction(
                type = type,
                surfaceId = id,
                componentId = componentId,
                timestamp = timeProvider(),
                context = context,
            )
        onDispatchAction(action)
    }

    /**
     * Reports a rendering or evaluation failure to the component registry and dispatches the error
     * to the registered error handler callback.
     *
     * @param componentId The unique identifier of the component that encountered the error.
     * @param exception The exception describing the failure.
     */
    public fun dispatchError(componentId: String, exception: A2uiException) {
        componentRegistry.reportError(componentId, exception)
        val error =
            A2uiClientError(
                code = exception.code,
                surfaceId = id,
                message = exception.message ?: "",
                context = exception.context,
            )
        onDispatchError(error)
    }

    /** Cleans up resources and active memory. */
    internal fun dispose() {
        dataModel.dispose()
        componentRegistry.dispose()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is A2uiSurfaceModel) return false
        return (id == other.id) &&
            (catalog == other.catalog) &&
            (dataModel == other.dataModel) &&
            (componentRegistry == other.componentRegistry) &&
            (theme == other.theme) &&
            (shouldSendDataModel == other.shouldSendDataModel)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = (31 * result) + catalog.hashCode()
        result = (31 * result) + dataModel.hashCode()
        result = (31 * result) + componentRegistry.hashCode()
        result = (31 * result) + theme.hashCode()
        result = (31 * result) + shouldSendDataModel.hashCode()
        return result
    }

    override fun toString(): String {
        return "A2uiSurfaceModel(id='$id', catalog=$catalog, theme=$theme, shouldSendDataModel=$shouldSendDataModel)"
    }
}
