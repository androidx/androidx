/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.validator.ModelValidator
import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertCompiles
import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertWithMessage
import java.nio.file.Files
import kotlin.io.path.Path

internal object AidlTestHelper {
    fun runGenerator(api: ParsedApi): AidlGenerationOutput {
        ModelValidator.validate(api).also {
            assertWithMessage("Tried to generate AIDL code for invalid interface:\n" +
                it.errors.joinToString("\n")
            ).that(it.isSuccess).isTrue()
        }
        val tmpDir = Files.createTempDirectory("aidlGenerationTest")
        val aidlCompilerPath = System.getProperty("aidl_compiler_path")?.let(::Path)
            ?: throw IllegalArgumentException("aidl_compiler_path flag not set.")
        // TODO(b/269458005): Make this a required argument once the fallback is not needed
        val frameworkAidlPath = System.getProperty("framework_aidl_path")?.let(::Path)
        val aidlCompiler = AidlCompiler(aidlCompilerPath, frameworkAidlPath)

        val javaGeneratedSources = AidlGenerator.generate(aidlCompiler, api, tmpDir)
        assertCompiles(
            javaGeneratedSources.map {
                Source.java(
                    "${it.packageName.replace('.', '/')}/${it.interfaceName}",
                    it.file.readText()
                )
            }.toList()
        )

        val aidlGeneratedSources = tmpDir.toFile().walk().filter { it.extension == "aidl" }
            .map { AidlSource(tmpDir.relativize(it.toPath()).toString(), it.readText()) }.toList()
        return AidlGenerationOutput(aidlGeneratedSources, javaGeneratedSources)
    }
}

data class AidlSource(val relativePath: String, val content: String)
data class AidlGenerationOutput(
    val aidlSources: List<AidlSource>,
    val javaSources: List<GeneratedSource>,
)
