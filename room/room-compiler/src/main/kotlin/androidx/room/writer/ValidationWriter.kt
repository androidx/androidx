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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XTypeName
import androidx.room.solver.CodeGenScope

/**
 * Common interface for database validation witters.
 */
abstract class ValidationWriter {

    private lateinit var countingScope: CountingCodeGenScope

    fun write(dbParamName: String, scope: CodeGenScope) {
        countingScope = CountingCodeGenScope(scope)
        write(dbParamName, countingScope)
    }

    protected abstract fun write(dbParamName: String, scope: CountingCodeGenScope)

    /**
     * The estimated amount of statements this writer will write.
     */
    fun statementCount() = countingScope.statementCount()

    protected class CountingCodeGenScope(private val scope: CodeGenScope) {

        val builder = CodeBlockWrapper(scope.builder)

        fun getTmpVar(prefix: String) = scope.getTmpVar(prefix)

        fun statementCount() = builder.statementCount
    }

    // A wrapper class that counts statements added to a CodeBlock
    protected class CodeBlockWrapper(
        private val builder: XCodeBlock.Builder
    ) : XCodeBlock.Builder by builder {

        var statementCount = 0
            private set

        override fun add(format: String, vararg args: Any?): XCodeBlock.Builder {
            statementCount++
            builder.add(format, *args)
            return this
        }

        override fun addLocalVariable(
            name: String,
            typeName: XTypeName,
            isMutable: Boolean,
            assignExpr: XCodeBlock?
        ): XCodeBlock.Builder {
            statementCount++
            builder.addLocalVariable(name, typeName, isMutable, assignExpr)
            return this
        }

        override fun addStatement(format: String, vararg args: Any?): CodeBlockWrapper {
            statementCount++
            builder.addStatement(format, *args)
            return this
        }

        override fun beginControlFlow(controlFlow: String, vararg args: Any?): CodeBlockWrapper {
            statementCount++
            builder.beginControlFlow(controlFlow, *args)
            return this
        }

        override fun endControlFlow(): CodeBlockWrapper {
            builder.endControlFlow()
            return this
        }
    }
}
