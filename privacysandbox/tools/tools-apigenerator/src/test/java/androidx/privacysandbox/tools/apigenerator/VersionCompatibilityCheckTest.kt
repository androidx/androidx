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
import androidx.privacysandbox.tools.core.proto.PrivacySandboxToolsProtocol.ToolMetadata
import androidx.room.compiler.processing.util.Source
import androidx.testutils.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VersionCompatibilityCheckTest {
    private val validSources = listOf(
        Source.kotlin(
            "com/mysdk/TestSandboxSdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk {
                      fun doSomething(number: Int)
                    }
                """
        )
    )

    private val validMetadataContent = Metadata.toolMetadata.toByteArray()

    @Test
    fun sdkDescriptorWithMissingMetadata_throws() {
        assertThrows<IllegalArgumentException> {
            runGeneratorWithResources(mapOf())
        }.hasMessageThat().contains("Missing tool metadata in SDK API descriptor")
    }

    @Test
    fun sdkDescriptorWithMetadataInWrongPath_throws() {
        assertThrows<IllegalArgumentException> {
            runGeneratorWithResources(
                mapOf(Path("invalid/dir/metadata.pb") to validMetadataContent)
            )
        }.hasMessageThat().contains("Missing tool metadata in SDK API descriptor")
    }

    @Test
    fun sdkDescriptorWithInvalidMetadataContent_throws() {
        assertThrows<IllegalArgumentException> {
            runGeneratorWithResources(
                mapOf(Metadata.filePath to "bogus data".toByteArray())
            )
        }.hasMessageThat().contains("Invalid Privacy Sandbox tool metadata")
    }

    @Test
    fun sdkDescriptorWithIncompatibleVersion_throws() {
        val sdkMetadata = ToolMetadata.newBuilder()
            .setCodeGenerationVersion(999)
            .build()
        assertThrows<IllegalArgumentException> {
            runGeneratorWithResources(
                mapOf(Metadata.filePath to sdkMetadata.toByteArray())
            )
        }.hasMessageThat().contains(
            "SDK uses incompatible Privacy Sandbox tooling (version 999)"
        )
    }

    @Test
    fun sdkDescriptorWithLowerVersion_isCompatible() {
        val sdkMetadata = ToolMetadata.newBuilder()
            .setCodeGenerationVersion(0)
            .build()
        runGeneratorWithResources(mapOf(Metadata.filePath to sdkMetadata.toByteArray()))
    }

    private fun runGeneratorWithResources(resources: Map<Path, ByteArray>) {
        val aidlCompilerPath = System.getProperty("aidl_compiler_path")?.let(::Path)
            ?: throw IllegalArgumentException("aidl_compiler_path flag not set.")
        val frameworkAidlPath = System.getProperty("framework_aidl_path")?.let(::Path)
            ?: throw IllegalArgumentException("framework_aidl_path flag not set.")
        val descriptors = compileIntoInterfaceDescriptorsJar(validSources, resources)
        val generator = PrivacySandboxApiGenerator()
        val outputDir = Files.createTempDirectory("output").also { it.toFile().deleteOnExit() }
        generator.generate(descriptors, aidlCompilerPath, frameworkAidlPath, outputDir)
    }
}
