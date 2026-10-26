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

package androidx.a2ui.compose.runtime

import androidx.compose.runtime.Immutable

/**
 * Identifies a child component to render, together with the data context it renders in.
 *
 * A reference pairs the [id] of a component in the surface with an optional [baseDataPath] that
 * scopes the component's data bindings, so that a single component definition can be rendered once
 * per item of a list. [A2uiComponentScope.bindChildReferences] produces references from a
 * [ChildListA2uiProperty]: a static list of component IDs yields one reference per ID with no
 * [baseDataPath], while a template bound to a data-model array (e.g. `{"path": "/items",
 * "componentId": "item"}`) yields one reference per array element, each scoped to that element
 * (`/items/0`, `/items/1`, …). A component renders a reference by observing its state with
 * [observeA2uiComponentState], and can construct one itself to render a component in a specific
 * data context.
 *
 * @sample androidx.a2ui.compose.runtime.samples.A2uiComponentReferenceSample
 * @property id The unique ID of the referenced component within the surface.
 * @property baseDataPath The data path against which the referenced component resolves its
 *   bindings, relative to the data context of the component that renders it, or absolute when it
 *   starts with `/`. `null` inherits the rendering component's data context.
 */
@Immutable
public class A2uiComponentReference(
    public val id: String,
    public val baseDataPath: String? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is A2uiComponentReference) return false

        if (id != other.id) return false
        if (baseDataPath != other.baseDataPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + baseDataPath.hashCode()
        return result
    }

    override fun toString(): String {
        return "ComponentReference(id='$id', baseDataPath='$baseDataPath')"
    }
}
