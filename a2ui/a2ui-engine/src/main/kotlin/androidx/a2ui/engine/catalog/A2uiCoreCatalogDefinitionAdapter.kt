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

@file:JvmName("A2uiCoreCatalogDefinitionAdapterKt")

package androidx.a2ui.engine.catalog

import androidx.a2ui.model.catalog.A2uiCatalogDefinition
import androidx.a2ui.model.protocol.A2uiInlineCatalog

/**
 * Adapts this [A2uiCatalogDefinition] into an [A2uiInlineCatalog] for use in client capabilities
 * advertisement.
 *
 * @throws IllegalStateException If [A2uiCatalogDefinition.isInline] is false.
 */
public fun A2uiCatalogDefinition.toInlineCatalog(): A2uiInlineCatalog {
    check(isInline) {
        "Cannot adapt A2uiCatalogDefinition(id='$id') to A2uiInlineCatalog because isInline is false."
    }
    return this as? A2uiInlineCatalog ?: A2uiCoreCatalogDefinitionInlineAdapter(this)
}

internal class A2uiCoreCatalogDefinitionInlineAdapter(private val catalog: A2uiCatalogDefinition) :
    A2uiInlineCatalog {
    private val serializer: A2uiCoreCatalogDefinitionSerializer by
        lazy(LazyThreadSafetyMode.PUBLICATION) { A2uiCoreCatalogDefinitionSerializer(catalog) }

    override val id: String
        get() = catalog.id

    override fun toJsonSchemaMap(): Map<String, Any?> = serializer.jsonSchemaMap

    override fun toJsonSchemaString(): String = serializer.jsonSchemaString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is A2uiCoreCatalogDefinitionInlineAdapter) return false
        return catalog == other.catalog
    }

    override fun hashCode(): Int = catalog.hashCode()

    override fun toString(): String = "A2uiInlineCatalog(id=$id)"
}
