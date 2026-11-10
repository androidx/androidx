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

package androidx.a2ui.model.catalog

import androidx.a2ui.model.schema.A2uiSchema

/**
 * Defines the UI components and functions of an A2UI catalog.
 *
 * A definition describes each component by its schema in [componentDefinitions] and holds the
 * [functions] that the agent can call. It does not say how a component is rendered: the catalog
 * type of a UI toolkit integration extends this interface and adds its component implementations.
 */
public interface A2uiCatalogDefinition {
    /**
     * The unique identifier for this catalog.
     *
     * This ID is announced to the agent during the capability negotiation phase. It is recommended
     * to use a URI format including a version number (e.g.,
     * `"https://example.com/a2ui/catalog/v1"`) to ensure global uniqueness and facilitate protocol
     * versioning. See https://a2ui.org/concepts/catalogs/#catalog-naming-versioning for the full
     * guidelines on catalog IDs.
     */
    public val id: String

    /**
     * The human-readable title of this catalog (e.g., `"A2UI Basic Catalog"`).
     *
     * The JSON Schema of this catalog includes the title in its `title` field, or omits the field
     * if the title is `null`. The title does not affect rendering.
     */
    public val title: String?
        get() = null

    /**
     * A human-readable description of this catalog.
     *
     * The JSON Schema of this catalog includes the description in its `description` field, or omits
     * the field if the description is `null`. Like the [title], the description does not affect
     * rendering.
     */
    public val description: String?
        get() = null

    /** The collection of component definitions available in this catalog. */
    public val componentDefinitions: A2uiComponentDefinitionCollection

    /**
     * The functions of this catalog, each with its implementation.
     *
     * An [A2uiFunction] pairs the [definition][A2uiFunction.definition] that is advertised to the
     * agent with the code that evaluates a call. This collection holds implementations, unlike
     * [componentDefinitions]: a function call is evaluated by calling [A2uiFunction.execute]
     * directly, whereas a component is rendered by a UI toolkit integration, which supplies the
     * component implementations.
     */
    public val functions: A2uiFunctionCollection

    /**
     * The optional JSON schema defining the dynamic theme overrides supported by this catalog.
     *
     * If provided, the agent can send theme properties conforming to this schema (e.g.,
     * `"primaryColor"`) within the `createSurface` message to customize the catalog's visual
     * appearance.
     */
    public val themeSchema: A2uiSchema?
        get() = null

    /**
     * Indicates whether this catalog's full JSON Schema should be serialized inline as part of the
     * capabilities advertisement sent to the agent.
     */
    public val isInline: Boolean
        get() = false
}
