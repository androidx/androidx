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

package android.support.tools.jetifier.core.map

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.rules.RewriteRule
import android.support.tools.jetifier.core.transform.proguard.ProGuardTypesMap
import com.google.common.truth.Truth
import org.junit.Test

class MapGenerationTest {

    @Test fun fromOneType_toOneType() {
        ScanTester
            .testThatRules(
                RewriteRule("android/support/v7/(.*)", "android/test/{0}")
            )
            .withAllowedPrefixes(
                "android/support/"
            )
            .forGivenTypes(
                JavaType("android/support/v7/pref/Preference")
            )
            .mapInto(
                types = mapOf(
                    "android/support/v7/pref/Preference" to "android/test/pref/Preference"
                )
            )
            .andIsComplete()
    }

    @Test fun fromTwoTypes_toOneType_prefixRespected() {
        ScanTester
            .testThatRules(
                RewriteRule("android/support/v7/(.*)", "android/test/{0}"),
                RewriteRule("android/support/v14/(.*)", "android/test/{0}")
            )
            .withAllowedPrefixes(
                "android/support/v7/"
            )
            .forGivenTypes(
                JavaType("android/support/v7/pref/Preference"),
                JavaType("android/support/v14/pref/PreferenceDialog")
            )
            .mapInto(
                types = mapOf(
                    "android/support/v7/pref/Preference" to "android/test/pref/Preference"
                )
            )
            .andIsComplete()
    }

    @Test fun fromTwoTypes_toTwoTypes_distinctRules() {
        ScanTester
            .testThatRules(
                RewriteRule("android/support/v7/(.*)", "android/test/{0}"),
                RewriteRule("android/support/v14/(.*)", "android/test/{0}")
            )
            .withAllowedPrefixes(
                "android/support/v7/",
                "android/support/v14/"
            )
            .forGivenTypes(
                JavaType("android/support/v7/pref/Preference"),
                JavaType("android/support/v14/pref/PreferenceDialog")
            )
            .mapInto(
                types = mapOf(
                    "android/support/v7/pref/Preference" to "android/test/pref/Preference",
                    "android/support/v14/pref/PreferenceDialog"
                        to "android/test/pref/PreferenceDialog"
                )
            )
            .andIsComplete()
    }

    @Test fun fromTwoTypes_toTwoTypes_respectsOrder() {
        ScanTester
            .testThatRules(
                RewriteRule("android/support/v14/(.*)", "android/test/{0}"),
                RewriteRule("android/support/(.*)", "android/fallback/{0}")
            )
            .withAllowedPrefixes(
                "android/support/"
            )
            .forGivenTypes(
                JavaType("android/support/v7/pref/Preference"),
                JavaType("android/support/v14/pref/PreferenceDialog")
            )
            .mapInto(
                types = mapOf(
                    "android/support/v7/pref/Preference" to "android/fallback/v7/pref/Preference",
                    "android/support/v14/pref/PreferenceDialog"
                        to "android/test/pref/PreferenceDialog"
                )
            )
            .andIsComplete()
    }

    @Test fun mapTwoTypes_shouldIgnoreFirstTwo() {
        ScanTester
            .testThatRules(
                RewriteRule("android/support/v7/(.*)", "ignore"),
                RewriteRule("android/support/v8/(.*)", "ignoreInPreprocessorOnly"),
                RewriteRule("android/support/v14/(.*)", "android/test/{0}")
            )
            .withAllowedPrefixes(
                "android/support/"
            )
            .forGivenTypes(
                JavaType("android/support/v7/pref/Preference"),
                JavaType("android/support/v8/pref/Preference"),
                JavaType("android/support/v14/pref/Preference")
            )
            .mapInto(
                types = mapOf(
                    "android/support/v14/pref/Preference" to "android/test/pref/Preference"
                )
            )
            .andIsComplete()
    }

    object ScanTester {

        fun testThatRules(vararg rules: RewriteRule) = Step1(rules.toList())


        class Step1(private val rules: List<RewriteRule>) {

            fun withAllowedPrefixes(vararg prefixes: String) = Step2(rules, prefixes.toList())


            class Step2(private val rules: List<RewriteRule>, private val prefixes: List<String>) {

                private val allTypes: MutableList<JavaType> = mutableListOf()
                private var wasMapIncomplete = false


                fun forGivenTypes(vararg types: JavaType): Step2 {
                    allTypes.addAll(types)
                    return this
                }


                fun mapInto(types: Map<String, String>): Step2 {
                    val config = Config(
                        restrictToPackagePrefixes = prefixes,
                        rewriteRules = rules,
                        pomRewriteRules = emptyList(),
                        typesMap = TypesMap.EMPTY,
                        proGuardMap = ProGuardTypesMap.EMPTY)
                    val scanner = MapGeneratorRemapper(config)

                    allTypes.forEach { scanner.rewriteType(it) }

                    val typesMap = scanner.createTypesMap().toJson()
                    wasMapIncomplete = scanner.isMapNotComplete

                    Truth.assertThat(typesMap.types).containsExactlyEntriesIn(types)
                    return this
                }

                fun andIsNotComplete() {
                    Truth.assertThat(wasMapIncomplete).isTrue()
                }

                fun andIsComplete() {
                    Truth.assertThat(wasMapIncomplete).isFalse()
                }
            }

        }

    }
}

