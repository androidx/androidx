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

package com.android.tools.build.jetifier.processor.transform.metainf

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.pom.DependencyVersions
import com.android.tools.build.jetifier.core.pom.DependencyVersionsMap
import com.android.tools.build.jetifier.core.pom.PomDependency
import com.android.tools.build.jetifier.core.pom.PomRewriteRule
import com.android.tools.build.jetifier.processor.Processor
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

class MetaInfTransformerTest {

    @Test
    fun rewriteVersionFile_jetification_shouldSkip() {
        testRewrite(
            given = "1.0.0",
            expected = "1.0.0",
            pomRules = setOf(
                PomRewriteRule(
                    from = PomDependency(
                        groupId = "com.android.support",
                        artifactId = "preference-v7",
                        version = "28.8.8"),
                    to = PomDependency(
                        groupId = "androidx.preference",
                        artifactId = "preference",
                        version = "1.0.0"))
            ),
            filePath = Paths.get("something/META-INF",
                "androidx.preference_preference.version"),
            expectedFilePath = Paths.get("something/META-INF",
                "androidx.preference_preference.version"),
            rewritingSupportLib = false
        )
    }

    @Test
    fun rewriteVersion_dejetification_shouldRewrite() {
        testRewrite(
            given = "1.0.0",
            expected = "28.8.8",
            pomRules = setOf(
                PomRewriteRule(
                    from = PomDependency(
                        groupId = "com.android.support",
                        artifactId = "preference-v7",
                        version = "28.8.8"),
                    to = PomDependency(
                        groupId = "androidx.preference",
                        artifactId = "preference",
                        version = "1.0.0"))
            ),
            filePath = Paths.get("something/META-INF",
                "androidx.preference_preference.version"),
            expectedFilePath = Paths.get("something/META-INF",
                "com.android.support_preference-v7.version"),
            rewritingSupportLib = true
        )
    }

    @Test
    fun rewriteVersion_dejetification_usingMap_shouldRewrite() {
        testRewrite(
            given = "1.0.0",
            expected = "29.9.9",
            pomRules = setOf(
                PomRewriteRule(
                    from = PomDependency(
                        groupId = "com.android.support",
                        artifactId = "preference-v7",
                        version = "{myVersion}"
                    ),
                    to = PomDependency(
                        groupId = "androidx.preference",
                        artifactId = "preference",
                        version = "1.0.0"
                    ))
            ),
            filePath = Paths.get("something/META-INF",
                "androidx.preference_preference.version"),
            expectedFilePath = Paths.get("something/META-INF",
                "com.android.support_preference-v7.version"),
            versionsMap = mapOf("myVersion" to "29.9.9"),
            rewritingSupportLib = true
        )
    }

    @Test
    fun rewriteVersion_dejetification_notInMetaInfDir_shouldSkip() {
        testRewrite(
            given = "1.0.0",
            expected = "1.0.0",
            pomRules = setOf(
                PomRewriteRule(
                    from = PomDependency(
                        groupId = "com.android.support",
                        artifactId = "preference-v7",
                        version = "28.8.8"
                    ),
                    to = PomDependency(
                        groupId = "androidx.preference",
                        artifactId = "preference",
                        version = "1.0.0"
                    ))
            ),
            filePath = Paths.get("something/notMeta",
                "androidx.preference_preference.version"),
            expectedFilePath = Paths.get("something/notMeta",
                "androidx.preference_preference.version"),
            rewritingSupportLib = true
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rewriteVersion_dejetification_missingPomRule_shouldCrash() {
        testRewrite(
            given = "1.0.0",
            expected = "28.8.8",
            pomRules = setOf(),
            filePath = Paths.get("something/META-INF",
                "androidx.preference_preference.version"),
            expectedFilePath = Paths.get("something/META-INF",
                "com.android.support_preference-v7.version"),
            rewritingSupportLib = true
        )
    }

    private fun testRewrite(
        given: String,
        expected: String,
        filePath: Path,
        expectedFilePath: Path = Paths.get(""),
        pomRules: Set<PomRewriteRule>,
        rewritingSupportLib: Boolean,
        versionsMap: Map<String, String> = emptyMap()
    ) {
        val config = Config.fromOptional(
            pomRewriteRules = pomRules,
            versionsMap = DependencyVersionsMap(
                mapOf(DependencyVersions.DEFAULT_DEPENDENCY_SET to versionsMap)))

        val file = ArchiveFile(filePath, given.toByteArray())

        @Suppress("deprecation")
        Processor
            .createProcessor(
                config = config,
                rewritingSupportLib = rewritingSupportLib,
                reversedMode = rewritingSupportLib)
            .visit(file)

        val strResult = file.data.toString(Charset.defaultCharset())

        Truth.assertThat(file.relativePath).isEqualTo(expectedFilePath)
        Truth.assertThat(strResult).isEqualTo(expected)
    }
}