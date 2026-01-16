/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.metalava

import androidx.build.checkapi.SourceSetInputs
import java.io.File
import java.io.StringWriter
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectXmlTest {
    private fun checkElementXml(element: Element, expectedXml: String) {
        val stringWriter = StringWriter()
        ProjectXml.writeXml(element, stringWriter)
        // Clean up the output for comparison
        val xml =
            stringWriter
                .toString()
                .removePrefix("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .trim()
        assertEquals(expectedXml, xml)
    }

    // Fake files for testing
    private val root = File("/fake/path/root")
    private val compiledSources = File(root, "compiledSources.jar")
    private val sourceDir = File(root, "sources")
    private val classpathDir = File(root, "classpath")

    @Test
    fun testEmptyAndroidSourceSetXml() {
        val element =
            ProjectXml.createSourceSetElement(
                "androidMain",
                emptyList(),
                emptyList(),
                emptyList(),
                compiledSources,
                setOf(KotlinPlatformType.androidJvm),
            )

        checkElementXml(
            element,
            """
                <module name="androidMain" android="true" kotlinPlatforms="JVM [1.8]">
                  <src jar="/fake/path/root/compiledSources.jar"/>
                </module>
            """
                .trimIndent(),
        )
    }

    @Test
    fun testEmptyCommonSourceSetXml() {
        val element =
            ProjectXml.createSourceSetElement(
                "commonMain",
                emptyList(),
                emptyList(),
                emptyList(),
                compiledSources,
                setOf(KotlinPlatformType.common, KotlinPlatformType.jvm),
            )

        checkElementXml(
            element,
            """
                <module name="commonMain" kotlinPlatforms="JVM [1.8]">
                  <src jar="/fake/path/root/compiledSources.jar"/>
                </module>
            """
                .trimIndent(),
        )
    }

    @Test
    fun testSourceSetXml() {
        val element =
            ProjectXml.createSourceSetElement(
                "androidMain",
                listOf("commonMain", "jvmMain"),
                listOf(
                    File(sourceDir, "Foo.kt"),
                    File(sourceDir, "Bar.kt"),
                    File(sourceDir, "Baz.java"),
                ),
                listOf(
                    File(classpathDir, "jarDependency.jar"),
                    File(classpathDir, "androidDependency.aar"),
                    File(classpathDir, "klibDependency.klib"),
                    File(classpathDir, "directoryDependency"),
                ),
                compiledSources,
                setOf(KotlinPlatformType.androidJvm),
            )
        checkElementXml(
            element,
            """
                <module name="androidMain" android="true" kotlinPlatforms="JVM [1.8]">
                  <dep module="commonMain" kind="dependsOn"/>
                  <dep module="jvmMain" kind="dependsOn"/>
                  <src file="/fake/path/root/sources/Foo.kt"/>
                  <src file="/fake/path/root/sources/Bar.kt"/>
                  <src file="/fake/path/root/sources/Baz.java"/>
                  <classpath jar="/fake/path/root/classpath/jarDependency.jar"/>
                  <classpath aar="/fake/path/root/classpath/androidDependency.aar"/>
                  <klib file="/fake/path/root/classpath/klibDependency.klib"/>
                  <classpath dir="/fake/path/root/classpath/directoryDependency"/>
                  <src jar="/fake/path/root/compiledSources.jar"/>
                </module>
            """
                .trimIndent(),
        )
    }

    @Test
    fun testKotlinPlatformTypes() {
        val element =
            ProjectXml.createSourceSetElement(
                "commonMain",
                emptyList(),
                emptyList(),
                emptyList(),
                compiledSources,
                setOf(
                    KotlinPlatformType.common,
                    KotlinPlatformType.jvm,
                    KotlinPlatformType.androidJvm,
                    KotlinPlatformType.native,
                    KotlinPlatformType.wasm,
                    KotlinPlatformType.js,
                ),
            )
        checkElementXml(
            element,
            """
                <module name="commonMain" kotlinPlatforms="JVM [1.8]/Native []/Native [general]/Wasm [general]/JS []">
                  <src jar="/fake/path/root/compiledSources.jar"/>
                </module>
            """
                .trimIndent(),
        )
    }

    @Test
    fun testEmptyProjectXml() {
        val element = ProjectXml.createProjectElement(emptyList())
        checkElementXml(
            element,
            """
                <project>
                  <root dir="."/>
                </project>
            """
                .trimIndent(),
        )
    }

    @Test
    fun testProjectXml() {
        val element =
            ProjectXml.createProjectElement(
                listOf(
                    DocumentHelper.createElement("someElement"),
                    DocumentHelper.createElement("anotherElement"),
                )
            )
        checkElementXml(
            element,
            """
                <project>
                  <root dir="."/>
                  <someElement/>
                  <anotherElement/>
                </project>
            """
                .trimIndent(),
        )
    }

    private fun fakeSourceSetInputs(
        sourceSetName: String,
        dependsOnSourceSets: List<String>,
        project: Project,
    ): SourceSetInputs {
        return SourceSetInputs(
            sourceSetName = sourceSetName,
            dependsOnSourceSets = dependsOnSourceSets,
            sourcePaths = project.files(),
            dependencyClasspath = project.files(),
            kotlinPlatforms = emptySet(),
        )
    }

    @Test
    fun testFilterSourceSets() {
        val project = ProjectBuilder.builder().build()
        val sourceSets =
            listOf(
                fakeSourceSetInputs("commonMain", emptyList(), project),
                fakeSourceSetInputs("jvmAndAndroidMain", listOf("commonMain"), project),
                fakeSourceSetInputs("androidMain", listOf("jvmAndAndroidMain"), project),
                fakeSourceSetInputs("jvmMain", listOf("jvmAndAndroidMain"), project),
                fakeSourceSetInputs("nonJvmMain", listOf("commonMain"), project),
                fakeSourceSetInputs("webMain", listOf("nonJvmMain"), project),
                fakeSourceSetInputs("jsMain", listOf("webMain"), project),
                fakeSourceSetInputs("wasmMain", listOf("webMain"), project),
                fakeSourceSetInputs("nativeMain", listOf("nonJvmMain"), project),
                fakeSourceSetInputs("linuxMain", listOf("nativeMain"), project),
                fakeSourceSetInputs("appleMain", listOf("nativeMain"), project),
                fakeSourceSetInputs("iosMain", listOf("appleMain"), project),
                fakeSourceSetInputs("watchosMain", listOf("appleMain"), project),
            )
        val filteredSourceSets =
            ProjectXml.filterSourceSets(
                sourceSets,
                mapOf(
                    "commonMain" to listOf(File("fake.kt")),
                    // Empty, but kept because androidMain depends on it
                    "jvmAndAndroidMain" to emptyList(),
                    "androidMain" to listOf(File("fake.kt")),
                    // Will be filtered
                    "jvmMain" to emptyList(),
                    "nonJvmMain" to listOf(File("fake.kt")),
                    "webMain" to listOf(File("fake.kt")),
                    "jsMain" to listOf(File("fake.kt")),
                    "wasmMain" to listOf(File("fake.kt")),
                    "nativeMain" to listOf(File("fake.kt")),
                    // Will be filtered
                    "linuxMain" to emptyList(),
                    // Will be filtered (first iosMain and watchosMain, and then appleMain)
                    "appleMain" to emptyList(),
                    "iosMain" to emptyList(),
                    "watchosMain" to emptyList(),
                ),
            )
        assertEquals(
            filteredSourceSets.map { it.sourceSetName },
            listOf(
                "commonMain",
                "jvmAndAndroidMain",
                "androidMain",
                "nonJvmMain",
                "webMain",
                "jsMain",
                "wasmMain",
                "nativeMain",
            ),
        )
    }
}
