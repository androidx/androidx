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

package com.android.tools.build.jetifier.processor.transform.resource

import com.android.tools.build.jetifier.core.PackageMap
import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.Processor
import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

class AnnotationsTransformationTest {

    @Test fun transformSampleAnnotationFile_oneToOne() {
        testRewrite(
            givenXmls = listOf(
                Paths.get("androidx/move/me/annotations.xml") to
                "<root>\n" +
                "  <item name=\"androidx.appcompat.app.ActionBar void setMode(int) 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "      <val name=\"value\" val=\"{" +
                "androidx.appcompat.app.ActionBar.NAVIGATION_MODE_STANDARD, " +
                "androidx.appcompat.app.ActionBar.NAVIGATION_MODE_LIST}\" />\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "</root>"
            ),
            expectedXml =
                "<root>\n" +
                "  <item name=\"android.support.appcompat.app.ActionBar void setMode(int) 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "      <val name=\"value\" val=\"{" +
                "android.support.appcompat.app.ActionBar.NAVIGATION_MODE_STANDARD, " +
                "android.support.appcompat.app.ActionBar.NAVIGATION_MODE_LIST}\" />\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "</root>",
            expectedPath = "android/support/moved/annotations.xml",
            reversedPrefixes = setOf("androidx/"),
            typesMap = mapOf(
                "android/support/appcompat/app/ActionBar"
                    to "androidx/appcompat/app/ActionBar"
            ),
            rulesMap = RewriteRulesMap(
                listOf(
                    RewriteRule(
                        "android/support/appcompat/app/ActionBar/(.*)",
                        "androidx/appcompat/app/ActionBar/(.*)"
                    ),
                    RewriteRule(
                        "android/support/moved/(.*)",
                        "androidx/move/me/(.*)"
                    )
                )),
            reversedMode = true
        )
    }

    @Test fun transformSampleAnnotationFiles_shouldBeMergedIntoOne() {
        testRewrite(
            givenXmls = listOf(
                Paths.get("androidx/move/me/annotations.xml") to
                "<root>\n" +
                "  <item name=\"androidx.appcompat.app.ActionBar void setMode(int) 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "      <val name=\"value\" val=\"{" +
                "androidx.appcompat.app.ActionBar.NAVIGATION_MODE_STANDARD, " +
                "androidx.appcompat.app.ActionBar.NAVIGATION_MODE_LIST}\" />\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "</root>\n",
                Paths.get("android/support/moved/annotations.xml") to
                "<root>\n" +
                "  <item name=\"hello.appcompat.app.ActionBar void setMode(int) 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "</root>\n",
                Paths.get("androidx/move/me/annotations.xml") to
                "<root>\n" +
                "  <item name=\"androidx.fragment.Fragment void setMode() 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "</root>"
            ),
            expectedXml =
                "<root>\n" +
                "  <item name=\"android.support.appcompat.app.ActionBar void setMode(int) 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "      <val name=\"value\" val=\"{" +
                "android.support.appcompat.app.ActionBar.NAVIGATION_MODE_STANDARD, " +
                "android.support.appcompat.app.ActionBar.NAVIGATION_MODE_LIST}\" />\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "  <item name=\"hello.appcompat.app.ActionBar void setMode(int) 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "  <item name=\"android.support.fragment.Fragment void setMode() 0\">\n" +
                "    <annotation name=\"android.support.annotation.IntDef\">\n" +
                "    </annotation>\n" +
                "  </item>\n" +
                "</root>",
            expectedPath = "android/support/moved/annotations.xml",
            reversedPrefixes = setOf("androidx/"),
            typesMap = mapOf(
                "android/support/appcompat/app/ActionBar"
                    to "androidx/appcompat/app/ActionBar"
            ),
            rulesMap = RewriteRulesMap(
                listOf(
                    RewriteRule(
                        "android/support/appcompat/app/ActionBar/(.*)",
                        "androidx/appcompat/app/ActionBar/(.*)"
                    ),
                    RewriteRule(
                        "android/support/fragment/(.*)",
                        "androidx/fragment/(.*)"
                    ),
                    RewriteRule(
                        "android/support/moved/(.*)",
                        "androidx/move/me/(.*)"
                    )
                )),
            reversedMode = true
        )
    }

    private fun testRewrite(
        givenXmls: List<Pair<Path, String>>,
        expectedXml: String,
        expectedPath: String,
        reversedPrefixes: Set<String> = emptySet(),
        typesMap: Map<String, String> = emptyMap(),
        rulesMap: RewriteRulesMap = RewriteRulesMap.EMPTY,
        packageMap: PackageMap = PackageMap.EMPTY,
        reversedMode: Boolean = false
    ) {
        val typeMap = TypesMap(typesMap.map { JavaType(it.key) to JavaType(it.value) }.toMap())
        val config = Config.fromOptional(
            reversedRestrictToPackagesPrefixes = reversedPrefixes,
            rulesMap = rulesMap,
            typesMap = typeMap,
            packageMap = packageMap
        )

        val archive = Archive(
            relativePath = Paths.get(""),
            files = givenXmls
                .map { ArchiveFile(it.first, data = addXmlHeader(it.second).toByteArray()) }
                .toList()
        )

        @Suppress("deprecation")
        Processor
            .createProcessor(config = config, reversedMode = reversedMode)
            .visit(archive)

        val mergedFile = archive.files.first() as ArchiveFile
        val strResult = mergedFile.data.toString(Charset.defaultCharset())

        Truth.assertThat(archive.files.size).isEqualTo(1)
        Truth.assertThat(strResult).isEqualTo(addXmlHeader(expectedXml))
        Truth.assertThat(mergedFile.relativePath.toString()).isEqualTo(expectedPath)
    }

    private fun addXmlHeader(innerXml: String): String {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "$innerXml\n"
    }
}