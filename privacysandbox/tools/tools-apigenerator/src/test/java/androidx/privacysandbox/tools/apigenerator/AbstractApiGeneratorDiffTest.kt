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
import androidx.privacysandbox.tools.testing.AbstractDiffTest
import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertCompiles
import androidx.privacysandbox.tools.testing.TestEnvironment
import androidx.privacysandbox.tools.testing.loadSourcesFromDirectory
import androidx.room.compiler.processing.util.Source
import java.nio.file.Path
import org.junit.Test

/**
 * Base test for API Compiler diff test. It calls the API Packager to generate true-to-production
 * SDK API descriptors, invokes the generator and compiles the generated sources.
 */
abstract class AbstractApiGeneratorDiffTest : AbstractDiffTest() {

    override fun generateSources(
        inputSources: List<Source>,
        outputDirectory: Path
    ): List<Source> {
        val descriptors =
            compileIntoInterfaceDescriptorsJar(
                inputSources,
                mapOf(Metadata.filePath to Metadata.toolMetadata.toByteArray())
            )
        val generator = PrivacySandboxApiGenerator()
        generator.generate(
            descriptors,
            TestEnvironment.aidlCompilerPath, TestEnvironment.frameworkAidlPath, outputDirectory)
        return loadSourcesFromDirectory(outputDirectory.toFile())
    }

    @Test
    fun generatedSourcesCompile() {
        assertCompiles(generatedSources)
    }
}