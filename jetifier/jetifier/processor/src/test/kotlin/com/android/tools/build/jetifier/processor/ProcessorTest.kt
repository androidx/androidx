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
import com.android.tools.build.jetifier.core.config.ConfigParser
import com.android.tools.build.jetifier.core.pom.PomDependency
import com.android.tools.build.jetifier.core.pom.PomRewriteRule
import com.google.common.truth.Truth
import org.junit.Test
import java.io.File

/**
 * Tests [Processor] functionality.
 */
class ProcessorTest {

    @Test
    fun processor_getDependenciesMap_checkNoVariablesLeft() {
        val processor = Processor.createProcessor(
                ConfigParser.loadDefaultConfig()!!,
                dataBindingVersion = "1.0.0")

        val dependenciesMap = processor.getDependenciesMap(filterOutBaseLibrary = false)

        dependenciesMap.forEach { _, to ->
            Truth.assertThat(to).doesNotContain("{")
            Truth.assertThat(to).doesNotContain("}")
            Truth.assertThat(to).doesNotContain("undefined")
        }

        Truth.assertThat(dependenciesMap.size).isGreaterThan(0)
        Truth.assertThat(dependenciesMap.any { it.key.contains("baseLibrary") }).isTrue()
    }

    @Test
    fun processor_getDependenciesMap_filterOutBaseLibrary() {
        val processor = Processor.createProcessor(
                ConfigParser.loadDefaultConfig()!!,
                dataBindingVersion = "1.0.0")

        val dependenciesMap = processor.getDependenciesMap(filterOutBaseLibrary = true)

        Truth.assertThat(dependenciesMap.any { it.key.contains("baseLibrary") }).isFalse()
    }

    @Test
    fun processor_isOldOrNewDependencyFile_shouldDetectProperly() {

        val processor = Processor.createProcessor(
            Config.fromOptional(
                pomRewriteRules = setOf(PomRewriteRule(
                    from = PomDependency("test.group", "artifactTest", "1.0.0"),
                    to = PomDependency("test2.group2", "artifactTest2", "1.0.0")
                ))
            )
        )

        Truth.assertThat(processor.isOldDependencyFile(
            File("test/group/artifactTest/1.0/artifactTest.aar"))).isTrue()

        Truth.assertThat(processor.isNewDependencyFile(
            File("test/group/artifactTest/1.0/artifactTest.aar"))).isFalse()

        Truth.assertThat(processor.isOldDependencyFile(
            File("test\\group\\artifactTest\\1.0\\artifactTest.aar"))).isTrue()

        Truth.assertThat(processor.isNewDependencyFile(
            File("test2\\group2\\artifactTest2\\1.0\\artifactTest2.aar"))).isTrue()

        Truth.assertThat(processor.isOldDependencyFile(
            File("test.group/artifactTest/1.0/artifactTest.aar"))).isTrue()

        Truth.assertThat(processor.isOldDependencyFile(
            File("test.group\\artifactTest\\1.0\\artifactTest.aar"))).isTrue()

        Truth.assertThat(processor.isOldDependencyFile(
            File("test2/group2/artifactTest2/1.0/artifactTest2.aar"))).isFalse()

        Truth.assertThat(processor.isNewDependencyFile(
            File("test2/group2/artifactTest2/1.0/artifactTest2.aar"))).isTrue()

        Truth.assertThat(processor.isOldDependencyFile(
            File("random.aar"))).isFalse()

        Truth.assertThat(processor.isNewDependencyFile(
            File("random.aar"))).isFalse()

        Truth.assertThat(processor.isOldDependencyFile(
            File("test\\group\\artifactTestDoNotMatch\\1.0\\artifactTest.aar"))).isFalse()

        Truth.assertThat(processor.isNewDependencyFile(
            File("test2\\group2\\artifactTest2DoNotMatch\\1.0\\artifactTest2.aar"))).isFalse()
    }
}