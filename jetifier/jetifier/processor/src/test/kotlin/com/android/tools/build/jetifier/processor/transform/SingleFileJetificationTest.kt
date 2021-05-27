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

package com.android.tools.build.jetifier.processor.transform

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import com.google.common.truth.Truth
import org.intellij.lang.annotations.Language
import java.io.File
import org.junit.Test

/**
 * Tests that the source jetifier changes source code in the expected manner
 */
class SingleFileJetificationTest {

    @Test
    fun xmlSourceJetifiedProperly() {
        testSingleFileJetification(
            givenFileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<android.support.v7.preference.Preference/>",
            expectedOutputFileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<androidx.preference.Preference/>",
            fileExtension = ".xml",
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to "androidx/preference/Preference"
            )
        )
    }

    @Test
    fun javaSourceJetifiedProperly() {
        testSingleFileJetification(
            givenFileContent = "import android.support.v7.preference.Preference;\n" +
                "import android.support.v7.widget.CardViewBaseImpl\n" +
                "import android.support.v7.widget.CardViewApi21Impl\n" +
                "android.support.v7.widget.CardViewDelegate\n" +
                "code\n" +
                "inlineUsage(android.support.v7.widget.ThemedSpinnerAdapter variable);",
            expectedOutputFileContent = "import androidx.preference.Preference;\n" +
                "import androidx.cardview.widget.CardViewBaseImpl\n" +
                "import androidx.cardview.widget.CardViewApi21Impl\n" +
                "androidx.cardview.widget.CardViewDelegate\n" +
                "code\n" +
                "inlineUsage(androidx.appcompat.widget.ThemedSpinnerAdapter variable);",
            fileExtension = ".java",
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to
                    "androidx/preference/Preference",
                "android/support/v7/widget/CardViewApi21Impl" to
                    "androidx/cardview/widget/CardViewApi21Impl",
                "android/support/v7/widget/CardViewBaseImpl" to
                    "androidx/cardview/widget/CardViewBaseImpl",
                "android/support/v7/widget/CardViewDelegate" to
                    "androidx/cardview/widget/CardViewDelegate",
                "android/support/v7/widget/ThemedSpinnerAdapter" to
                    "androidx/appcompat/widget/ThemedSpinnerAdapter"
            )
        )
    }

    @Test
    fun javaSourceDejetifiedProperly() {
        testSingleFileJetification(
            givenFileContent = "import androidx.preference.Preference;\n" +
                "import androidx.cardview.widget.CardViewBaseImpl\n" +
                "import androidx.cardview.widget.CardViewApi21Impl\n" +
                "androidx.cardview.widget.CardViewDelegate\n" +
                "code\n" +
                "inlineUsage(androidx.appcompat.widget.ThemedSpinnerAdapter variable);",
            expectedOutputFileContent = "import android.support.v7.preference.Preference;\n" +
                "import android.support.v7.widget.CardViewBaseImpl\n" +
                "import android.support.v7.widget.CardViewApi21Impl\n" +
                "android.support.v7.widget.CardViewDelegate\n" +
                "code\n" +
                "inlineUsage(android.support.v7.widget.ThemedSpinnerAdapter variable);",
            fileExtension = ".java",
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to
                    "androidx/preference/Preference",
                "android/support/v7/widget/CardViewApi21Impl" to
                    "androidx/cardview/widget/CardViewApi21Impl",
                "android/support/v7/widget/CardViewBaseImpl" to
                    "androidx/cardview/widget/CardViewBaseImpl",
                "android/support/v7/widget/CardViewDelegate" to
                    "androidx/cardview/widget/CardViewDelegate",
                "android/support/v7/widget/ThemedSpinnerAdapter" to
                    "androidx/appcompat/widget/ThemedSpinnerAdapter"
            ),
            isReversed = true
        )
    }

    @Test
    fun doesNotJetifySubstring() {
        val sameInputOutput = "import android.support.v7.preference.PreferenceExtension;\n" +
            "code\n" +
            "inlineUsage(android.support.v7.widget.ThemedSpinnerAdapter2 variable);"
        testSingleFileJetification(
            givenFileContent = sameInputOutput,
            expectedOutputFileContent = sameInputOutput,
            fileExtension = ".java",
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to
                    "androidx/preference/Preference",
                "android/support/v7/widget/ThemedSpinnerAdapter" to
                    "androidx/appcompat/widget/ThemedSpinnerAdapter"
            )
        )
    }

    @Test
    fun doesNotSourceJetifyNonJavaOrXMLFiles() {
        testSingleFileJetification(
            givenFileContent =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<android.support.v7.preference.Preference/>",
            expectedOutputFileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<androidx.preference.Preference/>",
            fileExtension = ".aar",
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to "androidx/preference/Preference"
            ),
            expectSpecifiedOutput = false
        )
    }

    @Test
    fun kotlinSourceDejetifiedProperly() {
        @Language("kotlin")
        val input = """
            package foo
            import androidx.shiny.library.Bar
                
            fun function(): Bar {
                TODO()
            }
            var property: androidx.shiny.library.Foo? = null
        """.trimIndent()

        @Language("kotlin")
        val expected = """
            package foo
            import android.matt.library.Bar
                
            fun function(): Bar {
                TODO()
            }
            var property: android.matt.library.Foo? = null
        """.trimIndent()

        testSingleFileJetification(
            givenFileContent = input,
            expectedOutputFileContent = expected,
            typesMap = mapOf(
                "android/matt/library/Bar" to "androidx/shiny/library/Bar",
                "android/matt/library/Foo" to "androidx/shiny/library/Foo",
            ),
            fileExtension = ".kt",
            isReversed = true
        )
    }

    /**
     * Runs the whole transformation process over the given single file and verifies that
     * currently transformable files transform properly while unsupported single file
     * transformations throw the appropriate error.
     */
    private fun testSingleFileJetification(
        givenFileContent: String,
        expectedOutputFileContent: String,
        fileExtension: String,
        typesMap: Map<String, String> = emptyMap(),
        isReversed: Boolean = false,
        expectSpecifiedOutput: Boolean = true
    ) {
        val typeMap = TypesMap(typesMap.map { JavaType(it.key) to JavaType(it.value) }.toMap())
        val config = Config.fromOptional(
            typesMap = typeMap
        )
        val processor = Processor.createProcessor4(
            config = config,
            reversedMode = isReversed,
            rewritingSupportLib = false
        )

        val inputFile = File("/tmp/singleFileTestInput" + fileExtension)
        inputFile.writeText(givenFileContent)
        val outputFile = File("/tmp/singleFileTestOutput" + fileExtension)
        @Suppress("deprecation")
        processor.transform(setOf(FileMapping(inputFile, outputFile)))
        if (expectSpecifiedOutput) {
            Truth.assertThat(outputFile.readText()).isEqualTo(expectedOutputFileContent)
        } else {
            Truth.assertThat(outputFile.readText()).isNotEqualTo(expectedOutputFileContent)
        }
    }
}