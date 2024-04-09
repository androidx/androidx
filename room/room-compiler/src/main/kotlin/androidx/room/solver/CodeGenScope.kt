/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.solver

import androidx.annotation.VisibleForTesting
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.writer.TypeWriter

/**
 * Defines a code generation scope where we can provide temporary variables, global variables etc
 */
class CodeGenScope(
    val writer: TypeWriter,
    // TODO(b/319660042): Remove once migration to driver API is done.
    val useDriverApi: Boolean = false
) {
    val language = writer.codeLanguage
    val builder by lazy { XCodeBlock.builder(language) }
    private val tmpVarIndices = mutableMapOf<String, Int>()

    companion object {
        const val TMP_VAR_DEFAULT_PREFIX = "_tmp"
        const val CLASS_PROPERTY_PREFIX = "__"

        @VisibleForTesting
        fun getTmpVarString(index: Int) =
            getTmpVarString(TMP_VAR_DEFAULT_PREFIX, index)

        private fun getTmpVarString(prefix: String, index: Int) =
            "$prefix${if (index == 0) "" else "_$index"}"
    }

    fun getTmpVar(): String {
        return getTmpVar(TMP_VAR_DEFAULT_PREFIX)
    }

    fun getTmpVar(prefix: String): String {
        require(prefix.startsWith("_")) { "Tmp variable prefixes should start with '_'." }
        require(!prefix.startsWith(CLASS_PROPERTY_PREFIX)) {
            "Cannot use '$CLASS_PROPERTY_PREFIX' for tmp variables."
        }
        val index = tmpVarIndices.getOrElse(prefix) { 0 }
        val result = getTmpVarString(prefix, index)
        tmpVarIndices[prefix] = index + 1
        return result
    }

    fun generate(): XCodeBlock = builder.build()

    /**
     * Copies all variable indices but excludes generated code.
     */
    fun fork(): CodeGenScope {
        val forked = CodeGenScope(writer, useDriverApi)
        forked.tmpVarIndices.putAll(tmpVarIndices)
        return forked
    }
}
