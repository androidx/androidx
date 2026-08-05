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

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Represents a stubbed or overridden Compose implementation for an A2UI component for testing.
 *
 * This allows tests to isolate specific components by stubbing out their children, or to inject
 * custom mock behaviors for components not natively present in the target catalog.
 */
public sealed interface A2uiComponentStub {

    public companion object {

        /**
         * Creates an [A2uiComponentStub] that targets a specific component instance by its unique
         * ID.
         *
         * Under the hood, this generates a synthetic component type bound only to this ID.
         *
         * @param id The exact ID of the component instance to stub (e.g., `"submit_button"`).
         * @param isReady A lambda to evaluate whether the stub is ready to render, defaults to
         *   ready.
         * @param content A composable lambda that emits the UI for this stubbed component.
         * @return An [A2uiComponentStub] constrained to the given ID.
         */
        public fun withId(
            id: String,
            isReady: @Composable A2uiComponentScope.(A2uiComponentProperties) -> Boolean = { true },
            content: @Composable A2uiComponentScope.(A2uiComponentProperties, Modifier) -> Unit,
        ): A2uiComponentStub = IdStubImpl(id, isReady, content)

        /**
         * Creates an [A2uiComponentStub] that overrides the rendering logic for all components of a
         * specific type.
         *
         * The component type may or may not already be present in the test catalog.
         *
         * @param type The string type identifier (e.g., `"Image"`, `"Video"`) to override.
         * @param isReady A lambda to evaluate whether the stub is ready to render, defaults to
         *   ready.
         * @param content A composable lambda that emits the UI for this component type.
         * @return An [A2uiComponentStub] constrained to the given type.
         */
        public fun withType(
            type: String,
            isReady: @Composable A2uiComponentScope.(A2uiComponentProperties) -> Boolean = { true },
            content: @Composable A2uiComponentScope.(A2uiComponentProperties, Modifier) -> Unit,
        ): A2uiComponentStub = TypeStubImpl(type, isReady, content)
    }
}

/**
 * Creates an [androidx.a2ui.model.protocol.A2uiComponentPayload] for an ID-scoped test stub without
 * requiring a dummy component type.
 *
 * This overload is strictly reserved for components stubbed via [A2uiComponentStub.withId].
 *
 * @param id The unique identifier of the stubbed component.
 * @param properties The initial property map to configure the stub.
 * @return An [A2uiComponentPayload] configured for the ID stub.
 * @throws IllegalArgumentException If used for a component ID that has not been registered as an ID
 *   stub via [A2uiComponentStub.withId].
 */
public fun A2uiComponentPayload(
    id: String,
    properties: Map<String, Any?> = emptyMap(),
): A2uiComponentPayload =
    A2uiComponentPayload(id = id, type = STUB_TYPE_SENTINEL, properties = properties)

/** Internal contract exposing readiness evaluation and composable rendering lambdas for stubs. */
internal sealed interface A2uiComponentStubImpl : A2uiComponentStub {
    /** Evaluates whether this stub is ready to render. */
    val isReady: @Composable A2uiComponentScope.(A2uiComponentProperties) -> Boolean
    /** Renders the composable UI hierarchy for this stub. */
    val content: @Composable A2uiComponentScope.(A2uiComponentProperties, Modifier) -> Unit
}

/** Backing implementation for ID-scoped component test stubs. */
internal class IdStubImpl(
    val id: String,
    override val isReady: @Composable A2uiComponentScope.(A2uiComponentProperties) -> Boolean,
    override val content: @Composable A2uiComponentScope.(A2uiComponentProperties, Modifier) -> Unit,
) : A2uiComponentStubImpl

/** Backing implementation for type-scoped component test stubs. */
internal class TypeStubImpl(
    val type: String,
    override val isReady: @Composable A2uiComponentScope.(A2uiComponentProperties) -> Boolean,
    override val content: @Composable A2uiComponentScope.(A2uiComponentProperties, Modifier) -> Unit,
) : A2uiComponentStubImpl

internal const val STUB_TYPE_SENTINEL = "__a2ui_stub_payload_sentinel__"
