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

import com.google.common.hash.Hashing

private val AIDL_MAX_TRANSACTION_ID = 16777114UL

/** The number of transaction IDs that we reserve for our own purposes. */
private val RESERVED_ID_COUNT = 100UL

private val JAVA_MAX_METHOD_COUNT = 65535UL

internal data class AidlMethodSpec(
    val name: String,
    val parameters: List<AidlParameterSpec>,
    val transactionId: Int,
) {
    constructor(name: String, parameters: List<AidlParameterSpec>) : this(
        name,
        parameters,
        aidlTransactionId(name, parameters)
    )

    override fun toString(): String {
        val params = parameters.joinToString(", ")

        return "void $name($params) = $transactionId;"
    }

    class Builder(val name: String) {
        private val parameters = mutableListOf<AidlParameterSpec>()
        private var transactionId: Int? = null

        fun addParameter(parameter: AidlParameterSpec) {
            parameters.add(parameter)
        }

        fun addParameter(name: String, type: AidlTypeSpec) {
            addParameter(AidlParameterSpec(name, type, isIn = type.isList || type.isParcelable))
        }

        fun build(): AidlMethodSpec {
            val txId = transactionId
            if (txId == null) {
                return AidlMethodSpec(name, parameters)
            }
            // TODO(b/271114359): Add special handling for manually-supplied transaction IDs
            return AidlMethodSpec(name, parameters, txId)
        }
    }
}

// This method must remain backwards-compatible to ensure SDK compatibility.
internal fun aidlTransactionId(name: String, parameters: List<AidlParameterSpec>): Int {
    val hash = Hashing.farmHashFingerprint64().hashString(
        signature(name, parameters),
        Charsets.UTF_8,
    ).asLong().toULong()
    val maxValue = AIDL_MAX_TRANSACTION_ID - RESERVED_ID_COUNT - JAVA_MAX_METHOD_COUNT + 1UL

    // toInt is safe because $maxValue is well under 2^32 (in fact, under 2^24)
    return (hash % maxValue).toInt()
}

// This method must remain backwards-compatible to ensure SDK compatibility.
private fun signature(name: String, parameters: List<AidlParameterSpec>): String {
    val params = parameters.joinToString(",") { it.type.signature() }
    return "$name($params)"
}

// This method must remain backwards-compatible to ensure SDK compatibility.
private fun AidlTypeSpec.signature(): String =
    innerType.qualifiedName + (if (isList) "[]" else "")
