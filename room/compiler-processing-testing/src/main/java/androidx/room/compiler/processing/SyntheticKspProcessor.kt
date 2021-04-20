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
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated

@ExperimentalProcessingApi
class SyntheticKspProcessor private constructor(
    private val impl: SyntheticProcessorImpl
) : SymbolProcessor, SyntheticProcessor by impl {
    constructor(handlers: List<(XTestInvocation) -> Unit>) : this(
        SyntheticProcessorImpl(handlers)
    )

    private lateinit var options: Map<String, String>
    private lateinit var codeGenerator: CodeGenerator
    private lateinit var logger: KSPLogger

    override fun finish() {
    }

    fun internalInit(
        options: Map<String, String>,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        this.options = options
        this.codeGenerator = codeGenerator
        this.logger = logger
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!impl.canRunAnotherRound()) {
            return emptyList()
        }
        val xEnv = XProcessingEnv.create(
            options,
            resolver,
            codeGenerator,
            logger,
            XProcessingEnv.Language.JAVA
        )
        val testInvocation = XTestInvocation(
            processingEnv = xEnv,
            roundEnv = XRoundEnv.create(xEnv)
        )
        impl.runNextRound(testInvocation)
        return emptyList()
    }

    internal fun asProvider(): SymbolProcessorProvider = Provider(this)

    private class Provider(
        private val delegate: SyntheticKspProcessor
    ) : SymbolProcessorProvider {
        override fun create(
            options: Map<String, String>,
            kotlinVersion: KotlinVersion,
            codeGenerator: CodeGenerator,
            logger: KSPLogger
        ): SymbolProcessor {
            delegate.internalInit(
                options = options,
                codeGenerator = codeGenerator,
                logger = logger
            )
            return delegate
        }
    }
}
