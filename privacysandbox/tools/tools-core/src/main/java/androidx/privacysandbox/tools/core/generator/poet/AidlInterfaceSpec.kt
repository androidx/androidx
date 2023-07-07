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
import java.lang.IllegalStateException

/** AIDL file with a single interface. */
internal data class AidlInterfaceSpec(
    override val type: Type,
    val methods: List<AidlMethodSpec>,
) : AidlFileSpec {
    companion object {
        fun aidlInterface(
            type: Type,
            block: Builder.() -> Unit = {}
        ): AidlInterfaceSpec {
            return Builder(type).also(block).build()
        }
    }

    override val typesToImport: Set<Type>
        get() {
            return methods.flatMap { method ->
                method.parameters.map { it.type }
                    .filter { it.requiresImport && it.innerType != type }.map { it.innerType }
            }.toSet()
        }

    override val innerContent: String
        get() {
            val body = methods.map { it.toString() }.sorted().joinToString("\n|    ")
            return """
                |oneway interface ${type.simpleName} {
                |    $body
                |}
            """.trimMargin()
        }

    class Builder(val type: Type) {
        private val methods = mutableListOf<AidlMethodSpec>()

        fun addMethod(method: AidlMethodSpec) {
            val methodsByTxId = methods.associateBy(AidlMethodSpec::transactionId)
            methodsByTxId[method.transactionId]?.let { conflictMethod ->
                throw IllegalStateException(
                    // TODO(b/271114359): Update this error message when manual IDs are possible
                    "Methods '${method.name}' and '${conflictMethod.name}' in interface " +
                        "'${type.simpleName}' have the same AIDL transaction ID. Please " +
                        "change one of the methods' name or type signature."
                )
            }
            methods.add(method)
        }

        fun addMethod(name: String, block: AidlMethodSpec.Builder.() -> Unit = {}) {
            addMethod(AidlMethodSpec.Builder(name).also(block).build())
        }

        fun build() = AidlInterfaceSpec(type, methods)
    }
}
