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

package androidx.compose.runtime.a2ui

import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * The receiver scope provided to an A2UI component for rendering.
 *
 * This scope provides a component-specific context, allowing the component to evaluate reactive
 * data bindings, render nested children, dispatch user actions, and report runtime errors.
 */
@Stable
public sealed interface A2uiComponentScope {

    /**
     * Resolves the current [A2uiComponentState] of a component by its unique ID and subscribes for
     * future updates.
     *
     * @param id The unique ID of the component to resolve.
     * @param dataScopePath An optional relative or absolute data path to override the component's
     *   data context.
     * @return The [A2uiComponentState] of the requested component.
     */
    @Composable
    public fun observeA2uiComponentState(
        id: String,
        dataScopePath: String? = null,
    ): A2uiComponentState

    /**
     * Dispatches an A2UI action (such as a server event or local function call) as defined in the
     * component's properties.
     *
     * @param actionPayload The action definition to dispatch, typically extracted from the
     *   component's properties.
     */
    public fun dispatchAction(actionPayload: Map<String, Any?>)

    /**
     * Reports a runtime error that occurred during the evaluation or rendering of this component.
     *
     * @param exception The exception detailing the error.
     */
    public fun reportError(exception: A2uiException)
}

/**
 * Resolves the current [A2uiComponentState] of a component by its [A2uiComponentReference] and
 * subscribes for future updates.
 *
 * @param reference The unique [A2uiComponentReference] of the component to resolve.
 * @return The [A2uiComponentState] of the requested component.
 */
@Composable
public fun A2uiComponentScope.observeA2uiComponentState(
    reference: A2uiComponentReference
): A2uiComponentState = observeA2uiComponentState(reference.id, reference.dataScopePath)

/** A stable updater for two-way dynamic A2UI property bindings. */
@Stable
public fun interface A2uiPropertyUpdater<T> {
    /**
     * Pushes a local update back to the data model.
     *
     * @param value The new value to write to the model.
     */
    public operator fun invoke(value: T?)
}
