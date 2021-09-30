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

import androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room.compiler.processing.util.XTestInvocation
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment

@ExperimentalProcessingApi
class SyntheticKspProcessor private constructor(
    symbolProcessorEnvironment: SymbolProcessorEnvironment,
    private val impl: SyntheticProcessorImpl
) : KspBasicAnnotationProcessor(symbolProcessorEnvironment), SyntheticProcessor by impl {
    constructor(
        symbolProcessorEnvironment: SymbolProcessorEnvironment,
        handlers: List<(XTestInvocation) -> Unit>
    ) : this(
        symbolProcessorEnvironment,
        SyntheticProcessorImpl(handlers)
    )

    override fun processingSteps(): Iterable<XProcessingStep> = impl.processingSteps()

    override fun postRound(env: XProcessingEnv, round: XRoundEnv) {
        if (!round.isProcessingOver) {
            impl.postRound(env, round)
        }
    }
}
