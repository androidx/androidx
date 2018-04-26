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

package com.android.tools.build.jetifier.processor.transform.resource

import com.android.tools.build.jetifier.core.PackageMap
import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.proguard.ProGuardTypesMap
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.charset.Charset
import java.nio.file.Paths

class XmlResourcesTransformerTest {

    @Test fun layout_noPrefix_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            prefixes = setOf(),
            map = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_noRule_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            prefixes = setOf("android/support/v7/"),
            map = mapOf(),
            errorsExpected = true
        )
    }

    @Test fun layout_notApplicablePrefix_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            prefixes = setOf("android/support/v14/"),
            map = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_notApplicablePrefix2_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<my.android.support.v7.preference.Preference>\n" +
                "</my.android.support.v7.preference.Preference>",
            prefixes = setOf("android/support/v7/"),
            map = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_notApplicableRule_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            prefixes = setOf("android/support/"),
            map = mapOf(
                "android/support2/v7/preference/Preference" to "android/test/pref/Preference"
            ),
            errorsExpected = true
        )
    }

    @Test fun layout_onePrefix_oneRule_oneRewrite() {
        testRewrite(
            givenXml =
                "<android.support.v7.preference.Preference/>",
            expectedXml =
                "<android.test.pref.Preference/>",
            prefixes = setOf("android/support/"),
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_attribute_oneRewrite() {
        testRewrite(
            givenXml =
                "<android.support.v7.preference.Preference \n" +
                "    someAttribute=\"android.support.v7.preference.Preference\"/>",
            expectedXml =
                "<android.test.pref.Preference \n" +
                "    someAttribute=\"android.support.v7.preference.Preference\"/>",
            prefixes = setOf("android/support/"),
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_twoRewrites() {
        testRewrite(
            givenXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            expectedXml =
                "<android.test.pref.Preference>\n" +
                "</android.test.pref.Preference>",
            prefixes = setOf("android/support/"),
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_viewTag_simple() {
        testRewrite(
            givenXml =
                "<view class=\"android.support.v7.preference.Preference\">",
            expectedXml =
                "<view class=\"android.test.pref.Preference\">",
            prefixes = setOf("android/support/"),
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_viewTag_stuffAround() {
        testRewrite(
            givenXml =
                "<view notRelated=\"true\" " +
                "      class=\"android.support.v7.preference.Preference\"" +
                "      ignoreMe=\"android.support.v7.preference.Preference\">",
            expectedXml =
                "<view notRelated=\"true\" " +
                "      class=\"android.test.pref.Preference\"" +
                "      ignoreMe=\"android.support.v7.preference.Preference\">",
            prefixes = setOf("android/support/"),
            typesMap = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_viewInText_notMatched() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<test attribute=\"view\" class=\"android.support.v7.preference.Preference\">",
            prefixes = setOf("android/support/"),
            map = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun application_appComponentFactory() {
        testRewrite(
            givenXml =
                "<application android:appComponentFactory=\"android.support.v7.Preference\" />",
            expectedXml =
                "<application android:appComponentFactory=\"android.test.pref.Preference\" />",
            prefixes = setOf("android/support/"),
            typesMap = mapOf(
                "android/support/v7/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_identity() {
        testRewrite(
            givenXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            expectedXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            prefixes = setOf("android/support/"),
            typesMap = mapOf(
                "android/support/v7/preference/Preference"
                    to "android/support/v7/preference/Preference"
            )
        )
    }

    @Test fun layout_twoPrefixes_threeRules_multipleRewrites() {
        testRewrite(
            givenXml =
                "<android.support.v7.preference.Preference>\n" +
                "  <android.support.v14.preference.DialogPreference" +
                "      someAttribute=\"someValue\"/>\n" +
                "  <android.support.v14.preference.DialogPreference" +
                "      someAttribute=\"someValue2\"/>\n" +
                "  <!-- This one should be ignored --> \n" +
                "  <android.support.v21.preference.DialogPreference" +
                "      someAttribute=\"someValue2\"/>\n" +
                "</android.support.v7.preference.Preference>\n" +
                "\n" +
                "<android.support.v7.preference.ListPreference/>",
            expectedXml =
                "<android.test.pref.Preference>\n" +
                "  <android.test14.pref.DialogPreference" +
                "      someAttribute=\"someValue\"/>\n" +
                "  <android.test14.pref.DialogPreference" +
                "      someAttribute=\"someValue2\"/>\n" +
                "  <!-- This one should be ignored --> \n" +
                "  <android.support.v21.preference.DialogPreference" +
                "      someAttribute=\"someValue2\"/>\n" +
                "</android.test.pref.Preference>\n" +
                "\n" +
                "<android.test.pref.ListPref/>",
            prefixes = setOf(
                "android/support/v7/",
                "android/support/v14/"
            ),
            typesMap = mapOf(
                "android/support/v7/preference/ListPreference"
                    to "android/test/pref/ListPref",
                "android/support/v7/preference/Preference"
                    to "android/test/pref/Preference",
                "android/support/v14/preference/DialogPreference"
                    to "android/test14/pref/DialogPreference",
                "android/support/v21/preference/DialogPreference"
                    to "android/test21/pref/DialogPreference"
            )
        )
    }

    @Test fun manifestFile_packageRewrite() {
        testRewrite(
            givenXml =
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "          package=\"android.support.v7.preference\">\n" +
                "    <uses-sdk android:minSdkVersion=\"14\"/>\n" +
                "</manifest>",
            expectedXml =
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "          package=\"androidx.preference\">\n" +
                "    <uses-sdk android:minSdkVersion=\"14\"/>\n" +
                "</manifest>",
            prefixes = setOf(
            ),
            typesMap = mapOf(
            ),
            packageMap = PackageMap(listOf(
                PackageMap.PackageRule(
                    from = "android/support/v7/preference",
                    to = "androidx/preference")
            )),
            rewritingSupportLib = true,
            isManifestFile = true
        )
    }

    @Test fun manifestFile_packageRewrite_chooseBasedOnArtifactName() {
        testRewrite(
            givenXml =
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "          package=\"androidx.preference\">\n" +
                "    <uses-sdk android:minSdkVersion=\"14\"/>\n" +
                "</manifest>",
            expectedXml =
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "          package=\"android.support.v14.preference\">\n" +
                "    <uses-sdk android:minSdkVersion=\"14\"/>\n" +
                "</manifest>",
            prefixes = setOf(
            ),
            typesMap = mapOf(
            ),
            packageMap = PackageMap(listOf(
                PackageMap.PackageRule(
                    from = "androidx/preference",
                    to = "android/support/v7/preference",
                    filePrefix = "preference-7"),
                PackageMap.PackageRule(
                    from = "androidx/preference",
                    to = "android/support/v14/preference",
                    filePrefix = "preference-v14")
            )),
            rewritingSupportLib = true,
            isManifestFile = true,
            libraryName = "preference-v14-28.0.0-123.aar"
        )
    }

    private fun testRewriteToTheSame(
        givenAndExpectedXml: String,
        prefixes: Set<String>,
        map: Map<String, String>,
        errorsExpected: Boolean = false
    ) {
        testRewrite(givenAndExpectedXml, givenAndExpectedXml, prefixes, map,
            errorsExpected = errorsExpected)
    }

    private fun testRewrite(
        givenXml: String,
        expectedXml: String,
        prefixes: Set<String>,
        typesMap: Map<String, String>,
        packageMap: PackageMap = PackageMap.EMPTY,
        rewritingSupportLib: Boolean = false,
        isManifestFile: Boolean = false,
        libraryName: String = "",
        errorsExpected: Boolean = false
    ) {
        val given =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$givenXml\n"

        val expected =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$expectedXml\n"

        val typeMap = TypesMap(typesMap.map { JavaType(it.key) to JavaType(it.value) }.toMap())
        val config = Config(
            restrictToPackagePrefixes = prefixes,
            rulesMap = RewriteRulesMap.EMPTY,
            slRules = emptyList(),
            pomRewriteRules = emptySet(),
            typesMap = typeMap,
            proGuardMap = ProGuardTypesMap.EMPTY,
            packageMap = packageMap
        )
        val context = TransformationContext(
            config,
            rewritingSupportLib = rewritingSupportLib,
            useFallbackIfTypeIsMissing = false)
        context.libraryName = libraryName
        val processor = XmlResourcesTransformer(context)
        val fileName = if (isManifestFile) {
            Paths.get("AndroidManifest.xml")
        } else {
            Paths.get("random.xml")
        }
        val file = ArchiveFile(fileName, given.toByteArray())
        processor.runTransform(file)
        val strResult = file.data.toString(Charset.defaultCharset())

        Truth.assertThat(strResult).isEqualTo(expected)

        if (errorsExpected) {
            Truth.assertThat(context.errorsTotal()).isAtLeast(1)
        } else {
            Truth.assertThat(context.errorsTotal()).isEqualTo(0)
        }
    }
}

