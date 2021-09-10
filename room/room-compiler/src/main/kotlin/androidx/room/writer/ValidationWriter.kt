/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.writer

import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterSpec

/**
 * Common interface for database validation witters.
 */
abstract class ValidationWriter {

    private lateinit var countingScope: CountingCodeGenScope

    fun write(dbParam: ParameterSpec, scope: CodeGenScope) {
        countingScope = CountingCodeGenScope(scope)
        write(dbParam, countingScope)
    }

    protected abstract fun write(dbParam: ParameterSpec, scope: CountingCodeGenScope)

    /**
     * The estimated amount of statements this writer will write.
     */
    fun statementCount() = countingScope.statementCount()

    protected class CountingCodeGenScope(val scope: CodeGenScope) {

        private var builder: CodeBlockWrapper? = null

        fun getTmpVar(prefix: String) = scope.getTmpVar(prefix)

        fun builder(): CodeBlockWrapper {
            if (builder == null) {
                builder = CodeBlockWrapper(scope.builder())
            }
            return builder!!
        }

        fun statementCount() = builder?.statementCount ?: 0
    }

    // A wrapper class that counts statements added to a CodeBlock
    protected class CodeBlockWrapper(val builder: CodeBlock.Builder) {

        var statementCount = 0
            private set

        fun addStatement(format: String, vararg args: Any?): CodeBlockWrapper {
            statementCount++
            builder.addStatement(format, *args)
            return this
        }

        fun beginControlFlow(controlFlow: String, vararg args: Any): CodeBlockWrapper {
            statementCount++
            builder.beginControlFlow(controlFlow, *args)
            return this
        }

        fun endControlFlow(): CodeBlockWrapper {
            builder.endControlFlow()
            return this
        }
    }
}