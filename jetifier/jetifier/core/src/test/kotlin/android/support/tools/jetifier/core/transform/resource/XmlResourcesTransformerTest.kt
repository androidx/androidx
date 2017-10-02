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
import android.support.tools.jetifier.core.rules.RewriteRule
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.charset.Charset

class XmlResourcesTransformerTest {

    @Test fun layout_noPrefix_noChange() {
        testRewriteToTheSame(
            // Given & expected
            "<android.support.v7.preference.Preference>\n" +
            "</android.support.v7.preference.Preference>",
            // Prefixes
            listOf(),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{1}"))
        )
    }

    @Test fun layout_noRule_noChange() {
        testRewriteToTheSame(
            // Given & expected
            "<android.support.v7.preference.Preference>\n" +
            "</android.support.v7.preference.Preference>",
            // Prefixes
            listOf("android/support/v7/"),
            // Rules
            listOf()
        )
    }

    @Test fun layout_notApplicablePrefix_noChange() {
        testRewriteToTheSame(
            // Given & expected
            "<android.support.v7.preference.Preference>\n" +
            "</android.support.v7.preference.Preference>",
            // Prefixes
            listOf("android/support/v14/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{1}"))
        )
    }

    @Test fun layout_notApplicablePrefix2_noChange() {
        testRewriteToTheSame(
            // Given & expected
            "<my.android.support.v7.preference.Preference>\n" +
            "</my.android.support.v7.preference.Preference>",
            // Prefixes
            listOf("android/support/v7/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{1}"))
        )
    }

    @Test fun layout_notApplicableRule_noChange() {
        testRewriteToTheSame(
            // Given & expected
            "<android.support.v7.preference.Preference>\n" +
            "</android.support.v7.preference.Preference>",
            // Prefixes
            listOf("android/support/"),
            // Rules
            listOf(RewriteRule(from = "android/support2/v7/(.*)", to = "android/test/{1}"))
        )
    }

    @Test fun layout_onePrefix_oneRule_oneRewrite() {
        testRewrite(
            // Given
            "<android.support.v7.preference.Preference/>",
            // Expected
            "<android.test.preference.Preference/>",
            // Prefixes
            listOf("android/support/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{0}"))
        )
    }

    @Test fun layout_onePrefix_oneRule_attribute_oneRewrite() {
        testRewrite(
            // Given
            "<android.support.v7.preference.Preference \n" +
            "    someAttribute=\"android.support.v7.preference.Preference\"/>",
            // Expected
            "<android.test.preference.Preference \n" +
            "    someAttribute=\"android.support.v7.preference.Preference\"/>",
            // Prefixes
            listOf("android/support/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{0}"))
        )
    }

    @Test fun layout_onePrefix_oneRule_twoRewrites() {
        testRewrite(
            // Given
            "<android.support.v7.preference.Preference>\n" +
            "</android.support.v7.preference.Preference>",
            // Expected
            "<android.test.preference.Preference>\n" +
            "</android.test.preference.Preference>",
            // Prefixes
            listOf("android/support/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{0}"))
        )
    }

    @Test fun layout_onePrefix_oneRule_viewTag_simple() {
        testRewrite(
            // Given
            "<view class=\"android.support.v7.preference.Preference\">",
            // Expected
            "<view class=\"android.test.preference.Preference\">",
            // Prefixes
            listOf("android/support/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{0}"))
        )
    }

    @Test fun layout_onePrefix_oneRule_viewTag_stuffAround() {
        testRewrite(
            // Given
            "<view notRelated=\"true\" " +
            "      class=\"android.support.v7.preference.Preference\"" +
            "      ignoreMe=\"android.support.v7.preference.Preference\">",
            // Expected
            "<view notRelated=\"true\" " +
            "      class=\"android.test.preference.Preference\"" +
            "      ignoreMe=\"android.support.v7.preference.Preference\">",
            // Prefixes
            listOf("android/support/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{0}"))
        )
    }

    @Test fun layout_onePrefix_oneRule_viewInText_notMatched() {
        testRewriteToTheSame(
            // Given & expected
            "<test attribute=\"view\" class=\"android.support.v7.preference.Preference\">",
            // Prefixes
            listOf("android/support/"),
            // Rules
            listOf(RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{0}"))
        )
    }

    @Test fun layout_twoPrefixes_threeRules_multipleRewrites() {
        testRewrite(
            // Given
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
            // Expected
            "<android.test.preference.Preference>\n" +
            "  <android.test14.preference.DialogPreference" +
            "      someAttribute=\"someValue\"/>\n" +
            "  <android.test14.preference.DialogPreference" +
            "      someAttribute=\"someValue2\"/>\n" +
            "  <!-- This one should be ignored --> \n" +
            "  <android.support.v21.preference.DialogPreference" +
            "      someAttribute=\"someValue2\"/>\n" +
            "</android.test.preference.Preference>\n" +
            "\n" +
            "<android.test.preference.ListPreference/>",
            // Prefixes
            listOf(
                "android/support/v7/",
                "android/support/v14/"
            ),
            // Rules
            listOf(
                RewriteRule(from = "android/support/v7/(.*)", to = "android/test/{0}"),
                RewriteRule(from = "android/support/v14/(.*)", to = "android/test14/{0}"),
                RewriteRule(from = "android/support/v21/(.*)", to = "android/test21/{0}")
            )
        )
    }

    private fun testRewriteToTheSame(
            givenXml : String, prefixes: List<String>, rules: List<RewriteRule>) {
        testRewrite(givenXml, givenXml, prefixes, rules)
    }

    private fun testRewrite(givenXml : String,
                            expectedXml : String,
                            prefixes: List<String>,
                            rules: List<RewriteRule>) {
        val given =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$givenXml\n"

        val expected =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "$expectedXml\n"

        val processor = XmlResourcesTransformer(Config(prefixes, rules, emptyList()))
        val result = processor.transform(given.toByteArray())
        val strResult = result.toString(Charset.defaultCharset())

        Truth.assertThat(strResult).isEqualTo(expected)
    }
}

