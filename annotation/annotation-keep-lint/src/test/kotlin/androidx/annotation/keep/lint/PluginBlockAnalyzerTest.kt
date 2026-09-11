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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginBlockAnalyzerTest {

    @Test
    fun testNullOrEmptyDefaults() {
        val analysisNull = PluginBlockAnalyzer.analyze(null, isKts = true)
        assertEquals("    ", analysisNull.indent)
        assertEquals("\"", analysisNull.quote)
        assertNull(analysisNull.anchorLine)
        assertFalse(analysisNull.hasPluginsBlock)

        val analysisEmpty = PluginBlockAnalyzer.analyze("plugins {\n}", isKts = false)
        assertEquals("    ", analysisEmpty.indent)
        assertEquals("'", analysisEmpty.quote)
        assertEquals("plugins {", analysisEmpty.anchorLine)
        assertTrue(analysisEmpty.hasPluginsBlock)
    }

    @Test
    fun testTwoSpaceIndentAndLastPluginLine() {
        val block =
            """
            plugins {
              id("com.android.application")
            }
            """
                .trimIndent()

        val analysis = PluginBlockAnalyzer.analyze(block, isKts = true)
        assertEquals("  ", analysis.indent)
        assertEquals("\"", analysis.quote)
        assertEquals("  id(\"com.android.application\")", analysis.anchorLine)
        assertTrue(analysis.hasPluginsBlock)
    }

    @Test
    fun testMultiplePluginsDetectsLastPluginLine() {
        val block =
            """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }
            """
                .trimIndent()

        val analysis = PluginBlockAnalyzer.analyze(block, isKts = true)
        assertEquals("    ", analysis.indent)
        assertEquals("    id(\"org.jetbrains.kotlin.android\")", analysis.anchorLine)
    }

    @Test
    fun testTabIndentDetection() {
        val block = "plugins {\n\tid 'com.android.application'\n}"

        val analysis = PluginBlockAnalyzer.analyze(block, isKts = false)
        assertEquals("\t", analysis.indent)
        assertEquals("\tid 'com.android.application'", analysis.anchorLine)
    }

    @Test
    fun testCommentsDoNotDistortIndentOrAnchors() {
        val block =
            """
            plugins {
                // Android application plugin
                id("com.android.application")
            }
            """
                .trimIndent()

        val analysis = PluginBlockAnalyzer.analyze(block, isKts = true)
        assertEquals("    ", analysis.indent)
        assertEquals("    id(\"com.android.application\")", analysis.anchorLine)
    }

    @Test
    fun testGroovyDoubleQuotes() {
        val block =
            """
            plugins {
                id "com.android.application"
            }
            """
                .trimIndent()

        val analysis = PluginBlockAnalyzer.analyze(block, isKts = false)
        assertEquals("\"", analysis.quote)
    }

    @Test
    fun testGroovySingleQuotes() {
        val block =
            """
            plugins {
                id 'com.android.application'
            }
            """
                .trimIndent()

        val analysis = PluginBlockAnalyzer.analyze(block, isKts = false)
        assertEquals("'", analysis.quote)
    }

    @Test
    fun testKotlinDslAlwaysDoubleQuotes() {
        val block =
            """
            // 'single quoted comment'
            plugins {
                id("com.android.application")
            }
            """
                .trimIndent()

        val analysis = PluginBlockAnalyzer.analyze(block, isKts = true)
        assertEquals("\"", analysis.quote)
    }

    @Test
    fun testNoPluginsBlock() {
        val script =
            """
            apply plugin: 'com.android.application'

            dependencies {
                implementation 'foo:bar:1.0'
            }
            """
                .trimIndent()

        val analysis = PluginBlockAnalyzer.analyze(script, isKts = false)
        assertFalse(analysis.hasPluginsBlock)
        assertNull(analysis.anchorLine)
    }
}
