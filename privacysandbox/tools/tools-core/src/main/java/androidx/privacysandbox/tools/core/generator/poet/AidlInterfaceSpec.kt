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

/** AIDL file with a single interface. */
internal data class AidlInterfaceSpec(
    override val type: Type,
    val methods: List<String>,
    val oneway: Boolean = true,
    override val typesToImport: Set<Type> = emptySet(),
) : AidlFileSpec {
    override val innerContent: String
        get() {
            val modifiers = if (oneway) "oneway " else ""
            val body = methods.sorted().joinToString("\n|    ")
            return """
                |${modifiers}interface ${type.simpleName} {
                |    $body
                |}
            """.trimMargin()
        }
}