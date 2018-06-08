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
import com.android.tools.build.jetifier.core.rule.RewriteRule
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

    @Test fun layout_notApplicablePrefix_shouldStillWork() {
        testRewrite(
            givenXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            expectedXml =
                "<android.test.pref.Preference>\n" +
                "</android.test.pref.Preference>",
            prefixes = setOf("android/support/v14/"),
            typesMap = mapOf(
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
                "    someAttribute=\"android.test.pref.Preference\"/>",
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

    @Test fun layout_onePrefix_oneRule_viewInText() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<test attribute=\"view\" class=\"something.Else\">",
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

    @Test fun layout_twoPrefixes_threeRules_ignoreRule_multipleRewrites() {
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
            rulesMap = RewriteRulesMap(
                RewriteRule(from = "android/support/v21/(.*)", to = "ignore")),
            typesMap = mapOf(
                "android/support/v7/preference/ListPreference"
                    to "android/test/pref/ListPref",
                "android/support/v7/preference/Preference"
                    to "android/test/pref/Preference",
                "android/support/v14/preference/DialogPreference"
                    to "android/test14/pref/DialogPreference"
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
                "android/support"
            ),
            typesMap = mapOf(
            ),
            packageMap = PackageMap(listOf(
                PackageMap.PackageRule(
                    from = "android/support/v7/preference",
                    to = "androidx/preference")
            )),
            rewritingSupportLib = true
        )
    }

    @Test fun manifestFile_packageRewrite_prefixMismatchShouldNotAffectRewrite() {
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
                "android/something/else"
            ),
            typesMap = mapOf(
            ),
            packageMap = PackageMap(listOf(
                PackageMap.PackageRule(
                    from = "android/support/v7/preference",
                    to = "androidx/preference")
            )),
            rewritingSupportLib = true
        )
    }

    @Test fun manifestFile_packageRewrite_shouldIgnore() {
        testRewrite(
            givenXml =
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "          package=\"android.support.test\">\n" +
                "</manifest>",
            expectedXml =
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "          package=\"android.support.test\">\n" +
                "</manifest>",
            prefixes = setOf(
                "android/support/"
            ),
            rulesMap = RewriteRulesMap(
                RewriteRule("android/support/test/(.*)", "ignore")
            ),
            packageMap = PackageMap.EMPTY,
            rewritingSupportLib = true
        )
    }

    @Test fun generic_sample_provider() {
        testRewrite(
            givenXml =
                "<provider\n" +
                "  android:authorities=\"support.Something\"\n" +
                "  android:name=\"support.Something\">\n" +
                "  <meta-data android:name=\"support.Something\">\n" +
                "</provider>",
            expectedXml =
                "<provider\n" +
                "  android:authorities=\"test.Something\"\n" +
                "  android:name=\"test.Something\">\n" +
                "  <meta-data android:name=\"test.Something\">\n" +
                "</provider>",
            prefixes = setOf("support/"),
            typesMap = mapOf(
                "support/Something" to "test/Something"
            )
        )
    }

    @Test fun generic_sample_intent() {
        testRewrite(
            givenXml =
                "<activity android:name=\"some\" android:configChanges=\"orientation\">\n" +
                "  <intent-filter>\n" +
                "    <action android:name=\"support.Something\" />\n" +
                "  </intent-filter>\n" +
                "</activity>",
            expectedXml =
                "<activity android:name=\"some\" android:configChanges=\"orientation\">\n" +
                "  <intent-filter>\n" +
                "    <action android:name=\"test.Something\" />\n" +
                "  </intent-filter>\n" +
                "</activity>",
            prefixes = setOf("support/"),
            typesMap = mapOf(
                "support/Something" to "test/Something"
            )
        )
    }

    @Test fun generic_sample_style() {
        testRewrite(
            givenXml =
                "<style name=\"AppCompat\" parent=\"Platform.AppCompat\">\n" +
                "  <item name=\"viewInflaterClass\">support.Something</item>\n" +
                "</style>",
            expectedXml =
                "<style name=\"AppCompat\" parent=\"Platform.AppCompat\">\n" +
                "  <item name=\"viewInflaterClass\">test.Something</item>\n" +
                "</style>",
            prefixes = setOf("support/"),
            typesMap = mapOf(
                "support/Something" to "test/Something"
            )
        )
    }

    @Test fun generic_sample_transition() {
        testRewrite(
            givenXml =
                "<transition\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:lb=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    class=\"support.Something\"\n" +
                "    lb:lb_slideEdge=\"top\" >\n" +
                "</transition>",
            expectedXml =
                "<transition\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:lb=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    class=\"test.Something\"\n" +
                "    lb:lb_slideEdge=\"top\" >\n" +
                "</transition>",
            prefixes = setOf("support/"),
            typesMap = mapOf(
                "support/Something" to "test/Something"
            )
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
        typesMap: Map<String, String> = emptyMap(),
        rulesMap: RewriteRulesMap = RewriteRulesMap.EMPTY,
        packageMap: PackageMap = PackageMap.EMPTY,
        rewritingSupportLib: Boolean = false,
        errorsExpected: Boolean = false
    ) {
        val given =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$givenXml\n"

        val expected =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$expectedXml\n"

        val typeMap = TypesMap(typesMap.map { JavaType(it.key) to JavaType(it.value) }.toMap())
        val config = Config.fromOptional(
            restrictToPackagePrefixes = prefixes,
            rulesMap = rulesMap,
            typesMap = typeMap,
            packageMap = packageMap
        )
        val context = TransformationContext(
            config,
            rewritingSupportLib = rewritingSupportLib,
            useFallbackIfTypeIsMissing = false)
        val processor = XmlResourcesTransformer(context)
        val fileName = Paths.get("random.xml")
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

