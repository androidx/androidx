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
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.jetbrains.kotlin.konan.file.use
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import org.junit.Before
import org.junit.Test

class ClangBuildServiceTest : BaseClangTest() {
    private lateinit var buildService: ClangBuildService

    @Before
    fun initBuildService() {
        buildService = ClangBuildService.obtain(project).get()
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
                    .trimIndent(),
            )
        assertThrows<GradleException> { buildService.compile(compileParams) }
            .hasMessageThat()
            .contains("expected ';' after return statement")
    }

    @Test
    fun invalidTarget() {
        val compileParams = createCompileParameters("code.c", C_HELLO_WORLD)
        compileParams.target.set("invalid_target")
        assertThrows<IllegalStateException> { buildService.compile(compileParams) }
            .hasMessageThat()
            .contains("Unknown native target: invalid_target")
    }

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

    @Test
    fun compileAndroid() {
        val compileParams =
            createCompileParameters(
                "android_code.c",
                """
                #include <android/log.h>
                void test_log() {
                    __android_log_print(ANDROID_LOG_INFO, "JetskiTest", "Hello from NDK!");
                }
                """
                    .trimIndent(),
                target = NativeTarget.ANDROID_ARM64,
            )
        buildService.compile(compileParams)
        val outputFiles = compileParams.output.getRegularFiles()
        assertThat(outputFiles).hasSize(1)
        val outputFile = outputFiles.single()
        assertThat(outputFile.name).isEqualTo("android_code.o")
        val strings = extractStrings(outputFile)
        assertThat(strings).contains("__android_log_print")
        assertThat(strings).contains("Hello from NDK!")
    }

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
                    .trimIndent(),
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

    @Test
    fun runLinker() {
        val compileParameters = createCompileParameters("code.c", C_HELLO_WORLD)
        buildService.compile(compileParameters)
        val sharedLibraryParameters = project.objects.newInstance(ClangLinkerParameters::class.java)
        sharedLibraryParameters.target.set(compileParameters.target)
        sharedLibraryParameters.objectFiles.from(compileParameters.output)
        sharedLibraryParameters.linkerOutputKind.set(LinkerOutputKind.DYNAMIC_LIBRARY)
        val outputFile = tmpFolder.newFile("code.so")
        sharedLibraryParameters.outputFile.set(outputFile)
        buildService.runLinker(sharedLibraryParameters)

        val strings = extractStrings(outputFile)
        assertThat(strings).contains("Hello, World!!")
        // should link with libc
        assertThat(strings).contains("libc")

        // verify shared lib files are aligned to 16Kb boundary for Android targets
        if (NativeTarget.fromName(sharedLibraryParameters.target.get()).isAndroid) {
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

    @Test
    fun archive() {
        val compileParams = createCompileParameters("code.c", C_HELLO_WORLD)
        buildService.compile(compileParams)
        val archiveParams = project.objects.newInstance(ClangArchiveParameters::class.java)
        archiveParams.target.set(compileParams.target)
        archiveParams.objectFiles.from(compileParams.output)
        val outputFile = tmpFolder.newFile("code.a")
        archiveParams.outputFile.set(outputFile)
        buildService.archiveLibrary(archiveParams)

        val strings = extractStrings(outputFile)
        assertThat(strings).contains("Hello, World!!")
        // should not with libc
        assertThat(strings).doesNotContain("libc")
    }

    private fun createCompileParameters(
        fileName: String,
        code: String,
        target: NativeTarget = NativeTarget.LINUX_X64,
    ): ClangCompileParameters {
        val srcDir = tmpFolder.newFolder("src")
        srcDir.resolve(fileName).writeText(code)
        val compileParams = project.objects.newInstance(ClangCompileParameters::class.java)
        compileParams.target.set(target.name)
        compileParams.output.set(tmpFolder.newFolder())
        compileParams.sources.from(srcDir)
        return compileParams
    }

    private fun DirectoryProperty.getRegularFiles() =
        get().asFile.walkTopDown().filter { it.isFile }.toList()

    /**
     * Extract strings from a binary file so that we can assert output contents.
     *
     * We used to use linux strings command here but it stopped working in CI. This implementation
     * pretty much matches our strings usage and good enough for these tests.
     * https://man7.org/linux/man-pages/man1/strings.1.html
     */
    private fun extractStrings(file: File): String {
        assertWithMessage("Cannot extract strings from file").that(file.isFile).isTrue()
        val finalString = StringBuilder()
        val currentSection = StringBuilder()
        fun finishSection() {
            if (currentSection.length > 4) {
                finalString.appendLine(currentSection)
            }
            currentSection.setLength(0)
        }
        file.inputStream().buffered(1024).use { inputStream ->
            var byte: Int
            do {
                byte = inputStream.read()
                // if it is a printable string, add it to the list.
                if (byte in 32..127) {
                    currentSection.append(byte.toChar())
                } else {
                    // cleanup the remaining
                    finishSection()
                }
            } while (byte != -1)
        }
        // one final cleanup
        finishSection()
        return finalString.toString()
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
