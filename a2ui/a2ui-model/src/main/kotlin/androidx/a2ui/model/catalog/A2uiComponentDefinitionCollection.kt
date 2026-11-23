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

@file:JvmName("A2uiComponentDefinitionCollectionKt")

package androidx.a2ui.model.catalog

import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap

/**
 * An immutable, indexed collection of [A2uiComponentDefinition]s registered within an
 * [A2uiCatalogDefinition].
 *
 * Provides list-based access that enables iterator-free iteration using indices, as well as lookup
 * by component name using the index operator.
 */
public sealed interface A2uiComponentDefinitionCollection : List<A2uiComponentDefinition> {
    /**
     * Retrieves a component definition by its unique name.
     *
     * @param name The name of the component.
     * @return The component definition, or `null` if it is not registered in this collection.
     */
    public operator fun get(name: String): A2uiComponentDefinition?
}

/**
 * Creates an immutable [A2uiComponentDefinitionCollection] from a list of
 * [A2uiComponentDefinition]s.
 *
 * @param definitions The list of component definitions to include.
 * @return An initialized [A2uiComponentDefinitionCollection].
 * @throws IllegalArgumentException If duplicate component definition names are detected.
 */
@JvmOverloads
public fun A2uiComponentDefinitionCollection(
    definitions: List<A2uiComponentDefinition> = emptyList()
): A2uiComponentDefinitionCollection {
    if (definitions.isEmpty()) {
        return A2uiComponentDefinitionCollectionImpl(emptyList(), emptyScatterMap())
    }
    val safeDefinitions = ArrayList(definitions)
    val map = MutableScatterMap<String, A2uiComponentDefinition>(safeDefinitions.size)
    for (i in safeDefinitions.indices) {
        val def = safeDefinitions[i]
        val name = def.name
        require(map.put(name, def) == null) {
            "Duplicate component definition registered for name '$name'."
        }
    }
    return A2uiComponentDefinitionCollectionImpl(safeDefinitions, map)
}

internal class A2uiComponentDefinitionCollectionImpl(
    private val definitions: List<A2uiComponentDefinition>,
    private val definitionMap: ScatterMap<String, A2uiComponentDefinition>,
) : A2uiComponentDefinitionCollection {

    override val size: Int
        get() = definitions.size

    override fun isEmpty(): Boolean = definitions.isEmpty()

    override fun contains(element: A2uiComponentDefinition): Boolean = definitions.contains(element)

    override fun containsAll(elements: Collection<A2uiComponentDefinition>): Boolean =
        definitions.containsAll(elements)

    override fun get(index: Int): A2uiComponentDefinition = definitions[index]

    override fun get(name: String): A2uiComponentDefinition? = definitionMap[name]

    override fun indexOf(element: A2uiComponentDefinition): Int = definitions.indexOf(element)

    override fun lastIndexOf(element: A2uiComponentDefinition): Int =
        definitions.lastIndexOf(element)

    override fun iterator(): Iterator<A2uiComponentDefinition> = definitions.iterator()

    override fun listIterator(): ListIterator<A2uiComponentDefinition> = definitions.listIterator()

    override fun listIterator(index: Int): ListIterator<A2uiComponentDefinition> =
        definitions.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<A2uiComponentDefinition> =
        definitions.subList(fromIndex, toIndex)

    override fun equals(other: Any?): Boolean = definitions == other

    override fun hashCode(): Int = definitions.hashCode()

    override fun toString(): String = definitions.toString()
}
