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

package androidx.room3

import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.XRoundEnv
import androidx.room3.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room3.verifier.DatabaseVerifier
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/** Entry point for processing using KSP. */
class RoomProcessor(environment: SymbolProcessorEnvironment) :
    KspBasicAnnotationProcessor(
        symbolProcessorEnvironment = environment,
        config = DatabaseProcessingStep.getEnvConfig(environment.options),
    ) {

    override fun processingSteps() = listOf(DatabaseProcessingStep())

    override fun postRound(env: XProcessingEnv, round: XRoundEnv) {
        if (round.isProcessingOver) {
            DatabaseVerifier.cleanup()
        }
    }

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return RoomProcessor(environment)
        }
    }
}
