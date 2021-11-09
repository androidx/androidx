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

package androidx.room

import androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room.processor.Context.BooleanProcessorOptions.USE_NULL_AWARE_CONVERTER
import androidx.room.processor.ProcessorErrors
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import javax.tools.Diagnostic

/**
 * Entry point for processing using KSP.
 */
class RoomKspProcessor(
    environment: SymbolProcessorEnvironment
) : KspBasicAnnotationProcessor(environment) {
    init {
        // print a warning if null aware converter is disabled because we'll remove that ability
        // soon.
        if (USE_NULL_AWARE_CONVERTER.getInputValue(xProcessingEnv) == false) {
            xProcessingEnv.messager.printMessage(
                kind = Diagnostic.Kind.WARNING,
                msg = """
                    Disabling null-aware type analysis in KSP is a temporary flag that will be
                    removed in a future release.
                    If the null-aware type analysis is causing a bug in your application,
                    please file a bug at ${ProcessorErrors.ISSUE_TRACKER_LINK} with
                    a sample app that reproduces your problem.
                """.trimIndent()
            )
        }
    }
    override fun processingSteps() = listOf(
        DatabaseProcessingStep()
    )

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return RoomKspProcessor(environment)
        }
    }
}