/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator.poet

internal data class AidlMethodSpec(val name: String, val parameters: List<AidlParameterSpec>) {
    override fun toString() = "void $name(${parameters.joinToString(", ")});"

    class Builder(val name: String) {
        val parameters = mutableListOf<AidlParameterSpec>()

        fun addParameter(parameter: AidlParameterSpec) {
            parameters.add(parameter)
        }

        fun addParameter(name: String, type: AidlTypeSpec, isIn: Boolean = false) {
            addParameter(AidlParameterSpec(name, type, isIn))
        }

        fun build() = AidlMethodSpec(name, parameters)
    }
}