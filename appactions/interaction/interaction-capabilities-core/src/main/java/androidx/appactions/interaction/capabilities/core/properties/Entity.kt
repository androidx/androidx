/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.appactions.interaction.capabilities.core.properties

/**
 * Entities are used when defining ActionCapability for defining possible values for ParamProperty.
 */
class Entity internal constructor(
    val id: String?,
    val name: String,
    val alternateNames: List<String>,
) {
    /** Builder class for Entity. */
    class Builder {
        private var id: String? = null
        private var name: String? = null
        private var alternateNames: List<String> = listOf()

        /** Sets the id of the Entity to be built. */
        fun setId(id: String) = apply {
            this.id = id
        }

        /** Sets the name of the Entity to be built. */
        fun setName(name: String) = apply {
            this.name = name
        }

        /** Sets the list of alternate names of the Entity to be built. */

        fun setAlternateNames(alternateNames: List<String>) = apply {
            this.alternateNames = alternateNames
        }

        /** Sets the list of alternate names of the Entity to be built. */

        fun setAlternateNames(vararg alternateNames: String) = setAlternateNames(
            alternateNames.asList(),
        )

        /** Builds and returns an Entity. */
        fun build() = Entity(
            id,
            requireNotNull(name, {
                "setName must be called before build"
            }),
            alternateNames,
        )
    }
}
