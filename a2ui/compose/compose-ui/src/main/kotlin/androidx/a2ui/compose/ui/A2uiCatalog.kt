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

package androidx.a2ui.compose.ui

import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.A2uiRuntimeCatalog
import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.runtime.Stable
import kotlin.math.ceil

/**
 * A Jetpack Compose implementation of an A2UI component catalog, which defines which
 * [A2uiComponent]s and [A2uiFunction]s are available to the agent for a particular A2UI surface.
 *
 * To instantiate a new catalog, use the `A2uiCatalog` factory function.
 */
@Stable
public sealed interface A2uiCatalog {

    /**
     * The unique identifier for this catalog.
     *
     * This ID is announced to the agent during the capability negotiation phase. It is recommended
     * to use a URI format including a version number (e.g.,
     * `"https://example.com/a2ui/catalog/v1"`) to ensure global uniqueness and facilitate protocol
     * versioning.
     */
    public val id: String

    /** The exhaustive list of [A2uiComponent] implementations registered in this catalog. */
    public val components: List<A2uiComponent>

    /** The exhaustive list of [A2uiFunction] implementations registered in this catalog. */
    public val functions: List<A2uiFunction>

    /**
     * The optional JSON schema defining the dynamic theme overrides supported by this catalog.
     *
     * If provided, the agent can send theme properties conforming to this schema (e.g.,
     * `"primaryColor"`) within the `createSurface` message to customize the catalog's visual
     * appearance.
     */
    public val themeSchema: A2uiSchema?

    /**
     * Retrieves a Compose component implementation by its unique name.
     *
     * @param name The unique type name of the component (e.g., `"Text"`, `"Button"`).
     * @return The [A2uiComponent] implementation, or `null` if the component is not registered in
     *   this catalog.
     */
    public fun getComponent(name: String): A2uiComponent?

    /**
     * Retrieves a local function implementation by its unique name.
     *
     * @param name The unique name of the function (e.g., `"formatString"`).
     * @return The [A2uiFunction] implementation, or `null` if the function is not registered in
     *   this catalog.
     */
    public fun getFunction(name: String): A2uiFunction?
}

/**
 * Creates a new, immutable [A2uiCatalog] mapping a list of [A2uiComponent]s and [A2uiFunction]s to
 * their respective protocol definitions.
 *
 * @param catalogId The unique identifier for this catalog.
 * @param components The list of [A2uiComponent] implementations supported by this catalog.
 * @param functions The optional list of [A2uiFunction]s supported by this catalog. Defaults to an
 *   empty list.
 * @param themeSchema An optional [A2uiSchema] defining the dynamic theme overrides. Defaults to
 *   null.
 * @return A fully initialized and validated [A2uiCatalog].
 * @throws IllegalArgumentException If duplicate component names or duplicate function names are
 *   detected.
 */
public fun A2uiCatalog(
    catalogId: String,
    components: List<A2uiComponent>,
    functions: List<A2uiFunction> = emptyList(),
    themeSchema: A2uiSchema? = null,
): A2uiCatalog {
    // Divide the expected size by the default HashMap load factor (0.75) to calculate a capacity
    // large enough to hold the elements without triggering an internal map resize.
    val componentCapacity = ceil(components.size / 0.75).toInt()
    val componentMap = HashMap<String, A2uiComponent>(componentCapacity)
    for (i in components.indices) {
        val component = components[i]
        val name = component.name
        require(componentMap.put(name, component) == null) {
            "Duplicate component registered for name '$name'. " +
                "Catalogs must have unique component types."
        }
    }

    val functionCapacity = ceil(functions.size / 0.75).toInt()
    val functionMap = HashMap<String, A2uiFunction>(functionCapacity)
    for (i in functions.indices) {
        val function = functions[i]
        val name = function.definition.name
        require(functionMap.put(name, function) == null) {
            "Duplicate function registered for name '$name'. " +
                "Catalogs must have unique function names."
        }
    }

    return A2uiCatalogImpl(catalogId, themeSchema, componentMap, functionMap)
}

internal class A2uiCatalogImpl(
    override val id: String,
    override val themeSchema: A2uiSchema?,
    private val componentMap: Map<String, A2uiComponent>,
    private val functionMap: Map<String, A2uiFunction>,
) : A2uiCatalog, A2uiRuntimeCatalog, A2uiCoreCatalog {

    override val components: List<A2uiComponent> = componentMap.values.toList()

    private val componentDefinitionMap: Map<String, A2uiCoreComponentDefinition> =
        componentMap.mapValues { (_, component) ->
            A2uiCoreComponentDefinitionImpl(
                name = component.name,
                description = component.description,
                properties = component.properties,
            )
        }

    override val componentDefinitions: List<A2uiCoreComponentDefinition> =
        componentDefinitionMap.values.toList()

    override val functions: List<A2uiFunction> = functionMap.values.toList()

    override fun getComponentDefinition(name: String): A2uiCoreComponentDefinition? =
        componentDefinitionMap[name]

    override fun getFunction(name: String): A2uiFunction? = functionMap[name]

    override fun getComponent(name: String): A2uiComponent? = componentMap[name]
}

private class A2uiCoreComponentDefinitionImpl(
    override val name: String,
    override val description: String,
    properties: List<A2uiProperty<*>>,
) : A2uiCoreComponentDefinition {

    override val propertySchema: A2uiSchema

    init {
        val schemaProperties = HashMap<String, A2uiSchema>(properties.size)
        val requiredProperties = HashSet<String>()
        for (i in properties.indices) {
            val prop = properties[i]
            schemaProperties[prop.key] = prop.schema
            if (prop.isRequired) {
                requiredProperties.add(prop.key)
            }
        }
        propertySchema =
            A2uiObjectSchema(properties = schemaProperties, required = requiredProperties)
    }
}
