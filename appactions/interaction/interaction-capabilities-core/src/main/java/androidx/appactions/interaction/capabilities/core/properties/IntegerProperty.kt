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

/** The property which describes an integer parameter for {@code ActionCapability}. */
class IntegerProperty internal constructor(override val isRequired: Boolean) : ParamProperty<Int> {
    override val possibleValues = listOf<Int>()
    override val isProhibited = false
    override val isValueMatchRequired = false

    /** Builder for {@link IntegerProperty}. */
    class Builder {
        private var isRequired: Boolean = false

        /** Sets whether this property is required for fulfillment. */
        fun setRequired(isRequired: Boolean) = apply {
            this.isRequired = isRequired
        }

        /** Builds the property for this integer parameter. */
        fun build() =
            IntegerProperty(
                isRequired,
            ) }
}
