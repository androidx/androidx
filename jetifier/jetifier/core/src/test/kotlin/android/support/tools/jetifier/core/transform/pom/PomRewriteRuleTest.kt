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

package android.support.tools.jetifier.core.transform.pom

import com.google.common.truth.Truth
import org.junit.Test

class PomRewriteRuleTest {

    @Test fun versions_nullInRule_match() {
        testVersionsMatch(
            ruleVersion = null,
            pomVersion = "27.0.0"
        )
    }

    @Test fun versions_nullInPom_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = null
        )
    }

    @Test fun versions_nullBoth_match() {
        testVersionsMatch(
            ruleVersion = null,
            pomVersion = null
        )
    }

    @Test fun versions_same_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "27.0.0"
        )
    }

    @Test fun versions_same_strict_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "[27.0.0]"
        )
    }

    @Test fun versions_different_noMatch() {
        testVersionsDoNotMatch(
            ruleVersion = "27.0.0",
            pomVersion = "26.0.0"
        )
    }

    @Test fun versions_release_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "release"
        )
    }

    @Test fun versions_latest_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "latest"
        )
    }

    @Test fun versions_range_rightOpen_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "(26.0.0,]"
        )
    }

    @Test fun versions_range_rightOpen2_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "(26.0.0,)"
        )
    }

    @Test fun versions_range_inclusive_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "[21.0.0,27.0.0]"
        )
    }

    @Test fun versions_range_inclusive_noMatch() {
        testVersionsDoNotMatch(
            ruleVersion = "27.0.0",
            pomVersion = "[21.0.0,26.0.0]"
        )
    }

    @Test fun versions_range_exclusive_noMatch() {
        testVersionsDoNotMatch(
            ruleVersion = "27.0.0",
            pomVersion = "[21.0.0,27.0.0)"
        )
    }

    @Test fun versions_exclusionRange_match() {
        testVersionsMatch(
            ruleVersion = "27.0.0",
            pomVersion = "(,26.0.0),(26.0.0,)"
        )
    }

    private fun testVersionsMatch(ruleVersion: String?, pomVersion: String?) {
        val from = PomDependency(version = ruleVersion)
        val pom = PomDependency(version = pomVersion)

        val rule = PomRewriteRule(from, listOf(from))

        Truth.assertThat(rule.validateVersion(pom)).isTrue()
    }

    private fun testVersionsDoNotMatch(ruleVersion: String?, pomVersion: String?) {
        val from = PomDependency(version = ruleVersion)
        val pom = PomDependency(version = pomVersion)

        val rule = PomRewriteRule(from, listOf(from))

        Truth.assertThat(rule.validateVersion(pom)).isFalse()
    }

}