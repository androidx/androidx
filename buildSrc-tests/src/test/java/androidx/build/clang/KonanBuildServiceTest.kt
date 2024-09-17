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

package androidx.build.clang

import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class KonanBuildServiceTest : BaseClangTest() {
    private lateinit var buildService: KonanBuildService

    @Before
    fun initBuildService() {
        buildService = KonanBuildService.obtain(project).get()
    }

    @Test
    fun compilationFailure() {
        val compileParams =
            createCompileParameters(
                "failedCode.c",
                """
            #include <stdio.h>
            int main() {
               printf("Hello, World!");
               return 0 // no ; :)
            }
        """
                    .trimIndent()
            )
        assertThrows<GradleException> { buildService.compile(compileParams) }
            .hasMessageThat()
            .contains("expected ';' after return statement")
    }

    @Ignore // b/366529564
    @Test
    fun compile() {
        val compileParams = createCompileParameters("code.c", C_HELLO_WORLD)
        buildService.compile(compileParams)
        val outputFiles = compileParams.output.getRegularFiles()
        assertThat(outputFiles).hasSize(1)
        val outputFile = outputFiles.single()
        assertThat(outputFile.name).isEqualTo("code.o")
        val strings = extractStrings(outputFile)
        assertThat(strings).contains("Hello, World!!")
        // shouldn't link yet
        assertThat(strings).doesNotContain("libc")
    }

    @Ignore // b/366529564
    @Test
    fun compileWithInclude() {
        val compileParameters =
            createCompileParameters(
                "code.c",
                """
            #include <stdio.h>
            #include "dependency.h"
            int my_function() {
               return dependency_method();
            }
            """
                    .trimIndent()
            )
        val dependency =
            tmpFolder.newFolder("depSrc").also {
                it.resolve("dependency.h")
                    .writeText(
                        """
                    int dependency_method();
                """
                            .trimIndent()
                    )
            }
        compileParameters.includes.from(dependency)
        buildService.compile(compileParameters)
        val outputFiles = compileParameters.output.getRegularFiles()
        val strings = extractStrings(outputFiles.single())
        assertThat(strings).contains("dependency_method")
    }

    @Ignore // b/366529564
    @Test
    fun createSharedLibrary() {
        val compileParameters = createCompileParameters("code.c", C_HELLO_WORLD)
        buildService.compile(compileParameters)
        val sharedLibraryParameters =
            project.objects.newInstance(ClangSharedLibraryParameters::class.java)
        sharedLibraryParameters.konanTarget.set(compileParameters.konanTarget)
        sharedLibraryParameters.objectFiles.from(compileParameters.output)
        val outputFile = tmpFolder.newFile("code.so")
        sharedLibraryParameters.outputFile.set(outputFile)
        buildService.createSharedLibrary(sharedLibraryParameters)

        val strings = extractStrings(outputFile)
        assertThat(strings).contains("Hello, World!!")
        // should link with libc
        assertThat(strings).contains("libc")

        // verify shared lib files are aligned to 16Kb boundary for Android targets
        if (sharedLibraryParameters.konanTarget.get().asKonanTarget.family == Family.ANDROID) {
            val alignment =
                ProcessBuilder("objdump", "-p", outputFile.path)
                    .start()
                    .inputStream
                    .bufferedReader()
                    .useLines { lines ->
                        lines
                            .filter { it.contains("LOAD") }
                            .map { it.split(" ").last() }
                            .firstOrNull()
                    }
            assertThat(alignment).isEqualTo("2**14")
        }
    }

    @Ignore // b/366529564
    @Test
    fun archive() {
        val compileParams = createCompileParameters("code.c", C_HELLO_WORLD)
        buildService.compile(compileParams)
        val archiveParams = project.objects.newInstance(ClangArchiveParameters::class.java)
        archiveParams.konanTarget.set(compileParams.konanTarget)
        archiveParams.objectFiles.from(compileParams.output)
        val outputFile = tmpFolder.newFile("code.a")
        archiveParams.outputFile.set(outputFile)
        buildService.archiveLibrary(archiveParams)

        val strings = extractStrings(outputFile)
        assertThat(strings).contains("Hello, World!!")
        // should not with libc
        assertThat(strings).doesNotContain("libc")
    }

    private fun createCompileParameters(fileName: String, code: String): ClangCompileParameters {
        val srcDir = tmpFolder.newFolder("src")
        srcDir.resolve(fileName).writeText(code)
        val compileParams = project.objects.newInstance(ClangCompileParameters::class.java)
        compileParams.konanTarget.set(SerializableKonanTarget(KonanTarget.LINUX_X64))
        compileParams.output.set(tmpFolder.newFolder())
        compileParams.sources.from(srcDir)
        return compileParams
    }

    private fun DirectoryProperty.getRegularFiles() =
        get().asFile.walkTopDown().filter { it.isFile }.toList()

    /** Extract strings from a binary file so that we can assert output contents. */
    private fun extractStrings(file: File): String {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val response =
            project.exec {
                it.executable = "strings"
                it.setErrorOutput(errorStream)
                it.setStandardOutput(outputStream)
                it.isIgnoreExitValue = true
                it.args(file.canonicalPath)
            }
        if (response.exitValue != 0) {
            throw AssertionError(
                """
                Couldn't read strings from file.
                Output:
                ${outputStream.toString(Charsets.UTF_8)}
                Error:
                ${errorStream.toString(Charsets.UTF_8)}
            """
                    .trimIndent()
            )
        }
        return outputStream.toString(Charsets.UTF_8)
    }

    companion object {
        private val C_HELLO_WORLD =
            """
            #include <stdio.h>
            int my_function() {
               printf("Hello, World!!");
               return 0;
            }
        """
                .trimIndent()
    }
}
