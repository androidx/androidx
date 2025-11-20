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
package androidx.appsearch.compiler

import androidx.room.compiler.processing.util.Source.Companion.kotlin
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.TestCompilationResult
import androidx.room.compiler.processing.util.compiler.compile
import com.google.auto.value.processor.AutoValueProcessor
import com.google.common.io.CharStreams
import com.google.common.io.Files
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.logging.Logger
import javax.tools.Diagnostic
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName

/**
 * Base class for AppSearchCompilerTest which provides some utilities for compiling and managing the
 * output folder.
 */
abstract class CompilerTestBase {
    @Rule @JvmField val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Rule @JvmField val testName: TestName = TestName()

    private lateinit var genFilesDir: File

    @Before
    fun setUp() {
        genFilesDir = temporaryFolder.newFolder("genFilesDir")
    }

    fun compileKotlin(classBody: String): TestCompilationResult {
        val src =
            """
            package com.example.appsearch
            import androidx.appsearch.annotation.Document
            import androidx.appsearch.annotation.Document.*
            $classBody
            """
                .trimIndent()
        val kotlinSource = kotlin("KotlinGift.kt", src)
        // We're compiling kotlin a bit differently, we need a fresh folder here
        val kotlinCompilationDir = temporaryFolder.newFolder("kt")
        return compile(
            kotlinCompilationDir,
            TestCompilationArguments(
                listOf(kotlinSource),
                inheritClasspath = true,
                kotlincArguments = listOf("-language-version=1.9", "-api-version=1.9"),
                kaptProcessors = listOf(AppSearchCompiler()),
                symbolProcessorProviders = listOf(),
                processorOptions =
                    mapOf(
                        "AppSearchCompiler.OutputDir" to genFilesDir.absolutePath,
                        "AppSearchCompiler.RestrictGeneratedCodeToLib" to "false",
                    ),
            ),
        )
    }

    /**
     * Checks that a [TestCompilationResult] succeeded and has no warnings.
     *
     * This is the Kotlin compilation equivalent of `CompilationSubject.succeededWithoutWarnings()`.
     * It provides detailed error messages if the compilation fails or has warnings.
     */
    fun checkKotlinCompilation(compilationResult: TestCompilationResult) {
        val warnings = compilationResult.diagnostics[Diagnostic.Kind.WARNING] ?: emptyList()
        val errors = compilationResult.diagnostics[Diagnostic.Kind.ERROR] ?: emptyList()
        if (!compilationResult.success) {
            val messages = mutableListOf<String>()
            if (warnings.isNotEmpty()) {
                messages.add("Compilation had warnings:\n${warnings.joinToString("\n")}")
            }
            if (errors.isNotEmpty()) {
                messages.add("Compilation failed with errors:\n${errors.joinToString("\n")}")
            } else {
                messages.add("Compilation failed with an unknown error.")
            }
            val finalMessage = messages.joinToString("\n\n")
            assertWithMessage(finalMessage).that(compilationResult.success).isTrue()
        }

        if (warnings.isNotEmpty()) {
            val warningMessage =
                ("Compilation succeeded but has warnings:\n${warnings.joinToString("\n")}")
            assertWithMessage(warningMessage).that(warnings).isEmpty()
        }
    }

    fun compile(classBody: String): Compilation {
        return compile("Gift", classBody, restrictGeneratedCodeToLibrary = false)
    }

    fun compile(
        classSimpleName: String,
        classBody: String,
        restrictGeneratedCodeToLibrary: Boolean,
    ): Compilation {
        val src =
            """
            package com.example.appsearch;
            import androidx.appsearch.annotation.Document;
            import androidx.appsearch.annotation.Document.*;
            $classBody
            """
                .trimIndent()
        val jfo = JavaFileObjects.forSourceString("com.example.appsearch.$classSimpleName", src)
        // Fully compiling this source code requires AppSearch to be on the classpath, but it only
        // builds on Android. Instead, this test configures the annotation processor to write to a
        // test-controlled path which is then diffed.
        val outputDirFlag = "-A${AppSearchCompiler.OUTPUT_DIR_OPTION}=${genFilesDir.absolutePath}"
        val restrictGeneratedCodeToLibraryFlag =
            "-A${AppSearchCompiler.RESTRICT_GENERATED_CODE_TO_LIB_OPTION}=" +
                restrictGeneratedCodeToLibrary
        return Compiler.javac()
            .withProcessors(AppSearchCompiler(), AutoValueProcessor())
            .withOptions(outputDirFlag, restrictGeneratedCodeToLibraryFlag)
            .compile(jfo)
    }

    fun checkEqualsGolden(className: String) {
        val goldenResPath = "goldens/${testName.methodName}.JAVA"
        val actualPackageDir = File(genFilesDir, "com/example/appsearch")
        val actualPath = File(actualPackageDir, IntrospectionHelper.GEN_CLASS_PREFIX + className)
        checkEqualsGoldenHelper(goldenResPath, actualPath)
    }

    fun checkDocumentMapEqualsGolden(roundIndex: Int) {
        val goldenResPath = "goldens/${testName.methodName}DocumentMap_${roundIndex}.JAVA"
        val actualPackageDir = File(genFilesDir, "com/example/appsearch")
        val files: Array<File>? =
            actualPackageDir.listFiles { dir: File, name: String ->
                name.startsWith("${IntrospectionHelper.GEN_CLASS_PREFIX}DocumentClassMap") &&
                    name.endsWith("_$roundIndex.java")
            }
        assertThat(files).isNotNull()
        assertThat(files).hasLength(1)
        checkEqualsGoldenHelper(goldenResPath, files!![0])
    }

    private fun checkEqualsGoldenHelper(goldenResPath: String, actualPath: File) {
        // Get the expected file contents
        var expected = ""
        javaClass.getResourceAsStream(goldenResPath).use { `is` ->
            if (`is` == null) {
                LOG.warning("Failed to find resource \"$goldenResPath\"; treating as empty")
            } else {
                val reader = InputStreamReader(`is`, StandardCharsets.UTF_8)
                expected = CharStreams.toString(reader)
            }
        }
        // Get the actual file contents
        assertWithMessage("Path $actualPath is not a file").that(actualPath.isFile()).isTrue()
        val actual = Files.asCharSource(actualPath, StandardCharsets.UTF_8).read()

        // Compare!
        if (expected == actual) {
            return
        }

        // Sadness. If we're running in an environment where source is available, rewrite the golden
        // to match the actual content for ease of updating the goldens.
        try {
            // At runtime, our resources come from the build tree. However, our cwd is
            // frameworks/support, so find the source tree from that.
            val goldenSrcDir = File("src/test/resources/androidx/appsearch/compiler")
            if (!goldenSrcDir.isDirectory()) {
                LOG.warning(
                    "Failed to update goldens: golden dir \"${goldenSrcDir.absolutePath} \"" +
                        " does not exist or is not a folder"
                )
                return
            }
            val goldenFile = File(goldenSrcDir, goldenResPath)
            Files.asCharSink(goldenFile, StandardCharsets.UTF_8).write(actual)
            LOG.info("Successfully updated golden file \"$goldenFile\"")
        } finally {
            // Now produce the real exception for the test runner.
            assertThat(actual).isEqualTo(expected)
        }
    }

    fun checkResultContains(className: String, content: String) {
        val fileContents = getClassFileContents(className)
        assertThat(fileContents).contains(content)
    }

    fun checkResultDoesNotContain(className: String, content: String) {
        val fileContents = getClassFileContents(className)
        assertThat(fileContents).doesNotContain(content)
    }

    private fun getClassFileContents(className: String): String {
        val actualPackageDir = File(genFilesDir, "com/example/appsearch")
        val actualPath = File(actualPackageDir, IntrospectionHelper.GEN_CLASS_PREFIX + className)
        assertWithMessage("Path $actualPath is not a file").that(actualPath.isFile()).isTrue()
        return Files.asCharSource(actualPath, StandardCharsets.UTF_8).read()
    }

    private companion object {
        private val LOG: Logger = Logger.getLogger(CompilerTestBase::class.java.getSimpleName())
    }
}
