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

package android.support.tools.jetifier.core.config

import com.google.common.truth.Truth
import org.junit.Test

class ConfigParserTest {

    @Test fun parseConfig_validInput() {
        val confStr =
                "{\n" +
                "    restrictToPackagePrefixes: [\"android/support/\"],\n" +
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
                "            to: [\n" +
                "                {groupId: \"g\", artifactId: \"a\", version: \"2.0\"} \n" +
                "            ]\n" +
                "        },\n" +
                "    ]\n" +
                "}"

        val config = ConfigParser.parse(confStr)

        Truth.assertThat(config).isNotNull()
        Truth.assertThat(config!!.restrictToPackagePrefixes[0]).isEqualTo("android/support/")
        Truth.assertThat(config.rewriteRules.size).isEqualTo(2)
    }
}

