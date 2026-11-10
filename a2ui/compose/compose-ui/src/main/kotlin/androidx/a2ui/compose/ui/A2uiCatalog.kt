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

import androidx.a2ui.compose.runtime.A2uiComponentModel
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.A2uiReadinessEvaluator
import androidx.a2ui.engine.catalog.A2uiCoreCatalogDefinitionSerializer
import androidx.a2ui.model.catalog.A2uiCatalogDefinition
import androidx.a2ui.model.catalog.A2uiComponentDefinition
import androidx.a2ui.model.catalog.A2uiComponentDefinitionCollection
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.protocol.A2uiInlineCatalog
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * A Jetpack Compose implementation of an A2UI component catalog, which defines which
 * [A2uiComponent]s and [A2uiFunction]s are available to the agent for a particular A2UI surface.
 *
 * A catalog is an [A2uiCatalogDefinition] that also holds the Compose implementations of its
 * [components].
 *
 * To instantiate a new catalog, use the `A2uiCatalog` factory function. To process agent messages
 * for surfaces that use the catalog, pass it to
 * [androidx.a2ui.compose.runtime.A2uiMessageProcessor].
 */
@Stable
public sealed interface A2uiCatalog : A2uiCatalogDefinition {

    /**
     * The collection of [A2uiComponent] implementations registered in this catalog.
     *
     * [componentDefinitions] describes each of these components, in the same order, by the schema
     * that is advertised to the agent.
     */
    public val components: A2uiComponentCollection
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
 * @param isInline Indicates whether this catalog's full JSON Schema should be serialized inline.
 *   Defaults to false.
 * @param title The optional title of this catalog, which is included in its JSON Schema. Defaults
 *   to null.
 * @param description The optional description of this catalog, which is included in its JSON
 *   Schema. Defaults to null.
 * @return A fully initialized and validated [A2uiCatalog].
 * @throws IllegalArgumentException If duplicate component names or duplicate function names are
 *   detected.
 */
public fun A2uiCatalog(
    catalogId: String,
    components: List<A2uiComponent>,
    functions: List<A2uiFunction> = emptyList(),
    themeSchema: A2uiSchema? = null,
    isInline: Boolean = false,
    title: String? = null,
    description: String? = null,
): A2uiCatalog {
    val componentCollection = A2uiComponentCollection(components)
    val functionCollection = A2uiFunctionCollection(functions)

    val componentDefinitions =
        ArrayList<A2uiComponentDefinition>(components.size).apply {
            for (i in components.indices) {
                val component = components[i]
                add(
                    A2uiComponentDefinitionImpl(
                        name = component.name,
                        description = component.description,
                        properties = component.properties,
                    )
                )
            }
        }
    val componentDefinitionCollection = A2uiComponentDefinitionCollection(componentDefinitions)

    return A2uiCatalogImpl(
        id = catalogId,
        title = title,
        description = description,
        themeSchema = themeSchema,
        componentDefinitions = componentDefinitionCollection,
        components = componentCollection,
        functions = functionCollection,
        isInline = isInline,
    )
}

/**
 * Creates an [A2uiReadinessEvaluator] that resolves readiness states using the components
 * registered in this catalog.
 *
 * This evaluator delegates to each specific [A2uiComponent.isReady] implementation to determine if
 * a component is ready to transition from a loading state to a success state (e.g., has loaded all
 * its required dynamic data).
 *
 * @return An [A2uiReadinessEvaluator] backed by this catalog.
 */
public fun A2uiCatalog.asReadinessEvaluator(): A2uiReadinessEvaluator {
    return object : A2uiReadinessEvaluator {
        @Composable
        override fun isReady(componentModel: A2uiComponentModel): Boolean {
            val component =
                components[componentModel.type]
                    ?: throw IllegalStateException(
                        "Component with type '${componentModel.type}' is not registered"
                    )
            return with(component) { componentModel.scope.isReady(componentModel.properties) }
        }
    }
}

/**
 * Serializes this catalog to its JSON Schema string representation.
 *
 * @return the serialized catalog JSON Schema string
 */
public fun A2uiCatalog.toJsonSchemaString(): String =
    when (this) {
        is A2uiCatalogImpl -> serializer.jsonSchemaString
    }

/**
 * Serializes this catalog to its JSON Schema Map representation.
 *
 * @return the serialized catalog JSON Schema Map
 */
public fun A2uiCatalog.toJsonSchemaMap(): Map<String, Any?> =
    when (this) {
        is A2uiCatalogImpl -> serializer.jsonSchemaMap
    }

internal class A2uiCatalogImpl(
    override val id: String,
    override val title: String?,
    override val description: String?,
    override val themeSchema: A2uiSchema?,
    override val componentDefinitions: A2uiComponentDefinitionCollection,
    override val components: A2uiComponentCollection,
    override val functions: A2uiFunctionCollection,
    override val isInline: Boolean,
) : A2uiCatalog, A2uiInlineCatalog {
    val serializer: A2uiCoreCatalogDefinitionSerializer by
        lazy(LazyThreadSafetyMode.PUBLICATION) { A2uiCoreCatalogDefinitionSerializer(this) }

    override fun toJsonSchemaMap(): Map<String, Any?> = serializer.jsonSchemaMap

    override fun toJsonSchemaString(): String = serializer.jsonSchemaString
}

private class A2uiComponentDefinitionImpl(
    override val name: String,
    override val description: String,
    properties: List<A2uiProperty<*>>,
) : A2uiComponentDefinition {

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
