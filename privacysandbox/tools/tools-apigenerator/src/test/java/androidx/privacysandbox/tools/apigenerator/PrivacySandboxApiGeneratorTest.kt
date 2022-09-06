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

import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrivacySandboxApiGeneratorTest {
    @Test
    fun annotatedInterface_isParsed() {
        val source =
            loadTestSource(inputTestDataDir, "com/mysdk/MySdk.kt")
        val descriptors = compileIntoInterfaceDescriptorsJar(source)

        val generator = PrivacySandboxApiGenerator()

        val outputDir = Files.createTempDirectory("output").also { it.toFile().deleteOnExit() }
        generator.generate(descriptors, Path.of(""), outputDir)

        assertOutputDirContainsSources(
            outputDir, listOf(
                loadTestSource(outputTestDataDir, "com/mysdk/MySdk.kt"),
                loadTestSource(outputTestDataDir, "com/mysdk/MySdkImpl.kt"),
            )
        )
    }

    private fun assertOutputDirContainsSources(outputDir: Path, expectedSources: List<Source>) {
        val outputMap =
            outputDir.toFile().walk().filter { it.isFile }.map {
                outputDir.relativize(it.toPath()).toString() to it.readText()
            }.toMap()
        val expectedSourcesMap = expectedSources.associate { it.relativePath to it.contents }
        assertCompiles(outputMap.map { (relativePath, contents) ->
            Source.kotlin(
                relativePath,
                contents
            )
        })
        // Not comparing the maps directly because the StringSubject error output is slightly
        // better than the binary assertion provided by the MapSubject.
        assertThat(outputMap.keys)
            .containsExactlyElementsIn(expectedSourcesMap.keys)
        for ((relativePath, sourceContent) in expectedSourcesMap) {
            assertThat(outputMap[relativePath]).isEqualTo(sourceContent)
        }
    }

    private fun loadTestSource(rootDir: File, relativePath: String): Source {
        val contents = rootDir.resolve(File(relativePath))
        return Source.loadKotlinSource(contents, relativePath)
    }
    companion object {
        val inputTestDataDir = File("src/test/data/input-src")
        val outputTestDataDir = File("src/test/data/output-src")
    }
}