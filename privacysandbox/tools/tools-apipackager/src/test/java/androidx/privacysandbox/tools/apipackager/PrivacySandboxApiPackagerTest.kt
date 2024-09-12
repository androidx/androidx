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

import androidx.privacysandbox.tools.core.Metadata
import androidx.privacysandbox.tools.core.proto.PrivacySandboxToolsProtocol.ToolMetadata
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
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrivacySandboxApiPackagerTest {

    @Test
    fun validSdkClasspath_onlyAnnotatedClassesAreReturned() {
        val packagedSdkClasspath =
            compileAndReturnUnzippedPackagedClasspath(
                Source.kotlin(
                    "com/mysdk/TestSandboxSdk.kt",
                    """
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
                """
                        .trimMargin()
                )
            )

        val relativeDescriptorPaths =
            packagedSdkClasspath
                .walk()
                .filter { it.isFile }
                .map { packagedSdkClasspath.toPath().relativize(it.toPath()).toString() }
                .toList()
        assertThat(relativeDescriptorPaths)
            .containsExactly(
                "com/mysdk/MySdk.class",
                "com/mysdk/MySdkCallback.class",
                "com/mysdk/Value.class",
            )
    }

    @Test
    fun validSdkClasspath_packagedDescriptorsCanBeLinkedAgainst() {
        val packagedSdkClasspath =
            compileAndReturnUnzippedPackagedClasspath(
                Source.kotlin(
                    "com/mysdk/TestSandboxSdk.kt",
                    """
                    |package com.mysdk
                    |
                    |import androidx.privacysandbox.tools.PrivacySandboxCallback
                    |import androidx.privacysandbox.tools.PrivacySandboxService
                    |import androidx.privacysandbox.tools.PrivacySandboxValue
                    |import androidx.privacysandbox.tools.internal.GeneratedPublicApi
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
                    |@GeneratedPublicApi
                    |object MySdkFactory {
                    |    fun wrapToMySdk(): MySdk = throw RuntimeException("Stub!")
                    |}
                """
                        .trimMargin()
                )
            )

        val appSource =
            Source.kotlin(
                "com/exampleapp/App.kt",
                """
                |package com.exampleapp
                |
                |import com.mysdk.MySdk
                |import com.mysdk.Value
                |import com.mysdk.MySdkCallback
                |import com.mysdk.MySdkFactory.wrapToMySdk
                |
                |class App(
                |    val sdk: MySdk = wrapToMySdk(),
                |    val sdkValue: Value,
                |    val callback: MySdkCallback,
                |)
            """
                    .trimMargin()
            )
        assertThat(compileWithExtraClasspath(appSource, packagedSdkClasspath)).succeeds()
    }

    @Test
    fun sdkClasspathWithMetadataFile_isKeptInDescriptor() {
        val metadata = ToolMetadata.newBuilder().setCodeGenerationVersion(42).build()
        val source =
            Source.kotlin(
                "com/mysdk/Valid.kt",
                """
            |package com.mysdk
            |interface Valid
        """
                    .trimMargin()
            )
        val sdkClasspath = compileAll(listOf(source)).outputClasspath.first().toPath()

        val metadataPath =
            sdkClasspath.resolve(Metadata.filePath).also {
                it.parent.createDirectories()
                it.createFile()
            }
        metadata.writeTo(metadataPath.outputStream())

        val sdkDescriptor = makeTestDirectory().resolve("sdk-descriptors.jar")
        PrivacySandboxApiPackager().packageSdkDescriptors(sdkClasspath, sdkDescriptor)
        val packagedMetadata =
            ZipInputStream(sdkDescriptor.inputStream()).use { input ->
                generateSequence { input.nextEntry }
                    .filter { it.name == Metadata.filePath.toString() }
                    .map { ToolMetadata.parseFrom(input.readAllBytes()) }
                    .toList()
            }

        assertThat(packagedMetadata).containsExactly(metadata)
    }

    @Test
    fun dirWithClassExtension_ignored() {
        val source =
            Source.kotlin(
                "com/mysdk/Valid.kt",
                """
            |package com.mysdk
            |interface Valid
        """
                    .trimMargin()
            )
        val sdkClasspath = compileAll(listOf(source)).outputClasspath.first().toPath()
        sdkClasspath.resolve("otherdir.class").createDirectories()
        val sdkDescriptor = makeTestDirectory().resolve("sdk-descriptors.jar")

        // Does not throw
        PrivacySandboxApiPackager().packageSdkDescriptors(sdkClasspath, sdkDescriptor)
    }

    @Test
    fun companionObject_preserved() {
        val packagedSdkClasspath =
            compileAndReturnUnzippedPackagedClasspath(
                Source.kotlin(
                    "com/mysdk/TestSandboxSdk.kt",
                    """
                    |package com.mysdk
                    |
                    |import androidx.privacysandbox.tools.PrivacySandboxCallback
                    |import androidx.privacysandbox.tools.PrivacySandboxService
                    |import androidx.privacysandbox.tools.PrivacySandboxValue
                    |
                    |@PrivacySandboxService
                    |interface MySdk {
                    |  companion object MyCompanion {
                    |    const val MY_CONST = 42
                    |  }
                    |}
                """
                        .trimMargin()
                )
            )

        val relativeDescriptorPaths =
            packagedSdkClasspath
                .walk()
                .filter { it.isFile }
                .map { packagedSdkClasspath.toPath().relativize(it.toPath()).toString() }
                .toList()
        assertThat(relativeDescriptorPaths)
            .containsExactly(
                "com/mysdk/MySdk.class",
                "com/mysdk/MySdk\$MyCompanion.class",
            )
    }

    @Test
    fun sdkClasspathDoesNotExist_throwException() {
        val invalidClasspathFile = makeTestDirectory().resolve("dir_that_does_not_exist")
        val validSdkDescriptor = makeTestDirectory().resolve("sdk-descriptors.jar")
        assertThrows<IllegalArgumentException> {
            PrivacySandboxApiPackager()
                .packageSdkDescriptors(invalidClasspathFile, validSdkDescriptor)
        }
    }

    @Test
    fun sdkClasspathNotADirectory_throwException() {
        val invalidClasspathFile =
            makeTestDirectory().resolve("invalid-file.txt").also { it.createFile() }
        val validSdkDescriptor = makeTestDirectory().resolve("sdk-descriptors.jar")
        assertThrows<IllegalArgumentException> {
            PrivacySandboxApiPackager()
                .packageSdkDescriptors(invalidClasspathFile, validSdkDescriptor)
        }
    }

    @Test
    fun outputAlreadyExists_throwException() {
        val source =
            Source.kotlin(
                "com/mysdk/Valid.kt",
                """
            |package com.mysdk
            |interface Valid
        """
                    .trimMargin()
            )
        val sdkClasspath = compileAll(listOf(source)).outputClasspath.first().toPath()
        val descriptorPathThatAlreadyExists =
            makeTestDirectory().resolve("sdk-descriptors.jar").also { it.createFile() }
        assertThrows<IllegalArgumentException> {
            PrivacySandboxApiPackager()
                .packageSdkDescriptors(sdkClasspath, descriptorPathThatAlreadyExists)
        }
    }

    /** Compiles the given source file and returns a classpath with the results. */
    private fun compileAndReturnUnzippedPackagedClasspath(source: Source): File {
        val result = compileAll(listOf(source))
        assertThat(result).succeeds()
        assertThat(result.outputClasspath).hasSize(1)

        val originalClasspath = result.outputClasspath.first().toPath()
        val descriptors = makeTestDirectory().resolve("sdk-descriptors.jar")
        PrivacySandboxApiPackager().packageSdkDescriptors(originalClasspath, descriptors)

        val outputDir = makeTestDirectory().toFile()
        ZipInputStream(descriptors.inputStream()).use { input ->
            generateSequence { input.nextEntry }
                .forEach {
                    val file: File = outputDir.resolve(it.name)
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                    input.copyTo(file.outputStream())
                }
        }
        return outputDir
    }

    private fun makeTestDirectory(): Path {
        return Files.createTempDirectory("test").also { it.toFile().deleteOnExit() }
    }

    private fun compileWithExtraClasspath(
        source: Source,
        extraClasspath: File
    ): TestCompilationResult {
        return compileAll(
            listOf(source),
            extraClasspath = listOf(extraClasspath),
        )
    }
}
