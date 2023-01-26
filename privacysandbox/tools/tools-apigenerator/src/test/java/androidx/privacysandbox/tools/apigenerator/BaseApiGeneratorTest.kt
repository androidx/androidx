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

package androidx.privacysandbox.tools.apigenerator

import androidx.privacysandbox.tools.core.Metadata
import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertCompiles
import androidx.privacysandbox.tools.testing.hasAllExpectedGeneratedSourceFilesAndContent
import androidx.privacysandbox.tools.testing.loadSourcesFromDirectory
import androidx.room.compiler.processing.util.Source
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import org.junit.Test

abstract class BaseApiGeneratorTest {
    abstract val inputDirectory: File
    abstract val outputDirectory: File
    abstract val relativePathsToExpectedAidlClasses: List<String>

    private val generatedSources: List<Source> by lazy {
        val descriptors =
            compileIntoInterfaceDescriptorsJar(
                loadSourcesFromDirectory(inputDirectory),
                mapOf(Metadata.filePath to Metadata.toolMetadata.toByteArray())
            )
        val aidlPath = System.getProperty("aidl_compiler_path")?.let(::Path)
            ?: throw IllegalArgumentException("aidl_compiler_path flag not set.")

        val generator = PrivacySandboxApiGenerator()

        val outputDir = Files.createTempDirectory("output").also { it.toFile().deleteOnExit() }
        generator.generate(descriptors, aidlPath, outputDir)
        loadSourcesFromDirectory(outputDir.toFile())
    }

    @Test
    fun generatedApi_compiles() {
        assertCompiles(generatedSources)
    }

    @Test
    fun generatedApi_hasExpectedContents() {
        val expectedKotlinSources = loadSourcesFromDirectory(outputDirectory)
        hasAllExpectedGeneratedSourceFilesAndContent(
            generatedSources,
            expectedKotlinSources,
            relativePathsToExpectedAidlClasses
        )
    }
}