/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util.compiler.steps

import androidx.room.compiler.processing.util.compiler.KotlinCliRunner
import androidx.room.compiler.processing.util.compiler.Ksp1Compilation
import androidx.room.compiler.processing.util.compiler.Ksp2Compilation
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import org.jetbrains.kotlin.config.LanguageVersion

/** Runs KSP to run the Symbol Processors */
internal class KspCompilationStep(
    private val symbolProcessorProviders: List<SymbolProcessorProvider>,
    private val processorOptions: Map<String, String>
) : KotlinCompilationStep {
    override val name: String = "ksp"

    override fun execute(
        workingDir: File,
        arguments: CompilationStepArguments
    ): CompilationStepResult {
        val languageVersion = KotlinCliRunner.getLanguageVersion(arguments.kotlincArguments)
        return if (languageVersion < LanguageVersion.KOTLIN_2_0) {
            Ksp1Compilation(name, symbolProcessorProviders, processorOptions)
                .execute(workingDir, arguments)
        } else {
            Ksp2Compilation(name, symbolProcessorProviders, processorOptions)
                .execute(workingDir, arguments)
        }
    }
}
