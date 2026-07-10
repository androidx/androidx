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

package androidx.a2ui.engine.catalog

import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.schema.A2uiSchema

/** A registry of UI components and functions that a client surface can render. */
public interface A2uiCoreCatalog {
    /**
     * A unique identifier for this catalog. Ideally, this is a URI including a version number. Full
     * guidelines regarding catalog id convention can be found in:
     * https://a2ui.org/concepts/catalogs/#catalog-naming-versioning
     */
    public val id: String

    /** The list of components available in this catalog. */
    public val components: List<A2uiCoreComponentDefinition>

    /** The list of functions available in this catalog. */
    public val functions: List<A2uiFunction>

    /** The schema this catalog uses to define the theme the is applied over the components. */
    public val themeSchema: A2uiSchema?

    /**
     * Retrieves a component definition by its unique name.
     *
     * @param name The name of the component.
     * @return The component definition, or `null` if it is not registered in this catalog.
     */
    public fun getComponent(name: String): A2uiCoreComponentDefinition?

    /**
     * Retrieves a function implementation by its unique name.
     *
     * @param name The name of the function.
     * @return The function implementation, or `null` if it is not registered in this catalog.
     */
    public fun getFunction(name: String): A2uiFunction?
}
