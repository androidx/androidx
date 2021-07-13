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
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@Suppress("VisibleForTests")
@ExperimentalProcessingApi
class SyntheticJavacProcessor private constructor(
    private val impl: SyntheticProcessorImpl
) : AbstractProcessor(), SyntheticProcessor by impl {
    constructor(handlers: List<(XTestInvocation) -> Unit>) : this(
        SyntheticProcessorImpl(handlers)
    )

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        if (roundEnv.processingOver()) {
            return true
        }
        if (!impl.canRunAnotherRound()) {
            return true
        }
        val xEnv = XProcessingEnv.create(processingEnv)
        val xRoundEnv = XRoundEnv.create(xEnv, roundEnv)
        val testInvocation = XTestInvocation(
            processingEnv = xEnv,
            roundEnv = xRoundEnv
        )
        impl.runNextRound(testInvocation)
        return impl.expectsAnotherRound()
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun getSupportedAnnotationTypes() = setOf("*")
}