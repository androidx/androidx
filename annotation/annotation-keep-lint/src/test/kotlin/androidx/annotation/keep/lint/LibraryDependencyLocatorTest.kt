/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.annotation.keep.lint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryDependencyLocatorTest {

    private val sampleAliases = setOf("annotation.keep")

    @Test
    fun testProjectDependencySingleLineDoubleQuotes() {
        val script = "implementation(project(\":annotation:annotation-keep\"))"
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = 15 until 53,
                text = "project(\":annotation:annotation-keep\")",
            ),
            match,
        )
    }

    @Test
    fun testProjectDependencySingleLineSingleQuotes() {
        val script = "implementation(project(':annotation:annotation-keep'))"
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = 15 until 53,
                text = "project(':annotation:annotation-keep')",
            ),
            match,
        )
    }

    @Test
    fun testProjectDependencyMultiline() {
        val script =
            """
            dependencies {
                implementation(
                    project(":annotation:annotation-keep")
                )
            }
            """
                .trimIndent()

        val expectedText = "project(\":annotation:annotation-keep\")"
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testProjectDependencyMultilineExtraWhitespace() {
        val script =
            """
            dependencies {
                implementation(
                    project(
                        ":annotation:annotation-keep"
                    )
                )
            }
            """
                .trimIndent()

        val start = script.indexOf("project(")
        val end = script.indexOf(')', script.indexOf(":annotation:annotation-keep")) + 1
        val expectedText = script.substring(start, end)
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = start until end,
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testProjectDependencyGroovyNoParens() {
        val script = "implementation project ':annotation:annotation-keep'"
        val expectedText = "project ':annotation:annotation-keep'"
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testMavenCoordinateDoubleQuotes() {
        val script = "implementation \"androidx.annotation:annotation-keep:1.0.0\""
        val expectedText = "\"androidx.annotation:annotation-keep:1.0.0\""
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testMavenCoordinateSingleQuotes() {
        val script = "implementation 'androidx.annotation:annotation-keep:1.0.0'"
        val expectedText = "'androidx.annotation:annotation-keep:1.0.0'"
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testMavenCoordinateMultiline() {
        val script =
            """
            dependencies {
                implementation(
                    "androidx.annotation:annotation-keep:1.0.0"
                )
            }
            """
                .trimIndent()

        val expectedText = "\"androidx.annotation:annotation-keep:1.0.0\""
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testCatalogAccessorSingleLine() {
        val script = "implementation(deps.annotation.keep)"
        val expectedText = "deps.annotation.keep"
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script, sampleAliases)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testCatalogAccessorMultiline() {
        val script =
            """
            dependencies {
                implementation(
                    deps.annotation.keep
                )
            }
            """
                .trimIndent()

        val expectedText = "deps.annotation.keep"
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script, sampleAliases)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testCatalogAccessorCustomCatalogName() {
        val script = "implementation(myCatalog.annotation.keep)"
        val expectedText = "myCatalog.annotation.keep"
        val start = script.indexOf(expectedText)
        val match = LibraryDependencyLocator.locate(script, sampleAliases)
        assertEquals(
            DependencyMatch(
                range = start until (start + expectedText.length),
                text = expectedText,
            ),
            match,
        )
    }

    @Test
    fun testUnrelatedDependencyReturnsNull() {
        val script =
            """
            dependencies {
                implementation(project(":work:work-runtime"))
                implementation("androidx.core:core:1.12.0")
            }
            """
                .trimIndent()

        assertNull(LibraryDependencyLocator.locate(script, sampleAliases))
    }

    @Test
    fun testEmptyContentsReturnsNull() {
        assertNull(LibraryDependencyLocator.locate("", sampleAliases))
    }
}
