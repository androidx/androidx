/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.XTestInvocation
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor

class SyntheticKspProcessor(
    private val handler: (XTestInvocation) -> Unit
) : SymbolProcessor {
    private var result: Result<Unit>? = null
    private lateinit var options: Map<String, String>
    private lateinit var codeGenerator: CodeGenerator
    private lateinit var logger: KSPLogger
    override fun finish() {
    }

    override fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        this.options = options
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    override fun process(resolver: Resolver) {
        result = kotlin.runCatching {
            handler(
                XTestInvocation(
                    processingEnv = XProcessingEnv.create(
                        options,
                        resolver,
                        codeGenerator,
                        logger
                    )
                )
            )
        }
    }

    fun throwIfFailed() {
        val result = checkNotNull(result) {
            "did not compile"
        }
        if (result.isFailure) {
            throw result.exceptionOrNull()!!
        }
    }
}
