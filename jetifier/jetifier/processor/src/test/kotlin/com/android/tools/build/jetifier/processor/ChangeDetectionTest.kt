/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.pom.PomDependency
import com.android.tools.build.jetifier.core.pom.PomRewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.google.common.truth.Truth
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Tests that transformed artifacts are properly marked as changed / unchanged base on whether there
 * was something to rewrite or not.
 */
class ChangeDetectionTest {

    private val prefRewriteConfig = Config.fromOptional(
        restrictToPackagePrefixes = setOf("android/support/v7/preference"),
        rulesMap =
            RewriteRulesMap(
                RewriteRule(from = "android/support/v7/preference/Preference(.+)", to = "ignore"),
                RewriteRule(from = "(.*)/R(.*)", to = "ignore")
            ),
        slRules = listOf(),
        pomRewriteRules = setOf(
            PomRewriteRule(
                PomDependency(
                    groupId = "supportGroup", artifactId = "supportArtifact", version = "4.0"),
                PomDependency(
                    groupId = "testGroup", artifactId = "testArtifact", version = "1.0")
            )),
        typesMap = TypesMap(
            JavaType("android/support/v7/preference/Preference")
                to JavaType("android/test/pref/Preference")
        )
    )

    @Test
    fun xmlRewrite_archiveChanged() {
        testChange(
            config = prefRewriteConfig,
            fileContent =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<android.support.v7.preference.Preference/>",
            fileName = "test.xml",
            areChangesExpected = true
        )
    }

    @Test
    fun xmlRewrite_archiveNotChanged() {
        testChange(
            config = Config.EMPTY,
            fileContent =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<android.support.v7.preference.Preference/>",
            fileName = "test.xml",
            areChangesExpected = false
        )
    }

    @Test
    fun proGuard_archiveChanged() {
        testChange(
            config = prefRewriteConfig,
            fileContent =
                "-keep public class * extends android.support.v7.preference.Preference { \n" +
                "  <fields>; \n" +
                "}",
            fileName = "proguard.txt",
            areChangesExpected = true
        )
    }

    @Test
    fun proGuard_archiveNotChanged() {
        testChange(
            config = Config.EMPTY,
            fileContent =
                "-keep public class * extends android.support.v7.preference.Preference { \n" +
                "  <fields>; \n" +
                "}",
            fileName = "test.xml",
            areChangesExpected = false
        )
    }

    @Test
    fun pom_archiveChanged() {
        testChange(
            config = prefRewriteConfig,
            fileContent =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0" +
                "  http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>supportGroup</groupId>\n" +
                "      <artifactId>supportArtifact</artifactId>\n" +
                "      <version>4.0</version>\n" +
                "    </dependency>\n" +
                "  </dependencies>" +
                "</project>\n",
            fileName = "pom.xml",
            areChangesExpected = true
        )
    }

    @Test
    fun pom_archiveNotChanged() {
        testChange(
            config = Config.EMPTY,
            fileContent =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0" +
                "  http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>supportGroup</groupId>\n" +
                "      <artifactId>supportArtifact</artifactId>\n" +
                "      <version>4.0</version>\n" +
                "    </dependency>\n" +
                "  </dependencies>" +
                "</project>\n",
            fileName = "pom.xml",
            areChangesExpected = true
        )
    }

    @Test
    fun javaClass_archiveChanged() {
        val inputClassPath = "/changeDetectionTest/testPreference.class"
        val inputFile = File(javaClass.getResource(inputClassPath).file)

        testChange(
            config = prefRewriteConfig,
            file = ArchiveFile(Paths.get("/", "preference.class"), inputFile.readBytes()),
            areChangesExpected = true
        )
    }

    @Test
    fun javaClass_archiveNotChanged() {
        val inputClassPath = "/changeDetectionTest/testPreference.class"
        val inputFile = File(javaClass.getResource(inputClassPath).file)

        testChange(
            config = Config.EMPTY,
            file = ArchiveFile(Paths.get("/", "preference.class"), inputFile.readBytes()),
            areChangesExpected = false
        )
    }

    private fun testChange(
        config: Config,
        fileContent: String,
        fileName: String,
        areChangesExpected: Boolean
    ) {
        testChange(
            config = config,
            file = ArchiveFile(Paths.get("/", fileName), fileContent.toByteArray()),
            areChangesExpected = areChangesExpected)
    }

    /**
     * Runs the whole transformation process over the given file and verifies if the parent
     * artifacts was properly marked as changed / unchanged base on [areChangesExpected] param.
     */
    private fun testChange(
        config: Config,
        file: ArchiveFile,
        areChangesExpected: Boolean
    ) {
        val archive = Archive(Paths.get("some/path"), listOf(file))
        val sourceArchive = archive.writeSelfToFile(Files.createTempFile("test", ".zip"))

        val expectedFileIfRefactored = Files.createTempFile("testRefactored", ".zip")
        val processor = Processor.createProcessor(config)
        val resultFiles = processor.transform(
            setOf(FileMapping(sourceArchive, expectedFileIfRefactored.toFile())),
            copyUnmodifiedLibsAlso = false)

        if (areChangesExpected) {
            Truth.assertThat(resultFiles).containsExactly(expectedFileIfRefactored.toFile())
        } else {
            Truth.assertThat(resultFiles).containsExactly(sourceArchive)
        }
    }
}