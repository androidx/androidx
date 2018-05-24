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

package com.android.tools.build.jetifier.core.config

import com.google.common.truth.Truth
import org.junit.Test

class ConfigParserTest {

    @Test fun parseConfig_validInput() {
        val confStr =
            "{\n" +
            "    restrictToPackagePrefixes: [\"android/support/\"],\n" +
            "    reversedRestrictToPackagePrefixes: [\"androidx/\"],\n" +
            "    # Sample comment \n" +
            "    rules: [\n" +
            "        {\n" +
            "            from: \"android/support/v14/preferences/(.*)\",\n" +
            "            to: \"android/jetpack/prefs/main/{0}\"\n" +
            "        },\n" +
            "        {\n" +
            "            from: \"android/support/v14/preferences/(.*)\",\n" +
            "            to: \"android/jetpack/prefs/main/{0}\",\n" +
            "            fieldSelectors: [\"dialog_(.*)\"]\n" +
            "        }\n" +
            "    ],\n" +
            "    pomRules: [\n" +
            "        {\n" +
            "            from: {groupId: \"g\", artifactId: \"a\", version: \"1.0\"},\n" +
            "            to: {groupId: \"g\", artifactId: \"a\", version: \"2.0\"} \n" +
            "        }\n" +
            "    ],\n" +
            "    versions: {\n" +
            "        \"latestReleased\": {\n" +
            "            \"something\": \"1.0.0\"\n" +
            "        }\n" +
            "    }," +
            "    proGuardMap: {\n" +
            "       rules: {\n" +
            "           \"android/support/**\": [\"androidx/**\"]\n" +
            "       }\n" +
            "    }" +
            "}"

        val config = ConfigParser.parseFromString(confStr)
        val jsonConfig = config!!.toJson()

        Truth.assertThat(config).isNotNull()
        Truth.assertThat(config!!.restrictToPackagePrefixes.first()).isEqualTo("android/support/")
        Truth.assertThat(config!!.reversedRestrictToPackagePrefixes.first()).isEqualTo("androidx/")
        Truth.assertThat(config.rulesMap.rewriteRules.size).isEqualTo(2)
        Truth.assertThat(config.versionsMap.data.size).isEqualTo(1)
        Truth.assertThat(config.versionsMap.data["latestReleased"])
            .containsExactly("something", "1.0.0")
        Truth.assertThat(config.proGuardMap.toJson().rules.size).isEqualTo(1)

        Truth.assertThat(jsonConfig.versions!!.size).isEqualTo(1)
        Truth.assertThat(jsonConfig.versions!!["latestReleased"])
            .containsExactly("something", "1.0.0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseConfig_pomMissingGroup_shouldFail() {
        val confStr =
            "{\n" +
            "    restrictToPackagePrefixes: [\"android/support/\"],\n" +
            "    rules: [\n" +
            "    ],\n" +
            "    pomRules: [\n" +
            "        {\n" +
            "            from: {artifactId: \"a\", version: \"1.0\"},\n" +
            "            to: {artifactId: \"a\", groupId: \"g\", version: \"1.0\"}\n" +
            "        }\n" +
            "    ]\n" +
            "}"
        ConfigParser.parseFromString(confStr)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseConfig_pomMissingArtifact_shouldFail() {
        val confStr =
            "{\n" +
            "    restrictToPackagePrefixes: [\"android/support/\"],\n" +
            "    rules: [\n" +
            "    ],\n" +
            "    pomRules: [\n" +
            "        {\n" +
            "            from: {groupId: \"g\", version: \"1.0\"},\n" +
            "            to: {artifactId: \"a\", groupId: \"g\", version: \"1.0\"}\n" +
            "        }\n" +
            "    ]\n" +
            "}"
        ConfigParser.parseFromString(confStr)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseConfig_pomMissingVersion_shouldFail() {
        val confStr =
            "{\n" +
            "    restrictToPackagePrefixes: [\"android/support/\"],\n" +
            "    rules: [\n" +
            "    ],\n" +
            "    pomRules: [\n" +
            "        {\n" +
            "            from: {artifactId: \"a\", groupId: \"g\"},\n" +
            "            to: {artifactId: \"a\", groupId: \"g\"}\n" +
            "        }\n" +
            "    ]\n" +
            "}"
        ConfigParser.parseFromString(confStr)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseConfig_duplicity_shouldFail() {
        val confStr =
            "{\n" +
            "    restrictToPackagePrefixes: [\"android/support/\"],\n" +
            "    rules: [\n" +
            "    ],\n" +
            "    pomRules: [\n" +
            "        {\n" +
            "            from: {artifactId: \"a\", groupId: \"g\", version: \"1.0\"},\n" +
            "            to: {artifactId: \"b\", groupId: \"g\", version: \"1.0\"}\n" +
            "        },\n" +
            "        {\n" +
            "            from: {artifactId: \"a\", groupId: \"g\", version: \"2.0\"},\n" +
            "            to: {artifactId: \"c\", groupId: \"g\", version: \"1.0\"}\n" +
            "        }\n" +
            "    ]\n" +
            "}"
        ConfigParser.parseFromString(confStr)
    }
}