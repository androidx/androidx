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

package androidx.privacysandbox.tools.apicompiler

import androidx.privacysandbox.tools.core.proto.PrivacySandboxToolsProtocol.ToolMetadata
import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertThat
import androidx.privacysandbox.tools.testing.resourceOutputDir
import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrivacySandboxKspCompilerTest {

    @Test
    fun compileEmpty_ok() {
        assertThat(compileWithPrivacySandboxKspCompiler(listOf())).apply {
            succeeds()
            hasNoGeneratedSourceFiles()
        }
    }

    @Test
    fun generatesMetadataFile() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk {
                        fun doStuff(x: Int, y: Int)
                    }
                """
            )
        val compilationResult = compileWithPrivacySandboxKspCompiler(listOf(source))
        assertThat(compilationResult).succeeds()

        val resourceMap = compilationResult.resourceOutputDir.walk()
            .filter { it.isFile }
            .map { it.toRelativeString(compilationResult.resourceOutputDir) to it.readBytes() }
            .toMap()
        val expectedMetadataRelativePath = "META-INF/privacysandbox/tool-metadata.pb"
        assertThat(resourceMap).containsKey(expectedMetadataRelativePath)
        assertThat(ToolMetadata.parseFrom(resourceMap[expectedMetadataRelativePath]))
            .isEqualTo(
                ToolMetadata.newBuilder()
                    .setCodeGenerationVersion(2)
                    .build()
            )
    }

    @Test
    fun compileInvalidServiceInterface_fails() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    abstract class MySdk {
                        fun doStuff(x: Int, y: Int): String
                        fun doMoreStuff()
                    }
                """
            )
        assertThat(compileWithPrivacySandboxKspCompiler(listOf(source))).fails()
    }
}