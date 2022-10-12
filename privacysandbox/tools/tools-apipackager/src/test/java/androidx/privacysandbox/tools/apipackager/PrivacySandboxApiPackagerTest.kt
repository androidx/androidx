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

package androidx.privacysandbox.tools.apipackager

import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertThat
import androidx.privacysandbox.tools.testing.CompilationTestHelper.compileAll
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationResult
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.createFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrivacySandboxApiPackagerTest {

    @Test
    fun validSdkClasspath_onlyAnnotatedClassesAreReturned() {
        val packagedSdkClasspath = compileAndReturnUnzippedPackagedClasspath(
            Source.kotlin(
                "com/mysdk/TestSandboxSdk.kt", """
                    |package com.mysdk
                    |
                    |import androidx.privacysandbox.tools.PrivacySandboxCallback
                    |import androidx.privacysandbox.tools.PrivacySandboxService
                    |import androidx.privacysandbox.tools.PrivacySandboxValue
                    |
                    |@PrivacySandboxService
                    |interface MySdk
                    |
                    |@PrivacySandboxValue
                    |data class Value(val id: Int)
                    |
                    |@PrivacySandboxCallback
                    |interface MySdkCallback
                    |
                    |interface InterfaceThatShouldBeIgnored {
                    |    val sdk: MySdk
                    |}
                    |
                    |data class DataClassThatShouldBeIgnored(val id: Int)
                """.trimMargin()
            )
        )

        val relativeDescriptorPaths = packagedSdkClasspath
            .walk()
            .filter { it.isFile }
            .map { packagedSdkClasspath.toPath().relativize(it.toPath()).toString() }
            .toList()
        assertThat(relativeDescriptorPaths).containsExactly(
            "com/mysdk/MySdk.class",
            "com/mysdk/MySdkCallback.class",
            "com/mysdk/Value.class",
        )
    }

    @Test
    fun validSdkClasspath_packagedDescriptorsCanBeLinkedAgainst() {
        val packagedSdkClasspath = compileAndReturnUnzippedPackagedClasspath(
            Source.kotlin(
                "com/mysdk/TestSandboxSdk.kt", """
                    |package com.mysdk
                    |
                    |import androidx.privacysandbox.tools.PrivacySandboxCallback
                    |import androidx.privacysandbox.tools.PrivacySandboxService
                    |import androidx.privacysandbox.tools.PrivacySandboxValue
                    |
                    |@PrivacySandboxService
                    |interface MySdk
                    |
                    |@PrivacySandboxValue
                    |data class Value(val id: Int)
                    |
                    |@PrivacySandboxCallback
                    |interface MySdkCallback
                """.trimMargin()
            )
        )

        val appSource =
            Source.kotlin(
                "com/exampleapp/App.kt", """
                |package com.exampleapp
                |
                |import com.mysdk.MySdk
                |import com.mysdk.Value
                |import com.mysdk.MySdkCallback
                |
                |class App(
                |    val sdk: MySdk,
                |    val sdkValue: Value,
                |    val callback: MySdkCallback,
                |)
            """.trimMargin()
            )
        assertThat(compileWithExtraClasspath(appSource, packagedSdkClasspath)).succeeds()
    }

    @Test
    fun sdkClasspathDoesNotExist_throwException() {
        val invalidClasspathFile = makeTestDirectory().resolve("dir_that_does_not_exist")
        val validSdkDescriptor = makeTestDirectory().resolve("sdk-descriptors.jar")
        assertThrows<IllegalArgumentException> {
            PrivacySandboxApiPackager().packageSdkDescriptors(
                invalidClasspathFile, validSdkDescriptor
            )
        }
    }

    @Test
    fun sdkClasspathNotADirectory_throwException() {
        val invalidClasspathFile = makeTestDirectory().resolve("invalid-file.txt").also {
            it.createFile()
        }
        val validSdkDescriptor = makeTestDirectory().resolve("sdk-descriptors.jar")
        assertThrows<IllegalArgumentException> {
            PrivacySandboxApiPackager().packageSdkDescriptors(
                invalidClasspathFile, validSdkDescriptor
            )
        }
    }

    @Test
    fun outputAlreadyExists_throwException() {
        val source = Source.kotlin(
            "com/mysdk/Valid.kt", """
            |package com.mysdk
            |interface Valid
        """.trimMargin()
        )
        val sdkClasspath = compileAll(listOf(source), includePrivacySandboxPlatformSources = false)
            .outputClasspath.first().toPath()
        val descriptorPathThatAlreadyExists =
            makeTestDirectory().resolve("sdk-descriptors.jar").also {
                it.createFile()
            }
        assertThrows<IllegalArgumentException> {
            PrivacySandboxApiPackager().packageSdkDescriptors(
                sdkClasspath, descriptorPathThatAlreadyExists
            )
        }
    }

    /** Compiles the given source files and returns a classpath with the results. */
    private fun compileAndReturnUnzippedPackagedClasspath(vararg sources: Source): File {
        val result = compileAll(sources.toList(), includePrivacySandboxPlatformSources = false)
        assertThat(result).succeeds()
        assertThat(result.outputClasspath).hasSize(1)

        val originalClasspath = result.outputClasspath.first().toPath()
        val descriptors = makeTestDirectory().resolve("sdk-descriptors.jar")
        PrivacySandboxApiPackager().packageSdkDescriptors(originalClasspath, descriptors)

        val outputDir = makeTestDirectory().toFile()
        ZipInputStream(descriptors.toFile().inputStream()).use { input ->
            generateSequence { input.nextEntry }
                .forEach {
                    val file: File = outputDir.resolve(it.name)
                    file.parentFile.mkdirs()
                    file.createNewFile()
                    input.copyTo(file.outputStream())
                }
        }
        return outputDir
    }

    private fun makeTestDirectory(): Path {
        return Files.createTempDirectory("test")
            .also {
                it.toFile().deleteOnExit()
            }
    }

    private fun compileWithExtraClasspath(source: Source, extraClasspath: File):
        TestCompilationResult {
        return compileAll(
            listOf(source),
            extraClasspath = listOf(extraClasspath),
            includePrivacySandboxPlatformSources = false
        )
    }
}
