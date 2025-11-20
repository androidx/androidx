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

import java.io.File
import java.io.StringWriter
import org.dom4j.DocumentHelper
import org.dom4j.Element
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
            )

        checkElementXml(
            element,
            """
                <module name="androidMain" android="true" kotlinPlatforms="JVM [1.8]">
                  <src jar="/fake/path/root/compiledSources.jar"/>
                </module>
            """
                .trimIndent()
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
            )

        checkElementXml(
            element,
            """
                <module name="commonMain" kotlinPlatforms="JVM [1.8]">
                  <src jar="/fake/path/root/compiledSources.jar"/>
                </module>
            """
                .trimIndent()
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
                    File(sourceDir, "Baz.java")
                ),
                listOf(
                    File(classpathDir, "jarDependency.jar"),
                    File(classpathDir, "androidDependency.aar"),
                    File(classpathDir, "klibDependency.klib"),
                    File(classpathDir, "directoryDependency")
                ),
                compiledSources,
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
                .trimIndent()
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
                .trimIndent()
        )
    }

    @Test
    fun testProjectXml() {
        val element =
            ProjectXml.createProjectElement(
                listOf(
                    DocumentHelper.createElement("someElement"),
                    DocumentHelper.createElement("anotherElement")
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
                .trimIndent()
        )
    }
}
