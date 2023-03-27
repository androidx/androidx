/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.properties

/**
 * A simple property which describes a parameter for {@code ActionCapability}. This property has
 * simple configurations available and is not tied to a specific type.
 */
class SimpleProperty internal constructor(
    override val isRequired: Boolean,
) : ParamProperty<Void> {

    override val possibleValues = emptyList<Void>()
    override val isValueMatchRequired = false
    override val isProhibited = false

    /** Builder for {@link SimpleProperty}. */
    class Builder {

        private var isRequired = false

        /** Sets whether or not this property requires a value for fulfillment. */
        fun setRequired(isRequired: Boolean) = apply {
            this.isRequired = isRequired
        }

        /** Builds the property for this string parameter. */
        fun build() = SimpleProperty(isRequired)
    }
}
