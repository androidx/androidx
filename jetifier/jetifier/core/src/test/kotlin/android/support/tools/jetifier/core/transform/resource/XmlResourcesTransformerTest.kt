/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.resource

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.map.TypesMap
import android.support.tools.jetifier.core.transform.TransformationContext
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.charset.Charset

class XmlResourcesTransformerTest {

    @Test fun layout_noPrefix_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            prefixes = listOf(),
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
            prefixes = listOf("android/support/v7/"),
            map = mapOf()
        )
    }

    @Test fun layout_notApplicablePrefix_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<android.support.v7.preference.Preference>\n" +
                "</android.support.v7.preference.Preference>",
            prefixes = listOf("android/support/v14/"),
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
            prefixes = listOf("android/support/v7/"),
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
            prefixes = listOf("android/support/"),
            map = mapOf(
                "android/support2/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_oneRewrite() {
        testRewrite(
            givenXml =
                "<android.support.v7.preference.Preference/>",
            expectedXml =
                "<android.test.pref.Preference/>",
            prefixes = listOf("android/support/"),
            map = mapOf(
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
            prefixes = listOf("android/support/"),
            map =  mapOf(
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
            prefixes = listOf("android/support/"),
            map = mapOf(
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
            prefixes = listOf("android/support/"),
            map = mapOf(
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
            prefixes = listOf("android/support/"),
            map = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
            )
        )
    }

    @Test fun layout_onePrefix_oneRule_viewInText_notMatched() {
        testRewriteToTheSame(
            givenAndExpectedXml =
                "<test attribute=\"view\" class=\"android.support.v7.preference.Preference\">",
            prefixes = listOf("android/support/"),
            map = mapOf(
                "android/support/v7/preference/Preference" to "android/test/pref/Preference"
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
            prefixes = listOf(
                "android/support/v7/",
                "android/support/v14/"
            ),
            map = mapOf(
                "android/support/v7/preference/ListPreference" to "android/test/pref/ListPref",
                "android/support/v7/preference/Preference" to "android/test/pref/Preference",
                "android/support/v14/preference/DialogPreference" to "android/test14/pref/DialogPreference",
                "android/support/v21/preference/DialogPreference" to "android/test21/pref/DialogPreference"
            )
        )
    }

    private fun testRewriteToTheSame(givenAndExpectedXml: String,
                                     prefixes: List<String>,
                                     map: Map<String, String>) {
        testRewrite(givenAndExpectedXml, givenAndExpectedXml, prefixes, map)
    }

    private fun testRewrite(givenXml : String,
                            expectedXml : String,
                            prefixes: List<String>,
                            map: Map<String, String>) {
        val given =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$givenXml\n"

        val expected =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$expectedXml\n"

        val typesMap = TypesMap(map.map{ JavaType(it.key) to JavaType(it.value) }.toMap(),
            emptyMap())
        val config = Config(prefixes, emptyList(), emptyList(), typesMap)
        val context = TransformationContext(config)
        val processor = XmlResourcesTransformer(context)
        val result = processor.transform(given.toByteArray())
        val strResult = result.toString(Charset.defaultCharset())

        Truth.assertThat(strResult).isEqualTo(expected)
    }
}

