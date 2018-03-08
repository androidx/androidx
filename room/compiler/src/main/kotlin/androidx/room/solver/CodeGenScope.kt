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

import androidx.room.writer.ClassWriter
import com.google.common.annotations.VisibleForTesting
import com.squareup.javapoet.CodeBlock

/**
 * Defines a code generation scope where we can provide temporary variables, global variables etc
 */
class CodeGenScope(val writer: ClassWriter) {
    private var tmpVarIndices = mutableMapOf<String, Int>()
    private var builder: CodeBlock.Builder? = null
    companion object {
        const val TMP_VAR_DEFAULT_PREFIX = "_tmp"
        const val CLASS_PROPERTY_PREFIX = "__"
        @VisibleForTesting
        fun _tmpVar(index: Int) = _tmpVar(TMP_VAR_DEFAULT_PREFIX, index)
        fun _tmpVar(prefix: String, index: Int) = "$prefix${if (index == 0) "" else "_$index"}"
    }

    fun builder(): CodeBlock.Builder {
        if (builder == null) {
            builder = CodeBlock.builder()
        }
        return builder!!
    }

    fun getTmpVar(): String {
        return getTmpVar(TMP_VAR_DEFAULT_PREFIX)
    }

    fun getTmpVar(prefix: String): String {
        if (!prefix.startsWith("_")) {
            throw IllegalArgumentException("tmp variable prefixes should start with _")
        }
        if (prefix.startsWith(CLASS_PROPERTY_PREFIX)) {
            throw IllegalArgumentException("cannot use $CLASS_PROPERTY_PREFIX for tmp variables")
        }
        val index = tmpVarIndices.getOrElse(prefix) { 0 }
        val result = _tmpVar(prefix, index)
        tmpVarIndices.put(prefix, index + 1)
        return result
    }

    fun generate() = builder().build().toString()

    /**
     * copies all variable indices but excludes generated code.
     */
    fun fork(): CodeGenScope {
        val forked = CodeGenScope(writer)
        forked.tmpVarIndices.putAll(tmpVarIndices)
        return forked
    }
}
