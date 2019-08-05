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

package com.android.tools.build.jetifier.processor.transform.proguard

import org.junit.Test

class ProGuardTypesMapperTest {

    @Test fun proGuard_typeMapper_wildcard_simple() {
        ProGuardTester()
            .testThatGivenType("*")
            .getsRewrittenTo("*")
    }

    @Test fun proGuard_typeMapper_wildcard_double() {
        ProGuardTester()
            .testThatGivenType("**")
            .getsRewrittenTo("**")
    }

    @Test fun proGuard_typeMapper_wildcard_composed() {
        ProGuardTester()
            .testThatGivenType("**/*")
            .getsRewrittenTo("**/*")
    }

    @Test fun proGuard_typeMapper_wildcard_viaMap() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMap(
                "support/v7/*" to "test/v7/*"
            )
            .testThatGivenType("support.v7.*")
            .getsRewrittenTo("test.v7.*")
    }

    @Test fun proGuard_typeMapper_wildcard_viaMap2() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMap(
                "support/v7/**" to "test/v7/**"
            )
            .testThatGivenType("support.v7.**")
            .getsRewrittenTo("test.v7.**")
    }

    @Test fun proGuard_typeMapper_wildcard_viaTypesMap() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/Activity" to "test/v7/Activity"
            )
            .testThatGivenType("support.v7.Activity")
            .getsRewrittenTo("test.v7.Activity")
    }

    @Test fun proGuard_typeMapper_wildcard_notFoundInMap() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMap(
                "support/**" to "test/**"
            )
            .testThatGivenType("keep.me.**")
            .getsRewrittenTo("keep.me.**")
    }

    @Test fun proGuard_typeMapper_differentPrefix_stillRewritten() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "hello/Activity" to "test/Activity"
            )
            .testThatGivenType("hello.Activity")
            .getsRewrittenTo("test.Activity")
    }

    @Test fun proGuard_typeMapper_differentPrefix_wildcard_getsRewritten() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMap(
                "hello/**" to "test/**"
            )
            .testThatGivenType("hello.**")
            .getsRewrittenTo("test.**")
    }

    @Test fun proGuard_typeMapper_innerClass() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .testThatGivenType("support.Activity\$InnerClass")
            .getsRewrittenTo("test.Activity\$InnerClass")
    }

    @Test fun proGuard_typeMapper_innerWithStar() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .testThatGivenType("support.Activity\$*")
            .getsRewrittenTo("test.Activity\$*")
    }

    @Test fun proGuard_typeMapper_innerWithDoubleStar() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .testThatGivenType("support.Activity\$**")
            .getsRewrittenTo("test.Activity\$**")
    }

    @Test fun proGuard_typeMapper_innerClass_wildcard() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMap(
                "**R\$Attrs" to "**R2\$Attrs"
            )
            .testThatGivenType("**R\$Attrs")
            .getsRewrittenTo("**R2\$Attrs")
    }

    @Test fun proGuard_argsMapper_tripleDots() {
        ProGuardTester()
            .testThatGivenArguments("...")
            .getRewrittenTo("...")
    }

    @Test fun proGuard_argsMapper_wildcard() {
        ProGuardTester()
            .testThatGivenArguments("*")
            .getRewrittenTo("*")
    }

    @Test fun proGuard_argsMapper_wildcards() {
        ProGuardTester()
            .testThatGivenArguments("**, **")
            .getRewrittenTo("**, **")
    }

    @Test fun proGuard_argsMapper_viaMaps() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .forGivenProGuardMap(
                "support/v7/**" to "test/v7/**"
            )
            .testThatGivenArguments("support.Activity, support.v7.**, keep.Me")
            .getRewrittenTo("test.Activity, test.v7.**, keep.Me")
    }

    @Test fun proGuard_argsMapper_viaMaps_spaces() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .forGivenProGuardMap(
                "support/v7/**" to "test/v7/**"
            )
            .testThatGivenArguments(" support.Activity , \t support.v7.**,  keep.Me ")
            .getRewrittenTo("test.Activity, test.v7.**, keep.Me")
    }

    @Test fun proGuard_shouldIgnore() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenRules(
                "support/v7/Activity" to "ignore"
            )
            .testThatGivenType("support.v7.Activity")
            .getsRewrittenTo("support.v7.Activity")
    }

    @Test fun proGuard_shouldIgnore_withWildcard() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenRules(
                "support/v7/(.*)" to "ignore"
            )
            .testThatGivenType("support.v7.**")
            .getsRewrittenTo("support.v7.**")
    }

    @Test(expected = AssertionError::class)
    fun proGuard_shouldNotIgnore() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenRules(
                "support/v7/Activity" to "ignoreInPreprocessor"
            )
            .testThatGivenType("support.v7.Activity")
            .getsRewrittenTo("support.v7.Activity")
    }

    @Test fun proGuard_solver_wildcard_shouldRewrite() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/preference/Preference" to "test/preference/Preference",
                "support/v4/preference/PreferenceDialog" to "test/preference/PreferenceDialog",
                "support/v4/preference/SomethingElse" to "test/preference/SomethingElse",
                "support/v7/Random" to "test/Random"
            )
            .testThatGivenType("support.v?.preference.**")
            .getsRewrittenTo("test.preference.**")
    }

    @Test fun proGuard_solver_wildcard2_shouldRewrite() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/preference/Preference" to "test/preference/Preference",
                "support/v4/preference/SomethingElse" to "test/preference/SomethingElse",
                "support/v7/Random" to "test/Random"
            )
            .testThatGivenType("support.*.preference.**")
            .getsRewrittenTo("test.preference.**")
    }

    @Test fun proGuard_solver_wildcard3_shouldRewrite() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/preference/Preference" to "test/preference/Preference",
                "support/v4/preference/SomethingElse" to "test/preference/SomethingElse",
                "support/v4/preference/internal/Something" to "test/preference/internal/Something",
                "support/v7/Random" to "test/Random"
            )
            .testThatGivenType("support.*.preference.**")
            .getsRewrittenTo("test.preference.**")
    }

    @Test fun proGuard_solver_wildcard4_shouldRewrite() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/preference/Preference" to "test/preference/Preference",
                "support/v4/preference/PreferenceDialog" to "test/preference/PreferenceDialog",
                "support/v7/Random" to "test2/Random"
            )
            .testThatGivenType("support.**")
            .getsRewrittenTo("test.**", "test2.**")
    }

    @Test fun proGuard_solver_wildcard_needToMapPartOfClass_shouldRewrite() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/preference/Preference" to "test/preference/Preference",
                "support/v4/preference/PreferenceDialog" to "test/preference/PreferenceDialog",
                "support/v4/preference/SomethingElse" to "test/preference/SomethingElse"
            )
            .testThatGivenType("support.v7.preference.Preference*")
            .getsRewrittenTo("test.preference.Preference*")
    }

    @Test fun proGuard_solver_wildcard_oneToOne_shouldRewrite() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/preference/Preference" to "test/preference/Preference",
                "support/v4/preference/PreferenceDialog" to "test/preference/PreferenceDialog"
            )
            .testThatGivenType("support.v?.preference.Preference")
            .getsRewrittenTo("test.preference.Preference")
    }

    @Test fun proGuard_solver_wildcard_oneToOneStar_shouldRewrite() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/v7/preference/Preference" to "test/preference/Preference"
            )
            .testThatGivenType("support.v?.preference.Preference*")
            .getsRewrittenTo("test.preference.Preference*")
    }
}