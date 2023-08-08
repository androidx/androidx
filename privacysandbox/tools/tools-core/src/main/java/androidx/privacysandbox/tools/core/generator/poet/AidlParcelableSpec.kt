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

import androidx.privacysandbox.tools.core.model.Type

/** AIDL file with a single parcelable defined. */
internal data class AidlParcelableSpec(
    override val type: Type,
    val properties: List<AidlPropertySpec>,
) : AidlFileSpec {
    companion object {
        fun aidlParcelable(
            type: Type,
            block: Builder.() -> Unit = {}
        ): AidlParcelableSpec {
            return Builder(type).also(block).build()
        }
    }

    override val typesToImport: Set<Type>
        get() {
            return properties.map { it.type }.filter { it.requiresImport && it.innerType != type }
                .map { it.innerType }
                .toSet()
        }

    override val innerContent: String
        get() {
            val body = properties.map { it.toString() }.sorted().joinToString("\n|    ")
            return """
                |parcelable ${type.simpleName} {
                |    $body
                |}
                """.trimMargin()
        }

    class Builder(val type: Type) {
        val properties = mutableListOf<AidlPropertySpec>()

        fun addProperty(property: AidlPropertySpec) {
            properties.add(property)
        }

        fun addProperty(name: String, type: AidlTypeSpec, isNullable: Boolean = false) {
            addProperty(AidlPropertySpec(name, type, isNullable))
        }

        fun build() = AidlParcelableSpec(type, properties)
    }
}
