/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.pom

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.pom.PomDependency
import com.android.tools.build.jetifier.core.pom.PomRewriteRule
import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.google.common.truth.Truth
import org.junit.Test
import java.io.File

/**
 * Tests that pom file inside a zip file was correctly rewritten.
 */
class PomRewriteInZipTest {

    companion object {
        private val TEST_CONFIG = Config.fromOptional(
            restrictToPackagePrefixes = setOf("com/sample"),
            pomRewriteRules = setOf(
                PomRewriteRule(
                    from = PomDependency(
                        groupId = "old.group",
                        artifactId = "myOldArtifact",
                        version = "0.1.0"),
                    to = PomDependency(
                        groupId = "com.sample.my.group",
                        artifactId = "myArtifact",
                        version = "1.0.0"
                    )
            ))
        )
    }

    @Test fun rewritePomInZip_rewritingSL_shouldRewrite() {
        val inputZipPath = "/pomRefactorTest/pomTest.zip"

        val processor = Processor.createProcessor(
            TEST_CONFIG,
            rewritingSupportLib = true,
            reversedMode = true)

        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val expectedFile = File(createTempDir(), "test.zip")

        val resultFiles = processor.transform(setOf(FileMapping(inputFile, expectedFile)))

        Truth.assertThat(resultFiles).hasSize(1)

        val returnedArchive = Archive.Builder.extract(expectedFile)
        val returnedPom = returnedArchive.files.first() as ArchiveFile
        val content = String(returnedPom.data)

        Truth.assertThat(returnedPom.fileName).isEqualTo("test.pom")

        Truth.assertThat(content).doesNotContain("com.sample.my.group")
        Truth.assertThat(content).doesNotContain("myArtifact")
        Truth.assertThat(content).doesNotContain("1.0.0")

        Truth.assertThat(content).contains("old.group")
        Truth.assertThat(content).contains("myOldArtifact")
        Truth.assertThat(content).contains("0.1.0")

        tempDir.delete()
    }

    @Test fun rewritePomInZip_notRewritingSL_shouldStillRewrite() {
        val inputZipPath = "/pomRefactorTest/pomTest.zip"

        val processor = Processor.createProcessor(
            TEST_CONFIG,
            rewritingSupportLib = false,
            reversedMode = true)

        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val expectedFile = File(createTempDir(), "test.zip")

        val resultFiles = processor.transform(setOf(FileMapping(inputFile, expectedFile)))

        Truth.assertThat(resultFiles).hasSize(1)

        val returnedArchive = Archive.Builder.extract(expectedFile)
        val returnedPom = returnedArchive.files.first() as ArchiveFile
        val content = String(returnedPom.data)

        Truth.assertThat(returnedPom.fileName).isEqualTo("test.pom")

        Truth.assertThat(content).doesNotContain("com.sample.my.group")
        Truth.assertThat(content).doesNotContain("myArtifact")
        Truth.assertThat(content).doesNotContain("1.0.0")

        Truth.assertThat(content).contains("old.group")
        Truth.assertThat(content).contains("myOldArtifact")
        Truth.assertThat(content).contains("0.1.0")

        tempDir.delete()
    }
}