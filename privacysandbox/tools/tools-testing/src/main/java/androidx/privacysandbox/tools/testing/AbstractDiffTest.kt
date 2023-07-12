/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.tools.testing

import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.Test

/** Base test class for diff testing Privacy Sandbox tool output. */
abstract class AbstractDiffTest {
    /** Name for the subdirectory used to read input and expected sources. */
    abstract val subdirectoryName: String

    /**
     * List of relative paths to expected AIDL files. We will assert that they are present in the
     * final output but we won't check their contents.
     */
    abstract val relativePathsToExpectedAidlClasses: List<String>

    /**
     * Generates the sources and stores them in the given [outputDirectory].
     * @param inputSources List of input sources read from the test-data directory with
     * [subdirectoryName].
     */
    abstract fun generateSources(
        inputSources: List<Source>,
        outputDirectory: Path,
    ): List<Source>

    protected val generatedSources: List<Source> by lazy {
        val inputSources =
            loadSourcesFromDirectory(File("src/test/test-data/$subdirectoryName/input"))
        outputDir.toFile().also {
            if (it.exists()) {
                it.deleteRecursively()
            }
            it.mkdirs()
        }
        generateSources(inputSources, outputDir)
    }

    @Test
    fun generatedSourcesHaveExpectedContents() {
        val expectedSourcesPath = "src/test/test-data/$subdirectoryName/output"
        val expectedKotlinSources =
            loadSourcesFromDirectory(File(expectedSourcesPath))

        val expectedRelativePaths =
            expectedKotlinSources.map(Source::relativePath) + relativePathsToExpectedAidlClasses
        Truth.assertThat(generatedSources.map(Source::relativePath))
            .containsExactlyElementsIn(expectedRelativePaths)

        val actualRelativePathMap = generatedSources.associateBy(Source::relativePath)
        for (expectedKotlinSource in expectedKotlinSources) {
            val outputFilePath = "$outputDir/${expectedKotlinSource.relativePath}"
            val goldenPath = System.getProperty("user.dir") + "/" + expectedSourcesPath + "/" +
                expectedKotlinSource.relativePath
            Truth.assertWithMessage(
                "Contents of generated file ${expectedKotlinSource.relativePath} don't " +
                    "match golden.\n" +
                    "Approval command:\n" +
                    "cp $outputFilePath $goldenPath"
            ).that(actualRelativePathMap[expectedKotlinSource.relativePath]?.contents)
                .isEqualTo(expectedKotlinSource.contents)
        }
    }

    private val outputDir: Path by lazy {
        requireNotNull(System.getProperty("test_output_dir")) {
            "test_output_dir not set for diff test."
        }.let { Path(it).resolve(subdirectoryName) }
    }
}